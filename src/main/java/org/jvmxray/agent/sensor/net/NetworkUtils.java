package org.jvmxray.agent.sensor.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for network operations and metadata extraction.
 * Provides IP classification, TLS/SSL metadata extraction, and network analysis.
 *
 * @author Milton Smith
 */
public class NetworkUtils {

    // Private IP ranges (RFC 1918)
    private static final int PRIVATE_A_START = 0x0A000000; // 10.0.0.0
    private static final int PRIVATE_A_END = 0x0AFFFFFF;   // 10.255.255.255
    private static final int PRIVATE_B_START = 0xAC100000; // 172.16.0.0
    private static final int PRIVATE_B_END = 0xAC1FFFFF;   // 172.31.255.255
    private static final int PRIVATE_C_START = 0xC0A80000; // 192.168.0.0
    private static final int PRIVATE_C_END = 0xC0A8FFFF;   // 192.168.255.255

    /**
     * Extracts metadata from a socket connection.
     *
     * @param socket The socket to analyze
     * @param endpoint The endpoint address
     * @param operationType The type of operation (CONNECT, BIND, etc.)
     * @return Map of network metadata
     */
    public static Map<String, String> extractSocketMetadata(Socket socket, SocketAddress endpoint, String operationType) {
        Map<String, String> metadata = new HashMap<>();

        metadata.put("operation_type", operationType);

        try {
            // Protocol detection
            String protocol = "TCP";
            if (socket.getClass().getName().contains("SSL") ||
                socket.getClass().getName().contains("Ssl")) {
                protocol = "SSL/TLS";
            }
            metadata.put("protocol", protocol);

            // Local address info
            if (socket.isBound()) {
                InetAddress localAddr = socket.getLocalAddress();
                metadata.put("local_address", localAddr.getHostAddress());
                metadata.put("local_port", String.valueOf(socket.getLocalPort()));
            }

            // Remote address info
            if (endpoint instanceof InetSocketAddress) {
                InetSocketAddress inetEndpoint = (InetSocketAddress) endpoint;
                InetAddress remoteAddr = inetEndpoint.getAddress();

                metadata.put("remote_address", remoteAddr.getHostAddress());
                metadata.put("remote_port", String.valueOf(inetEndpoint.getPort()));
                metadata.put("remote_hostname", inetEndpoint.getHostName());

                // IP classification
                metadata.put("is_loopback", String.valueOf(remoteAddr.isLoopbackAddress()));
                metadata.put("is_private_ip", String.valueOf(isPrivateIP(remoteAddr)));
                metadata.put("is_ipv6", String.valueOf(remoteAddr instanceof java.net.Inet6Address));
                metadata.put("is_multicast", String.valueOf(remoteAddr.isMulticastAddress()));

                // Build destination string for backward compatibility
                metadata.put("dst", remoteAddr.getHostAddress() + ":" + inetEndpoint.getPort());
            }

            // Socket configuration
            try {
                int timeout = socket.getSoTimeout();
                if (timeout > 0) {
                    metadata.put("socket_timeout_ms", String.valueOf(timeout));
                }
            } catch (Exception e) {
                // Timeout not available
            }

            // Connection direction
            metadata.put("connection_direction", "OUTBOUND");

        } catch (Exception e) {
            metadata.put("metadata_error", e.getClass().getSimpleName());
        }

        return metadata;
    }

