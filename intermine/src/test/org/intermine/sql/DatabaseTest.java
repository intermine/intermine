package org.flymine.sql;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.Properties;

public class DatabaseTest extends TestCase
{
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
        try {
            Database db = new Database(invalidProps);
        } catch (Exception e) {
            fail("Did not expect an exception to be thrown");
        }
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
}
