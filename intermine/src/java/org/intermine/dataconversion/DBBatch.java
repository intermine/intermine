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

import java.util.Map;
import java.util.List;

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

    /**
     * Constructs a new DBBatch with the given contents.
     *
     * @param offset the row offset of this batch in the whole results
     * @param rows a List of Maps, each being a map from column name to value for a row
     * @param cache a Map from SQL String to prefabricated results in the form of a List of Maps
     * from column name to value
     * @param idField the name of the objectId field
     */
    public DBBatch(int offset, List rows, Map cache, String idField) {
        this.offset = offset;
        this.rows = rows;
        this.cache = cache;
        this.idField = idField;
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
        return (String) ((Map) rows.get(0)).get(idField);
    }

    /**
     * Getter for lastId
     *
     * @return a String
     */
    public String getLastId() {
        return (String) ((Map) rows.get(rows.size() - 1)).get(idField);
    }
}
