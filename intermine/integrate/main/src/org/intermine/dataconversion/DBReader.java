package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * Provides an interface between a DBRetriever and the source SQL database.
 *
 * @author Matthew Wakeling
 */
public interface DBReader
{
    /**
     * Returns an iterator through the rows of an SQL results set, from a given SQL query. Note that
     * the SQL query MUST have a column in the SELECT list that corresponds to the ID of the object
     * that the row represents, which MUST be unique. The query MUST NOT be ordered by anything,
     * but will end up coming back ordered by the objectid column, and MUST NOT have an OFFSET or
     * LIMIT. If a WHERE clause exists, it MUST be of a form that allows one to put "AND ..." on
     * the end. These facts are used by some implementations of DBReader for performance
     * enhancements. The implementation should be expected to not load the entire results set into
     * memory, but load it in batches as it is used.
     *
     * @param sql an SQL String query
     * @param idField a String describing the field name of the object id field
     * @param tableName a String describing the name of the table that this query accesses
     * @return an Iterator through the result rows
     */
    public Iterator sqlIterator(String sql, String idField, String tableName);

    /**
     * Runs an SQL query and returns an sql ResultSet. It is expected that this facility will be
     * used to gather additional information about a particular object, by ID. Some implementations
     * may use this fact for performance improvements, in which case there may be a recommended form
     * for the query to take.
     *
     * @param sql an SQL String query
     * @return a List of Maps from column name to column value
     * @throws SQLException if something goes wrong
     */
    public List execute(String sql) throws SQLException;

    /**
     * Closes down this instance, guaranteeing that no part of it remains that ties up precious
     * resources or cannot be garbage collected.
     */
    public void close();
}
