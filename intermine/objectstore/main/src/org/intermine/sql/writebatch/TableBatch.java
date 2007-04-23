package org.intermine.sql.writebatch;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.intermine.util.NullFirstComparator;

/**
 * A class representing all changes to be made to an SQL table.
 *
 * @author Matthew Wakeling
 */
public class TableBatch implements Table
{
    private static final Logger LOG = Logger.getLogger(TableBatch.class);
    private static final Integer NULL_VALUE = new Integer(0);

    private String idField;
    private String colNames[];
    private Set idsToDelete;
    private Map idsToInsert;
    private int size = 0;

    /**
     * Constructor for this class. Generates a table batch with no data to write.
     */
    public TableBatch() {
        idField = null;
        colNames = null;
        idsToDelete = null;
        idsToInsert = null;
    }

    /**
     * Adds a row to the batch. This action depends on any previous information on the given id
     * value. If the id value is currently marked as deleted, then it will change to "delete, then
     * insert". This system allows multiple rows to be inserted for the same id, so there are three
     * insertion modes:
     * <ul><li>If there is no insertion data, then the row is inserted straight</li>
     *     <li>If there is only one insertion entry, it is converted into a List containing two
     *         entries</li>
     *     <li>If there is a List, then the new entry is added to it</li>
     * </ul>
     *
     * @param idValue the value of the ID field for this row, or null if there is no relevant
     * idField
     * @param colNames an array of names of fields that are in the table
     * @param values an array of Objects to be put in the row, in the same order as colNames
     * @return the number of bytes by which the batch should be deemed to have expanded
     */
    public int addRow(Object idValue, String colNames[], Object values[]) {
        if (this.colNames == null) {
            this.colNames = colNames;
            idsToInsert = new TreeMap(NullFirstComparator.SINGLETON);
        } else {
            if (colNames != this.colNames) {
                if (colNames.length != this.colNames.length) {
                    throw new IllegalStateException("Cannot change colNames once it is set");
                }
                for (int i = 0; i < colNames.length; i++) {
                    if (!colNames[i].equals(this.colNames[i])) {
                        throw new IllegalStateException("Cannot change colNames once it is set");
                    }
                }
                Exception e = new Exception();
                e.fillInStackTrace();
                LOG.warn("Potential inefficiency - seen two equivalent column name arrays, and had"
                        + " to compare them as arrays (slowly). Try to cache the column name array"
                        + " so they can be compared by reference (fast). old = " + this.colNames
                        + ", new: " + colNames + ", stack trace = ", e);
                this.colNames = colNames;
            }
        }
        //if (idValue == null) {
        //    idValue = this;
        //}
        Object currentEntry = idsToInsert.get(idValue);
        if (currentEntry == null) {
            idsToInsert.put(idValue, values);
        } else if (currentEntry instanceof List) {
            ((List) currentEntry).add(values);
        } else {
            List newEntry = new ArrayList();
            newEntry.add(currentEntry);
            newEntry.add(values);
            idsToInsert.put(idValue, newEntry);
        }
        int deltaSize = sizeOfArray(values) + 16;
        size += deltaSize;
        return deltaSize;
    }

    /**
     * Deletes a row from the batch. This action depends on any previous information on the given id
     * value:
     * <ul><li>If there is no previous information, a row is added to the list of rows to
     *         delete</li>
     *     <li>If there is only an entry for deletion, nothing happens</li>
     *     <li>If there is only an entry for insertion, that entry is removed, and an entry created
     *         for deletion</li>
     *     <li>If there is both an entry for insertion and deletion, the insertion entry is
     *         removed</li>
     * </ul>
     *
     * @param idField the name of the field that is the ID
     * @param idValue the value of the ID field for this row
     * @return the number of bytes by which the batch should be deemed to have expanded
     */
    public int deleteRow(String idField, Object idValue) {
        if (this.idField == null) {
            this.idField = idField;
            idsToDelete = new TreeSet(NullFirstComparator.SINGLETON);
        } else if (!this.idField.equals(idField)) {
            throw new IllegalStateException("Cannot change idField once it is set");
        }
        int retval = 50;
        if (idsToInsert != null) {
            Object removed = idsToInsert.remove(idValue);
            if (removed != null) {
                if (removed instanceof Object[]) {
                    retval -= sizeOfArray((Object[]) removed);
                } else {
                    retval -= sizeOfList((List) removed);
                }
            }
        }
        idsToDelete.add(idValue);
        size += retval;
        return retval;
    }

    /**
     * Returns the column names.
     *
     * @return an array of Strings
     */
    public String[] getColNames() {
        return colNames;
    }

    /**
     * Returns the idField.
     *
     * @return a String
     */
    public String getIdField() {
        return idField;
    }

    /**
     * Returns the insert map.
     *
     * @return a Map
     */
    public Map getIdsToInsert() {
        return idsToInsert;
    }

    /**
     * Returns the delete set.
     *
     * @return a Set
     */
    public Set getIdsToDelete() {
        return idsToDelete;
    }

    /**
     * Clears the batch.
     */
    public void clear() {
        if (idsToDelete != null) {
            idsToDelete.clear();
        }
        if (idsToInsert != null) {
            idsToInsert.clear();
        }
        size = 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        return size;
    }

    /**
     * Calculates the size of an array, in bytes.
     *
     * @param array the array
     * @return an int
     */
    protected static int sizeOfArray(Object[] array) {
        int retval = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] instanceof String) {
                retval += ((String) array[i]).length() * 2;
            } else if ((array[i] instanceof Long) || (array[i] instanceof Double)) {
                retval += 8;
            } else if (array[i] instanceof BigDecimal) {
                retval += 50;
            } else {
                retval += 4;
            }
        }
        return retval;
    }

    /**
     * Calculates the size of a List of arrays, in bytes.
     *
     * @param list a List of arrays
     * @return an int
     */
    protected static int sizeOfList(List list) {
        int retval = 0;
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            Object array[] = (Object[]) iter.next();
            retval += sizeOfArray(array);
        }
        return retval;
    }
}
