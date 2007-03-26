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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.intermine.sql.DatabaseTestCase;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;
import org.intermine.sql.query.*;
import org.intermine.sql.writebatch.Batch;
import org.intermine.sql.writebatch.BatchWriterPostgresCopyImpl;
import org.intermine.util.StringUtil;

import org.apache.log4j.Logger;

public class QueryOptimiserFunctionalTest extends DatabaseTestCase
{
    private static final Logger LOG = Logger.getLogger(QueryOptimiserFunctionalTest.class);
    protected Map queries = new HashMap();
    protected Map precomps = new HashMap();
    protected PrecomputedTable toDelete;

    protected static final int DATA_SIZE = 1000;

    public QueryOptimiserFunctionalTest(String arg1) {
        super(arg1);
    }

    private Database database = null;
    protected Database getDatabase() throws Exception {
        if (database == null) {
            database = DatabaseFactory.getDatabase("db.unittest");
        }
        return database;
    }

    /**
     * Set up the test
     *
     * @throws Exception if an error occurs
     */
    public void setUp() throws Exception {
        super.setUp();

        setUpData();
        setUpPrecomputedTables();
        setUpQueries();
    }

    /**
     * Tear down the test
     *
     * @throws Exception if an error occurs
     */
    public void tearDown() throws Exception {
        tearDownData();
        tearDownPrecomputedTables();
        super.tearDown();
    }

    protected void setUpData() throws Exception {
        Connection con = getDatabase().getConnection();
        Statement stmt = con.createStatement();
        try {
            stmt.execute("DROP TABLE table1");
        } catch (SQLException e) {
            con.rollback();
        }
        try {
            stmt.execute("DROP TABLE table2");
        } catch (SQLException e) {
            con.rollback();
        }
        try {
            stmt.execute("DROP TABLE table3");
        } catch (SQLException e) {
            con.rollback();
        }
        try {
            stmt.execute("DROP TABLE table4");
        } catch (SQLException e) {
            con.rollback();
        }
        Batch batch = new Batch(new BatchWriterPostgresCopyImpl());
        Random random = new Random(27278383973L);
        stmt.addBatch("CREATE TABLE table1(col1 int, col2 int)");
        stmt.addBatch("CREATE TABLE table2(col1 int, col2 int)");
        stmt.addBatch("CREATE TABLE table3(col1 int, col2 int)");
        stmt.addBatch("CREATE TABLE table4(col1 int, col2 int, col3 int)");
        stmt.executeBatch();
        stmt.clearBatch();
        String colnames[] = new String[] {"col1", "col2"};
        for (int i = 1; i<=DATA_SIZE; i++) {
            batch.addRow(con, "table1", null, colnames, new Object[] {new Integer(i), new Integer(DATA_SIZE + 1 - i)});
            //stmt.addBatch("INSERT INTO table1 VALUES(" + i + ", " + (DATA_SIZE + 1 - i) + ")" );
        }
        for (int i = 1; i<DATA_SIZE; i++) {
            batch.addRow(con, "table2", null, colnames, new Object[] {new Integer(i), new Integer(random.nextInt(DATA_SIZE/10))});
            //stmt.addBatch("INSERT INTO table2 VALUES(" + i + ", " + random.nextInt(DATA_SIZE/10) + ")" );
        }
        for (int i = 1; i<DATA_SIZE; i++) {
            batch.addRow(con, "table3", null, colnames, new Object[] {new Integer(i), new Integer(random.nextInt(DATA_SIZE/10))});
            //stmt.addBatch("INSERT INTO table3 VALUES(" + i + ", " + random.nextInt(DATA_SIZE/10) + ")" );
        }
        colnames = new String[] {"col1", "col2", "col3"};
        for (int i = 0; i < 50000; i++) {
            batch.addRow(con, "table4", null, colnames, new Object[] {new Integer(random.nextInt(10)), new Integer(random.nextInt(10)), new Integer(random.nextInt(10))});
            //String sql = "INSERT INTO table4 VALUES(" + random.nextInt(10) + ", " + random.nextInt(10) + ", " + random.nextInt(10) + ")";
            //stmt.addBatch(sql);
        }
        batch.close(con);
        stmt.addBatch("CREATE INDEX table1_index1 ON table1(col1)");
        stmt.addBatch("CREATE INDEX table1_index2 ON table1(col2)");
        stmt.addBatch("CREATE INDEX table2_index1 ON table2(col1)");
        stmt.addBatch("CREATE INDEX table2_index2 ON table2(col2)");
        stmt.addBatch("CREATE INDEX table3_index1 ON table3(col1)");
        stmt.addBatch("CREATE INDEX table3_index2 ON table3(col2)");
        stmt.executeBatch();
        con.commit();
        con.close();
    }

