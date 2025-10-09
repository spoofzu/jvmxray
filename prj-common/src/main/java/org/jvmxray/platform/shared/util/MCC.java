package org.jvmxray.platform.shared.util;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MCC - Mapped Correlation Context
 *
 * <p>Thread-scoped key-value storage for event correlation metadata across execution paths.
 * Enables correlation of security events, diagnostic data, and forensic analysis across
 * single-threaded and multi-threaded execution flows.</p>
 *
 * <p>MCC provides a thread-local context similar to SLF4J's MDC (Mapped Diagnostic Context)
 * but designed specifically for JVMXRay's agent architecture where standard logging frameworks
 * cannot be used due to bootloader constraints.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 * <li><strong>Thread-Local Storage:</strong> Each thread maintains its own independent correlation context</li>
 * <li><strong>Scope-Based Lifecycle:</strong> Sensors use enterScope/exitScope for nested correlation management</li>
 * <li><strong>Automatic Propagation:</strong> Context can be captured and restored across thread boundaries</li>
 * <li><strong>Security-Focused:</strong> Track trace_id, user_id, session_id, and custom metadata</li>
 * <li><strong>Zero Schema Impact:</strong> All MCC data stored in existing KEYPAIRS column</li>
 * <li><strong>Forensic Analysis:</strong> Reconstruct complete execution chains for security investigation</li>
 * </ul>
 *
 * <h2>Standard Correlation Fields</h2>
 * <p>While MCC accepts any key-value pairs, these standard fields are recommended:</p>
 * <ul>
 * <li><code>trace_id</code> - Unique identifier for entire request/transaction chain</li>
 * <li><code>parent_thread_id</code> - ID of thread that spawned current thread</li>
 * <li><code>user_id</code> - Authenticated user identifier</li>
 * <li><code>session_id</code> - HTTP session or equivalent session identifier</li>
 * <li><code>client_ip</code> - Source IP address of request</li>
 * <li><code>request_uri</code> - HTTP request URI (for web requests)</li>
 * </ul>
 *
 * <h2>Usage Example - HTTP Request Correlation</h2>
 * <pre>{@code
 * // HTTP sensor entry point
 * MCC.clear();  // Clean slate for new request
 * MCC.put("trace_id", GUID.generate());
 * MCC.put("user_id", extractUser(request));
 * MCC.put("session_id", extractSessionId(request));
 * MCC.put("client_ip", request.getRemoteAddr());
 *
 * // All downstream sensors (SQL, File, Network) automatically include this context
 * // via LogProxy integration
 *
 * // HTTP sensor exit
 * MCC.clear();  // Clean up for thread pool reuse
 * }</pre>
 *
 * <h2>Usage Example - Thread Propagation</h2>
 * <pre>{@code
 * // Parent thread sets context
 * MCC.put("trace_id", "ABC123");
 * MCC.put("user_id", "admin");
 *
 * // Spawn child thread with context propagation
 * Map<String, String> parentContext = MCC.captureContext();
 * executor.submit(() -> {
 *     MCC.restoreContext(parentContext);  // Inherit parent's correlation context
 *     MCC.put("parent_thread_id", String.valueOf(parentThreadId));
 *
 *     try {
 *         // Child thread work - all events include parent's trace_id and user_id
 *         performDatabaseQuery();
 *     } finally {
 *         MCC.clear();  // Clean up thread-local storage
 *     }
 * });
 * }</pre>
 *
 * <h2>Integration with LogProxy</h2>
 * <p>When sensors call {@code LogProxy.logMessage()}, the MCC context is automatically merged
 * into the event metadata. This allows correlation data to propagate transparently without
 * sensors explicitly passing context through call chains.</p>
 *
 * <h2>Security and Forensics Benefits</h2>
 * <ul>
 * <li><strong>Attack Chain Reconstruction:</strong> Group all events by trace_id to see complete attack</li>
 * <li><strong>Cross-Thread Attribution:</strong> Link spawned thread activities back to initiating request</li>
 * <li><strong>User Activity Tracking:</strong> Correlate all actions by specific user_id or session_id</li>
 * <li><strong>DDOS Detection:</strong> Identify resource consumption patterns by trace_id</li>
 * <li><strong>Data Exfiltration:</strong> Trace file access → network egress by correlation context</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>MCC is thread-safe. Each thread has its own isolated context. Parent contexts must be
 * explicitly captured and restored when spawning threads.</p>
 *
 * @author Milton Smith
 */
