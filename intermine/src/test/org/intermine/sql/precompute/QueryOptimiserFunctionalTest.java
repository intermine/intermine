package org.flymine.sql.precompute;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

import org.flymine.testing.sql.DatabaseTestCase;
import org.flymine.sql.Database;
import org.flymine.sql.DatabaseFactory;
import org.flymine.sql.query.*;
import org.flymine.util.DatabaseUtil;

public class QueryOptimiserFunctionalTest extends DatabaseTestCase
{
    protected Map queries = new HashMap();
    protected Map precomps = new HashMap();

    protected static final int DATA_SIZE = 1000;

    public QueryOptimiserFunctionalTest(String arg1) {
        super(arg1);
    }

    protected Database getDatabase() throws Exception {
        return DatabaseFactory.getDatabase("db.unittest");
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
        Random random = new Random(27278383973L);
        stmt.addBatch("CREATE TABLE table1(col1 int, col2 int)");
        for (int i = 1; i<=DATA_SIZE; i++) {
            stmt.addBatch("INSERT INTO table1 VALUES(" + i + ", " + (DATA_SIZE + 1 - i) + ")" );
        }
        stmt.addBatch("CREATE INDEX table1_index1 ON table1(col1)");
        stmt.addBatch("CREATE INDEX table1_index2 ON table1(col2)");
        stmt.addBatch("CREATE TABLE table2(col1 int, col2 int)");
        for (int i = 1; i<DATA_SIZE; i++) {
            stmt.addBatch("INSERT INTO table2 VALUES(" + i + ", " + random.nextInt(DATA_SIZE/10) + ")" );
        }
        stmt.addBatch("CREATE INDEX table2_index1 ON table2(col1)");
        stmt.addBatch("CREATE INDEX table2_index2 ON table2(col2)");
        stmt.addBatch("CREATE TABLE table3(col1 int, col2 int)");
        for (int i = 1; i<DATA_SIZE; i++) {
            stmt.addBatch("INSERT INTO table3 VALUES(" + i + ", " + random.nextInt(DATA_SIZE/10) + ")" );
        }
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
        queries.put("table2Table3groupByAvgHaving", "SELECT table2.col1, avg(table2.col2 + table3.col2) as xxx FROM table2, table3 WHERE table2.col1 = table3.col1 GROUP BY table2.col1 HAVING avg(table2.col2 + table3.col2) > " + (DATA_SIZE/5) + " ORDER BY table2.col1");
    }

    // Add some precomputed tables into the database
    public void setUpPrecomputedTables() throws Exception {

        precomps.put("precomp_countTable2", "SELECT table2.col2 AS table2_col2, count(*) AS c FROM table2 GROUP BY table2.col2 ORDER BY table2.col2");
        precomps.put("precomp_avgTable2Table3", "SELECT table2.col1 AS table2_col1, avg(table2.col2 + table3.col2) AS a FROM table2, table3 WHERE table2.col1 = table3.col1 GROUP BY table2.col1 ORDER BY table2.col1");
        precomps.put("precomp_table2Table3onCol2", "SELECT table2.col1 AS table2_col1, table2.col2 AS table2_col2, table3.col1 AS table3_col1, table3.col2 AS table3_col2 FROM table2, table3 WHERE table2.col2 = table3.col2 ORDER BY table2.col1, table2.col2, table3.col1, table3.col2");
        precomps.put("precomp_table2Table3onCol1", "SELECT table2.col1 AS table2_col1, table2.col2 AS table2_col2, table3.col1 AS table3_col1, table3.col2 AS table3_col2 FROM table2, table3 WHERE table2.col1 = table3.col1 ORDER BY table2.col1, table2.col2, table3.col1, table3.col2");
        precomps.put("precomp_halfOfTable2", "SELECT table2.col1 AS table2_col1, table2.col2 AS table2_col2 FROM table2 WHERE table2.col1 < " + (DATA_SIZE/2));
        precomps.put("precomp_halfOfTable3", "SELECT table3.col1 AS table3_col1, table3.col2 AS table3_col2 FROM table3 WHERE table3.col1 < " + (DATA_SIZE/2));

        PrecomputedTableManager ptm = PrecomputedTableManager.getInstance(getDatabase());
        Iterator precompsIter = precomps.keySet().iterator();
        while (precompsIter.hasNext()) {
            String name = (String) precompsIter.next();
            Query q = new Query((String) precomps.get(name));
            PrecomputedTable pt = new PrecomputedTable(q, name);
            ptm.add(pt);
        }

        // Set up table statistics for the database
        Connection con = getDatabase().getConnection();
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
        Iterator precompsIter = precomps.keySet().iterator();
        while (precompsIter.hasNext()) {
            String name = (String) precompsIter.next();
            Query q = new Query((String) precomps.get(name));
            PrecomputedTable pt = new PrecomputedTable(q, name);
            ptm.delete(pt);
        }
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
        Set precomps = PrecomputedTableManager.getInstance(getDatabase()).getPrecomputedTables();
        QueryOptimiser.recursiveOptimise(precomps, q, bestQuery, q);

        Set optimisedQueries = bestQuery.getQueries();
        // optimisedQueries now contains the set of queries we need to see all give the same results

        Iterator queriesIter = optimisedQueries.iterator();
        while (queriesIter.hasNext()) {
            Query optimisedQuery = (Query) queriesIter.next();
            checkResultsForQueries(q, optimisedQuery);
        }

        System.out.println(type + "(" + optimisedQueries.size() + " choices)");

        // Now try the optimise() method. We don't know which of the
        // set of queries we are actually going to get back, but we
        // can check that it takes less time than the original query.

        String optimisedQuery = QueryOptimiser.optimise(queryString, getDatabase());
        if (queryString.equals(optimisedQuery)) {
            System.out.println(": ORIGINAL ");
        } else {
            System.out.println(": OPTIMISED ");
        }
        checkResultsForQueries(q, new Query(optimisedQuery));

    }

    /**
     * Asserts that the results of two queries are the same
     */
    public void checkResultsForQueries(Query q1, Query q2) throws Exception {
        Connection con1 = null;
        Connection con2 = null;

        try {
            // The original query
            con1 = getDatabase().getConnection();
            Statement stmt1 = con1.createStatement();
            ResultSet rs1 = stmt1.executeQuery(q1.getSQLString());

            // The optimised query
            con2 = getDatabase().getConnection();
            Statement stmt2 = con2.createStatement();
            ResultSet rs2 = stmt2.executeQuery(q2.getSQLString());

            assertEquals(rs1, rs2);
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



}
