package org.owasp.jvmxray;

import org.owasp.jvmxray.collector.JVMXRayServletContainer;

public class JVMXRayStandaloneServer {

    public JVMXRayStandaloneServer() {
    }

    public static void main(String[] args) {
        JVMXRayStandaloneServer server = new JVMXRayStandaloneServer();
        server.execute();
    }

    private void execute() {
        try {
            JVMXRayServletContainer server = JVMXRayServletContainer.getInstance();
            server.listen();
        }catch(Throwable t) {
            t.printStackTrace();
        }
    }

}
