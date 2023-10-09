package org.jvmxray.graphql.controller;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.auth.ProgrammaticPlainTextAuthProvider;

import java.net.InetSocketAddress;

public class JVMXRayCassandraConnectionFactory {

    private static JVMXRayCassandraConnectionFactory ctrl = null;
    private String id;
    private String pwd;
    private String node;
    private int port;
    private String datacenter;
    private CqlSession session;

    // Prevent instance creation.
    private JVMXRayCassandraConnectionFactory() {};

    // Obtain Singleton
    public static synchronized final JVMXRayCassandraConnectionFactory getInstance() {
        JVMXRayCassandraConnectionFactory result = null;
        if( ctrl == null ) {
            ctrl = new JVMXRayCassandraConnectionFactory();
            result = ctrl;
        } else {
            result = ctrl;
        }
        return result;
    }

    /**
     * Connected to Cassandra
     * @param id Cassandra user id
     * @param pwd Cassndra password
     * @param node Host/ip of Cassandra cluster
     * @param port Cluster port
     * @param datacenter Datacenter (for load balancing).
     */
    public void setConnectionMeta(String id, String pwd, String node, int port, String datacenter) {
        this.id = id;
        this.pwd = pwd;
        this.node = node;
        this.port = port;
        this.datacenter = datacenter;
    }

    public void connect() {
        CqlSessionBuilder builder = CqlSession.builder();
        builder.addContactPoint(new InetSocketAddress(node,port));
        builder.withLocalDatacenter(datacenter);
        builder.withAuthProvider(new ProgrammaticPlainTextAuthProvider(id, pwd));
        session = builder.build();
    }

    public CqlSession getSession() {
        return session;
    }

    public void close() {
        session.close();
    }

}
