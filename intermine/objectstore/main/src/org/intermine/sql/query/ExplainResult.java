package org.intermine.sql.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

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
     * Constructs an instance of ExplainResult for a given query String and
     * database Connection.
     *
     * @param query    the String query to be explained. It need not start with "EXPLAIN"
     * @param database a java.sql.Connection by which to access the
     *        database. The particular subclass of ExplainResult returned
     *        depends on the type of this Connection
     * @return an instance of ExplainResult
     * @throws SQLException if the query cannot be explained by that database
     */
    public static ExplainResult getInstance(String query, Connection database) throws SQLException {
        if (database == null) {
            return new DummyExplainResult(new Query(query));
        }
        return new PostgresExplainResult(query, database);
    }

    /**
     * Constructs an instance of ExplainResult for a given Query and
     * database Connection.
     *
     * @param query    the org.intermine.sql.query.Query to be explained
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
     * @param estimatedRows the better estimate, in rows
     */
    public void setEstimatedRows(long estimatedRows) {
        this.estimatedRows = estimatedRows;
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
        return (long) ((((double) complete) * ((double) estimatedRows)) / ((double) rows));
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "ExplainResult: rows=" + rows + " start=" + start + " complete=" + complete
            + " width=" + width;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (!(o instanceof ExplainResult)) {
            return false;
        }
        ExplainResult e = (ExplainResult) o;
        return e.rows == rows 
            && e.start == start 
            && e.complete == complete
            && e.width == width;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return (int) (2 * rows
                      + 3 * start
                      + 5 * complete
                      + 7 * width);
    }
}
