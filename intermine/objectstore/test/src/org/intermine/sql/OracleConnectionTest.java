package org.intermine.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 *  class to test a connection to oracle
 */
public class OracleConnectionTest {
    
    private static final String INSTANCE = "db.sgd";
    private static Properties props = new Properties();
    private static final String TEST_QUERY = "select feature_name from feature"; 
    
    /**
     * set up the connection to the database using properties specified in setUpProps()
     * run the sample query
     */
    public static void testConnection() {
        setUpProps();
        Database database = null;
        try {
            database = new Database(props);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise " + INSTANCE, e);
        }
        Connection conn = null;
        try {
            conn = database.getConnection();
            runQuery(conn);
        } catch (SQLException e) {
            throw new RuntimeException("couldn't load properties:", e);
        }
    }
    
    /** 
     * load the properties directly. 
     *
     * note that the beginning of the properties (eg. db.sgd) are stripped off.
     * 
     * see org.intermine.sql.DatabaseFactory
     * 
     *  When the database is created, the starting instance is removed:
     *    
     *      database = new Database(PropertiesUtil.stripStart(instance, props));
     */
    private static void setUpProps() {
        props = new Properties();
        props.put("datasource.class", "oracle.jdbc.pool.OracleDataSource");
        props.put("datasource.serverName", "oracle.flymine.org");
        props.put("datasource.databaseName", "XE");
        props.put("datasource.user", "");
        props.put("datasource.password", "");
        props.put("datasource.maxConnections", "10");
        props.put("platform", "Oracle");
        props.put("driver", "org.oracle.jdbc.OracleDriver");
        props.put("datasource.driverType", "thin");
        props.put("datasource.portNumber", "1521");
    }

    /**
     * test the driver by calling the class directly 
     * pass the connection string to the driver manager, test the connection by running a query
     */
    public static void testDriver() {
        Connection conn = null;
        try {
            // register the driver directly
            DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
        } catch (SQLException e) {
            System.out.println("oops:" + e.getMessage());
        }

        try {
            conn = DriverManager.getConnection("jdbc:oracle:thin:@oracle.flymine.org:1521:XE", "bud", "bud");
            runQuery(conn);
        } catch (SQLException e) {
            throw new RuntimeException("oops:", e);
        }
    }
    
    /**
     * test the connection to the database by running a sample query and outputting the first column of results
     * @param conn connection to the database
     */    
    private static void runQuery(Connection conn) {
        Statement stmt = null;
        ResultSet rset = null;
        
        try {

            stmt = conn.createStatement();

            // execute query 
            rset = stmt.executeQuery(TEST_QUERY);
            System.out.println("running query ....");
            
            // loop through results
            while (rset.next()) {
                System.out.println(rset.getString(1));
            }
            
            // done!
            System.out.println("query done!");
            
            // clean up
            rset.close();
            rset = null;
            stmt.close();
            stmt = null;
            conn.close();
            conn = null;
        } catch (SQLException e) {
            throw new RuntimeException("oops", e);
        }
    }

    public static void main(String[] args) {
        testDriver();
//        testConnection();
    } 
}
