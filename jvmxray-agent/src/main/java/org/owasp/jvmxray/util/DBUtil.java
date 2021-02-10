package org.owasp.jvmxray.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.owasp.jvmxray.event.EventFactory;
import org.owasp.jvmxray.event.IEvent;
import org.owasp.jvmxray.event.IEvent.Events;

public class DBUtil {

    private static DBUtil u = null;
    private static Properties p = null;

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
    public Connection createConnection() throws SQLException {
        StringBuffer sql = new StringBuffer();
        String homedir = System.getProperty("user.home");
        String proprdir = p.getProperty(PropertyUtil.CONF_PROP_EVENT_SPOOL_DIRECTORY);
        String propfile = p.getProperty(PropertyUtil.CONF_PROP_EVENT_SPOOL_FILE);

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
        Connection conn = null;
        Statement stmt = null;


        File f = new File(proprdir+propfile);
        boolean dbexists = f.exists();

        try {
            conn = DriverManager.getConnection(url);
            if (conn != null && !dbexists ) {
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


    public void insertEvent(Connection conn, IEvent event ) throws SQLException {

        StringBuffer sql = new StringBuffer();
        PreparedStatement pstmt = null;

        try {
            String p1 = "";
            String p2 = "";
            String p3 = "";

            String[] params = event.getParams();
            if (params != null && params.length > 0 ) {
                p1 = ( params[0] == null ) ? "" : params[0];
            }
            if( params.length > 1 ) {
                p2 = ( params[1] == null ) ? "" : params[1];
            }
            if( params.length > 2 ) {
                p3 = ( params[2] == null ) ? "" : params[2];
            }

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
            pstmt.setInt(1, event.getState());
            pstmt.setLong(2, event.getTimeStamp());
            pstmt.setString(3, event.getThreadId());
            pstmt.setString(4, event.getEventType().toString());
            pstmt.setString(5, event.getIdentity());
            pstmt.setString(6, event.getStackTrace());
            pstmt.setString(7, p1 );
            pstmt.setString(8, p2 );
            pstmt.setString(9, p3 );
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
    public IEvent getNextEvent(Connection conn, IEvent event) throws SQLException {

        StringBuffer sql = new StringBuffer();
        Statement stmt = null;
        ResultSet rs = null;
        IEvent result = null;
        try {

            int idx = (event == null ) ? 0 : event.getPK()+1;
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

                    EventFactory factory = EventFactory.getInstance();
                    Events ett = Events.valueOf(et);
                    result = factory.createEventByEventType(ett, id, st, ts, tid, it, tr, p1, p2, p3);
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


    public void deleteEvent(Connection conn, IEvent event) throws SQLException {

        StringBuffer sql = new StringBuffer();
        Statement stmt = null;
        ResultSet rs = null;

        try {

            sql.append("DELETE ");
            sql.append("FROM ");
            sql.append("spool ");
            sql.append("WHERE id=");
            sql.append(String.format("%s",event.getPK()));
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
