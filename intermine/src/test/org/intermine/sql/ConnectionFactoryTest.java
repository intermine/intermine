package org.flymine.sql;

import junit.framework.*;
import java.sql.Connection;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;
import org.flymine.sql.ConnectionFactory;

public class ConnectionFactoryTest extends TestCase
{
    Properties props;

    public ConnectionFactoryTest(String arg1) {
        super(arg1);
    }

    public void testConfigureDataSource() throws Exception {
        props = new Properties();
        props.put("db.test.dataSource", "org.postgresql.jdbc2.optional.PoolingDataSource");
        props.put("db.test.serverName", "dbserver.mydomain.org");
        props.put("db.test.databaseName", "test");
        props.put("db.test.maxConnections", "10");

        DataSource ds = ConnectionFactory.configureDataSource("db.test", props);
        assertTrue(ds instanceof org.postgresql.jdbc2.optional.PoolingDataSource);
        assertEquals("dbserver.mydomain.org", ((org.postgresql.jdbc2.optional.PoolingDataSource) ds).getServerName());
        assertEquals("test", ((org.postgresql.jdbc2.optional.PoolingDataSource) ds).getDatabaseName());
        assertEquals(10, ((org.postgresql.jdbc2.optional.PoolingDataSource) ds).getMaxConnections());

    }

    public void testInvalidDataSource() throws Exception {
        Properties invalidProps = new Properties();
        invalidProps.put("db.test.dataSource", "org.postgresql.jdbc2.optional.PoolingDataSource");
        try {
            DataSource ds = ConnectionFactory.configureDataSource("db.notthere", invalidProps);
            fail("Expected: ClassNotFoundException");
        } catch (ClassNotFoundException e) {
        }
    }

    public void testInvalidDataSourceClass() throws Exception {
        Properties invalidProps = new Properties();
        invalidProps.put("db.test.dataSource", "org.class.that.cannot.be.Found");
        try {
            DataSource ds = ConnectionFactory.configureDataSource("db.test", invalidProps);
            fail("Expected: ClassNotFoundException");
        } catch (ClassNotFoundException e) {
        }
    }

    public void testInvalidMethod() throws Exception {
        Properties invalidProps = new Properties();
        invalidProps.put("db.test.dataSource", "org.postgresql.jdbc3.Jdbc3PoolingDataSource");
        invalidProps.put("db.test.someRubbish", "blahblahblah");
        try {
            DataSource ds = ConnectionFactory.configureDataSource("db.test", invalidProps);
        } catch (Exception e) {
            fail("Did not expect an exception to be thrown");
        }
    }

    public void testNullProperties() throws Exception {
        try {
            DataSource ds = ConnectionFactory.configureDataSource("db.test", null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testNullName() throws Exception {
        try {
            DataSource ds = ConnectionFactory.configureDataSource(null, new Properties());
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

}
