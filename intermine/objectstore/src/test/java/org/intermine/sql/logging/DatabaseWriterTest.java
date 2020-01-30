package org.intermine.sql.logging;

/*
 * Copyright (C) 2002-2020 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import junit.framework.TestCase;

import org.intermine.sql.DatabaseFactory;

public class DatabaseWriterTest extends TestCase
{
    Connection con;
    DatabaseWriter writer;

    public DatabaseWriterTest(String arg1) {
        super(arg1);
    }

    protected void setUp() throws Exception {
        con = DatabaseFactory.getDatabase("db.unittest").getConnection();
        con.setAutoCommit(false);
    }

    protected void tearDown() throws Exception {
        // return con to pool
        con.close();
    }

    private void createTable() throws Exception {
        Statement stmt = con.createStatement();
        stmt.execute("CREATE TABLE table1(col1 varchar(10), col2 varchar(10), col3 varchar(10))");
        con.commit();
    }

    private void dropTable() throws Exception {
        Statement stmt = con.createStatement();
        stmt.execute("DROP TABLE table1");
        con.commit();
    }

    private ResultSet getResults() throws Exception {
        Statement stmt = con.createStatement();
        return stmt.executeQuery("SELECT * FROM table1");
    }

    public void testCompleteRows() throws Exception {
        try {
            writer = new DatabaseWriter(con, "table1");
            createTable();
            writer.write("first\tsecond\tthird" + System.getProperty("line.separator")
                    + "fourth\tfifth\tsixth" + System.getProperty("line.separator"));

            ResultSet res = getResults();
            assertTrue(res.next());
            assertEquals("first", res.getString(1));
            assertEquals("second", res.getString(2));
            assertEquals("third", res.getString(3));
            assertTrue(res.next());
            assertEquals("fourth", res.getString(1));
            assertEquals("fifth", res.getString(2));
            assertEquals("sixth", res.getString(3));
            assertTrue(!(res.next()));
        } finally {
            dropTable();
        }
    }

    public void testShortRow() throws Exception {
        writer = new DatabaseWriter(con, "table1");
        createTable();
        try {
            writer.write("first\tsecond\tthird" + System.getProperty("line.separator")
                         + "fourth\tfifth" + System.getProperty("line.separator"));
            fail("Expected: IOException");
        }
        catch (IOException e) {
        }
        dropTable();
    }

    public void testLongRow() throws Exception {
        writer = new DatabaseWriter(con, "table1");
        createTable();
        try {
            writer.write("first\tsecond\tthird" + System.getProperty("line.separator")
                         + "fourth\tfifth\tsixth\tseventh" + System.getProperty("line.separator"));
            fail("Expected: IOException");
        }
        catch (IOException e) {
        }
        dropTable();
    }

    public void testPartialRows() throws Exception {
        writer = new DatabaseWriter(con, "table1");
        createTable();
        writer.write("first\tsecond\tthird" + System.getProperty("line.separator")
                     + "fourth\tfif");

        ResultSet res = getResults();
        assertTrue(res.next());
        assertEquals("first", res.getString(1));
        assertEquals("second", res.getString(2));
        assertEquals("third", res.getString(3));
        assertTrue(!(res.next()));
        dropTable();
    }

    public void testPartialRowsWithRestOnSecondWrite() throws Exception {
        writer = new DatabaseWriter(con, "table1");
        createTable();
        con.createStatement().execute("SELECT * FROM table1");
        writer.write("first\tsecond\tthird" + System.getProperty("line.separator")
                     + "fourth\tfif");
        con.createStatement().execute("SELECT * FROM table1");
        writer.write("th\tsixth" + System.getProperty("line.separator"));
        con.createStatement().execute("SELECT * FROM table1");

        ResultSet res = getResults();
        assertTrue(res.next());
        assertEquals("first", res.getString(1));
        assertEquals("second", res.getString(2));
        assertEquals("third", res.getString(3));
        assertTrue(res.next());
        assertEquals("fourth", res.getString(1));
        assertEquals("fifth", res.getString(2));
        assertEquals("sixth", res.getString(3));
        assertTrue(!(res.next()));
        dropTable();
    }
}
