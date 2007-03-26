package org.intermine.sql.precompute;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;
import org.intermine.sql.DatabaseUtil;
import org.intermine.sql.query.*;
import org.intermine.util.Util;

public class PrecomputedTableManagerTest extends TestCase
{
    private PrecomputedTable pt1;
    private Database database;

    public PrecomputedTableManagerTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {

        database = DatabaseFactory.getDatabase("db.unittest");
        Query q1 = new Query();
        Table t = new Table("tabletest");
        Constant c = new Constant("50");
        Field f1 = new Field("col1", t);
        Field f2 = new Field("col2", t);
        SelectValue sv1 = new SelectValue(f1, null);
        SelectValue sv2 = new SelectValue(f2, null);
        q1.addFrom(t);
        q1.addSelect(sv1);
        q1.addSelect(sv2);
        q1.addWhere(new Constraint(f1, Constraint.LT, c));
        q1.addOrderBy(f1);

        Connection con = database.getConnection();
        pt1 = new PrecomputedTable(q1, q1.getSQLString(), "precomp1", "test", con);
        con.close();
    }

    protected void createTable() throws Exception {
        // Set up some tables in the database
        Connection con = database.getConnection();
        Statement stmt = con.createStatement();
        stmt.addBatch("CREATE TABLE tabletest(col1 int, col2 int)");
        for (int i = 1; i<100; i++) {
            stmt.addBatch("INSERT INTO tabletest VALUES(" + i + ", " + (101-i) + ")" );
        }
        stmt.executeBatch();
        con.commit();
        con.close();

    }

    protected void deleteTable() throws Exception {
        Connection con = database.getConnection();
        Statement stmt = con.createStatement();
        stmt.addBatch("DROP TABLE tabletest");
        stmt.addBatch("DROP TABLE precompute_index");
        stmt.executeBatch();
        con.commit();
        con.close();
    }

