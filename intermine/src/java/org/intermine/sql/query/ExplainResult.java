package org.flymine.sql.query;

import java.sql.*;

/**
 * Represents a result from an EXPLAIN request to a database.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 * @version 1.0
 */
public class ExplainResult
{

    protected long rows;
    protected long start;
    protected long complete;
    protected long width;

    protected long estimatedRows;

    /**
     * Constructs an instance of ExplainResult for a given Query and
     * database Connection.
     *
     * @param query    the org.flymine.sql.query.Query to be explained
     * @param database a java.sql.Connection by which to access the
     *        database. The particular subclass of ExplainResult returned
     *        depends on the type of this Connection
     * @return an instance of ExplainResult
     * @throws SQLException if the query cannot be explained by that database
     */
    public static ExplainResult getInstance(Query query, Connection database) throws SQLException {
        if (database == null) {
            return new DummyExplainResult(query);
        }
        return new PostgresExplainResult(query, database);
    }


    /**
     * Constructs an instance of ExplainResult given a PreparedStatement
     * object.  Assumes that sql string already has EXPLAIN at beginning
     *
     * @param stmt the PreparedStatement to be explained
     * @return an instance of ExplainResult
     * @throws SQLException if the query cannot be explained by that database
     */
    public static ExplainResult getInstance(PreparedStatement stmt) throws SQLException {
        return new PostgresExplainResult(stmt);
    }

    /**
     * Returns the number of rows estimated by the database for this query.
     *
     * @return estimated number of rows
     */
    public long getRows() {
        return rows;
    }

    /**
     * Returns the number of page requests before the first row is returned, estimated by the
     * database for this query.
     *
     * @return estimated number page requests before the first row
     */
    public long getStart() {
        return start;
    }

    /**
     * Returns the number of page requests before the query is completed, estimated by the
     * database for this query.
     *
     * @return estimated number page requests before query completion
     */
    public long getComplete() {
        return complete;
    }

    /**
     * Returns the width of the data returned by the database for this query.
     *
     * @return width in characters
     */
    public long getWidth() {
        return width;
    }

    /**
     * Use this to provide the object with a "better" estimate of the number of rows in the results
     * of this Query.
     *
     * @param newEstimatedRows the better estimate, in rows
     */
    public void setEstimatedRows(long newEstimatedRows) {
        estimatedRows = newEstimatedRows;
    }

    /**
     * Returns the best current estimate for the number of rows for this query.
     *
     * @return estimated number of rows
     */
    public long getEstimatedRows() {
        return estimatedRows;
    }

    /**
     * Returns an estimate of the time it will take to complete the query.
     * The estimate is based on information from the EXPLAIN database call, and
     * possibly a better estimate of how many rows will be returned.
     *
     * @return estimate of time in milliseconds
     */
    public long getTime() {
        // TODO: Do this properly.
        return (complete * estimatedRows) / rows;
    }
}
