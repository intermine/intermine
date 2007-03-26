package org.intermine.sql;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

public class DatabaseTest extends TestCase
{
    private static final Logger LOG = Logger.getLogger(DatabaseTest.class);
    private Properties props;

    public void setUp() throws Exception {
        props = new Properties();
        props.put("datasource.class", "org.postgresql.jdbc2.optional.PoolingDataSource");
        props.put("datasource.serverName", "dbserver.mydomain.org");
        props.put("datasource.databaseName", "test");
        props.put("datasource.user", "auser");
        props.put("datasource.password", "secret");
        props.put("datasource.maxConnections", "10");
        props.put("platform", "PostgreSQL");
    }


    public DatabaseTest(String arg1) {
        super(arg1);
    }

    public void testConfigure() throws Exception {
        Database db = new Database();
        db.configure(props);
        assertTrue(db.getDataSource() != null);
        assertTrue(db.getDataSource() instanceof org.postgresql.jdbc2.optional.PoolingDataSource);
        assertEquals("PostgreSQL", db.getPlatform());
        assertEquals("dbserver.mydomain.org", ((org.postgresql.jdbc2.optional.PoolingDataSource) db.getDataSource()).getServerName());
        assertEquals("test", ((org.postgresql.jdbc2.optional.PoolingDataSource) db.getDataSource()).getDatabaseName());
        assertEquals(10, ((org.postgresql.jdbc2.optional.PoolingDataSource) db.getDataSource()).getMaxConnections());
    }

    public void testInvalidDataSourceClass() throws Exception {
        Properties invalidProps = new Properties();
        invalidProps.put("datasource.class", "org.class.that.cannot.be.Found");
        try {
            Database db = new Database(invalidProps);
            fail("Expected: ClassNotFoundException");
        } catch (ClassNotFoundException e) {
        }
    }

    public void testInvalidMethod() throws Exception {
        Properties invalidProps = new Properties();
        invalidProps.put("datasource.class", "org.postgresql.jdbc3.Jdbc3PoolingDataSource");
        invalidProps.put("datasource.someRubbish", "blahblahblah");
        Database db = new Database(invalidProps);
    }

    public void testNullProperties() throws Exception {
        try {
            Database db = new Database(null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testURL() throws Exception {
        Database db = new Database(props);
        assertEquals("jdbc:postgresql://dbserver.mydomain.org/test", db.getURL());
    }

    public void testUser() throws Exception {
        Database db = new Database(props);
        assertEquals("auser", db.getUser());
    }

    public void testPassword() throws Exception {
        Database db = new Database(props);
        assertEquals("secret", db.getPassword());
    }
/*
    public void manyTables(int tableCount) throws Exception {
        LOG.error("Starting test with tableCount = " + tableCount);
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection c = db.getConnection();
        c.setAutoCommit(true);
        Statement s = c.createStatement();
        Exception e2 = null;
        try {
            long start = System.currentTimeMillis();
            for (int i = 0; i < tableCount; i++) {
                String tableName = "table" + i + "test";
                s.addBatch("CREATE TABLE " + tableName + " (col int)");
                s.addBatch("INSERT INTO " + tableName + " (col) VALUES (" + i + ")");
            }
            s.executeBatch();
            long end = System.currentTimeMillis();
            System.out.println("Took " + (end - start) + " ms to create " + tableCount + " tables - average of " + ((tableCount * 1000) / (end - start)) + " tables per second");
            LOG.error("Took " + (end - start) + " ms to create " + tableCount + " tables - average of " + ((tableCount * 1000) / (end - start)) + " tables per second");
            start = System.currentTimeMillis();
            for (int i = 0; i < tableCount; i++) {
                String tableName = "table" + i + "test";
                ResultSet r = s.executeQuery("SELECT col FROM " + tableName);
                assertTrue(r.next());
                assertEquals(i, r.getInt(1));
                assertFalse(r.next());
            }
            end = System.currentTimeMillis();
            System.out.println("Took " + (end - start) + " ms to read " + tableCount + " tables - average of " + ((tableCount * 1000) / (end - start)) + " tables per second");
            LOG.error("Took " + (end - start) + " ms to read " + tableCount + " tables - average of " + ((tableCount * 1000) / (end - start)) + " tables per second");
        } catch (Exception e) {
            e2 = e;
            throw e;
        } finally {
            try {
                long start = System.currentTimeMillis();
                for (int i = 0; i < tableCount; i++) {
                    String tableName = "table" + i + "test";
                    s.addBatch("DROP TABLE " + tableName);
                }
                s.executeBatch();
                long end = System.currentTimeMillis();
                System.out.println("Took " + (end - start) + " ms to drop " + tableCount + " tables - average of " + ((tableCount * 1000) / (end - start)) + " tables per second");
                LOG.error("Took " + (end - start) + " ms to drop " + tableCount + " tables - average of " + ((tableCount * 1000) / (end - start)) + " tables per second");
                c.close();
            } catch (Exception e) {
                if (e2 == null) {
                    throw e;
                }
            }
        }
    }

    public void testManyTables() throws Exception {
        manyTables(100);
        manyTables(200);
        manyTables(400);
        manyTables(800);
        manyTables(1600);
        manyTables(3200);
        manyTables(6400);
        manyTables(12800);
        manyTables(25600);
        manyTables(51200);
        manyTables(102400);
    }
    */
}