public class MCC {

    /**
     * Thread-local storage for correlation context.
     * Each thread maintains its own independent Map of correlation data.
     */
    private static final ThreadLocal<Map<String, String>> context =
        ThreadLocal.withInitial(HashMap::new);

    /**
     * Thread-local stack tracking nested scope ownership.
     * Used to manage lifecycle of correlation context across nested sensor entry/exit points.
     * When stack is empty, context is cleared. First entry initializes trace_id.
     */
    private static final ThreadLocal<Deque<String>> ownerStack =
        ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * Thread-local storage for last access time (milliseconds).
     * Used for TTL-based defensive cleanup of leaked scopes.
     */
    private static final ThreadLocal<Long> lastAccessTime =
        ThreadLocal.withInitial(System::currentTimeMillis);

    /**
     * Thread-local storage for last TTL check time (milliseconds).
     * Used to throttle TTL checks to once per 100ms per thread for performance.
     */
    private static final ThreadLocal<Long> lastTTLCheck =
        ThreadLocal.withInitial(() -> 0L);

    /**
     * TTL for defensive cleanup of leaked scopes (milliseconds).
     * Configurable via system property org.jvmxray.agent.mcc.ttl.seconds (default: 300 seconds = 5 minutes).
     * If a thread's MCC context hasn't been accessed for longer than this TTL, it will be cleaned up
     * defensively to prevent memory leaks from sensors that fail to call exitScope().
     */
    private static volatile long ttlMillis = 300_000; // 5 minutes default

    /**
     * Throttle interval for TTL checks (milliseconds).
     * TTL cleanup checks are performed at most once per this interval to reduce overhead.
     */
    private static final long TTL_CHECK_THROTTLE_MS = 100;

    // Metrics tracking
    private static final AtomicLong totalContextsCreated = new AtomicLong(0);
    private static final AtomicLong totalTTLCleanups = new AtomicLong(0);
    private static final AtomicInteger maxContextSizeEverSeen = new AtomicInteger(0);
    private static final AtomicInteger currentActiveContexts = new AtomicInteger(0);

    // Static initializer to load TTL configuration
    static {
        try {
            String ttlProp = System.getProperty("org.jvmxray.agent.mcc.ttl.seconds", "300");
            long ttlSeconds = Long.parseLong(ttlProp);
            ttlMillis = ttlSeconds * 1000;
        } catch (Exception e) {
            // Fallback to default if property is invalid
            ttlMillis = 300_000;
        }
    }

    /**
     * Private constructor to prevent instantiation.
     * MCC is a utility class with only static methods.
     */
    private MCC() {
        throw new UnsupportedOperationException("MCC is a utility class and cannot be instantiated");
    }

    /**
     * Associates a value with a key in the current thread's correlation context.
     * If the key already exists, its value is replaced.
     *
     * <p>Both key and value must be non-null. Use {@link #remove(String)} to delete entries.</p>
     *
     * @param key the correlation key (e.g., "trace_id", "user_id")
     * @param value the correlation value
     * @throws IllegalArgumentException if key or value is null
     */
    public static void put(String key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("MCC key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("MCC value cannot be null");
        }
        Map<String, String> ctx = context.get();
        ctx.put(key, value);

        // Track max context size for monitoring
        int currentSize = ctx.size();
        int currentMax = maxContextSizeEverSeen.get();
        if (currentSize > currentMax) {
            maxContextSizeEverSeen.compareAndSet(currentMax, currentSize);
        }
    }

    /**
     * Retrieves the value associated with the specified key in the current thread's context.
     *
     * @param key the correlation key
     * @return the correlation value, or {@code null} if the key is not present
     */
    public static String get(String key) {
        return context.get().get(key);
    }

