package org.flymine.sql.precompute;

import org.flymine.sql.query.Query;
import java.util.Collection;
import java.util.HashSet;
import java.sql.SQLException;

/**
 * Stores each query added.
 *
 * @author Andrew Varley
 */
public class BestQueryStorer extends BestQuery
{
    protected Collection queries = new HashSet();

    /**
     * Constructs a BestQueryStorer
     *
     */
    public BestQueryStorer() {
        super();
    }

    /**
     * Allows a Query to be added to this tracker.
     *
     * @param q a Query to be added to the tracker
     * @throws BestQueryException if the current best Query is the best we think we are going to get
     * @throws SQLException if there is an error in the underlying database
     */
    public void add(Query q) throws BestQueryException, SQLException {
        if (q == null) {
            throw new NullPointerException("Cannot add null queries to a BestQueryStorer");
        }
        queries.add(q);
    }

    /**
     * Gets the set of queries added to this object
     *
     * @return the set of queries
     */
    public Collection getQueries() {
        return queries;
    }


}
