package org.flymine.util;

import junit.framework.*;

import java.sql.Connection;
import org.flymine.sql.ConnectionFactory;

public class DatabaseUtilTest extends TestCase
{
    private Connection con;

    public DatabaseUtilTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        con = ConnectionFactory.getConnection("db.unittest");
    }

    public void tearDown() throws Exception {
        con.close();
    }

    protected void createTable() throws Exception {
        con.createStatement().execute("CREATE TABLE table1(col1 int)");
        con.commit();
    }

    protected void dropTable() throws Exception {
        con.createStatement().execute("DROP TABLE table1");
        con.commit();
    }

    public void testTableExistsNullConnection() throws Exception {
        try {
            DatabaseUtil.tableExists(null, "table1");
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testTableExistsNullTable() throws Exception {
        try {
            DatabaseUtil.tableExists(ConnectionFactory.getConnection("db.unittest"), null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testTableExists() throws Exception {
        synchronized (con) {
            createTable();
            assertTrue(DatabaseUtil.tableExists(ConnectionFactory.getConnection("db.unittest"), "table1"));
            dropTable();
        }
    }

    public void testTableNotExists() throws Exception {
        synchronized (con) {
            createTable();
            assertTrue(!(DatabaseUtil.tableExists(ConnectionFactory.getConnection("db.unittest"), "table2")));
            dropTable();
        }
    }
}