    /**
     * Removes the value associated with the specified key from the current thread's context.
     *
     * @param key the correlation key to remove
     * @return the previous value associated with the key, or {@code null} if not present
     */
    public static String remove(String key) {
        return context.get().remove(key);
    }

    /**
     * Returns an immutable copy of the current thread's correlation context.
     *
     * <p>This method is used internally by LogProxy to merge MCC data into event metadata.
     * The returned map is a defensive copy to prevent external modification.</p>
     *
     * @return immutable copy of current thread's correlation context, never null
     */
    public static Map<String, String> getCopyOfContext() {
        Map<String, String> ctx = context.get();
        if (ctx.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new HashMap<>(ctx));
    }

    /**
     * Clears all correlation data from the current thread's context.
     *
     * <p><strong>Important:</strong> Call this method when a thread completes its work
     * to prevent correlation data leakage in thread pool environments where threads
     * are reused across multiple requests.</p>
     *
     * <p>Typical usage pattern:</p>
     * <pre>{@code
     * try {
     *     MCC.put("trace_id", generateTraceId());
     *     // ... process request ...
     * } finally {
     *     MCC.clear();  // Always clean up
     * }
     * }</pre>
     */
    public static void clear() {
        context.get().clear();
    }

    /**
     * Completely removes the ThreadLocal context for the current thread.
     *
     * <p>This is a more aggressive cleanup than {@link #clear()} as it removes the
     * ThreadLocal entry entirely rather than just clearing the Map. Use this when
     * a thread is terminating or in long-running threads to prevent memory leaks.</p>
     *
     * <p>For most use cases, {@link #clear()} is sufficient and preferred.</p>
     */
    public static void remove() {
        context.remove();
    }

    /**
     * Captures the current thread's correlation context for propagation to spawned threads.
     *
     * <p>This method creates a defensive copy of the current context that can be safely
     * passed to child threads and restored via {@link #restoreContext(Map)}.</p>
     *
     * <p>Usage pattern for thread spawning:</p>
     * <pre>{@code
     * // Parent thread captures its context
     * Map<String, String> parentContext = MCC.captureContext();
     *
     * executor.submit(() -> {
     *     // Child thread restores parent's context
     *     MCC.restoreContext(parentContext);
     *     MCC.put("parent_thread_id", String.valueOf(parentThreadId));
     *
     *     try {
     *         // Child thread work inherits parent's correlation data
     *     } finally {
     *         MCC.clear();
     *     }
     * });
     * }</pre>
     *
     * @return defensive copy of current correlation context, never null
     */
    public static Map<String, String> captureContext() {
        Map<String, String> ctx = context.get();
        if (ctx.isEmpty()) {
            return new HashMap<>();
        }
        return new HashMap<>(ctx);
    }

    /**
     * Restores a previously captured correlation context into the current thread.
     *
     * <p><strong>Warning:</strong> This method replaces the current thread's entire
     * correlation context. Any existing context is lost. If you want to merge contexts,
     * manually iterate and put each entry.</p>
     *
     * @param capturedContext the context to restore (typically from {@link #captureContext()})
     * @throws IllegalArgumentException if capturedContext is null
     */
    public static void restoreContext(Map<String, String> capturedContext) {
        if (capturedContext == null) {
            throw new IllegalArgumentException("Cannot restore null context");
        }
        Map<String, String> ctx = context.get();
        ctx.clear();
        ctx.putAll(capturedContext);
    }

    /**
     * Checks if the current thread's correlation context is empty.
     *
     * @return true if context contains no correlation data, false otherwise
     */
    public static boolean isEmpty() {
        return context.get().isEmpty();
    }

    /**
     * Returns the number of correlation entries in the current thread's context.
     *
     * @return number of key-value pairs in current context
     */
    public static int size() {
        return context.get().size();
    }

