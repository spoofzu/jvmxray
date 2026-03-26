package org.jvmxray.agent.util;

import java.lang.instrument.Instrumentation;
import java.util.*;

/**
 * Utility class for opening Java module packages to the JVMXRay agent.
 * Uses Instrumentation.redefineModule() to grant the agent unrestricted
 * reflective access to monitored application code.
 *
 * <p>This addresses Java Module System (JPMS) restrictions that would otherwise
 * cause IllegalAccessException when sensors attempt to use reflection on
 * classes in other modules (e.g., Jakarta Servlet, application classes).</p>
 *
 * <p>The agent runs in the unnamed module, so we open packages from named
 * modules to ALL-UNNAMED, allowing full reflective access.</p>
 *
 * @author Milton Smith
 */
public class ModuleOpener {

    /**
     * Opens all packages from key modules to allow the agent unrestricted
     * reflective access. Should be called early in agent initialization,
     * before sensors are configured.
     *
     * @param instrumentation The Instrumentation instance from the JVM
     * @return A summary of opened packages for logging
     */
    public static String openModulesForAgent(Instrumentation instrumentation) {
        StringBuilder summary = new StringBuilder();
        int totalOpened = 0;

        try {
            // Get the unnamed module (where the agent resides)
            Module unnamedModule = ModuleOpener.class.getModule();
            Set<Module> unnamedModuleSet = Set.of(unnamedModule);

            // Process all loaded modules
            for (Module module : ModuleLayer.boot().modules()) {
                try {
                    int opened = openModulePackages(instrumentation, module, unnamedModuleSet);
                    if (opened > 0) {
                        totalOpened += opened;
                    }
                } catch (Exception e) {
                    // Log but continue - some modules may resist opening
                    summary.append(String.format("  Warning: Could not open %s: %s%n",
                            module.getName(), e.getMessage()));
                }
            }

            // Also try to open any dynamically loaded modules from the application
            // These might include Jakarta Servlet, Spring, etc.
            openDynamicModules(instrumentation, unnamedModuleSet);

            summary.insert(0, String.format("Module packages opened: %d total%n", totalOpened));

        } catch (Exception e) {
            summary.append(String.format("ModuleOpener error: %s%n", e.getMessage()));
        }

        return summary.toString();
    }

    /**
     * Opens all packages from a specific module to the target modules.
     *
     * @param instrumentation The Instrumentation instance
     * @param sourceModule The module to open packages from
     * @param targetModules The modules to open packages to (typically unnamed module)
     * @return Number of packages opened
     */
    private static int openModulePackages(Instrumentation instrumentation,
                                          Module sourceModule,
                                          Set<Module> targetModules) {
        if (sourceModule == null || !sourceModule.isNamed()) {
            return 0;
        }

        Set<String> packages = sourceModule.getPackages();
        if (packages.isEmpty()) {
            return 0;
        }

        // Build the map of packages to open
        Map<String, Set<Module>> extraOpens = new HashMap<>();
        for (String pkg : packages) {
            // Check if already open to unnamed module
            if (!sourceModule.isOpen(pkg, targetModules.iterator().next())) {
                extraOpens.put(pkg, targetModules);
            }
        }

        if (extraOpens.isEmpty()) {
            return 0;
        }

        // Use redefineModule to open the packages
        instrumentation.redefineModule(
                sourceModule,
                Set.of(),           // extraReads
                Map.of(),           // extraExports
                extraOpens,         // extraOpens - this is what we need
                Set.of(),           // extraUses
                Map.of()            // extraProvides
        );

        return extraOpens.size();
    }

    /**
     * Attempts to open dynamically loaded modules (e.g., from web containers).
     * Handles modules that might be loaded after JVM boot.
     *
     * @param instrumentation The Instrumentation instance
     * @param targetModules The modules to open packages to
     */
    private static void openDynamicModules(Instrumentation instrumentation,
                                           Set<Module> targetModules) {
        // Common module names that might be dynamically loaded
        String[] dynamicModulePatterns = {
                "jakarta.servlet",
                "javax.servlet",
                "org.apache.tomcat",
                "org.eclipse.jetty",
                "io.undertow"
        };

        // Get all loaded classes and find their modules
        Set<Module> processedModules = new HashSet<>();

        for (Class<?> loadedClass : instrumentation.getAllLoadedClasses()) {
            try {
                Module module = loadedClass.getModule();
                if (module != null && module.isNamed() && !processedModules.contains(module)) {
                    // Check if this module matches our patterns
                    String moduleName = module.getName();
                    for (String pattern : dynamicModulePatterns) {
                        if (moduleName != null && moduleName.startsWith(pattern)) {
                            openModulePackages(instrumentation, module, targetModules);
                            processedModules.add(module);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore individual class errors
            }
        }
    }

    /**
     * Opens a specific module's packages. Useful for on-demand opening
     * when a sensor encounters an inaccessible class at runtime.
     *
     * @param instrumentation The Instrumentation instance (must be stored from premain)
     * @param targetClass The class whose module packages should be opened
     * @return true if packages were successfully opened
     */
    public static boolean openModuleForClass(Instrumentation instrumentation, Class<?> targetClass) {
        try {
            Module targetModule = targetClass.getModule();
            if (targetModule == null || !targetModule.isNamed()) {
                return true; // Unnamed module, no opening needed
            }

            Module unnamedModule = ModuleOpener.class.getModule();
            Set<Module> targetModules = Set.of(unnamedModule);

            int opened = openModulePackages(instrumentation, targetModule, targetModules);
            return opened > 0 || isAlreadyOpen(targetModule, targetClass.getPackageName(), unnamedModule);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if a package is already open to the target module.
     */
    private static boolean isAlreadyOpen(Module sourceModule, String packageName, Module targetModule) {
        try {
            return sourceModule.isOpen(packageName, targetModule);
        } catch (Exception e) {
            return false;
        }
    }
}
