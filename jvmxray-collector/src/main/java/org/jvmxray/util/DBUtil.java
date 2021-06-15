package org.jvmxray.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;


/**
 * Database utilities to create and manage databases on both client/server sides.
 */
public class DBUtil {

    // Event utilitizes
    private static final EventUtil eu = EventUtil.getInstance();

    private static final String CONF_PROP_EVENT_SERVER_DIRECTORY = "jvmxray.event.server.directory";
    private static final String CONF_PROP_EVENT_SERVER_FILE = "jvmxray.event.server.event.db";

    private static DBUtil u = null;
    private static Properties p = null;
    private Connection conn = null;

    private DBUtil() {}

    public static final synchronized DBUtil getInstance(Properties p) {
        if ( u == null ) {
            u = new DBUtil();
        }
        DBUtil.p = p;
        return u;
    }

    /**
     * Create SQLite DB.  Create a new DB, with schema, if one does not exist already.
     * @throws SQLException
     */
    public synchronized Connection createConnection() throws SQLException {

        // If connection is alive return cashed.
        if( conn != null ) {
            if( conn.isValid(500) ) {
                return conn;
            }
        }

        StringBuffer sql = new StringBuffer();
        String homedir = System.getProperty("user.home");
        String proprdir = p.getProperty(CONF_PROP_EVENT_SERVER_DIRECTORY);
        String propfile = p.getProperty(CONF_PROP_EVENT_SERVER_FILE);

        // The default db is {user.home}\jvmxrayspool.db unless the user overrides
        // with their preferences as a property setting.
        //
        if( homedir != null ) {
            homedir = homedir.trim();
            if( !homedir.endsWith(File.separator) ) {
                homedir += File.separator;
            }
        }

        if( proprdir != null ) {
            proprdir = proprdir.trim();
            if( !proprdir.endsWith(File.separator) ) {
                proprdir += File.separator;
            }
        }

        if( propfile != null ) {
            propfile = propfile.trim();
        }

        String dbfile = null;
        if( proprdir != null && propfile != null ) {
            dbfile = proprdir + propfile;
        } else {
            // *** If no db configured return null.
            return null;
        }

        String url = "jdbc:sqlite:" + dbfile;
        Statement stmt = null;


        File f = new File(proprdir+propfile);
        boolean dbexists = f.exists();

        try {
            conn = DriverManager.getConnection(url);
            if (conn != null && !dbexists ) {
                // Create primary events table.
                sql.append( "CREATE TABLE IF NOT EXISTS spool (");
                sql.append( "id integer PRIMARY KEY, ");   // Primary key
                sql.append( "state integer NOT NULL, ");   // State (currently not used)
                sql.append( "timestamp long NOT NULL, ");  // Event creation timestamp
                sql.append( "threadid text NOT NULL, ");   // Thread ID
                sql.append( "eventtype text NOT NULL, ");  // Event type.  IEvent.Events
                sql.append( "identity text NOT NULL, ");   // App server identifier
                sql.append( "stacktrace text NOT NULL, "); // Stacktrace, if available
                sql.append( "param1 text NOT NULL, ");     // First parameter, event type dependent
                sql.append( "param2 text NOT NULL, ");     // Second parameter, event type dependent
                sql.append( "param3 text NOT NULL ");      // Third paramameter, event type dependent
                sql.append( ");" );
                stmt = conn.createStatement();
                stmt.execute(sql.toString());
            }
        } finally {
            if( stmt != null ) {
                try {
                    stmt.close();
                } catch (SQLException e) {}
            }
        }

        return conn;

    }

