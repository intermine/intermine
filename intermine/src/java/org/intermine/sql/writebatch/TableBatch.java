package org.intermine.sql.writebatch;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A class representing all changes to be made to an SQL table.
 *
 * @author Matthew Wakeling
 */
public class TableBatch
{
    private String idField;
    private String colNames[];
    private Set idsToDelete;
    private Map idsToInsert;

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
     * value:
     * <ul><li>If there is no previous information, a row is added to the list of rows to
     *         insert</li>
     *     <li>If there is only an entry for deletion, a row is added to the list of rows to
     *         insert (ie the row will be deleted then inserted)</li>
     *     <li>If there is only an entry for insertion, an exception will be thrown</li>
     *     <li>If there is both an entry for insertion and deletion, an exception will be
     *         thrown</li>
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
            idsToInsert = new HashMap();
        } else {
            if (colNames.length != this.colNames.length) {
                throw new IllegalStateException("Cannot change colNames once it is set");
            }
            for (int i = 0; i < colNames.length; i++) {
                if (!colNames[i].equals(this.colNames[i])) {
                    throw new IllegalStateException("Cannot changed colNames once it is set");
                }
            }
        }
        if (idValue == null) {
            idValue = new Object();
        }
        if (idsToInsert.containsKey(idValue)) {
            throw new IllegalStateException("Cannot insert row with id " + idValue + " twice");
        }
        idsToInsert.put(idValue, values);
        return sizeOfArray(values) + 16;
    }

    /**
     * Deletes a row from the batch. This action depends on any previous information on the given id
     * value:
     * <ul><li>If there is no previous information, a row is added to the list of rows to
     *         delete</li>
     *     <li>If there is only an entry for deletion, nothing happens</li>
     *     <li>If there is only an entry for insertion, that entry is removed</li>
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
            idsToDelete = new HashSet();
        } else if (!this.idField.equals(idField)) {
            throw new IllegalStateException("Cannot change idField once it is set");
        }
        int retval = 20;
        if ((idsToInsert != null) && idsToInsert.containsKey(idValue)) {
            retval -= sizeOfArray((Object[]) idsToInsert.remove(idValue));
        } else {
            idsToDelete.add(idValue);
        }
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
}
