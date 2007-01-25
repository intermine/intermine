package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.List;

import org.intermine.sql.DatabaseUtil;

/**
 * An object designed to represent a batch of a read from an SQL database.
 *
 * @author Matthew Wakeling
 */
public class DBBatch
{
    private int offset;
    private List rows;
    private Map cache;
    private String idField;
    private String sizeQuery;

    /**
     * Constructs a new DBBatch with the given contents.
     *
     * @param offset the row offset of this batch in the whole results
     * @param rows a List of Maps, each being a map from column name to value for a row
     * @param cache a Map from SQL String to prefabricated results in the form of a List of Maps
     * from column name to value
     * @param idField the name of the objectId field
     * @param sizeQuery an SQL query that will return the sizes of all the rows in the table.
     */
    public DBBatch(int offset, List rows, Map cache, String idField, String sizeQuery) {
        this.offset = offset;
        this.rows = rows;
        this.cache = cache;
        this.idField = idField;
        this.sizeQuery = sizeQuery;
    }

    /**
     * Getter for offset.
     *
     * @return an int
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Getter for rows
     *
     * @return a List of Maps from column name to value
     */
    public List getRows() {
        return rows;
    }

    /**
     * Getter for cache
     *
     * @return a Map from SQL String to Lists of Maps from column name to value
     */
    public Map getCache() {
        return cache;
    }

    /**
     * Getter for firstId
     *
     * @return a String
     */
    public String getFirstId() {
        return DatabaseUtil.objectToString(((Map) rows.get(0)).get(idField));
    }

    /**
     * Getter for lastId
     *
     * @return a String
     */
    public String getLastId() {
        return DatabaseUtil.objectToString(((Map) rows.get(rows.size() - 1)).get(idField));
    }

    /**
     * Getter for sizeQuery
     *
     * @return a String
     */
    public String getSizeQuery() {
        return sizeQuery;
    }
}
