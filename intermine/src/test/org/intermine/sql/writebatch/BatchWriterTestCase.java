package org.intermine.sql.writebatch;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;

import junit.framework.*;

/**
 * TestCase for doing tests on the BatchWriter system.
 *
 * @author Matthew Wakeling
 */
public abstract class BatchWriterTestCase extends TestCase
{
    public BatchWriterTestCase(String arg) {
        super(arg);
    }

    public void testOpsWithId() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        try {
            Statement s = con.createStatement();
            try {
                s.execute("DROP TABLE table1");
            } catch (SQLException e) {
                con.rollback();
            }
            s.addBatch("CREATE TABLE table1(col1 int, col2 int)");
            s.addBatch("INSERT INTO table1 VALUES (11, 101)");
            s.addBatch("INSERT INTO table1 VALUES (12, 102)");
            s.addBatch("INSERT INTO table1 VALUES (22, 202)");
            s.addBatch("INSERT INTO table1 VALUES (32, 302)");
            s.addBatch("INSERT INTO table1 VALUES (13, 103)");
            s.addBatch("INSERT INTO table1 VALUES (23, 203)");
            s.addBatch("INSERT INTO table1 VALUES (33, 303)");
            s.executeBatch();
            con.commit();
            s = null;
            BatchWriter writer = getWriter();
            Batch batch = new Batch(writer);
            String colNames[] = new String[] {"col1", "col2"};
            batch.addRow(con, "table1", new Integer(14), colNames, new Object[] {new Integer(14), new Integer(104)});
            batch.addRow(con, "table1", new Integer(15), colNames, new Object[] {new Integer(15), new Integer(105)});
            batch.addRow(con, "table1", new Integer(25), colNames, new Object[] {new Integer(25), new Integer(205)});
            batch.addRow(con, "table1", new Integer(35), colNames, new Object[] {new Integer(35), new Integer(305)});
            batch.deleteRow(con, "table1", "col1", new Integer(11));
            batch.deleteRow(con, "table1", "col1", new Integer(12));
            batch.deleteRow(con, "table1", "col1", new Integer(22));
            batch.deleteRow(con, "table1", "col1", new Integer(32));
            batch.deleteRow(con, "table1", "col1", new Integer(14));
            batch.deleteRow(con, "table1", "col1", new Integer(24));
            batch.deleteRow(con, "table1", "col1", new Integer(34));
            batch.addRow(con, "table1", new Integer(12), colNames, new Object[] {new Integer(12), new Integer(112)});
            batch.addRow(con, "table1", new Integer(22), colNames, new Object[] {new Integer(22), new Integer(212)});
            batch.addRow(con, "table1", new Integer(32), colNames, new Object[] {new Integer(32), new Integer(312)});
            batch.flush(con);
            con.commit();
            s = con.createStatement();
            ResultSet r = s.executeQuery("SELECT col1, col2 FROM table1");
            Map got = new TreeMap();
            while (r.next()) {
                got.put(r.getObject(1), r.getObject(2));
            }
            Map expected = new TreeMap();
            expected.put(new Integer(12), new Integer(112));
            expected.put(new Integer(22), new Integer(212));
            expected.put(new Integer(32), new Integer(312));
            expected.put(new Integer(13), new Integer(103));
            expected.put(new Integer(23), new Integer(203));
            expected.put(new Integer(33), new Integer(303));
            expected.put(new Integer(15), new Integer(105));
            expected.put(new Integer(25), new Integer(205));
            expected.put(new Integer(35), new Integer(305));
            assertEquals(expected, got);
            batch.addRow(con, "table1", new Integer(42), colNames, new Object[] {new Integer(42), new Integer(402)});
            batch.addRow(con, "table1", new Integer(52), colNames, new Object[] {new Integer(52), new Integer(502)});
            batch.addRow(con, "table1", new Integer(53), colNames, new Object[] {new Integer(53), new Integer(503)});
            batch.addRow(con, "table1", new Integer(55), colNames, new Object[] {new Integer(55), new Integer(505)});
            batch.deleteRow(con, "table1", "col1", new Integer(12));
            batch.deleteRow(con, "table1", "col1", new Integer(13));
            batch.deleteRow(con, "table1", "col1", new Integer(15));
            batch.deleteRow(con, "table1", "col1", new Integer(22));
            batch.deleteRow(con, "table1", "col1", new Integer(23));
            batch.deleteRow(con, "table1", "col1", new Integer(25));
            batch.deleteRow(con, "table1", "col1", new Integer(42));
            batch.deleteRow(con, "table1", "col1", new Integer(43));
            batch.deleteRow(con, "table1", "col1", new Integer(45));
            batch.addRow(con, "table1", new Integer(22), colNames, new Object[] {new Integer(22), new Integer(222)});
            batch.addRow(con, "table1", new Integer(23), colNames, new Object[] {new Integer(23), new Integer(223)});
            batch.addRow(con, "table1", new Integer(25), colNames, new Object[] {new Integer(25), new Integer(225)});
            batch.close(con);
            con.commit();
            s = con.createStatement();
            r = s.executeQuery("SELECT col1, col2 FROM table1");
            got = new TreeMap();
            while (r.next()) {
                got.put(r.getObject(1), r.getObject(2));
            }
            expected = new TreeMap();
            expected.put(new Integer(22), new Integer(222));
            expected.put(new Integer(23), new Integer(223));
            expected.put(new Integer(25), new Integer(225));
            expected.put(new Integer(32), new Integer(312));
            expected.put(new Integer(33), new Integer(303));
            expected.put(new Integer(35), new Integer(305));
            expected.put(new Integer(52), new Integer(502));
            expected.put(new Integer(53), new Integer(503));
            expected.put(new Integer(55), new Integer(505));
            assertEquals(expected, got);
            s = con.createStatement();
            r = s.executeQuery("SELECT col1, col2 FROM table1");
            got = new TreeMap();
            while (r.next()) {
                got.put(r.getObject(1), r.getObject(2));
            }
            assertEquals(expected, got);
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            while (e != null) {
                e.printStackTrace(pw);
                e = e.getNextException();
            }
            pw.flush();
            throw new Exception(sw.toString());
        } finally {
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table1");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }

    public void testInsertOnly() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        try {
            Statement s = con.createStatement();
            try {
                s.execute("DROP TABLE table1");
            } catch (SQLException e) {
                con.rollback();
            }
            s.addBatch("CREATE TABLE table1(col1 int, col2 int)");
            s.addBatch("INSERT INTO table1 VALUES (1, 201)");
            s.executeBatch();
            con.commit();
            s = null;
            BatchWriter writer = getWriter();
            Batch batch = new Batch(writer);
            String colNames[] = new String[] {"col1", "col2"};
            batch.addRow(con, "table1", null, colNames, new Object[] {new Integer(2), new Integer(202)});
            batch.addRow(con, "table1", null, colNames, new Object[] {new Integer(3), new Integer(203)});
            batch.addRow(con, "table1", null, colNames, new Object[] {new Integer(4), new Integer(204)});
            batch.close(con);
            con.commit();
            s = con.createStatement();
            ResultSet r = s.executeQuery("SELECT col1, col2 FROM table1");
            Map got = new TreeMap();
            while (r.next()) {
                got.put(r.getObject(1), r.getObject(2));
            }
            Map expected = new TreeMap();
            expected.put(new Integer(1), new Integer(201));
            expected.put(new Integer(2), new Integer(202));
            expected.put(new Integer(3), new Integer(203));
            expected.put(new Integer(4), new Integer(204));
            assertEquals(expected, got);
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            while (e != null) {
                e.printStackTrace(pw);
                e = e.getNextException();
            }
            pw.flush();
            throw new Exception(sw.toString());
        } finally {
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table1");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }

    public void testDeleteOnly() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        try {
            Statement s = con.createStatement();
            try {
                s.execute("DROP TABLE table1");
            } catch (SQLException e) {
                con.rollback();
            }
            s.addBatch("CREATE TABLE table1(col1 int, col2 int)");
            s.addBatch("INSERT INTO table1 VALUES (1, 201)");
            s.addBatch("INSERT INTO table1 VALUES (2, 202)");
            s.addBatch("INSERT INTO table1 VALUES (3, 203)");
            s.addBatch("INSERT INTO table1 VALUES (4, 204)");
            s.addBatch("INSERT INTO table1 VALUES (5, 205)");
            s.executeBatch();
            con.commit();
            s = null;
            BatchWriter writer = getWriter();
            Batch batch = new Batch(writer);
            batch.deleteRow(con, "table1", "col1", new Integer(2));
            batch.deleteRow(con, "table1", "col1", new Integer(4));
            batch.close(con);
            con.commit();
            s = con.createStatement();
            ResultSet r = s.executeQuery("SELECT col1, col2 FROM table1");
            Map got = new TreeMap();
            while (r.next()) {
                got.put(r.getObject(1), r.getObject(2));
            }
            Map expected = new TreeMap();
            expected.put(new Integer(1), new Integer(201));
            expected.put(new Integer(3), new Integer(203));
            expected.put(new Integer(5), new Integer(205));
            assertEquals(expected, got);
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            while (e != null) {
                e.printStackTrace(pw);
                e = e.getNextException();
            }
            pw.flush();
            throw new Exception(sw.toString());
        } finally {
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table1");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }

    public void testTypes() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        try {
            Statement s = con.createStatement();
            try {
                s.execute("DROP TABLE table1");
            } catch (SQLException e) {
                con.rollback();
            }
            s.addBatch("CREATE TABLE table1(key int, int2 smallint, int4 int, int8 bigint, float real, double float, bool boolean, bigdecimal numeric, string text)");
            s.executeBatch();
            con.commit();
            s = null;
            BatchWriter writer = getWriter();
            Batch batch = new Batch(writer);
            batch.addRow(con, "table1", new Integer(1), new String[] {"key", "int2", "int4", "int8", "float", "double", "bool", "bigdecimal", "string"},
                    new Object[] {new Integer(1),
                        new Short((short) 45),
                        new Integer(765234),
                        new Long(86523876513242L),
                        new Float(5.45),
                        new Double(7632.234134),
                        Boolean.TRUE,
                        new BigDecimal("982413415465245.87639871238764321"),
                        "kjhlasdurhe"});
            batch.close(con);
            con.commit();
            s = con.createStatement();
            ResultSet r = s.executeQuery("SELECT key, int2, int4, int8, float, double, bool, bigdecimal, string FROM table1");
            Map got = new TreeMap();
            while (r.next()) {
                for (int i = 1; i <= 8; i++) {
                    got.put(new Integer(r.getInt(1) * 10 + i), r.getObject(i + 1));
                }
            }
            Map expected = new TreeMap();
            expected.put(new Integer(11), new Short((short) 45));
            expected.put(new Integer(12), new Integer(765234));
            expected.put(new Integer(13), new Long(86523876513242L));
            expected.put(new Integer(14), new Float(5.45));
            expected.put(new Integer(15), new Double(7632.234134));
            expected.put(new Integer(16), Boolean.TRUE);
            expected.put(new Integer(17), new BigDecimal("982413415465245.87639871238764321"));
            expected.put(new Integer(18), "kjhlasdurhe");
            assertEquals(expected, got);
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            while (e != null) {
                e.printStackTrace(pw);
                e = e.getNextException();
            }
            pw.flush();
            throw new Exception(sw.toString());
        } finally {
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table1");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }

    /*
    public void testPerformance() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        try {
            Statement s = con.createStatement();
            try {
                s.execute("DROP TABLE table1");
            } catch (SQLException e) {
                con.rollback();
            }
            s.addBatch("CREATE TABLE table1(key int, int4 int)");
            s.executeBatch();
            con.commit();
            s = null;
            BatchWriter writer = getWriter();
            Batch batch = new Batch(writer);
            String[] colNames = new String[] {"key", "int4"};
            for (int i = 0; i < 100000; i++) {
                batch.addRow(con, "table1", new Integer(i), colNames, new Object[] {new Integer(i), new Integer(765234 * i)});
            }
            batch.close(con);
            con.commit();
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            while (e != null) {
                e.printStackTrace(pw);
                e = e.getNextException();
            }
            pw.flush();
            throw new Exception(sw.toString());
        } finally {
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table1");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }
*/

    public void testUTF() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        try {
            Statement s = con.createStatement();
            try {
                s.execute("DROP TABLE table1");
            } catch (SQLException e) {
                con.rollback();
            }
            s.addBatch("CREATE TABLE table1(key text)");
            s.executeBatch();
            con.commit();
            s = null;
            BatchWriter writer = getWriter();
            Batch batch = new Batch(writer);
            batch.addRow(con, "table1", null, new String[] {"key"}, new Object[] {"Flibble\u00A0fds\u786f"});
            batch.close(con);
            con.commit();
            s = con.createStatement();
            ResultSet r = s.executeQuery("SELECT key FROM table1");
            assertTrue(r.next());
            assertEquals("Flibble\u00A0fds\u786f", r.getString(1));
            assertFalse(r.next());
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            while (e != null) {
                e.printStackTrace(pw);
                e = e.getNextException();
            }
            pw.flush();
            throw new Exception(sw.toString());
        } finally {
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table1");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }

    public void testIndirections() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        try {
            Statement s = con.createStatement();
            try {
                s.execute("DROP TABLE table1");
            } catch (SQLException e) {
                con.rollback();
            }
            s.addBatch("CREATE TABLE table1(a int, b int)");
            s.executeBatch();
            con.commit();
            s = null;
            BatchWriter writer = getWriter();
            Batch batch = new Batch(writer);
            batch.addRow(con, "table1", "a", "b", 1, 2);
            batch.flush(con);
            con.commit();
            Set expected = new HashSet();
            expected.add(new Row(1, 2));
            assertEquals(expected, getGot(con));

            batch.deleteRow(con, "table1", "a", "b", 1, 2);
            batch.flush(con);
            con.commit();
            expected.remove(new Row(1, 2));
            assertEquals(expected, getGot(con));

            batch.addRow(con, "table1", "a", "b", 1, 2);
            batch.deleteRow(con, "table1", "a", "b", 1, 2);
            batch.flush(con);
            con.commit();
            assertEquals(expected, getGot(con));

            batch.deleteRow(con, "table1", "a", "b", 1, 2);
            batch.addRow(con, "table1", "a", "b", 1, 2);
            batch.flush(con);
            con.commit();
            expected.add(new Row(1, 2));
            assertEquals(expected, getGot(con));

            batch.addRow(con, "table1", "a", "b", 1, 2);
            batch.addRow(con, "table1", "a", "b", 1, 2);
            batch.addRow(con, "table1", "a", "b", 1, 2);
            batch.addRow(con, "table1", "a", "b", 1, 2);
            batch.addRow(con, "table1", "a", "b", 1, 3);
            batch.flush(con);
            con.commit();
            expected.add(new Row(1, 2));
            expected.add(new Row(1, 3));
            assertEquals(expected, getGot(con));

            batch.deleteRow(con, "table1", "a", "b", 1, 2);
            batch.close(con);
            con.commit();
            expected.remove(new Row(1, 2));
            assertEquals(expected, getGot(con));
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            while (e != null) {
                e.printStackTrace(pw);
                e = e.getNextException();
            }
            pw.flush();
            throw new Exception(sw.toString());
        } finally {
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table1");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }

    public void testManyDeletes() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        try {
            Statement s = con.createStatement();
            try {
                s.execute("DROP TABLE table1");
            } catch (SQLException e) {
                con.rollback();
            }
            s.addBatch("CREATE TABLE table1(a int, b int)");
            s.addBatch("CREATE INDEX table1_key on table1(a, b)");
            s.executeBatch();
            con.commit();
            s = null;
            BatchWriter writer = getWriter();
            Batch batch = new Batch(writer);
            String colNames[] = new String[] {"a", "b"};
            for (int i = 0; i < 10000; i++) {
                batch.addRow(con, "table1", new Integer(i), colNames, new Object[] {new Integer(i), new Integer(i * 2876123)});
            }
            batch.flush(con);
            con.commit();
            con.createStatement().execute("ANALYSE");
            for (int i = 0; i < 10000; i++) {
                batch.deleteRow(con, "table1", "a", new Integer(i));
            }
            batch.flush(con);
            con.commit();
            Set expected = new HashSet();
            assertEquals(expected, getGot(con));
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            while (e != null) {
                e.printStackTrace(pw);
                e = e.getNextException();
            }
            pw.flush();
            throw new Exception(sw.toString());
        } finally {
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table1");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }
    
    public void testManyIndirectionDeletes() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        try {
            Statement s = con.createStatement();
            try {
                s.execute("DROP TABLE table1");
            } catch (SQLException e) {
                con.rollback();
            }
            s.addBatch("CREATE TABLE table1(a int, b int)");
            s.addBatch("CREATE INDEX table1_key on table1(a, b)");
            s.executeBatch();
            con.commit();
            s = null;
            BatchWriter writer = getWriter();
            Batch batch = new Batch(writer);
            for (int i = 0; i < 10000; i++) {
                batch.addRow(con, "table1", "a", "b", i, i * 2876123);
            }
            batch.flush(con);
            con.commit();
            con.createStatement().execute("ANALYSE");
            for (int i = 0; i < 10000; i++) {
                batch.deleteRow(con, "table1", "a", "b", i, i * 2876123);
            }
            batch.flush(con);
            con.commit();
            Set expected = new HashSet();
            assertEquals(expected, getGot(con));
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            while (e != null) {
                e.printStackTrace(pw);
                e = e.getNextException();
            }
            pw.flush();
            throw new Exception(sw.toString());
        } finally {
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table1");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }
    
    private Set getGot(Connection con) throws SQLException {
        Statement s = con.createStatement();
        ResultSet r = s.executeQuery("SELECT a, b FROM table1");
        Set set = new HashSet();
        while (r.next()) {
            set.add(new Row(r.getInt(1), r.getInt(2)));
        }
        return set;
    }

    public abstract BatchWriter getWriter();
}
