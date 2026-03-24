package org.jvmxray.platform.shared.util;

/**
 * MCCScope - Utility for sensor-managed MCC (Mapped Correlation Context) lifecycle.
 *
 * <p>Provides a simple API for sensors to manage correlation scope via reflection,
 * avoiding direct dependencies on prj-common classes (bootloader isolation).</p>
 *
 * <h2>Usage Pattern</h2>
 * <pre>{@code
 * // Sensor enter point
 * @Advice.OnMethodEnter
 * public static void enter() {
 *     MCCScope.enter("FileIO");  // Establishes correlation scope
 *     // ... sensor-specific logic ...
 * }
 *
 * // Sensor exit point
 * @Advice.OnMethodExit
 * public static void exit() {
 *     MCCScope.exit("FileIO");  // Cleans up correlation scope
 * }
 * }</pre>
 *
 * <h2>Scope Lifecycle</h2>
 * <ul>
 * <li><strong>First Entry:</strong> Generates trace_id for correlation root</li>
 * <li><strong>Nested Entry:</strong> Inherits trace_id from parent scope</li>
 * <li><strong>Nested Exit:</strong> Preserves context for parent scope</li>
 * <li><strong>Last Exit:</strong> Clears all correlation context (thread pool safety)</li>
 * </ul>
 *
 * @author JVMXRay Development Team
 */
public class MCCScope {

    private static final String MCC_CLASS_NAME = "org.jvmxray.platform.shared.util.MCC";

    // Cached reflection methods for performance
    private static volatile Class<?> mccClass;
    private static volatile java.lang.reflect.Method enterScopeMethod;
    private static volatile java.lang.reflect.Method exitScopeMethod;
    private static volatile boolean initialized = false;
    private static volatile boolean available = false;

    /**
     * Private constructor - utility class with only static methods.
     */
    private MCCScope() {
        throw new UnsupportedOperationException("MCCScope is a utility class");
    }

    /**
     * Initializes reflection access to MCC class (lazy, thread-safe).
     * Only performs initialization once, caches results for performance.
     */
    private static void initialize() {
        if (!initialized) {
            synchronized (MCCScope.class) {
                if (!initialized) {
                    try {
                        mccClass = Class.forName(MCC_CLASS_NAME);
                        enterScopeMethod = mccClass.getMethod("enterScope", String.class);
                        exitScopeMethod = mccClass.getMethod("exitScope", String.class);
                        available = true;
                    } catch (ClassNotFoundException e) {
                        // MCC not available - expected in minimal deployments
                        available = false;
                    } catch (NoSuchMethodException e) {
                        System.err.println("MCCScope: MCC class found but methods missing: " + e.getMessage());
                        available = false;
                    } catch (Exception e) {
                        System.err.println("MCCScope: Unexpected error during initialization: " + e.getMessage());
                        available = false;
                    } finally {
                        initialized = true;
                    }
                }
            }
        }
    }

    /**
     * Enters a correlation scope for a sensor.
     *
     * <p>If this is the first scope entry (no parent scope), generates a new trace_id.
     * If nested within existing scope, inherits the parent's trace_id.</p>
     *
     * <p>Safe to call even if MCC is not available - silently no-ops.</p>
     *
     * @param scopeId Unique identifier for this scope (e.g., "FileIO", "SQL", "HTTP")
     */
    public static void enter(String scopeId) {
        if (scopeId == null || scopeId.trim().isEmpty()) {
            return; // Defensive - don't fail sensor if invalid scope ID
        }

        initialize();

        if (!available) {
            return; // MCC not available - silently skip
        }

        try {
            enterScopeMethod.invoke(null, scopeId);
        } catch (Exception e) {
            // Don't fail sensor operations due to MCC errors
            System.err.println("MCCScope: Failed to enter scope '" + scopeId + "': " + e.getMessage());
        }
    }

    /**
     * Exits a correlation scope for a sensor.
     *
     * <p>If this is the last scope (no parent scopes remain), clears all correlation context
     * to prevent data leakage in thread pool environments.</p>
     *
     * <p>If nested scopes remain, preserves correlation context for parent scopes.</p>
     *
     * <p>Safe to call even if MCC is not available - silently no-ops.</p>
     *
     * @param scopeId Unique identifier for this scope (must match enter() call)
     */
    public static void exit(String scopeId) {
        if (scopeId == null || scopeId.trim().isEmpty()) {
            return; // Defensive - don't fail sensor if invalid scope ID
        }

        initialize();

        if (!available) {
            return; // MCC not available - silently skip
        }

        try {
            exitScopeMethod.invoke(null, scopeId);
        } catch (Exception e) {
            // Don't fail sensor operations due to MCC errors
            System.err.println("MCCScope: Failed to exit scope '" + scopeId + "': " + e.getMessage());
        }
    }

    /**
     * Checks if MCC is available in the current deployment.
     *
     * @return true if MCC class was successfully loaded, false otherwise
     */
    public static boolean isAvailable() {
        initialize();
        return available;
    }
}
