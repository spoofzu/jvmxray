package org.jvmxray.appender.logback;

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

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * JVMXRayCassandraAppender logs JVMXRay events to a Cassandra database
 * for offline processing/reporting. Specifically for use with
 * JVMXRay and logback logging framework.<br/>
 * Note: not a generized Cassndra appender.
 * @see JVMXRayAppenderBase
 * @author Milton Smith
 */
public class JVMXRayCassandraAppender extends JVMXRayAppenderBase {

    // Defaults
    // 7199 - JMX (was 8080 pre Cassandra 0.8.xx)
    // 7000 - Internode communication (not used if TLS enabled)
    // 7001 - TLS Internode communication (used if TLS enabled)
    // 9160 - Thrift client API
    // 9042 - CQL native transport port

    private static final String KEYSPACE_NAME = "JVMXRAY";
    private static final String EVENT_TBL = "EVENTS";
    private static final String EVENT_META_TBL = "EVENTSMETA";
    private static final int KEYSPACE_REPLICATION_FACTOR = 1;

    private CqlSession session;
    private String node;
    private String datacenter;
    private int port;
    private String user;
    private String password;

    /**
     * Log JVMXRay event from logback info Cassandra database.
     * @see JVMXRayAppenderBase
     */
    @Override
    protected void processEvent(String eventid, long ts,
                                String eventtp, String loglevel, String loggernamespace,
                                String threadname, String aid, String cat,
                                String p1, String p2, String p3,
                                ILoggingEvent event ) {

        insertEvent(eventid, ts, eventtp, loglevel,
                    loggernamespace, threadname,
                    aid, cat, p1,
                    p2, p3);

    }

    /**
     * Log JVMXRay event metadata from logback info Cassandra database (if present).
     * @see JVMXRayAppenderBase
     */
    @Override
    protected void processEventFrame(String eventid, int level, String clzldr,
                                     String clzcn, String clzmethnm, String clzmodnm,
                                     String clzmodvr, String clzfilenm, int clzlineno,
                                     String clzlocation, String clznative) {

        insertEventMeta( eventid, level, clzldr,
                         clzcn, clzmethnm, clzmodnm,
                         clzmodvr, clzfilenm, clzlineno,
                         clzlocation, clznative);
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
                    + "eventtp VARCHAR,"
                    + "loglevel VARCHAR,"
                    + "loggernamespace VARCHAR,"
                    + "threadname VARCHAR,"
                    + "aid VARCHAR,"
                    + "cat VARCHAR,"
                    + "p1 VARCHAR,"
                    + "p2 VARCHAR,"
                    + "p3 VARCHAR)";
            session.execute(createTableQuery);
        }

