package org.jvmxray.agent.sensor.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for extracting system context information.
 * Provides JVM details, process information, container detection, and cloud metadata.
 *
 * @author Milton Smith
 */
public class SystemContextUtils {

    private static final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

    /**
     * Collects JVM and process context metadata.
     *
     * @return Map of system context fields
     */
    public static Map<String, String> getJVMContext() {
        Map<String, String> metadata = new HashMap<>();

        try {
            // JVM Information
            metadata.put("jvm_vendor", System.getProperty("java.vendor"));
            metadata.put("jvm_version", System.getProperty("java.version"));
            metadata.put("jvm_name", System.getProperty("java.vm.name"));
            metadata.put("jvm_spec_version", System.getProperty("java.specification.version"));

            // JVM start time
            long startTime = runtimeMXBean.getStartTime();
            metadata.put("jvm_start_time", String.valueOf(startTime));
            metadata.put("jvm_start_time_iso", new java.util.Date(startTime).toInstant().toString());
            metadata.put("jvm_uptime_ms", String.valueOf(runtimeMXBean.getUptime()));

            // Process information (Java 9+)
            try {
                ProcessHandle current = ProcessHandle.current();
                metadata.put("process_id", String.valueOf(current.pid()));

                current.parent().ifPresent(parent ->
                        metadata.put("parent_process_id", String.valueOf(parent.pid())));

                current.info().command().ifPresent(cmd ->
                        metadata.put("process_command", cmd));

                current.info().commandLine().ifPresent(cmdLine ->
                        metadata.put("command_line", cmdLine));

                current.info().user().ifPresent(user ->
                        metadata.put("process_user", user));

                current.info().startInstant().ifPresent(instant ->
                        metadata.put("process_start_time", instant.toString()));
            } catch (Exception e) {
                // ProcessHandle not available (Java 8)
                String name = runtimeMXBean.getName();
                if (name != null && name.contains("@")) {
                    metadata.put("process_id", name.split("@")[0]);
                }
            }

            // Working directory and user info
            metadata.put("working_directory", System.getProperty("user.dir"));
            metadata.put("user_name", System.getProperty("user.name"));
            metadata.put("user_home", System.getProperty("user.home"));
            metadata.put("os_name", System.getProperty("os.name"));
            metadata.put("os_version", System.getProperty("os.version"));
            metadata.put("os_arch", System.getProperty("os.arch"));

            // JVM arguments
            List<String> jvmArgs = runtimeMXBean.getInputArguments();
            if (!jvmArgs.isEmpty()) {
                metadata.put("jvm_args", String.join(" ", jvmArgs));
                metadata.put("jvm_args_count", String.valueOf(jvmArgs.size()));

                // Check for debug port
                for (String arg : jvmArgs) {
                    if (arg.contains("-agentlib:jdwp") || arg.contains("-Xdebug")) {
                        metadata.put("debug_enabled", "true");
                        break;
                    }
                }
            }

            // Security Manager
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                metadata.put("security_manager_present", "true");
                metadata.put("security_manager_class", sm.getClass().getName());
            } else {
                metadata.put("security_manager_present", "false");
            }

            // JMX remote check
            String jmxRemote = System.getProperty("com.sun.management.jmxremote");
            String jmxPort = System.getProperty("com.sun.management.jmxremote.port");
            if (jmxRemote != null || jmxPort != null) {
                metadata.put("jmx_remote_enabled", "true");
                if (jmxPort != null) {
                    metadata.put("jmx_remote_port", jmxPort);
                }
            }

        } catch (Exception e) {
            metadata.put("jvm_context_error", e.getClass().getSimpleName());
        }