    /**
     * Checks if the current thread's correlation context has been initialized.
     *
     * <p>A context is considered initialized if it contains a {@code trace_id} field.
     * This is used to determine whether auto-initialization is needed or whether
     * the context was explicitly set by an entry point (HTTP request, test, batch job, etc.).</p>
     *
     * <p>Use this method to:</p>
     * <ul>
     * <li>Check if auto-initialization should be triggered</li>
     * <li>Verify context exists before enriching with additional fields</li>
     * <li>Determine if context should be cleared on exit (don't clear auto-initialized contexts)</li>
     * </ul>
     *
     * @return true if context contains a trace_id, false otherwise
     */
    public static boolean isInitialized() {
        return context.get().containsKey("trace_id");
    }

    /**
     * Checks TTL and performs defensive cleanup if needed.
     *
     * <p>This method is called at the start of scope lifecycle methods (enterScope/exitScope)
     * to defensively cleanup leaked scopes. It is throttled to check at most once per 100ms
     * per thread to minimize performance overhead.</p>
     *
     * <p>If the TTL has elapsed since the last MCC access on this thread, and the scope stack
     * or context is not empty, a WARNING is logged with diagnostic information to help identify
     * the sensor bug causing the leak. The ThreadLocals are then forcibly cleaned up.</p>
     *
     * <p><strong>Note:</strong> This is a defensive mechanism for sensor bugs. In healthy systems,
     * sensors should always call exitScope() in finally blocks, and this cleanup should never
     * trigger (mcc_ttl_cleanups metric should remain 0).</p>
     */
    private static void checkTTLIfNeeded() {
        // Throttle: only check once per 100ms per thread for performance
        long now = System.currentTimeMillis();
        Long lastCheck = lastTTLCheck.get();

        if (lastCheck == null || (now - lastCheck) > TTL_CHECK_THROTTLE_MS) {
            checkAndCleanupTTL();
            lastTTLCheck.set(now);
        }
    }

    /**
     * Performs TTL check and cleanup for the current thread.
     *
     * <p>If the TTL has elapsed, logs diagnostic information about the leaked scope
     * and forcibly cleans up all ThreadLocals. This helps prevent memory leaks in
     * thread pool environments where threads are reused.</p>
     */
    private static void checkAndCleanupTTL() {
        long now = System.currentTimeMillis();
        Long lastAccess = lastAccessTime.get();

        if (lastAccess != null && (now - lastAccess) > ttlMillis) {
            // TTL expired - perform defensive cleanup
            Deque<String> stack = ownerStack.get();
            Map<String, String> ctx = context.get();

            // Log WARNING only if there's actually leaked data
            if (!stack.isEmpty() || !ctx.isEmpty()) {
                System.err.println(String.format(
                    "[MCC-TTL-CLEANUP] Defensive cleanup triggered | Thread: %s | Elapsed: %dms | " +
                    "Stack: %s | Context size: %d | Context keys: %s",
                    Thread.currentThread().getName(),
                    (now - lastAccess),
                    stack.isEmpty() ? "[]" : stack.toString(),
                    ctx.size(),
                    ctx.isEmpty() ? "[]" : ctx.keySet().toString()
                ));

                // Update metrics
                totalTTLCleanups.incrementAndGet();
                if (!stack.isEmpty()) {
                    currentActiveContexts.decrementAndGet();
                }
            }

            // Force cleanup of all ThreadLocals
            context.remove();
            ownerStack.remove();
            lastAccessTime.remove();
        }

        // Update last access time
        lastAccessTime.set(now);
    }

    /**
     * Updates statistics in the StatsRegistry (if available).
     *
     * <p>This method uses reflection to avoid creating a hard dependency on the agent module.
     * If StatsRegistry is not available (e.g., in minimal deployments), updates are silently skipped.</p>
     */
    private static void updateStats() {
        try {
            Class<?> statsRegistryClass = Class.forName("org.jvmxray.agent.util.StatsRegistry");
            java.lang.reflect.Method registerMethod = statsRegistryClass.getMethod("register", String.class, String.class);

            registerMethod.invoke(null, "mcc_contexts_created", String.valueOf(totalContextsCreated.get()));
            registerMethod.invoke(null, "mcc_ttl_cleanups", String.valueOf(totalTTLCleanups.get()));
            registerMethod.invoke(null, "mcc_max_context_size", String.valueOf(maxContextSizeEverSeen.get()));
            registerMethod.invoke(null, "mcc_active_contexts", String.valueOf(currentActiveContexts.get()));
            registerMethod.invoke(null, "mcc_ttl_seconds", String.valueOf(ttlMillis / 1000));
        } catch (ClassNotFoundException e) {
            // StatsRegistry not available - this is okay for minimal deployments
        } catch (Exception e) {
            // Silently ignore errors in stats reporting - shouldn't affect core functionality
        }
    }