    /**
     * Extracts SSL/TLS session metadata from an SSLSocket.
     *
     * @param socket The socket (may or may not be SSL)
     * @return Map of TLS metadata, empty if not SSL
     */
    public static Map<String, String> extractTLSMetadata(Object socket) {
        Map<String, String> metadata = new HashMap<>();

        try {
            // Check if this is an SSL socket using reflection
            Class<?> sslSocketClass = Class.forName("javax.net.ssl.SSLSocket");
            if (!sslSocketClass.isInstance(socket)) {
                return metadata; // Not an SSL socket
            }

            // Get SSL session
            java.lang.reflect.Method getSessionMethod = sslSocketClass.getMethod("getSession");
            Object session = getSessionMethod.invoke(socket);

            if (session != null) {
                Class<?> sslSessionClass = Class.forName("javax.net.ssl.SSLSession");

                // Get protocol (TLSv1.2, TLSv1.3, etc.)
                try {
                    java.lang.reflect.Method getProtocolMethod = sslSessionClass.getMethod("getProtocol");
                    String protocol = (String) getProtocolMethod.invoke(session);
                    metadata.put("ssl_protocol", protocol);

                    // Check for deprecated protocols
                    if (protocol != null) {
                        if (protocol.contains("SSLv") || protocol.equals("TLSv1") || protocol.equals("TLSv1.1")) {
                            metadata.put("ssl_protocol_deprecated", "true");
                        }
                    }
                } catch (Exception e) {
                    // Protocol not available
                }

                // Get cipher suite
                try {
                    java.lang.reflect.Method getCipherSuiteMethod = sslSessionClass.getMethod("getCipherSuite");
                    String cipherSuite = (String) getCipherSuiteMethod.invoke(session);
                    metadata.put("ssl_cipher_suite", cipherSuite);

                    // Check for weak ciphers
                    if (cipherSuite != null) {
                        if (cipherSuite.contains("NULL") || cipherSuite.contains("EXPORT") ||
                            cipherSuite.contains("DES") || cipherSuite.contains("RC4") ||
                            cipherSuite.contains("MD5")) {
                            metadata.put("ssl_cipher_weak", "true");
                        }
                    }
                } catch (Exception e) {
                    // Cipher suite not available
                }

                // Get peer certificates
                try {
                    java.lang.reflect.Method getPeerCertsMethod = sslSessionClass.getMethod("getPeerCertificates");
                    Object[] certs = (Object[]) getPeerCertsMethod.invoke(session);
                    if (certs != null && certs.length > 0) {
                        // Get first (peer) certificate
                        Class<?> x509Class = Class.forName("java.security.cert.X509Certificate");
                        if (x509Class.isInstance(certs[0])) {
                            Object cert = certs[0];

                            // Get subject DN
                            java.lang.reflect.Method getSubjectDNMethod = x509Class.getMethod("getSubjectDN");
                            Object subjectDN = getSubjectDNMethod.invoke(cert);
                            if (subjectDN != null) {
                                metadata.put("ssl_peer_certificate_subject", subjectDN.toString());
                            }

                            // Get issuer DN
                            java.lang.reflect.Method getIssuerDNMethod = x509Class.getMethod("getIssuerDN");
                            Object issuerDN = getIssuerDNMethod.invoke(cert);
                            if (issuerDN != null) {
                                metadata.put("ssl_peer_certificate_issuer", issuerDN.toString());
                            }

                            // Get expiration date
                            java.lang.reflect.Method getNotAfterMethod = x509Class.getMethod("getNotAfter");
                            Object notAfter = getNotAfterMethod.invoke(cert);
                            if (notAfter != null) {
                                metadata.put("ssl_certificate_expiry", notAfter.toString());

                                // Check if certificate is expired
                                if (notAfter instanceof java.util.Date) {
                                    java.util.Date expiryDate = (java.util.Date) notAfter;
                                    if (expiryDate.before(new java.util.Date())) {
                                        metadata.put("ssl_certificate_expired", "true");
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Certificates not available (may not have completed handshake)
                }

                metadata.put("ssl_handshake_success", "true");
            }
        } catch (ClassNotFoundException e) {
            // SSL classes not available
        } catch (Exception e) {
            metadata.put("ssl_metadata_error", e.getClass().getSimpleName());
        }

        return metadata;
    }

    /**
     * Checks if an IP address is in private (RFC 1918) range.
     *
     * @param addr The IP address to check
     * @return true if private
     */
    public static boolean isPrivateIP(InetAddress addr) {
        if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress()) {
            return true;
        }

        byte[] bytes = addr.getAddress();
        if (bytes.length == 4) {
            int ip = ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) |
                    ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);

            return (ip >= PRIVATE_A_START && ip <= PRIVATE_A_END) ||
                   (ip >= PRIVATE_B_START && ip <= PRIVATE_B_END) ||
                   (ip >= PRIVATE_C_START && ip <= PRIVATE_C_END);
        }
        return false;
    }

    /**
     * Extracts metadata for incoming connections (accept operations).
     *
     * @param clientSocket The accepted client socket
     * @return Map of connection metadata
     */
    public static Map<String, String> extractInboundConnectionMetadata(Socket clientSocket) {
        Map<String, String> metadata = new HashMap<>();

        metadata.put("operation_type", "ACCEPT");
        metadata.put("connection_direction", "INBOUND");

        try {
            // Client (remote) info
            InetAddress remoteAddr = clientSocket.getInetAddress();
            metadata.put("client_address", remoteAddr.getHostAddress());
            metadata.put("client_port", String.valueOf(clientSocket.getPort()));
            metadata.put("is_client_loopback", String.valueOf(remoteAddr.isLoopbackAddress()));
            metadata.put("is_client_private_ip", String.valueOf(isPrivateIP(remoteAddr)));

            // Server (local) info
            InetAddress localAddr = clientSocket.getLocalAddress();
            metadata.put("server_address", localAddr.getHostAddress());
            metadata.put("server_port", String.valueOf(clientSocket.getLocalPort()));

            // Protocol
            String protocol = "TCP";
            if (clientSocket.getClass().getName().contains("SSL") ||
                clientSocket.getClass().getName().contains("Ssl")) {
                protocol = "SSL/TLS";
            }
            metadata.put("protocol", protocol);

        } catch (Exception e) {
            metadata.put("metadata_error", e.getClass().getSimpleName());
        }

        return metadata;
    }
}
