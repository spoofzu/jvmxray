package org.jvmxray.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.auth.AuthProvider;
import com.datastax.oss.driver.api.core.auth.ProgrammaticPlainTextAuthProvider;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import com.datastax.oss.driver.api.core.metadata.schema.TableMetadata;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.insert.RegularInsert;
import org.jvmxray.platform.shared.logback.XRLogPairCodec;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;

/**
 * XRCassandraAppender logs JVMXRay events to a Cassandra database
 * for offline processing/reporting. Specifically for use with
 * JVMXRay and logback logging framework.<br/>
 * Note: not a generized Cassndra appender.
 * @see XRAppenderBase
 * @author Milton Smith
 */
public class XRCassandraAppender extends XRAppenderBase {

    // Default Cassandra ports.
    // 7199 - JMX (was 8080 pre Cassandra 0.8.xx)
    // 7000 - Internode communication (not used if TLS enabled)
    // 7001 - TLS Internode communication (used if TLS enabled)
    // 9160 - Thrift client API
    // 9042 - CQL native transport port

    private static final String KEYSPACE_NAME = "JVMXRAY";
    private static final String EVENT_TBL = "EVENTS";
    private static final int KEYSPACE_REPLICATION_FACTOR = 1;

    private CqlSession session;
    private String node;
    private String datacenter;
    private int port;
    private String user;
    private String password;

    /**
     * Log JVMXRay event from logback info Cassandra database.
     * @see XRAppenderBase
     */
    @Override
    protected void processEvent(String eventId, long timeStamp, String logLevel,
                                String loggerNamespace, String threadName, String aid,
                                String cat, String p1, String p2, String p3,
                                String dumpStack, Map<String, String> keyPairs,
                                ILoggingEvent event) {

        // Packs user defined keys like so, A=B C=D E=F
        // The values of the key/value pairs are URL encoded
        //
        String packed = serializeKeyPairs(keyPairs);

        insertEvent(eventId, timeStamp, logLevel,
                loggerNamespace, threadName, aid,
                cat, p1, p2,
                p3,
                packed,
                dumpStack);

    }

    private String serializeKeyPairs(Map<String,String> map) {
        StringBuilder builder = new StringBuilder();
        String rawValue = null;
        String value = null;
        for(String key : map.keySet() ) {
            rawValue = map.get(key);
            value = XRLogPairCodec.encode(rawValue, Charset.forName("UTF-8") );
            builder.append(' ');
            builder.append(key);
            builder.append('=');
            builder.append(value);
        }
        return builder.toString().trim();
    }

    /**
     * Thread safe initialization for initializing Cassndra appender.  Initialized only
     * once on the first event logged per logback framework design.  Once completed successfully,
     * it's not called again.
     * @param event Logback ILoggingEvent. Unmodified log message from logback logging framework.
     */
    protected synchronized void initializeAppender(ILoggingEvent event) {
        if (node == null || node.length() < 0) {
            addError("Cassandra 'node' property unassigned.");
            return;
        }
        if (port == 0) {
            addError("Cassandrda 'port' property unassigned.");
            return;
        }
        if (datacenter == null || datacenter.length() < 0) {
            addError("Cassandra 'datacenter' property unassigned.");
            return;
        }
        // Register shutdownhook.  Stop tasks as clean as possible on service shutdown (CTRL-c, etc).
        Thread sdHook = new Thread(() -> {
            shutDown();
        });
        Runtime.getRuntime().addShutdownHook(sdHook);
        initCassandra();
    }

    /**
     * Attempt safe shutdown on normal exits.
     */
    private void shutDown() {
        close();
    }

    /**
     * @param user Appender user property from logback.xml.  JavaBean introspection method.
     */
    public void setUser(String user) { this.user = user; }

    /**
     * @return user Appender user property from logback.xml.  JavaBean introspection method.
     */
    public String getUser() { return user; }

    /**
     * @param password Appender password property from logback.xml.  JavaBean introspection method.
     */
    public void setPassword(String password) { this.password = password; }

    /**
     * @return password Appender password property from logback.xml.  JavaBean introspection method.
     */
    public String getPassword() { return password; }

    /**
     * @param node Appender host property from logback.xml.  JavaBean introspection method.
     */
    public void setNode(String node) {
        this.node = node;
    }

    /**
     * @return Appender host property from logback.xml.  JavaBean introspection method.
     */
    public String getNode() {
        return node;
    }

    /**
     * @param port Appender port property from logback.xml.  JavaBean introspection method.
     */
    public void setPort(int port ) {
        this.port = port;
    }

    /**
     * @return Port property from logback.xml.  JavaBean introspection method.
     */
    public int getPort() {
        return port;
    }

    /**
     * @param datacenter Appender datacenter property from logback.xml.  JavaBean introspection method.
     */
    public void setDatacenter(String datacenter) {
        this.datacenter = datacenter;
    }

    /**
     * @return datacenter property from logback.xml.  JavaBean introspection method.
     */
    public String getDatacenter() {
        return datacenter;
    }

    /**
     * Intialize Cassandra, create Cassndra keyspace and table schema if
     * undefined.
     */
    private void initCassandra() {
        // Build a custom configuration with authentication options
        AuthProvider authProvider = new ProgrammaticPlainTextAuthProvider(getUser(), getPassword());
        setSession(connect(authProvider, getNode(),getPort(),getDatacenter()));
        createKeyspaceIfNotExists(session,KEYSPACE_NAME,KEYSPACE_REPLICATION_FACTOR);
        createSchemaIfNotExists(session);
    }

