package org.intermine.sql;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.postgresql.ds.PGPoolingDataSource;

public class DatabaseTest extends TestCase
{
    private Properties props;

    public void setUp() throws Exception {
        props = new Properties();
        props.put("datasource.class", "org.postgresql.ds.PGPoolingDataSource");
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
        assertTrue(db.getDataSource() instanceof PGPoolingDataSource);
        assertEquals("PostgreSQL", db.getPlatform());
        assertEquals("dbserver.mydomain.org", ((PGPoolingDataSource) db.getDataSource()).getServerName());
        assertEquals("test", ((PGPoolingDataSource) db.getDataSource()).getDatabaseName());
        assertEquals(10, ((PGPoolingDataSource) db.getDataSource()).getMaxConnections());
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
        invalidProps.put("datasource.class", "org.postgresql.ds.PGPoolingDataSource");
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

    public void testVersionIsAtLeast() throws Exception {
        Database db = new Database(props);
        db.version = "9.3";
        assertTrue(db.isVersionAtLeast("9.2"));
        assertTrue(db.isVersionAtLeast("9.1.7"));
        assertTrue(db.isVersionAtLeast("8"));
        assertTrue(db.isVersionAtLeast("9.3"));
        assertTrue(db.isVersionAtLeast("9.3.0"));
        assertFalse(db.isVersionAtLeast("10"));
        assertFalse(db.isVersionAtLeast("9.4"));
        assertFalse(db.isVersionAtLeast("9.3.1"));
        db.version = "9.2.1";
        assertTrue(db.isVersionAtLeast("9.2"));
        assertTrue(db.isVersionAtLeast("9.2.0"));
        assertTrue(db.isVersionAtLeast("9.2.1.0"));
        assertFalse(db.isVersionAtLeast("9.2.1.1"));
        assertFalse(db.isVersionAtLeast("9.2.2"));
        db.version = "9.4beta3";
        assertTrue(db.isVersionAtLeast("9.2"));
        assertTrue(db.isVersionAtLeast("9.4.0"));
        assertFalse(db.isVersionAtLeast("9.5"));
    }

/*
    public void manyTables(int tableCount) throws Exception {
        LOG.warn("Starting test with tableCount = " + tableCount);
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
            LOG.warn("Took " + (end - start) + " ms to create " + tableCount + " tables - average of " + ((tableCount * 1000) / (end - start)) + " tables per second");
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
            LOG.warn("Took " + (end - start) + " ms to read " + tableCount + " tables - average of " + ((tableCount * 1000) / (end - start)) + " tables per second");
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
                LOG.warn("Took " + (end - start) + " ms to drop " + tableCount + " tables - average of " + ((tableCount * 1000) / (end - start)) + " tables per second");
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