    public synchronized void insertEvent(Connection conn, String event ) throws SQLException {

        StringBuffer sql = new StringBuffer();
        PreparedStatement pstmt = null;

        try {
            sql.append( "INSERT INTO spool (");
            sql.append( "state, ");
            sql.append( "timestamp, ");
            sql.append( "threadid, ");
            sql.append( "eventtype, ");
            sql.append( "identity, ");
            sql.append( "stacktrace, ");
            sql.append( "param1, ");
            sql.append( "param2, ");
            sql.append( "param3");
            sql.append( ") " );
            sql.append( "VALUES(?,?,?,?,?,?,?,?,?);" );

            pstmt = conn.prepareStatement(sql.toString());
            pstmt.setInt(1, EventUtil.getInstance().getState(event));
            pstmt.setLong(2, EventUtil.getInstance().getTimeStamp(event));
            pstmt.setString(3, EventUtil.getInstance().getThreadId(event));
            pstmt.setString(4, EventUtil.getInstance().getEventType(event));
            pstmt.setString(5, EventUtil.getInstance().getIdentity(event));
            pstmt.setString(6, EventUtil.getInstance().getStackTrace(event));
            pstmt.setString(7, EventUtil.getInstance().getParam1(event) );
            pstmt.setString(8, EventUtil.getInstance().getParam2(event) );
            pstmt.setString(9, EventUtil.getInstance().getParam3(event) );
            pstmt.executeUpdate();
        } finally {
            if( pstmt != null ) {
                try {
                    pstmt.close();
                } catch (SQLException e) {}
            }
        }

    }

    /**
     * Retrieve next event to process.  Processes events in ascending order.
     * @param conn
     * @param event Returns next event to process.  Pass null to retrieve first event or
     * pass previous event to retrieve the next.
     * @return
     * @throws SQLException
     */
    public synchronized String getNextEvent(Connection conn, String event) throws SQLException {
        StringBuffer sql = new StringBuffer();
        Statement stmt = null;
        ResultSet rs = null;
        String result = null;
        try {
            int idx = (event == null ) ? 0 : EventUtil.getInstance().getPK(event)+1;
            if ( idx < Integer.MAX_VALUE ) {
                sql.append("SELECT ");
                sql.append("id, ");
                sql.append("state, ");
                sql.append("timestamp, ");
                sql.append("threadid, ");
                sql.append("eventtype, ");
                sql.append("identity, ");
                sql.append("stacktrace, ");
                sql.append("param1, ");
                sql.append("param2, ");
                sql.append("param3 ");
                sql.append("FROM spool ");
                sql.append("WHERE ");
                sql.append("id BETWEEN "+idx);
                sql.append(" AND ");
                sql.append(Integer.MAX_VALUE);
                sql.append(" ");
                sql.append("LIMIT 1;" );

                stmt  = conn.createStatement();
                rs    = stmt.executeQuery(sql.toString());

                while( rs.next() ) {
                    int id = rs.getInt("id");
                    int st = rs.getInt("state");
                    long ts = rs.getLong("timestamp");
                    String tid = rs.getString("threadid");
                    String et = rs.getString("eventtype");
                    String it = rs.getString("identity");
                    String tr = rs.getString("stacktrace");
                    String p1 = rs.getString("param1");
                    String p2 = rs.getString("param2");
                    String p3 = rs.getString("param3");

                    StringBuffer buff = new StringBuffer();
                    buff.append(id);
                    buff.append(',');
                    buff.append(st);
                    buff.append(',');
                    buff.append(ts);
                    buff.append(',');
                    buff.append(tid);
                    buff.append(',');
                    buff.append(et);
                    buff.append(',');
                    buff.append(it);
                    buff.append(',');
                    buff.append(tr);
                    buff.append(',');
                    buff.append(p1);
                    buff.append(',');
                    buff.append(p2);
                    buff.append(',');
                    buff.append(p3);

                    result = event;
                }
            }
        } finally {
            if( stmt != null ) {
                try {
                    stmt.close();
                } catch (SQLException e) {}
            }
            if( rs != null ) {
                try {
                    rs.close();
                } catch (SQLException e) {}
            }
        }
        return result;
    }


    public synchronized void deleteEvent(Connection conn, String event) throws SQLException {

        StringBuffer sql = new StringBuffer();
        Statement stmt = null;
        ResultSet rs = null;

        int ipk = eu.getPK(event);
        String pk = Integer.toString(ipk);

        try {

            sql.append("DELETE ");
            sql.append("FROM ");
            sql.append("spool ");
            sql.append("WHERE id=");
            sql.append(pk);
            sql.append(";");

            stmt  = conn.createStatement();
            stmt.execute(sql.toString());

        } finally {
            if( stmt != null ) {
                try {
                    stmt.close();
                } catch (SQLException e) {}
            }
            if( rs != null ) {
                try {
                    rs.close();
                } catch (SQLException e) {}
            }
        }


    }


}