    /**
     * Enters a new correlation scope for a sensor or component.
     *
     * <p>This method implements the entry point for sensor-managed MCC lifecycle:</p>
     * <ul>
     * <li>Performs defensive TTL-based cleanup check (throttled to once per 100ms)</li>
     * <li>If this is the FIRST scope (stack empty), initializes correlation context with new trace_id</li>
     * <li>If scopes already exist, inherits existing trace_id (nested sensor)</li>
     * <li>Pushes scope ID onto owner stack for proper cleanup on exit</li>
     * <li>Updates metrics for monitoring</li>
     * </ul>
     *
     * <p><strong>Usage Pattern:</strong></p>
     * <pre>{@code
     * // Sensor entry point
     * @Advice.OnMethodEnter
     * public static void enter() {
     *     MCC.enterScope("HTTPSensor");  // First entry → generates trace_id
     *     MCC.put("user_id", extractUser());
     *     // ... downstream SQL sensor also enters scope, inherits trace_id ...
     * }
     * }</pre>
     *
     * <p><strong>Design Principle:</strong> ANY sensor can be first entry point.
     * HTTP, SQL, File I/O, or any other sensor - whichever fires first owns the correlation root.</p>
     *
     * @param scopeId Unique identifier for this scope (typically sensor name like "HTTP", "SQL", "FileIO")
     * @throws IllegalArgumentException if scopeId is null or empty
     */
    public static void enterScope(String scopeId) {
        if (scopeId == null || scopeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Scope ID cannot be null or empty");
        }

        // Defensive TTL check (throttled)
        checkTTLIfNeeded();

        Deque<String> stack = ownerStack.get();

        if (stack.isEmpty()) {
            // First entry point - initialize correlation context
            context.get().clear();
            put("trace_id", GUID.generate());

            // Update metrics
            totalContextsCreated.incrementAndGet();
            currentActiveContexts.incrementAndGet();
        }
        // If stack not empty, context already initialized - inherit trace_id

        stack.push(scopeId);

        // Update stats registry
        updateStats();
    }

    /**
     * Exits the current correlation scope for a sensor or component.
     *
     * <p>This method implements the exit point for sensor-managed MCC lifecycle:</p>
     * <ul>
     * <li>Performs defensive TTL-based cleanup check (throttled to once per 100ms)</li>
     * <li>Verifies scope ID matches top of stack (safety check for proper pairing)</li>
     * <li>Pops scope ID from owner stack</li>
     * <li>If this was the LAST scope (stack now empty), clears ALL correlation context and ThreadLocals</li>
     * <li>If scopes remain (nested sensors), preserves correlation context for parent scopes</li>
     * <li>Updates metrics for monitoring</li>
     * </ul>
     *
     * <p><strong>Usage Pattern:</strong></p>
     * <pre>{@code
     * // Sensor exit point
     * @Advice.OnMethodExit
     * public static void exit() {
     *     MCC.exitScope("HTTPSensor");  // Last exit → clears context
     * }
     * }</pre>
     *
     * <p><strong>Thread Pool Safety:</strong> When last scope exits, context is completely cleared
     * including ThreadLocal cleanup to prevent correlation data leakage when threads are reused
     * across logical transactions.</p>
     *
     * @param scopeId Unique identifier for this scope (must match enterScope call)
     * @throws IllegalArgumentException if scopeId is null or empty
     */
    public static void exitScope(String scopeId) {
        if (scopeId == null || scopeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Scope ID cannot be null or empty");
        }

        // Defensive TTL check (throttled)
        checkTTLIfNeeded();

        Deque<String> stack = ownerStack.get();

        if (!stack.isEmpty() && scopeId.equals(stack.peek())) {
            stack.pop();

            if (stack.isEmpty()) {
                // Last exit - clear all correlation context and ThreadLocals
                context.get().clear();
                context.remove();         // Clean up ThreadLocal
                ownerStack.remove();      // Clean up ThreadLocal
                lastAccessTime.remove();  // Clean up ThreadLocal

                // Update metrics
                currentActiveContexts.decrementAndGet();
            }
            // If stack not empty, keep context for parent scopes
        }
        // If scope mismatch or stack empty, do nothing (defensive - shouldn't happen with proper instrumentation)

        // Update stats registry
        updateStats();
    }