        tableMetadata = session.getMetadata().getKeyspace(KEYSPACE_NAME).flatMap(keyspace -> keyspace.getTable(EVENT_META_TBL));
        if (tableMetadata.isEmpty()) {
            String createTableQuery = "CREATE TABLE IF NOT EXISTS " + KEYSPACE_NAME + "." + EVENT_META_TBL + " ("
                    + "eventid VARCHAR,"
                    + "level INT,"
                    + "clzldr VARCHAR,"
                    + "clzcn VARCHAR,"
                    + "clzmethnm VARCHAR,"
                    + "clzmodnm VARCHAR,"
                    + "clzmodvr VARCHAR,"
                    + "clzfilenm VARCHAR,"
                    + "clzlineno INT,"
                    + "clzlocation VARCHAR,"
                    + "clznative VARCHAR,"
                    + "PRIMARY KEY(eventid, level))";
            session.execute(createTableQuery);
        }

    }

    /**
     * Insert log event into Cassandra.
     * @param eventid  JVMXRay event id.  Example: 0c07057d0c3b7fd6-3549b40a-189550d0635-7fe6
     * @param ts JVMXRay timestamp.  Time event was logged.  Example: 1689349064265
     * @param eventtp  JVMXRay event type.  Around 30 different event types.  Example: PACKAGE_DEFINE
     * @param loglevel Logback log level.  Log level assigned to this event.  Example: WARN
     * @param loggernamespace Logback namespace.  Logging namespace assigned to event.  Example: org.jvmxray.agent.driver.jvmxraysecuritymanager.events.clz.packagedefine
     * @param threadname Java thread name.  Example: main
     * @param aid JVMXRay application id.  GUID assigned to service instance. Example: 045db2423ef7485e-40573590-18725e16a5e-8000
     * @param cat JVMXRay category id.  Categorize event data like production or test.  Example: unit-test
     * @param p1 JVMXRay parameter 1, optional.  Defination of the metadata depends on event type(eventtp) value.
     * @param p2 JVMXRay parameter 2, optional.  Defination of the metadata depends on event type(eventtp) value.
     * @param p3 JVMXRay parameter 3, optional.  Defination of the metadata depends on event type(eventtp) value.
     */
    private void insertEvent(String eventid, long ts, String eventtp, String loglevel,
                             String loggernamespace, String threadname, String aid,
                             String cat, String p1, String p2, String p3) {

        RegularInsert ri = QueryBuilder.insertInto(KEYSPACE_NAME,EVENT_TBL)
                .value("eventid",QueryBuilder.bindMarker())
                .value("ts",QueryBuilder.bindMarker())
                .value("eventtp",QueryBuilder.bindMarker())
                .value("loglevel",QueryBuilder.bindMarker())
                .value("loggernamespace",QueryBuilder.bindMarker())
                .value("threadname",QueryBuilder.bindMarker())
                .value("aid",QueryBuilder.bindMarker())
                .value("cat",QueryBuilder.bindMarker())
                .value("p1",QueryBuilder.bindMarker())
                .value("p2",QueryBuilder.bindMarker())
                .value("p3",QueryBuilder.bindMarker());

        SimpleStatement insertstmt = ri.build();
        insertstmt = insertstmt.setKeyspace(KEYSPACE_NAME);
        PreparedStatement ps = session.prepare(insertstmt);
        BoundStatement bs = ps.bind()
                .setString(0,eventid)
                .setLong(1, ts)
                .setString(2,eventtp)
                .setString(3,loglevel)
                .setString(4,loggernamespace)
                .setString(5,threadname)
                .setString(6,aid)
                .setString(7,cat)
                .setString(8,p1)
                .setString(9,p2)
                .setString(10,p3);

        session.execute(bs);

    }

    private void insertEventMeta(String eventid, int level, String clzldr,
                                 String clzcn, String clzmethnm, String clzmodnm,
                                 String clzmodvr, String clzfilenm, int clzlineno,
                                 String clzlocation, String clznative) {

        RegularInsert ri = QueryBuilder.insertInto(KEYSPACE_NAME,EVENT_META_TBL)
                .value("eventid",QueryBuilder.bindMarker())
                .value("level",QueryBuilder.bindMarker())
                .value("clzldr",QueryBuilder.bindMarker())
                .value("clzcn",QueryBuilder.bindMarker())
                .value("clzmethnm", QueryBuilder.bindMarker())
                .value("clzmodnm", QueryBuilder.bindMarker())
                .value("clzmodvr",QueryBuilder.bindMarker())
                .value("clzfilenm",QueryBuilder.bindMarker())
                .value("clzlineno",QueryBuilder.bindMarker())
                .value("clzlocation",QueryBuilder.bindMarker())
                .value("clznative",QueryBuilder.bindMarker());

        SimpleStatement insertstmt = ri.build();
        insertstmt = insertstmt.setKeyspace(KEYSPACE_NAME);
        PreparedStatement ps = session.prepare(insertstmt);
        BoundStatement bs = ps.bind()
                .setString(0,eventid)
                .setInt(1,level)
                .setString(2,clzldr)
                .setString(3,clzcn)
                .setString(4,clzmethnm)
                .setString(5,clzmodnm)
                .setString(6,clzmodvr)
                .setString(7,clzfilenm)
                .setInt(8,clzlineno)
                .setString(9,clzlocation)
                .setString(10,clznative);
        session.execute(bs);

    }


}
