package org.flymine.sql.query;

import java.sql.*;

/**
 * Subclass of ExplainResult not associated with a database. The amount of
 * time this query will take is the number of tables plus the number of constraints
 * in milliseconds.
 *
 * @author Andrew Varley
 */
public class DummyExplainResult extends ExplainResult
{
    /*
     * Fields inherited from ExplainResult:
     * protected long rows, start, complete, width, estimatedRows
     */
    private long time;

    /**
     * Constructs an instance of PostgresExplainResult without any data.
     *
     * @param q the Query to explain
     *
     */
    public DummyExplainResult(Query q) {
        time = q.getFrom().size() + q.getWhere().size();
    }

    /**
     * Returns an estimate of the time it will take to complete the query.
     * This is the number of tables plus the number of constraints in
     * milliseconds.
     *
     * @return estimate of time in milliseconds
     */
    public long getTime() {
        return time;
    }


}
