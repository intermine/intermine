package org.flymine.util;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import java.sql.Connection;
import org.flymine.sql.DatabaseFactory;

public class DatabaseUtilTest extends TestCase
{
    private Connection con;

    public DatabaseUtilTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        con = DatabaseFactory.getDatabase("db.unittest").getConnection();
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
            DatabaseUtil.tableExists(DatabaseFactory.getDatabase("db.unittest").getConnection(), null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testTableExists() throws Exception {
        synchronized (con) {
            createTable();
            assertTrue(DatabaseUtil.tableExists(DatabaseFactory.getDatabase("db.unittest").getConnection(), "table1"));
            dropTable();
        }
    }

    public void testTableNotExists() throws Exception {
        synchronized (con) {
            createTable();
            assertTrue(!(DatabaseUtil.tableExists(DatabaseFactory.getDatabase("db.unittest").getConnection(), "table2")));
            dropTable();
        }
    }
}
