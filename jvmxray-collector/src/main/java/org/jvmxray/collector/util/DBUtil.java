package org.jvmxray.collector.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.jvmxray.agent.exception.JVMXRayDBException;

import org.jvmxray.agent.event.EventDAO;
import org.jvmxray.agent.event.IEvent;
import org.jvmxray.agent.event.IStackTrace;
import org.jvmxray.agent.event.StackTraceDAO;
import org.jvmxray.agent.util.EventUtil;
import org.jvmxray.agent.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Relational DB utilities for persisting events.
 * @author Milton Smith
 */
public class DBUtil {

    private static final Logger logger = LoggerFactory.getLogger("org.jvmxray.collector.util.DBUtil");
    private static DBUtil u = null;

    private DBUtil() {}

    public static final synchronized DBUtil getInstance() {
        if ( u == null ) {
            u = new DBUtil();
        }
        return u;
    }

    /**
     * Create a new DB, with schema, if one does not exist already.
     * @throws SQLException
     */
    public Connection createConnection() throws ClassNotFoundException, SQLException {
        PropertyUtil pu = PropertyUtil.getInstance(PropertyUtil.SYS_PROP_SERVER_CONFIG_DEFAULT);
        StringBuffer sql = new StringBuffer();
        //String basedir = pu.getStringProperty(PropertyUtil.SYS_PROP_AGENT_BASE_DIR);
        String driver = pu.getStringProperty(PropertyUtil.SYS_PROP_SERVER_JDBC_DRIVER);
        String connection = pu.getStringProperty(PropertyUtil.SYS_PROP_SERVER_JDBC_CONNECTION);
        String dbuser = pu.getStringProperty(PropertyUtil.SYS_PROP_SERVER_JDBC_USERNAME);
        String dbpwd = pu.getStringProperty(PropertyUtil.SYS_PROP_SERVER_JDBC_PASSWORD);
        Connection conn = null;
        Statement stmt = null;
        // Attempt to load driver.  Must be in cp.
        Class.forName(driver);
        try {
            conn = DriverManager.getConnection(connection,dbuser,dbpwd);
            if (conn != null ) {
                // CREATE EVENTSPOOL TABLE
                sql.append( "CREATE TABLE IF NOT EXISTS EVENTSPOOL (");
                sql.append( "id integer AUTO_INCREMENT, "); // PK, automatic index
                sql.append( "st integer NOT NULL, ");       // State
                sql.append( "ts long NOT NULL, ");          // Event timestamp (created on Agent)
                sql.append( "tid text NOT NULL, ");         // Thread ID
                sql.append( "et text NOT NULL, ");          // Event type.  IEvent.Events
                sql.append( "aid text NOT NULL, ");         // App server identity.  Sec hash.
                sql.append( "p1 text NOT NULL, ");          // First parameter, event type dependent
                sql.append( "p2 text NOT NULL, ");          // Second parameter, event type dependent
                sql.append( "p3 text NOT NULL, ");          // Third paramameter, event type dependent
                sql.append( "mr text NOT NULL, ");          // ID/property name of rule when event fired
                sql.append( "PRIMARY KEY (id) ");           // Define PK
                sql.append( ");" );
                stmt = conn.createStatement();
                stmt.execute(sql.toString());
                sql.setLength(0);
                // CREATE STACKTRACE TABLE
                sql.append( "CREATE TABLE IF NOT EXISTS STACKTRACE (");
                sql.append( "id integer, ");                // FK, from EVENTSPOOL
                sql.append( "sl integer, ");                // Stacktrace, level
                sql.append( "clsloadernm long NOT NULL, "); // Classloader name
                sql.append( "filenm text NOT NULL, ");      // Target source file of class
                sql.append( "clsnm text NOT NULL, ");       // Class name
                sql.append( "methnm text NOT NULL, ");      // Method name
                sql.append( "linenum integer NOT NULL, ");  // Line number
                sql.append( "loc text NOT NULL, ");         // Location (loaded from)
                sql.append( "modulenm text NOT NULL, ");    // Module name
                sql.append( "modulevr text NOT NULL, ");    // Module version
                sql.append( "isnative text NOT NULL, ");    // Is native method? t/f
                sql.append( "ds text NOT NULL, ");          // String representation
                sql.append( "PRIMARY KEY (id, sl) ");       // Define PK
                sql.append( ");" );
                stmt = conn.createStatement();
                stmt.execute(sql.toString());
                // CREATE STACKTRACE FK TO EVENTSPOOL
                sql.setLength(0);
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

    /**
     * Inserts an event into the DB.
     * @param conn
     * @param event Event to insert.  After insert IEvent.getPk() is modified to
     *              return the Primary Key value.
     * @throws SQLException On SQL exception
     * @throws JVMXRayDBException On event.getPk()>-1.
     */
    public void insertEvent(Connection conn, EventDAO event) throws SQLException,JVMXRayDBException {
        if( event.getPk()>-1 ) throw new JVMXRayDBException("Must be a new event. event.getPk()==-1");
        StringBuffer sql = new StringBuffer();
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        try {
            try {
                conn.setAutoCommit(false);
            }catch(SQLException e) {
                logger.error("Insert event failed. err=No transaction support.", e);
                throw e;
            }
            // PK, auto incremented
            int st = event.getSt();
            long ts = event.getTs();
            String tid = event.getTid();
            String et = event.getEt();
            String aid = event.getAid();
            String p1 = event.getP1();
            String p2 = event.getP2();
            String p3 = event.getP3();
            String mr = event.getMatchingRule();
            sql.append("INSERT INTO EVENTSPOOL (");
            sql.append("st, ");
            sql.append("ts, ");
            sql.append("tid, ");
            sql.append("et, ");
            sql.append("aid, ");
            sql.append("p1, ");
            sql.append("p2, ");
            sql.append("p3, ");
            sql.append("mr");
            sql.append(") ");
            sql.append("VALUES(?,?,?,?,?,?,?,?,?);");
            pstmt = conn.prepareStatement(sql.toString(),Statement.RETURN_GENERATED_KEYS);
            pstmt.setInt(1, st);
            pstmt.setLong(2, ts);
            pstmt.setString(3, tid);
            pstmt.setString(4, et);
            pstmt.setString(5, aid);
            pstmt.setString(6, p1);
            pstmt.setString(7, p2);
            pstmt.setString(8, p3);
            pstmt.setString(9, mr);
            pstmt.executeUpdate();
            sql.setLength(0);
            rs = pstmt.getGeneratedKeys();
            int id = -1;
            if(rs.next()) {
                id = rs.getInt(1);
            }
            // Assign pk to the event instance.
            event.setPk(id);
            insertStackTraceForEvent(conn, (IEvent)event);
            try {
                conn.commit();
            }catch(SQLException e) {
                logger.error("Insert event failed. err=Commit failure",e);
                throw e;
            }
        }catch(SQLException e){
            try {
                conn.rollback();
                logger.error("Insert failed.  Rollback success.",e);
            }catch(SQLException e1) {
                logger.error("Insert event failed. err=Rollback failed.",e1);
                throw e1;
            }
        } finally {
            if( pstmt != null ) {
                try {
                    pstmt.close();
                } catch (SQLException e) {}
            }
            try {
                conn.setAutoCommit(true);
            }catch(SQLException e) {
                logger.error("Insert event auto-commit failure. err="+e.getMessage(), e);
                return;
            }
        }
    }

    private void insertStackTraceForEvent(Connection conn, IEvent event) throws SQLException {
        StringBuffer sql = new StringBuffer();
        PreparedStatement pstmt = null;
        IStackTrace str = event.getStackTrace();
        // Build preparedstatement
        sql.append( "INSERT INTO STACKTRACE (");
        sql.append( "id, ");
        sql.append( "sl, ");
        sql.append( "clsloadernm, ");
        sql.append( "filenm, ");
        sql.append( "clsnm, ");
        sql.append( "methnm, ");
        sql.append( "linenum, ");
        sql.append( "loc, ");
        sql.append( "modulenm, ");
        sql.append( "modulevr, ");
        sql.append( "isnative, ");
        sql.append( "ds ");
        sql.append( ") " );
        sql.append( "VALUES(?,?,?,?,?,?,?,?,?,?,?,?);" );
        // Initialize variables
        int eFK = event.getPk();
        int sl = 0;
        String clsloadernm = "";
        String filenm = "";
        String clsnm = "";
        String methnm = "";
        int linenum = 0;
        String loc = "";
        String modulenm = "";
        String modulevr = "";
        boolean isnative = false;
        String ds = "";
        // Insert each level of the stackframe
        while( str!=null ) {
            clsloadernm = str.getClsloadernm();
            clsnm = str.getClsnm();
            filenm = str.getFilenm();
            methnm = str.getMethnm();
            linenum = str.getLinenum();
            loc = str.getLoc();
            modulenm = str.getModulenm();
            modulevr = str.getModulevr();
            isnative = str.isNative();
            ds = str.getDesc();
            // Make assignments and execute.
            try {
                pstmt = conn.prepareStatement(sql.toString());
                pstmt.setInt(1, eFK);
                pstmt.setInt(2, sl);
                pstmt.setString(3, clsloadernm);
                pstmt.setString(4, filenm);
                pstmt.setString(5, clsnm);
                pstmt.setString(6, methnm);
                pstmt.setInt(7, linenum);
                pstmt.setString(8, loc );
                pstmt.setString(9, modulenm );
                pstmt.setString(10, modulevr );
                pstmt.setBoolean(11, isnative );
                pstmt.setString(12, ds );
                pstmt.executeUpdate();
                sl++; // Incremement stack level in compound key.
            } finally {
                if( pstmt != null ) {
                    try {
                        pstmt.close();
                    } catch (SQLException e) {}
                }
            }
            str = str.getNextStackTrace();
        }
    }

    public IStackTrace getStackTraceForEvent(Connection conn, IEvent event) throws SQLException {
        StringBuffer sql = new StringBuffer();
        Statement stmt = null;
        ResultSet rs = null;
        IStackTrace result = null;
        //
        int id = event.getPk();
        int sl = 0;
        String clsloadernm = "";
        String filenm = "";
        String clsnm = "";
        String methnm = "";
        int linenum = 0;
        String loc = "";
        String modulenm = "";
        String modulevr = "";
        boolean isnative = false;
        String desc = "";
        //
        try {
            sql.append("SELECT ");
            sql.append( "id, ");
            sql.append( "sl, ");
            sql.append( "clsloadernm, ");
            sql.append( "clsnm, ");
            sql.append( "filenm, ");
            sql.append( "methnm, ");
            sql.append( "linenum, ");
            sql.append( "loc, ");
            sql.append( "modulenm, ");
            sql.append( "modulevr, ");
            sql.append( "isnative, ");
            sql.append( "ds ");
            sql.append("FROM STACKTRACE ");
            sql.append("WHERE ");
            sql.append("id="+id+" ");
            sql.append("ORDER BY id,sl ASC");
            stmt  = conn.createStatement();
            rs    = stmt.executeQuery(sql.toString());
            StackTraceDAO sDAO = null;
            while( rs.next() ) {
                id = rs.getInt("id");
                sl = rs.getInt("sl");
                clsloadernm = rs.getString("clsloadernm");
                filenm = rs.getString("filenm");
                clsnm = rs.getString("clsnm");
                methnm = rs.getString("methnm");
                linenum = rs.getInt("linenum");
                loc = rs.getString("loc");
                modulenm = rs.getString("modulenm");
                modulevr = rs.getString("modulevr");
                isnative = rs.getBoolean("isnative");
                desc = rs.getString("ds");
                // Parent node
                if( sDAO == null ) {
                    sDAO = new StackTraceDAO(clsloadernm, filenm, clsnm, methnm, linenum,
                            loc, modulenm, modulevr, isnative, desc);
                    // Decendant node
                } else {
                    StackTraceDAO tDAO = new StackTraceDAO(clsloadernm, filenm, clsnm, methnm, linenum,
                            loc, modulenm, modulevr, isnative, desc);
                    sDAO.setNextstacktrace(tDAO);
                }
            }
            result = sDAO;
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
            try {
                conn.setAutoCommit(false);
            }catch(SQLException e) {
                logger.error("Delete event failed. err=No transaction support.", e);
                return;
            }
            EventUtil eu = EventUtil.getInstance();
            int iPk = event.getPk();
            String sPk = Integer.valueOf(iPk).toString();
            sql.append("DELETE ");
            sql.append("FROM ");
            sql.append("STACKTRACE ");
            sql.append("WHERE id=");
            sql.append(String.format("%s",sPk));
            sql.append(";");
            stmt  = conn.createStatement();
            stmt.execute(sql.toString());
            sql.setLength(0);
            sql.append("DELETE ");
            sql.append("FROM ");
            sql.append("EVENTSPOOL ");
            sql.append("WHERE id=");
            sql.append(String.format("%s",sPk));
            sql.append(";");
            stmt  = conn.createStatement();
            stmt.execute(sql.toString());
            try {
                conn.commit();
            }catch(SQLException e) {
                logger.error("Delete event failed. err=Commit failure",e);
                try {
                    conn.rollback();
                    logger.warn("Rollback success.");
                }catch(SQLException e1) {
                    logger.error("Delete event failed. err=Rollback failed.",e);
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
            try {
                conn.setAutoCommit(true);
            }catch(SQLException e) {
                logger.error("Delete event auto-commit failure. err="+e.getMessage(), e);
                return;
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
    public EventDAO getNextEvent(Connection conn, IEvent event) throws SQLException {
        StringBuffer sql = new StringBuffer();
        Statement stmt = null;
        ResultSet rs = null;
        EventDAO result = null;
        try {
            int iPk = event.getPk();
            String sPk = Integer.toString(iPk);
            int idx = (event == null ) ? 0 : iPk+1;
            if ( idx < Integer.MAX_VALUE ) {
                sql.append("SELECT ");
                sql.append("id, ");
                sql.append("st, ");
                sql.append("ts, ");
                sql.append("tid, ");
                sql.append("et, ");
                sql.append("aid, ");
                sql.append("p1, ");
                sql.append("p2, ");
                sql.append("p3, ");
                sql.append("mr ");
                sql.append("FROM EVENTSPOOL ");
                sql.append("WHERE ");
                sql.append("id BETWEEN "+idx);
                sql.append(" AND ");
                sql.append(Integer.MAX_VALUE);
                sql.append(" ");
                sql.append("LIMIT 1;" );
                stmt  = conn.createStatement();
                rs    = stmt.executeQuery(sql.toString());
                while( rs.next() ) {
                    int pk = rs.getInt("id");
                    int st = rs.getInt("st");
                    long ts = rs.getLong("ts");
                    String tid = rs.getString("tid");
                    String et = rs.getString("et");
                    String aid = rs.getString("aid");
                    String p1 = rs.getString("p1");
                    String p2 = rs.getString("p2");
                    String p3 = rs.getString("p3");
                    String mr = rs.getString("mr");
                    IStackTrace stacktrace = getStackTraceForEvent(conn, event);
                    result = new EventDAO(mr, pk,st,ts,tid,et,aid,stacktrace,p1,p2,p3);
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


}
