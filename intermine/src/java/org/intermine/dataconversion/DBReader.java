package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

/**
 * Provides an interface between a DBConverter and the source SQL database.
 *
 * @author Matthew Wakeling
 */
public interface DBReader
{
    /**
     * Returns an iterator through the rows of an SQL results set, from a given SQL query. Note that
     * the SQL query MUST have a column in the SELECT list that corresponds to the ID of the object
     * that the row represents, and that column MUST have an alias of "objectid". This fact is used
     * by some implementations of DBReader for performance enhancements. The implementation should
     * be expected to not load the entire results set into memory, but load it in batches as it is
     * used.
     *
     * @param sql an SQL String query
     * @return an Iterator through the result rows
     */
    public Iterator sqlIterator(String sql);

    /**
     * Runs an SQL query and returns an sql ResultSet. It is expected that this facility will be
     * used to gather additional information about a particular object, by ID. Some implementations
     * may use this fact for performance improvements, in which case there may be a recommended form
     * for the query to take.
     *
     * @param sql an SQL String query
     * @return an sql ResultSet
     * @throws SQLException if something goes wrong
     */
    public ResultSet execute(String sql) throws SQLException;
}


