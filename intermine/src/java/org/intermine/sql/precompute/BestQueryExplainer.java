package org.flymine.sql.precompute;

import org.flymine.sql.query.Query;
import org.flymine.sql.query.ExplainResult;
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
    protected Query bestQuery;
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
        if (bestQuery == null) {
            bestQuery = q;
            bestExplainResult = er;
        }

        // store if better than anything we have already seen
        if (er.getTime() < bestExplainResult.getTime()) {
            bestQuery = q;
            bestExplainResult = er;
        }

        // throw BestQueryException if the bestQuery is will take less time to run than the
        // amount of time we have spent optimising so far
        Date elapsed = new Date();
        if (bestExplainResult.getTime() < (elapsed.getTime() - start.getTime())) {
            throw (new BestQueryException());
        }
    }

    /**
     * Gets the best Query found so far
     *
     * @return the best Query, or null if no Queries added to this object
     */
    public Query getBestQuery() {
        return bestQuery;
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