    /**
     * Set up the set of queries we are testing
     *
     * @throws Exception if an error occurs
     */
    public void setUpQueries() throws Exception {
        queries.put("table2", "SELECT table2.col2 AS hello FROM table2 ORDER BY table2.col1");
        queries.put("table2LimitOffset", "SELECT table2.col2 AS hello FROM table2 ORDER BY table2.col1 LIMIT 20 OFFSET 10");
        queries.put("table2DifferentLimitOffset", "SELECT table2.col2 AS hello FROM table2 ORDER BY table2.col1 LIMIT 20 OFFSET 15");
        queries.put("table2Table3JoinOnCol1", "SELECT table2.col1, table2.col2, table3.col1, table3.col2 FROM table2, table3 WHERE table2.col1 = table3.col1 ORDER BY table2.col1, table2.col2, table3.col1, table3.col2");
        queries.put("halfOfTable2Table3JoinOnCol1", "SELECT table2.col1, table2.col2, table3.col1, table3.col2 FROM table2, table3 WHERE table2.col1 = table3.col1 AND table2.col1 < " + (DATA_SIZE/2) + " ORDER BY table2.col1, table2.col2, table3.col1, table3.col2");
        queries.put("table2Table3JoinOnCol2", "SELECT table2.col1, table2.col2, table3.col1, table3.col2 FROM table2, table3 WHERE table2.col2 = table3.col2 ORDER BY table2.col1, table2.col2, table3.col1, table3.col2");
        queries.put("table2HalfOfTable3JoinOnCol2", "SELECT table2.col1, table2.col2, table3.col1, table3.col2 FROM table2, table3 WHERE table2.col2 = table3.col2 AND table3.col1 < " + (DATA_SIZE/2) + " ORDER BY table2.col1, table2.col2, table3.col1, table3.col2");
        queries.put("table2WithGroupBy", "SELECT t2.col2 AS hello, count(*) as xxx FROM table2 t2 GROUP BY t2.col2 ORDER BY t2.col2");
        queries.put("halfOftable2halfOfTable3groupByCount", "SELECT table2.col2, count(*) as xxx FROM table2, table3 WHERE table2.col1 = table3.col1 AND table2.col1 < " + (DATA_SIZE/2) + " AND table3.col1 < " + (DATA_SIZE/2) + " GROUP BY table2.col2 ORDER BY table2.col2");
        queries.put("halfOftable2HalfOfTable3groupByAvg", "SELECT table2.col1, avg(table2.col2 + table3.col2) as xxx FROM table2, table3 WHERE table2.col1 = table3.col1 AND table2.col1 < " + (DATA_SIZE/2) + " AND table3.col1 < " + (DATA_SIZE/2) + " GROUP BY table2.col1 ORDER BY table2.col1");
        queries.put("table2Table3groupByAvg", "SELECT table2.col1, avg(table2.col2 + table3.col2) as xxx FROM table2, table3 WHERE table2.col1 = table3.col1 GROUP BY table2.col1 ORDER BY table2.col1");
        queries.put("table2Table3groupByAvgHaving", "SELECT table2.col1 AS wotsit, avg(table2.col2 + table3.col2) as xxx FROM table2, table3 WHERE table2.col1 = table3.col1 GROUP BY table2.col1 HAVING avg(table2.col2 + table3.col2) > " + (DATA_SIZE/5) + " ORDER BY table2.col1");
        queries.put("table4Sorted", "SELECT table4.col1, table4.col2, table4.col3 FROM table4 ORDER BY table4.col1, table4.col2, table4.col3");
        queries.put("inList", "SELECT table2.col1, table2.col2 FROM table2 WHERE table2.col2 IN (5, 7, 8, 12) AND table2.col1 < " + (DATA_SIZE/2) + " ORDER BY table2.col1, table2.col2");
    }