        return metadata;
    }

    /**
     * Detects if running in a container environment.
     *
     * @return Map of container-related fields
     */
    public static Map<String, String> getContainerContext() {
        Map<String, String> metadata = new HashMap<>();

        try {
            boolean isContainer = false;

            // Check for Docker
            File dockerEnv = new File("/.dockerenv");
            if (dockerEnv.exists()) {
                isContainer = true;
                metadata.put("container_type", "docker");
            }

            // Check cgroup for container ID
            File cgroupFile = new File("/proc/1/cgroup");
            if (cgroupFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(cgroupFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("docker") || line.contains("kubepods") ||
                                line.contains("containerd") || line.contains("cri-o")) {
                            isContainer = true;

                            // Extract container ID
                            String[] parts = line.split("/");
                            if (parts.length > 0) {
                                String lastPart = parts[parts.length - 1];
                                if (lastPart.length() >= 12) {
                                    metadata.put("container_id", lastPart.length() > 64 ?
                                            lastPart.substring(0, 64) : lastPart);
                                }
                            }

                            if (line.contains("kubepods")) {
                                metadata.put("container_type", "kubernetes");
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                    // Cgroup file not readable
                }
            }

            metadata.put("is_container", String.valueOf(isContainer));

            // Kubernetes-specific environment variables
            String podName = System.getenv("HOSTNAME");
            String k8sNamespace = System.getenv("KUBERNETES_NAMESPACE");
            String k8sPodName = System.getenv("KUBERNETES_POD_NAME");
            String k8sServiceHost = System.getenv("KUBERNETES_SERVICE_HOST");

            if (k8sServiceHost != null) {
                metadata.put("kubernetes_detected", "true");
                if (k8sNamespace != null) {
                    metadata.put("kubernetes_namespace", k8sNamespace);
                }
                if (k8sPodName != null) {
                    metadata.put("kubernetes_pod_name", k8sPodName);
                } else if (podName != null) {
                    metadata.put("kubernetes_pod_name", podName);
                }
            }

        } catch (Exception e) {
            metadata.put("container_context_error", e.getClass().getSimpleName());
        }

        return metadata;
    }

    /**
     * Attempts to detect cloud provider from instance metadata.
     *
     * @return Map of cloud-related fields
     */
    public static Map<String, String> getCloudContext() {
        Map<String, String> metadata = new HashMap<>();

        try {
            // Check for AWS
            String awsRegion = System.getenv("AWS_REGION");
            String awsDefaultRegion = System.getenv("AWS_DEFAULT_REGION");
            if (awsRegion != null || awsDefaultRegion != null) {
                metadata.put("cloud_provider", "aws");
                metadata.put("aws_region", awsRegion != null ? awsRegion : awsDefaultRegion);
            }

            // Check for GCP
            String gcpProject = System.getenv("GOOGLE_CLOUD_PROJECT");
            String gaeService = System.getenv("GAE_SERVICE");
            if (gcpProject != null || gaeService != null) {
                metadata.put("cloud_provider", "gcp");
                if (gcpProject != null) {
                    metadata.put("gcp_project", gcpProject);
                }
            }

            // Check for Azure
            String azureSubscription = System.getenv("AZURE_SUBSCRIPTION_ID");
            String azureRegion = System.getenv("REGION_NAME");
            if (azureSubscription != null) {
                metadata.put("cloud_provider", "azure");
                metadata.put("azure_subscription", azureSubscription);
            }

            // Environment type indicators
            String env = System.getenv("ENVIRONMENT");
            if (env == null) env = System.getenv("ENV");
            if (env == null) env = System.getenv("NODE_ENV");
            if (env != null) {
                metadata.put("environment", env);
            }

        } catch (Exception e) {
            metadata.put("cloud_context_error", e.getClass().getSimpleName());
        }

        return metadata;
    }

    /**
     * Collects all system context metadata.
     *
     * @return Combined map of all context
     */
    public static Map<String, String> getAllContext() {
        Map<String, String> metadata = new HashMap<>();
        metadata.putAll(getJVMContext());
        metadata.putAll(getContainerContext());
        metadata.putAll(getCloudContext());
        return metadata;
    }

    /**
     * Checks for sensitive environment variables and redacts them.
     *
     * @param name The environment variable name
     * @param value The environment variable value
     * @return Redacted value if sensitive, original otherwise
     */
    public static String redactSensitiveValue(String name, String value) {
        if (name == null || value == null) return value;

        String upperName = name.toUpperCase();
        if (upperName.contains("PASSWORD") ||
                upperName.contains("SECRET") ||
                upperName.contains("TOKEN") ||
                upperName.contains("API_KEY") ||
                upperName.contains("APIKEY") ||
                upperName.contains("PRIVATE_KEY") ||
                upperName.contains("CREDENTIAL")) {
            return "***REDACTED***";
        }
        return value;
    }
}
