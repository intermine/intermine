package org.flymine.objectstore.query;

import java.util.List;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreException;

/**
 * Results representation as a List of ResultRows
 * Extending AbstractList requires implementation of get(int) and size()
 * In addition subList(int, int) overrides AbstractList implementation for efficiency
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class Results extends AbstractList
{
    protected Query query;
    protected ObjectStore os;
    protected int size = 0;
    protected int batchSize = 1;
    protected boolean initialised = false;

    // A map of batch number against a List of ResultsRows
    protected Map batches = new HashMap();

    /**
     * No argument constructor for testing purposes
     *
     */
    protected Results() {
    }

    /**
     * Constructor for a Results object
     *
     * @param q the Query that produces this Results
     * @param os the ObjectStore that can be used to get results rows from
     */
     public Results(Query q, ObjectStore os) {
         if ((q == null) || (os == null)) {
             throw new NullPointerException("Arguments must not be null");
         }
         this.query = q;
         this.os = os;
     }

    /**
     * Returns a range of rows of results. Will fetch batches from the
     * underlying ObjectStore if necessary.
     *
     * @param start the start index
     * @param end the end index
     * @return the relevant ResultRows as a List
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    public List range(int start, int end) throws ObjectStoreException {
        List rows;

        if (start > end) {
            throw new IllegalArgumentException();
        }

        int startBatch = getBatchNoForRow(start);
        int endBatch = getBatchNoForRow(end);

        if (end >= size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        for (int i = startBatch; i <= endBatch; i++) {
            // Only one thread does this test at a time to save on calls to ObjectStore
            synchronized (batches) {
                if (!batches.containsKey(new Integer(i))) {
                    fetchBatchFromObjectStore(i);
                }
            }
        }
        return localRange(start, end);
    }

    /**
     * Returns a combined list of objects from the batches Map
     *
     * @param start the start row
     * @param end the end row
     * @return a List of ResultsRows made up of the ResultsRows in the individual batches
     */
    protected List localRange(int start, int end) {
        List ret = new ArrayList();
        int startBatch = getBatchNoForRow(start);
        int endBatch = getBatchNoForRow(end);

        for (int i = startBatch; i <= endBatch; i++) {
            ret.addAll(getRowsFromBatch(i, start, end));
        }
        return ret;
    }

    /**
     * Gets a range of rows from within a batch
     *
     * @param batchNo the batch number
     * @param start the row to start from (based on total rows)
     * @param end the row to end at (based on total rows) - returned List includes this row
     * @return the rows in the range
     */
    protected List getRowsFromBatch(int batchNo, int start, int end) {
        List batchList = (List) batches.get(new Integer(batchNo));

        int startRowInBatch = batchNo * batchSize;
        int endRowInBatch = startRowInBatch + batchSize - 1;

        start = Math.max(start, startRowInBatch);
        end = Math.min(end, endRowInBatch);

        return batchList.subList(start - startRowInBatch, end - startRowInBatch + 1);
    }

    /**
     * Gets a batch from the ObjectStore
     *
     * @param batchNo the batch number to get (zero-indexed)
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    protected void fetchBatchFromObjectStore(int batchNo) throws ObjectStoreException {
        int start = batchNo * batchSize;
        int end = start + batchSize - 1;
        initialised = true;
        batches.put(new Integer(batchNo), os.execute(query, start, end));
    }

    /**
     * @see AbstractList#get
     * @param index of the ResultsRow required
     * @return the relevant ResultsRow as an Object
     */
    public Object get(int index) {
        List resultList = null;
        try {
            resultList = range(index, index);
        } catch (ObjectStoreException e) {
            throw new ArrayIndexOutOfBoundsException("ObjectStore error has occured");
        }
        return resultList.get(0);
    }

    /**
     * @see List#subList
     * @param start the index to start from (inclusive)
     * @param end the index to end at (exclusive)
     * @return the sub-list
     */
    public List subList(int start, int end) {
        List ret = null;
        try {
            ret = range(start, end - 1);
        } catch (ObjectStoreException e) {
            throw new ArrayIndexOutOfBoundsException("ObjectStore error has occured");
        }
        return ret;
    }

    /**
     * Sets the number of results rows in this Results object
     *
     * @param size the number of rows in this Results object
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Gets the number of results rows in this Results object
     *
     * @see AbstractList#size
     * @return the number of rows in this Results object
     */
    public int size() {
        return size;
    }

    /**
     * Sets the number of rows requested from the ObjectStore whenever an execute call is made
     *
     * @param size the number of rows
     */
    public void setBatchSize(int size) {
        if (initialised) {
            throw new IllegalStateException("Cannot set batchSize if rows have been retrieved");
        }
        batchSize = size;
    }

    /**
     * Gets the number of rows requested from the ObjectStore whenever an execute call is made
     *
     * @return the number of rows
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Gets the batch for a particular row
     *
     * @param row the row to get the batch for
     * @return the batch number
     */
    protected int getBatchNoForRow(int row) {
        return (int) (row / batchSize);
    }

}