    // Add some precomputed tables into the database
    public void setUpPrecomputedTables() throws Exception {
        Connection con = getDatabase().getConnection();
        precomps.put("precomp_countTable2", "SELECT table2.col2 AS table2_col2, count(*) AS c FROM table2 GROUP BY table2.col2 ORDER BY table2.col2");
        precomps.put("precomp_avgTable2Table3", "SELECT table2.col1 AS table2_col1, avg(table2.col2 + table3.col2) AS a FROM table2, table3 WHERE table2.col1 = table3.col1 GROUP BY table2.col1 ORDER BY table2.col1");
        precomps.put("precomp_table2Table3onCol2", "SELECT table2.col1 AS table2_col1, table2.col2 AS table2_col2, table3.col1 AS table3_col1, table3.col2 AS table3_col2 FROM table2, table3 WHERE table2.col2 = table3.col2 ORDER BY table2.col1, table2.col2, table3.col1, table3.col2");
        precomps.put("precomp_table2Table3onCol1", "SELECT table2.col1 AS table2_col1, table2.col2 AS table2_col2, table3.col1 AS table3_col1, table3.col2 AS table3_col2 FROM table2, table3 WHERE table2.col1 = table3.col1 ORDER BY table2.col1, table2.col2, table3.col1, table3.col2");
        precomps.put("precomp_halfOfTable2", "SELECT table2.col1 AS table2_col1, table2.col2 AS table2_col2 FROM table2 WHERE table2.col1 < " + (DATA_SIZE/2));
        precomps.put("precomp_halfOfTable3", "SELECT table3.col1 AS table3_col1, table3.col2 AS table3_col2 FROM table3 WHERE table3.col1 < " + (DATA_SIZE/2));
        precomps.put("precomp_table4", "SELECT table4.col1 AS col1, table4.col2 AS col2, table4.col3 AS col3 FROM table4 ORDER BY table4.col1, table4.col3, table4.col2");

        PrecomputedTableManager ptm = PrecomputedTableManager.getInstance(getDatabase());
        Iterator precompsIter = precomps.keySet().iterator();
        while (precompsIter.hasNext()) {
            String name = (String) precompsIter.next();
            Query q = new Query((String) precomps.get(name));
            PrecomputedTable pt = new PrecomputedTable(q, q.getSQLString(), name, "test", con);
            if ("precomp_table2Table3onCol1".equals(name)) {
                toDelete = pt;
            }
            ptm.add(pt);
        }

        // Set up table statistics for the database
        con.setAutoCommit(true);
        Statement stmt = con.createStatement();
        stmt.execute("ANALYZE");
        con.close();
    }

    public void tearDownData() throws Exception {
        Connection con = getDatabase().getConnection();
        Statement stmt = con.createStatement();
        stmt.addBatch("DROP TABLE table1");
        stmt.addBatch("DROP TABLE table2");
        stmt.addBatch("DROP TABLE table3");
        stmt.executeBatch();
        con.commit();
        con.close();
    }

    public void tearDownPrecomputedTables() throws Exception {
        PrecomputedTableManager ptm = PrecomputedTableManager.getInstance(getDatabase());
        ptm.dropEverything();
    }


