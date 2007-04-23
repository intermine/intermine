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

import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/**
 * A class representing all changes to be made to an SQL indirection table.
 *
 * @author Matthew Wakeling
 */
public class IndirectionTableBatch implements Table
{
    private static final Logger LOG = Logger.getLogger(IndirectionTableBatch.class);

    private String leftColName, rightColName;
    private Set rowsToDelete;
    private Set rowsToInsert;
    private int size = 0;

    /**
     * Constructor for this class. Generates a batch with no data to write.
     *
     * @param leftColName the left column name
     * @param rightColName the right column name
     */
    public IndirectionTableBatch(String leftColName, String rightColName) {
        this.leftColName = leftColName;
        this.rightColName = rightColName;
        rowsToDelete = new TreeSet();
        rowsToInsert = new TreeSet();
    }

    /**
     * Constructor for dodgy temp table deleting stuff
     *
     * @param leftColName the left column name
     * @param rightColName the right column name
     * @param toInsert the Set of Rows to insert
     */
    protected IndirectionTableBatch(String leftColName, String rightColName, Set toInsert) {
        this.leftColName = leftColName;
        this.rightColName = rightColName;
        rowsToDelete = new TreeSet();
        this.rowsToInsert = toInsert;
    }

    /**
     * Adds a row to the batch. Whatever the previous state of the database and batch, the table
     * will end up with one row matching the arguments.
     *
     * @param left the left int value
     * @param right the right int value
     * @return the number of bytes by which the batch should be deemed to have expanded
     */
    public int addRow(int left, int right) {
        Row row = new Row(left, right);
        boolean removedDelete = rowsToDelete.remove(row);
        boolean addedInsert = rowsToInsert.add(row);
        int deltaSize = (addedInsert ? (removedDelete ? 0 : 16) : 0);
        size += deltaSize;
        return deltaSize;
    }

    /**
     * Deletes a row from the batch. Whatever the previous state of the database and batch, the
     * table will end up with no rows that match the arguments.
     *
     * @param left the left int value
     * @param right the right int value
     * @return the number of bytes by which the batch should be deemed to have expanded
     */
    public int deleteRow(int left, int right) {
        Row row = new Row(left, right);
        boolean removedInsert = rowsToInsert.remove(row);
        boolean addedDelete = rowsToDelete.add(row);
        int deltaSize = (addedDelete ? (removedInsert ? 0 : 16) : 0);
        size += deltaSize;
        return deltaSize;
    }

    /**
     * Returns the left column name.
     *
     * @return leftColName
     */
    public String getLeftColName() {
        return leftColName;
    }

    /**
     * Returns the right column name.
     *
     * @return rightColName
     */
    public String getRightColName() {
        return rightColName;
    }

    /**
     * Returns the Set of rows to delete.
     *
     * @return rowsToDelete
     */
    public Set getRowsToDelete() {
        return rowsToDelete;
    }

    /**
     * Returns the Set of rows to insert.
     *
     * @return rowsToInsert
     */
    public Set getRowsToInsert() {
        return rowsToInsert;
    }

    /**
     * Clears the batch.
     */
    public void clear() {
        rowsToInsert.clear();
        rowsToDelete.clear();
        size = 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        return size;
    }
}
