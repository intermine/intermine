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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.intermine.sql.query.ExplainResult;
import org.intermine.sql.query.Query;

/**
 * Gets the database to explain each Query added and keeps hold of the best one so far.
 *
 * @author Andrew Varley
 */
public class BestQueryExplainer extends BestQuery
{
    private static final int OVERHEAD = 300;
    protected static final int ALWAYS_EXPLAIN_TABLES = 3;
    protected static final int NEVER_EXPLAIN_TABLES = 8;

    protected List candidates = new ArrayList();
    protected int candidateTables = Integer.MAX_VALUE;
    protected Candidate bestCandidate;
    protected Connection con;
    protected Date start = new Date();
    protected long timeLimit = 0;

    /**
     * Constructs an empty BestQueryExplainer for testing purposes
     *
     */
    public BestQueryExplainer() {
        super();
        timeLimit = -1;
    }

    /**
     * Constructs a BestQueryExplainer that will use the given Connection to explain Queries.
     *
     * @param con the Connection to use
     * @param timeLimit a time limit in milliseconds
     */
    public BestQueryExplainer(Connection con, long timeLimit) {
        if (con == null) {
            throw (new NullPointerException());
        }
        this.con = con;
        this.timeLimit = timeLimit;
    }

    /**
     * Allows a Query to be added to this tracker.
     *
     * @param q a Query to be added to the tracker
     * @throws BestQueryException if the current best Query is the best we think we are going to get
     * @throws SQLException if error occurs in the underlying database
     */
    @Override
    public void add(Query q) throws BestQueryException, SQLException {

        Candidate c = new Candidate(q);
        add(c);
    }

    /**
     * Allows a Query to be added to this tracker.
     *
     * @param q a query String to be added to the tracker
     * @throws BestQueryException if the current best Query is the best we think we are going to get
     * @throws SQLException if error occurs in the underlying database
     */
    @Override
    public void add(String q) throws BestQueryException, SQLException {

        Candidate c = new Candidate(q);
        add(c);
    }

    /**
     * Allows a Candidate to be added to this tracker.
     *
     * @param c the Candidate
     * @throws BestQueryException if the current best Candidate is the best we think we are going to
     * get
     * @throws SQLException if an error occurs in the underlying database
     */
    protected void add(Candidate c) throws BestQueryException, SQLException {
        int tableCount = c.getTableCount();
        boolean doExplain = (tableCount <= ALWAYS_EXPLAIN_TABLES);
        if (tableCount < candidateTables) {
            candidateTables = tableCount;
            candidates.clear();
            if (tableCount < NEVER_EXPLAIN_TABLES) {
                doExplain = true;
            }
        }
        if (doExplain) {
            if (c.betterThan(bestCandidate)) {
                bestCandidate = c;
            }
        } else {
            didNotExplain(c);
            if (tableCount == candidateTables) {
                candidates.add(c);
            }
        }

        long elapsed = System.currentTimeMillis() - start.getTime();
        if ((timeLimit >= 0) && (elapsed > timeLimit)) {
            throw new BestQueryException("Optimiser reached time limit (limit = " + timeLimit
                    + "ms, elapsed = " + elapsed + "ms)");
        }
        if (bestCandidate != null) {
            // throw BestQueryException if the bestQuery will take less time to run than the
            // amount of time we have spent optimising so far
            if (bestCandidate.getExplain().getTime() < (elapsed + OVERHEAD)) {
                throw (new BestQueryException("Explain time: "
                            + bestCandidate.getExplain().getTime() + ", elapsed time: "
                            + elapsed));
            }
        }
    }

    /**
     * Internal method that creates an ExplainResult. It can be overridden by subclasses.
     *
     * @param q the Query
     * @return an ExplainResult
     * @throws SQLException if an error occurs in the underlying database
     */
    protected ExplainResult getExplainResult(Query q) throws SQLException {
        return ExplainResult.getInstance(q, con);
    }

    /**
     * Internal method that creates an ExplainResult. It can be overridden by subclasses.
     *
     * @param q the query String
     * @return an ExplainResult
     * @throws SQLException if an error occurs in the underlying database
     */
    protected ExplainResult getExplainResult(String q) throws SQLException {
        return ExplainResult.getInstance(q, con);
    }

    /**
     * Internal method that records that a query was not explained. It can be overridden by
     * subclasses.
     *
     * @param c the Candidate
     */
    protected void didNotExplain(@SuppressWarnings("unused") Candidate c) {
        // empty
    }

    /**
     * Gets the best Query found so far
     *
     * @return the best Query, or null if no Queries added to this object
     * @throws SQLException if an error occurs in the underlying database
     */
    @Override
    public Query getBestQuery() throws SQLException {
        Candidate best = getBest();
        if (best == null) {
            return null;
        }
        return best.getQuery();
    }

