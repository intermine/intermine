package org.intermine.sql.precompute;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.sql.query.Query;
import org.intermine.sql.query.ExplainResult;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

/**
 * Gets the database to explain each Query added and keeps hold of the best one so far.
 *
 * @author Andrew Varley
 */
public class BestQueryExplainer extends BestQuery
{
    private static final int OVERHEAD = 300;

    protected Query bestQuery;
    protected String bestQueryString;
    protected ExplainResult bestExplainResult;
    protected Connection con;
    protected Date start = new Date();

    /**
     * Constructs an empty BestQueryExplainer for testing purposes
     *
     */
    public BestQueryExplainer() {
        super();
    }

    /**
     * Constructs a BestQueryExplainer that will use the given Connection to explain Queries.
     *
     * @param con the Connection to use
     */
    public BestQueryExplainer(Connection con) {
        if (con == null) {
            throw (new NullPointerException());
        }
        this.con = con;
    }

    /**
     * Allows a Query to be added to this tracker.
     *
     * @param q a Query to be added to the tracker
     * @throws BestQueryException if the current best Query is the best we think we are going to get
     * @throws SQLException if error occurs in the underlying database
     */
    public void add(Query q) throws BestQueryException, SQLException {

        ExplainResult er = ExplainResult.getInstance(q, con);

        // store if this is the first we have seen
        if ((bestQuery == null) && (bestQueryString == null)) {
            bestQuery = q;
            bestQueryString = null;
            bestExplainResult = er;
        }

        // store if better than anything we have already seen
        if (er.getTime() < bestExplainResult.getTime()) {
            bestQuery = q;
            bestQueryString = null;
            bestExplainResult = er;
        }

        // throw BestQueryException if the bestQuery is will take less time to run than the
        // amount of time we have spent optimising so far
        Date now = new Date();
        long elapsed = now.getTime() - start.getTime();
        if (bestExplainResult.getTime() < (elapsed + OVERHEAD)) {
            throw (new BestQueryException("Explain time: " + bestExplainResult.getTime()
                        + ", elapsed time: " + elapsed));
        }
    }

    /**
     * Allows a Query to be added to this tracker.
     *
     * @param q a query String to be added to the tracker
     * @throws BestQueryException if the current best Query is the best we think we are going to get
     * @throws SQLException if error occurs in the underlying database
     */
    public void add(String q) throws BestQueryException, SQLException {

        ExplainResult er = ExplainResult.getInstance(q, con);

        // store if this is the first we have seen
        if ((bestQuery == null) && (bestQueryString == null)) {
            bestQuery = null;
            bestQueryString = q;
            bestExplainResult = er;
        }

        // store if better than anything we have already seen
        if (er.getTime() < bestExplainResult.getTime()) {
            bestQuery = null;
            bestQueryString = q;
            bestExplainResult = er;
        }

        // throw BestQueryException if the bestQuery is will take less time to run than the
        // amount of time we have spent optimising so far
        Date elapsed = new Date();
        if (bestExplainResult.getTime() < (elapsed.getTime() + OVERHEAD - start.getTime())) {
            throw (new BestQueryException());
        }
    }

    /**
     * Gets the best Query found so far
     *
     * @return the best Query, or null if no Queries added to this object
     */
    public Query getBestQuery() {
        return (bestQueryString == null ? bestQuery : new Query(bestQueryString));
    }

    /**
     * Gets the best query String found so far
     *
     * @return the best Query, or null if no Queries added to this object
     */
    public String getBestQueryString() {
        return (bestQuery == null ? bestQueryString : bestQuery.getSQLString());
    }

    /**
     * Gets the ExpainResult for the best Query found so far
     *
     * @return the best ExplainResult, or null if no Queries added to this object
     */
    public ExplainResult getBestExplainResult() {
        return bestExplainResult;
    }
}
