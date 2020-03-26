package org.owasp.jvmxray.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

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
	 * @param filename Fully qualified filename of database.
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
    		dbfile = homedir + "jvmxrayspool.db";
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
	                sql.append( "id integer PRIMARY KEY, ");
	                sql.append( "state integer NOT NULL, ");
	                sql.append( "timestamp long NOT NULL, ");
	                sql.append( "eventtype text NOT NULL, ");
	                sql.append( "identity text NOT NULL, ");
	                sql.append( "stacktrace text, ");
	                sql.append( "memo text");
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
	
	
	public void insertEvent(Connection conn, int state, long timestamp, String eventtype, String identity, String stacktrace, String memo ) throws SQLException {
		
		StringBuffer sql = new StringBuffer();
        PreparedStatement pstmt = null;
                 
        try {
                
            sql.append( "INSERT INTO spool (");
            sql.append( "state, ");
            sql.append( "timestamp, ");
            sql.append( "eventtype, ");
            sql.append( "identity, ");
            sql.append( "stacktrace, ");
            sql.append( "memo");
            sql.append( ") " );
            sql.append( "VALUES(?,?,?,?,?,?);" );
            
            pstmt = conn.prepareStatement(sql.toString());
            pstmt.setInt(1, state);
            pstmt.setLong(2, timestamp);
            pstmt.setString(3, eventtype);
            pstmt.setString(4, identity);
            pstmt.setString(5, stacktrace);
            pstmt.setString(6, memo);
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
	public EventDAO getNextEvent(Connection conn, EventDAO event) throws SQLException {
	
		StringBuffer sql = new StringBuffer();
		Statement stmt = null;
		ResultSet rs = null;
		EventDAO result = null;
        try {
           
        	int idx = (event == null ) ? 0 : event.id+1;
        	
        	if ( idx < Integer.MAX_VALUE ) {
        	
	        	sql.append("SELECT ");
	        	sql.append("id, ");
	        	sql.append("state, ");
	        	sql.append("timestamp, ");
	        	sql.append("eventtype, ");
	        	sql.append("identity, ");
	        	sql.append("stacktrace, ");
	        	sql.append("memo ");
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
		            String et = rs.getString("eventtype");
		            String it = rs.getString("identity");
		            String tr = rs.getString("stacktrace");
		            String me = rs.getString("memo");
	                    
		            result = new EventDAO(id, st, ts, et, it, tr, me);
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
	
	
	public void deleteEvent(Connection conn, EventDAO event) throws SQLException {
		
		StringBuffer sql = new StringBuffer();
		Statement stmt = null;
		ResultSet rs = null;

        try {
      
	        	sql.append("DELETE ");
	        	sql.append("FROM ");
	        	sql.append("spool ");
	        	sql.append("WHERE id=");
	        	sql.append(String.format("%s",event.id));
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
