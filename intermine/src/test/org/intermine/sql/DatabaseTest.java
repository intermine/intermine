package org.flymine.sql;

import junit.framework.*;
import java.sql.Connection;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;

public class DatabaseTest extends TestCase
{
    Properties props;

    public DatabaseTest(String arg1) {
        super(arg1);
    }

    public void testConfigure() throws Exception {
        props = new Properties();
        props.put("datasource.class", "org.postgresql.jdbc2.optional.PoolingDataSource");
        props.put("datasource.serverName", "dbserver.mydomain.org");
        props.put("datasource.databaseName", "test");
        props.put("datasource.maxConnections", "10");
        props.put("type", "PostgreSQL");

        Database db = new Database();
        db.configure(props);
        assertTrue(db.getDataSource() != null);
        assertTrue(db.getDataSource() instanceof org.postgresql.jdbc2.optional.PoolingDataSource);
        assertEquals("PostgreSQL", db.getType());
        assertEquals("dbserver.mydomain.org", ((org.postgresql.jdbc2.optional.PoolingDataSource) db.getDataSource()).getServerName());
        assertEquals("test", ((org.postgresql.jdbc2.optional.PoolingDataSource) db.getDataSource()).getDatabaseName());
        assertEquals(10, ((org.postgresql.jdbc2.optional.PoolingDataSource) db.getDataSource()).getMaxConnections());
    }

    public void testInvalidDataSourceClass() throws Exception {
        Properties invalidProps = new Properties();
        invalidProps.put("datasource.class", "org.class.that.cannot.be.Found");
        try {
            Database db = new Database();
            db.configure(invalidProps);
            fail("Expected: ClassNotFoundException");
        } catch (ClassNotFoundException e) {
        }
    }

    public void testInvalidMethod() throws Exception {
        Properties invalidProps = new Properties();
        invalidProps.put("datasource.class", "org.postgresql.jdbc3.Jdbc3PoolingDataSource");
        invalidProps.put("datasource.someRubbish", "blahblahblah");
        try {
            Database db = new Database();
            db.configure(invalidProps);
        } catch (Exception e) {
            fail("Did not expect an exception to be thrown");
        }
    }

    public void testNullProperties() throws Exception {
        try {
            Database db = new Database();
            db.configure(null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

}