    /**
     * Execute a test for a query. This should run the query and
     * contain an assert call to assert that the returned results are
     * those expected.
     *
     * @param type the type of query we are testing (ie. the key in the queries Map)
     * @throws Exception if type does not appear in the queries map
     */
    public void executeTest(String type) throws Exception {
        String queryString = (String) queries.get(type);
        Query q = new Query(queryString);

        BestQueryStorer bestQuery = new BestQueryStorer();
        Set precompsSet = PrecomputedTableManager.getInstance(getDatabase()).getPrecomputedTables();
        QueryOptimiser.recursiveOptimise(precompsSet, q, bestQuery, q);

        Set optimisedQueries = bestQuery.getQueries();
        // optimisedQueries now contains the set of queries we need to see all give the same results

        System.out.println(type + "(" + optimisedQueries.size() + " choices): " + optimisedQueries);

        Iterator queriesIter = optimisedQueries.iterator();
        while (queriesIter.hasNext()) {
            Query optimisedQuery = (Query) queriesIter.next();
            checkResultsForQueries(q, optimisedQuery);
        }

        // Now try the optimise() method. We don't know which of the
        // set of queries we are actually going to get back, but we
        // can check that it takes less time than the original query.

        String optimisedQuery = QueryOptimiser.optimise(queryString, getDatabase());
        if (queryString.equals(optimisedQuery)) {
            System.out.println(": ORIGINAL " + optimisedQuery);
        } else {
            System.out.println(": OPTIMISED " + queryString + " -> " + optimisedQuery);
        }
        checkResultsForQueries(q, new Query(optimisedQuery));
    }

    /**
     * Asserts that the results of two queries are the same
     */
    public void checkResultsForQueries(Query q1, Query q2) throws Exception {
        Connection con1 = null;
        Connection con2 = null;
        String sql = null;
        
        try {
            // The original query
            con1 = getDatabase().getConnection();
            Statement stmt1 = con1.createStatement();
            sql = q1.getSQLString();
            ResultSet rs1 = stmt1.executeQuery(q1.getSQLString());

            // The optimised query
            con2 = getDatabase().getConnection();
            Statement stmt2 = con2.createStatement();
            sql = q2.getSQLString();
            ResultSet rs2 = stmt2.executeQuery(q2.getSQLString());

            assertEquals("Results for queries do not match: Q1 = \"" + q1.getSQLString() + "\", Q2 = \"" + q2.getSQLString() + "\"", rs1, rs2);
        } catch (SQLException e) {
            SQLException e2 = new SQLException(sql);
            e2.initCause(e);
            throw e2;
        } finally {
            con1.close();
            con2.close();
        }
    }


    /**
     * Test the queries produce the appropriate result
     *
     * @throws Exception if an error occurs
     */
    public void testQueries() throws Throwable {
        Iterator i = queries.keySet().iterator();
        while (i.hasNext()) {
            String type = (String) i.next();
            try {
                executeTest(type);
            } catch (Throwable t) {
                throw new Throwable("Failed on " + type, t);
            }
        }
    }

    public void testCacheFlush() throws Throwable {
        StringUtil.setNextUniqueNumber(35);
        String sql1 = "SELECT table2.col1, table2.col2, table3.col1, table3.col2 FROM table2, table3 WHERE table2.col1 = table3.col1 ORDER BY table2.col1, table2.col2, table3.col1, table3.col2";
        //String sql1 = "SELECT table3.col1 AS table3_col1, table3.col2 AS table3_col2 FROM table3 WHERE table3.col1 < " + (DATA_SIZE/2);
        String sqlOpt1 = QueryOptimiser.optimise(sql1, getDatabase());
        assertEquals("SELECT P35.table2_col1 AS col1, P35.table2_col2 AS col2, P35.table3_col1 AS col1, P35.table3_col2 AS col2 FROM precomp_table2Table3onCol1 AS P35 ORDER BY P35.orderby_field", sqlOpt1);
        PrecomputedTableManager ptm = PrecomputedTableManager.getInstance(getDatabase());
        ptm.delete(toDelete);
        String sqlOpt2 = QueryOptimiser.optimise(sql1, getDatabase());
        assertEquals(sql1, sqlOpt2);
    }
}