    public void testNullDatabase() throws Exception {
        try {
            PrecomputedTableManager ptm = new PrecomputedTableManager((Database) null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testValidDatabase() throws Exception {
        try {
            PrecomputedTableManager ptm = new PrecomputedTableManager(database);
        }
        catch (RuntimeException e) {
            fail("No exception should be thrown");
        }
    }

    public void testAddNull() throws Exception {
        try {
            PrecomputedTableManager ptm = new PrecomputedTableManager(database);
            ptm.add(null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testDeleteNullTable() throws Exception {
        try {
            PrecomputedTableManager ptm = new PrecomputedTableManager(database);
            ptm.delete((PrecomputedTable) null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testGetInstanceForNullDatabase() throws Exception {
        try {
            PrecomputedTableManager ptm = PrecomputedTableManager.getInstance((Database) null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testGetInstance() throws Exception {
        try {
            PrecomputedTableManager ptm1 = PrecomputedTableManager.getInstance(database);
            PrecomputedTableManager ptm2 = PrecomputedTableManager.getInstance(database);
            assertTrue(ptm1 == ptm2);
        } finally {
            // Make sure we don't muck up any tests in the future by having a PrecomputedTableManager lying around with no index table to match it.
            PrecomputedTableManager.instances = new HashMap();
        }
    }

    public void testAddDelete() throws Exception {
        synchronized (pt1) {
            Connection con = database.getConnection();
            PrecomputedTableManager ptm = new PrecomputedTableManager(database);
            try {
                createTable();
                ptm.add(pt1);
                assertTrue(ptm.getPrecomputedTables().contains(pt1));
                assertEquals(pt1, ptm.lookupSql("test", pt1.getOriginalSql()));
                assertTrue(DatabaseUtil.tableExists(con, "precomp1"));
                ptm.delete(pt1);
                assertTrue(!(ptm.getPrecomputedTables().contains(pt1)));
                assertNull(ptm.lookupSql("test", pt1.getOriginalSql()));
                assertTrue(!(DatabaseUtil.tableExists(con, "precomp1")));
            } catch (SQLException e) {
                throw (SQLException) Util.verboseException(e);
            } finally {
                deleteTable();
                con.close();
            }
        }
    }

    public void testAddDeleteWithConnection() throws Exception {
        synchronized (pt1) {
            Connection con = database.getConnection();
            PrecomputedTableManager ptm = new PrecomputedTableManager(con);
            try {
                createTable();
                ptm.add(pt1);
                assertTrue(ptm.getPrecomputedTables().contains(pt1));
                assertEquals(pt1, ptm.lookupSql("test", pt1.getOriginalSql()));
                assertTrue(DatabaseUtil.tableExists(con, "precomp1"));
                ptm.delete(pt1);
                assertTrue(!(ptm.getPrecomputedTables().contains(pt1)));
                assertNull(ptm.lookupSql("test", pt1.getOriginalSql()));
                assertTrue(!(DatabaseUtil.tableExists(con, "precomp1")));
            } catch (SQLException e) {
                throw (SQLException) Util.verboseException(e);
            } finally {
                deleteTable();
                con.close();
            }
        }
    }

    public void testAddInvalid() throws Exception {
        Connection c = database.getConnection();
        PrecomputedTableManager ptm = new PrecomputedTableManager(database);
        try {
            ptm.add(new PrecomputedTable(new Query("select table.blah from table"), "select table.blah from table", "precomp1", null, c));
            fail("Expected: SQLException");
        } catch (SQLException e) {
        } finally {
            c.close();
        }
    }

    public void testDeleteInvalid() throws Exception {
        Connection c = database.getConnection();
        PrecomputedTableManager ptm = new PrecomputedTableManager(database);
        try {
            ptm.delete(new PrecomputedTable(new Query(), "", "tablenotthere", null, c));
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        } finally {
            c.close();
        }
    }


    public void testExistingTables() throws Exception {
        synchronized (pt1) {
            PrecomputedTableManager ptm1 = new PrecomputedTableManager(database);
            try {
                createTable();
                ptm1.add(pt1);

                PrecomputedTableManager ptm2 = new PrecomputedTableManager(database);

                PrecomputedTable pt2 = (PrecomputedTable) ptm2.getPrecomputedTables().iterator().next();

                assertEquals(pt1, pt2);
            } catch (SQLException e) {
                throw (SQLException) Util.verboseException(e);
            } finally {
                ptm1.delete(pt1);
                deleteTable();
            }
        }
    }

    public void testCanonicaliseIndexes() throws Exception {
        Set indexes = new HashSet();
        indexes.add("a");
        indexes.add("b");
        indexes.add("a, b, c");
        indexes.add("a, c");
        indexes.add("a, b");

        Set expected = new HashSet();
        expected.add("a, b, c");
        expected.add("a, c");
        expected.add("b");

        Set got = PrecomputedTableManager.canonicaliseIndexes(indexes);

        assertEquals(expected, got);
    }

    public void testAddMultiple() throws Exception {
        synchronized (pt1) {
            Connection con = database.getConnection();
            PrecomputedTableManager ptm = new PrecomputedTableManager(database);
            try {
                createTable();
                ptm.add(pt1);
                assertTrue(ptm.getPrecomputedTables().contains(pt1));
                assertTrue(DatabaseUtil.tableExists(con, "precomp1"));
                PrecomputedTable pt2 = new PrecomputedTable(pt1.getQuery(), pt1.getQuery().getSQLString(), "precomp2", "test", con);
                ptm.add(pt2);
                assertFalse(ptm.getPrecomputedTables().contains(pt2));
                assertFalse(DatabaseUtil.tableExists(con, "precomp2"));
                ptm.delete(pt1);
                assertTrue(!(ptm.getPrecomputedTables().contains(pt1)));
                assertTrue(!(DatabaseUtil.tableExists(con, "precomp1")));
            } catch (SQLException e) {
                throw (SQLException) Util.verboseException(e);
            } finally {
                deleteTable();
                con.close();
            }
        }
    }
}