    /**
     * Gets the best query String found so far.
     *
     * @return the best Query, or null if no Queries added to this object
     * @throws SQLException if an error occurs in the underlying database
     */
    @Override
    public String getBestQueryString() throws SQLException {
        Candidate best = getBest();
        if (best == null) {
            return null;
        }
        return best.getQueryString();
    }

    /**
     * Gets the ExpainResult for the best Query found so far.
     *
     * @return the best ExplainResult, or null if no Queries added to this object
     * @throws SQLException if an error occurs in the underlying database
     */
    public ExplainResult getBestExplainResult() throws SQLException {
        Candidate best = getBest();
        if (best == null) {
            return null;
        }
        return best.getExplain();
    }

    /**
     * Gets the best Candidate found so far.
     *
     * @return the best Candidate
     * @throws SQLException if an error occurs in the underlying database
     */
    protected Candidate getBest() throws SQLException {
        Iterator iter = candidates.iterator();
        while (iter.hasNext()) {
            if (bestCandidate != null) {
                long elapsed = System.currentTimeMillis() - start.getTime();
                if ((timeLimit >= 0) && (elapsed > timeLimit)) {
                    //System.out .println("QueryOptimiser: bailing out early: Time limit reached");
                    return bestCandidate;
                }
                if (bestCandidate.getExplain().getTime() < (elapsed + OVERHEAD)) {
                    //System.out .println("QueryOptimiser: bailing out early: Explain time: "
                    //        + bestCandidate.getExplain().getTime() + ", elapsed time: " + elapsed
                    //        + ", time limit: " + timeLimit);
                    return bestCandidate;
                }
            }
            Candidate c = (Candidate) iter.next();
            iter.remove();
            if (c.betterThan(bestCandidate)) {
                bestCandidate = c;
            }
        }
        return bestCandidate;
    }

    /**
     * A class representing a candidate optimised query.
     */
    protected class Candidate
    {
        protected String queryString;
        protected Query query;
        protected ExplainResult explainResult = null;
        protected int tableCount;

        /**
         * Constructor.
         *
         * @param queryString an SQL query String
         */
        public Candidate(String queryString) {
            this.queryString = queryString;
            this.query = null;
            String afterFrom = queryString.substring(queryString.indexOf(" FROM ") + 6);
            int wherePos = afterFrom.indexOf(" WHERE ");
            if (wherePos == -1) {
                wherePos = afterFrom.indexOf(" GROUP BY ");
            }
            if (wherePos == -1) {
                wherePos = afterFrom.indexOf(" HAVING ");
            }
            if (wherePos == -1) {
                wherePos = afterFrom.indexOf(" ORDER BY ");
            }
            afterFrom = afterFrom.substring(0, wherePos);
            tableCount = 1;
            int commaPos = afterFrom.indexOf(", ");
            while (commaPos != -1) {
                afterFrom = afterFrom.substring(commaPos + 2);
                tableCount++;
                commaPos = afterFrom.indexOf(", ");
            }
        }

        /**
         * Constructor.
         *
         * @param query a Query
         */
        public Candidate(Query query) {
            this.query = query;
            this.queryString = null;
            tableCount = query.getFrom().size();
        }

        /**
         * Returns the number of tables in this query.
         *
         * @return an int
         */
        public int getTableCount() {
            return tableCount;
        }

        /**
         * Returns the String query of this Candidate, converting from Query if necessary.
         *
         * @return a String
         */
        public String getQueryString() {
            return (queryString == null ? query.getSQLString() : queryString);
        }

        /**
         * Returns the Query of this Candidate, converting from a String if necessary.
         *
         * @return a Query
         */
        public Query getQuery() {
            return (query == null ? new Query(queryString) : query);
        }

        /**
         * Returns the ExplainResult of this Candidate, fetching the data if not already present.
         *
         * @return an ExplainResult
         * @throws SQLException if an error occurs in the underlying database
         */
        public ExplainResult getExplain() throws SQLException {
            if (explainResult == null) {
                explainResult = (query == null ? getExplainResult(queryString)
                        : getExplainResult(query));
            }
            return explainResult;
        }

        /**
         * Returns true if the argument is slower than this, or if the argument is null.
         *
         * @param c a Candidate
         * @return a boolean
         * @throws SQLException if an error occurs in the underlying database
         */
        public boolean betterThan(Candidate c) throws SQLException {
            return (c == null ? true : getExplain().getTime() < c.getExplain().getTime());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "tables = " + tableCount + (query != null ? ", query = " + query
                    : ", queryString = " + queryString);
        }
    }
}