    /**
     * Connected to Cassandra based upon logback.xml properties.
     * @param authProvider Authorization provider
     * @param node Host/ip of Cassandra cluster
     * @param port Cluster port
     * @param dataCenter Datacenter (for load balancing).
     * @return Initialized session.
     */
    private CqlSession connect(AuthProvider authProvider, String node, Integer port, String dataCenter) {
        CqlSessionBuilder builder = CqlSession.builder();
        builder.addContactPoint(new InetSocketAddress(node,port));
        builder.withLocalDatacenter(dataCenter);
        builder.withAuthProvider(authProvider);
        return builder.build();
    }

    /**
     * Create Cassandra keyspace if it doesn't exist.
     */
    private void createKeyspaceIfNotExists(CqlSession session, String keyspaceName, int replicationFactor) {
        Optional<KeyspaceMetadata> keyspaceMetadata = session.getMetadata().getKeyspace(keyspaceName);
        if (keyspaceMetadata.isEmpty()) {
            String createKeyspaceQuery = "CREATE KEYSPACE IF NOT EXISTS " + keyspaceName + " WITH "
                    + "replication = {'class': 'SimpleStrategy', 'replication_factor': 1}";
            session.execute(createKeyspaceQuery);
        }
    }

    /**
     * Cassandra session
     * @return Current session or null.
     */
    private CqlSession getSession() {
        return this.session;
    }

    /**
     * Cassandra session
     * @param session Current Cassandra session.
     */
    private void setSession(CqlSession session) {
        this.session = session;
    }
    /**
     * Close Cassandra if session action or NOP.
     */
    private void close() {
        CqlSession tmp = getSession();
        if( tmp != null ) {
            tmp.close();
        }
    }

    /**
     * Create JVMXRay schema.
     * @param session
     */
    private void createSchemaIfNotExists(CqlSession session) {
        Optional<TableMetadata> tableMetadata = session.getMetadata().getKeyspace(KEYSPACE_NAME).flatMap(keyspace -> keyspace.getTable(EVENT_TBL));
        if (tableMetadata.isEmpty()) {
            String createTableQuery = "CREATE TABLE IF NOT EXISTS " + KEYSPACE_NAME + "." + EVENT_TBL + " ("
                    + "eventid text PRIMARY KEY,"
                    + "ts BIGINT,"
                    + "loglevel VARCHAR,"
                    + "loggernamespace VARCHAR,"
                    + "threadname VARCHAR,"
                    + "aid VARCHAR,"
                    + "cat VARCHAR,"
                    + "p1 VARCHAR,"
                    + "p2 VARCHAR,"
                    + "p3 VARCHAR,"
                    + "keypairs VARCHAR,"
                    + "dumpstack VARCHAR)";
            session.execute(createTableQuery);
        }

    }

    /**
     * Insert log event into Cassandra.
     * @param eventId  Event id.  Example: 0c07057d0c3b7fd6-3549b40a-189550d0635-7fe6
     * @param timeStamp Timestamp.  Time event was logged.  Example: 1689349064265
     * @param logLevel Logback log level.  Log level assigned to this event.  Example: WARN
     * @param loggerNamespace Logback logger namespace.  Logging namespace assigned to event.  Example: org.jvmxray.agent.driver.jvmxraysecuritymanager.events.clz.packagedefine
     * @param threadName Java thread name.  Example: main
     * @param aid Application instance id.  GUID assigned to service instance. Example: 045db2423ef7485e-40573590-18725e16a5e-8000
     * @param cat Category id.  Categorize event data like production or test.  Example: unit-test
     * @param p1 Parameter 1, optional.  Defination of the metadata depends on event type(eventtp) value.
     * @param p2 Parameter 2, optional.  Defination of the metadata depends on event type(eventtp) value.
     * @param p3 Parameter 3, optional.  Defination of the metadata depends on event type(eventtp) value.
     * @param keyPairs User keypairs.  These are X=Y pairs stored in the logback message.
     * @param dumpstack Caller stacktrace metadata, optional.  Only for TRACE events.
     */
    private void insertEvent(String eventId, long timeStamp, String logLevel,
                             String loggerNamespace, String threadName, String aid,
                             String cat, String p1, String p2, String p3, String keyPairs, String dumpstack) {

        RegularInsert ri = QueryBuilder.insertInto(KEYSPACE_NAME,EVENT_TBL)
                .value("eventid",QueryBuilder.bindMarker())
                .value("ts",QueryBuilder.bindMarker())
                .value("loglevel",QueryBuilder.bindMarker())
                .value("loggernamespace",QueryBuilder.bindMarker())
                .value("threadname",QueryBuilder.bindMarker())
                .value("aid",QueryBuilder.bindMarker())
                .value("cat",QueryBuilder.bindMarker())
                .value("p1",QueryBuilder.bindMarker())
                .value("p2",QueryBuilder.bindMarker())
                .value("p3",QueryBuilder.bindMarker())
                .value("keypairs",QueryBuilder.bindMarker())
                .value("dumpstack",QueryBuilder.bindMarker());

        SimpleStatement insertstmt = ri.build();
        insertstmt = insertstmt.setKeyspace(KEYSPACE_NAME);
        PreparedStatement ps = session.prepare(insertstmt);
        BoundStatement bs = ps.bind()
                .setString(0,eventId)
                .setLong(1, timeStamp)
                .setString(2,logLevel)
                .setString(3,loggerNamespace)
                .setString(4,threadName)
                .setString(5,aid)
                .setString(6,cat)
                .setString(7,p1)
                .setString(8,p2)
                .setString(9,p3)
                .setString(10,keyPairs)
                .setString(11,dumpstack);

        session.execute(bs);

    }


}
