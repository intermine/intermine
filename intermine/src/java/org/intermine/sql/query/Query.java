package org.flymine.sql.query;

/**
 * Represents an SQL query in parsed form.
 * TODO: Make it really a parsed form
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 * @version 1.0
 */
public class Query
{
    private String queryString;

    /**
     * Construct a new parsed Query.
     *
     * @param sqlString a SQL query to parse, in String form
     */
    public Query(String sqlString) {
        queryString = sqlString;
    }

    /**
     * Convert this Query into a SQL String query.
     *
     * @return this Query in String form
     */
    public String getSQLString() {
        return queryString;
    }
}
