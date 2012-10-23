package org.intermine.common.swing.table;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A generic and extended version of the AbstractTableModel.
 * 
 * @param <T> The type of items stored in this model.
 */
public abstract class GenericTableModel<T> extends AbstractTableModel implements Iterable<T>
{
    private static final long serialVersionUID = 5726481155581036139L;

    /**
     * A logger for this model.
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    /**
     * The rows (objects) in this model.
     * <p>This is transient as the objects in this list may cause issues with serialization.</p>
     */
    protected transient ArrayList<T> rows = new ArrayList<T>();


    /**
     * Create a new GenericTableModel.
     */
    public GenericTableModel() {
    }

    /**
     * Deserialisation method. Recreates a logger and tries to deserialise the rows.
     * 
     * @param in The ObjectInputStream doing the reading.
     * 
     * @throws ClassNotFoundException if the class of a deserialised object cannot
     * be found. 
     * @throws IOException if there is a problem reading from the stream.
     * 
     * @serialData Recreates the transient logger after deserialisation, and reads the
     * rows of the model if any were written out.
     */
    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        logger = LogFactory.getLog(getClass());
        in.defaultReadObject();
        
        @SuppressWarnings("unchecked")
        ArrayList<T> fetched = (ArrayList) in.readObject();
        if (fetched == null) {
            rows = new ArrayList<T>();
        } else {
            rows = fetched;
        }
    }

    /**
     * Serialisation method. Checks whether the model objects in <code>rows</code> can be
     * written and does so if this is possible.
     * 
     * @param out The ObjectOutputStream to write to.
     * 
     * @throws IOException if there is a problem writing the object.
     * 
     * @serialData If all objects in <code>rows</code> can be serialised, the list is written
     * to the stream. Otherwise, a <code>null</code> is written and the data of this model
     * is not written. 
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        
        // Strictly speaking, each item in rows may be Serializable but may link to
        // something else that is not. Really, we should perform a test write of the
        // list and proceed from there.
        
        boolean canSerialize = true;
        for (T next : rows) {
            if (next != null) {
                canSerialize = canSerialize && next instanceof Serializable;
            }
        }
        out.writeObject(canSerialize ? rows : null);
    }

    /**
     * Remove all rows from this model.
     */
    public void clear() {
        int size = rows.size();
        rows.clear();
        fireTableRowsDeleted(0, size - 1);
    }

    /**
     * Append a row to this model.
     * 
     * @param row The row to add. If <code>null</code>, no action is taken.
     */
    public void addRow(T row) {
        if (row != null) {
            int rowIndex = rows.size();
            rows.add(row);
            fireTableRowsInserted(rowIndex, rowIndex);
        }
    }

    /**
     * Append a collection of row objects to this model.
     * <p>Any <code>null</code> elements in <code>newRows</code> are skipped.
     * 
     * @param newRows The collection of objects to add.
     */
    public void addRows(Collection<T> newRows) {
        if (newRows != null && !newRows.isEmpty()) {
            int index = rows.size();
            int addCount = 0;
            for (T next : newRows) {
                if (next != null) {
                    rows.add(next);
                    ++addCount;
                }
            }
            if (addCount > 0) {
                fireTableRowsInserted(index, index + addCount - 1);
            }
        }
    }

    /**
     * Insert a row object at the given index.
     * 
     * @param row The object to add.
     * @param rowIndex The index at which to insert the object.
     * 
     * @throws IndexOutOfBoundsException if <code>rowIndex</code> is out of range.
     */
    public void insertRow(T row, int rowIndex) {
        if (rowIndex < 0 || rowIndex > rows.size()) {
            throw new IndexOutOfBoundsException("rowIndex must be between 0 and " + rows.size());
        }

        if (row != null) {
            rows.add(rowIndex, row);
            fireTableRowsInserted(rowIndex, rowIndex);
        }
    }

    /**
     * Replace the current objects in this model with those given.
     * 
     * @param newRows A collection of replacement row objects.
     */
    public void setRows(Collection<T> newRows) {
        if (newRows != null && rows != newRows) {
            rows.clear();
            rows.ensureCapacity(newRows.size());
            for (T next : newRows) {
                if (next != null) {
                    rows.add(next);
                }
            }
            fireTableDataChanged();
        }
    }

    /**
     * Remove the given row from this model.
     * 
     * @param row The row to remove.
     */
    public void removeRow(T row) {
        if (row != null) {
            int index = rows.indexOf(row);
            if (index >= 0) {
                rows.remove(row);
                fireTableRowsDeleted(index, index);
            }
        }
    }

    /**
     * Get the number of rows in this model.
     * 
     * @return The number of rows.
     */
    @Override
    public int getRowCount() {
        return rows.size();
    }

    /**
     * Get the row at the given index.
     * 
     * @param rowIndex The row index.
     * 
     * @return The object at the given row index.
     * 
     * @throws IndexOutOfBoundsException if <code>rowIndex</code> is out of range.
     * 
     * @see java.util.List#get(int)
     */
    public T getRowAt(int rowIndex) {
        return rows.get(rowIndex);
    }

    /**
     * Get the index in this model of the given object.
     * 
     * @param thing The object to find.
     * 
     * @return The index of <code>thing</code> in the model, or -1 if it cannot be found.
     * 
     * @see java.util.List#indexOf(Object)
     */
    public int indexOf(T thing) {
        return rows.indexOf(thing);
    }

    /**
     * Method to call when an object in this model is changed. This allows the appropriate
     * <code>TableModelEvent</code> to be raised.
     * 
     * @param row The object that has changed.
     * 
     * @see #rowUpdated(int)
     */
    public void rowUpdated(T row) {
        int index = rows.indexOf(row);
        if (index >= 0) {
            rowUpdated(index);
        }
    }

    /**
     * Fires the appropriate <code>TableModelEvent</code> indicating that the given row
     * has changed.
     * 
     * @param rowIndex The index of the row that has changed.
     */
    public void rowUpdated(int rowIndex) {
        fireTableRowsUpdated(rowIndex, rowIndex);
    }

    /**
     * Get an iterator over the rows in this model.
     * <p>All the normal rules about concurrent modification of this model and using
     * the iterator apply.</p>
     * 
     * @return An iterator.
     */
    public Iterator<T> iterator() {
        return new TableIterator();
    }

    /**
     * Get a ListIterator over the rows in this model.
     * <p>All the normal rules about concurrent modification of this model and using
     * the iterator apply.</p>
     * 
     * @return A ListIterator, initialised before the start of the rows.
     */
    public ListIterator<T> listIterator() {
        return new TableIterator();
    }

    /**
     * ListIterator implementation for the parent GenericTableModel.
     * Removals are permitted and raise the appropriate events.
     * Additions and changes are not.
     */
    private class TableIterator implements ListIterator<T>
    {
        /**
         * The internal ListIterator over the parent's <code>rows</code> list.
         */
        private ListIterator<T> realIterator;
        
        /**
         * The object currently indicated by <code>realIterator</code>.
         */
        private T current;

        
        /**
         * Create a new TableIterator, indicating before the start of the rows list.
         */
        public TableIterator() {
            realIterator = rows.listIterator();
        }

        /**
         * Test whether there is an element following the current element of the iterator.
         * 
         * @return <code>true</code> if there is a subsequent element, <code>false</code>
         * if not.
         * 
         * @see ListIterator#hasNext()
         */
        public boolean hasNext() {
            return realIterator.hasNext();
        }

        /**
         * Get the index of the next object indicated by this iterator.
         * 
         * @return The index of the next object.
         * 
         * @see ListIterator#nextIndex()
         */
        public int nextIndex() {
            return realIterator.nextIndex();
        }

        /**
         * Get the next element following the current element of the iterator.
         * 
         * @return The subsequent element.
         * 
         * @throws java.util.NoSuchElementException if there is no following element.
         * 
         * @see ListIterator#next()
         */
        public T next() {
            current = realIterator.next();
            return current;
        }

        /**
         * Remove the element currently indicated by this iterator from the model.
         * <p>Raises the appropriate <code>TableModelEvent</code> for the deletion
         * for the parent model.</p>
         * 
         * @see GenericTableModel#removeRow(Object)
         */
        public void remove() {
            int index = -1;
            if (current != null) {
                index = rows.indexOf(current);
            }

            realIterator.remove();
            current = null;

            if (index >= 0) {
                fireTableRowsDeleted(index, index);
            }
        }

        /**
         * Add an object to the model via this iterator. This operation is not supported.
         * 
         * @param e The object to add.
         * 
         * @throws UnsupportedOperationException always, as this is not supported.
         */
        public void add(T e) {
            throw new UnsupportedOperationException(
                    "add(T) is not supported on GenericTableModels. "
                    + "Use addRow(T) on the model itself.");
        }

        /**
         * Check whether there are rows prior to the one currently indicated by this
         * iterator.
         * 
         * @return <code>true</code> if there is a preceding row, <code>false</code> if not.
         * 
         * @see ListIterator#hasPrevious()
         */
        public boolean hasPrevious() {
            return realIterator.hasPrevious();
        }

        /**
         * Get the index of the previous object indicated by this iterator.
         * 
         * @return The index of the previous object.
         * 
         * @see ListIterator#previousIndex()
         */
        public int previousIndex() {
            return realIterator.previousIndex();
        }

        /**
         * Get the element preceding the current element of the iterator.
         * 
         * @return The prior element.
         * 
         * @throws java.util.NoSuchElementException if there is no preceding element.
         * 
         * @see ListIterator#previous()
         */
        public T previous() {
            current = realIterator.previous();
            return current;
        }

        /**
         * Replace the current object with that given. This operation is not supported.
         * 
         * @param e The replacement object.
         * 
         * @throws UnsupportedOperationException always, as this is not supported.
         */
        public void set(T e) {
            throw new UnsupportedOperationException(
                    "set(T) is not supported on GenericTableModels.");
        }

    }
}