    /**
     * Checks if the current thread is within any correlation scope.
     *
     * <p>Returns true if at least one scope is active (sensor entry has been called).
     * This indicates that correlation context is actively managed and should not
     * be externally cleared.</p>
     *
     * @return true if within one or more scopes, false if no scopes active
     */
    public static boolean isInScope() {
        return !ownerStack.get().isEmpty();
    }

    /**
     * Wraps a Runnable to automatically propagate the current thread's correlation context
     * to the thread that executes the Runnable.
     *
     * <p>This is a convenience method for thread pool executors. The wrapped Runnable will:</p>
     * <ul>
     * <li>Capture the calling thread's MCC context</li>
     * <li>Restore that context when executed</li>
     * <li>Clean up the context after execution (in finally block)</li>
     * </ul>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * MCC.put("trace_id", "ABC123");
     * executor.submit(MCC.wrap(() -> {
     *     // This code executes with trace_id=ABC123 in MCC
     *     performWork();
     * }));
     * }</pre>
     *
     * @param task the Runnable to wrap
     * @return wrapped Runnable that propagates correlation context
     * @throws IllegalArgumentException if task is null
     */
    public static Runnable wrap(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("Cannot wrap null Runnable");
        }
        Map<String, String> capturedContext = captureContext();
        return new CorrelationAwareRunnable(task, capturedContext);
    }

    /**
     * Wraps a Callable to automatically propagate the current thread's correlation context
     * to the thread that executes the Callable.
     *
     * <p>Similar to {@link #wrap(Runnable)} but for Callable tasks that return values.</p>
     *
     * @param <V> the return type of the Callable
     * @param task the Callable to wrap
     * @return wrapped Callable that propagates correlation context
     * @throws IllegalArgumentException if task is null
     */
    public static <V> Callable<V> wrap(Callable<V> task) {
        if (task == null) {
            throw new IllegalArgumentException("Cannot wrap null Callable");
        }
        Map<String, String> capturedContext = captureContext();
        return new CorrelationAwareCallable<>(task, capturedContext);
    }

    /**
     * Internal wrapper for Runnables that propagates correlation context.
     */
    private static class CorrelationAwareRunnable implements Runnable {
        private final Runnable delegate;
        private final Map<String, String> capturedContext;

        CorrelationAwareRunnable(Runnable delegate, Map<String, String> capturedContext) {
            this.delegate = delegate;
            this.capturedContext = capturedContext;
        }

        @Override
        public void run() {
            Map<String, String> originalContext = captureContext();
            try {
                restoreContext(capturedContext);
                delegate.run();
            } finally {
                restoreContext(originalContext);
            }
        }
    }

    /**
     * Internal wrapper for Callables that propagates correlation context.
     */
    private static class CorrelationAwareCallable<V> implements Callable<V> {
        private final Callable<V> delegate;
        private final Map<String, String> capturedContext;

        CorrelationAwareCallable(Callable<V> delegate, Map<String, String> capturedContext) {
            this.delegate = delegate;
            this.capturedContext = capturedContext;
        }

        @Override
        public V call() throws Exception {
            Map<String, String> originalContext = captureContext();
            try {
                restoreContext(capturedContext);
                return delegate.call();
            } finally {
                restoreContext(originalContext);
            }
        }
    }
}
