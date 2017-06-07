package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.DataChangedException;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.CacheMap;

/**
 * Class holding the data batches for the Results object. Possibly multiple Results objects with
 * different settings (for prefetch, explain, optimise) will use the same ResultsBatches object.
 *
 * @author Matthew Wakeling
 */
public class ResultsBatches
{
    /** This is the default batch size for Results objects */
    public static final int DEFAULT_BATCH_SIZE = 1000;

    protected Query query;
    protected ObjectStore os;
    protected Map<Object, Integer> sequence;

    protected int minSize = 0;
    // TODO: update this to use ObjectStore.getMaxRows().
    protected int maxSize = Integer.MAX_VALUE;
    protected int batchSize = DEFAULT_BATCH_SIZE;
    protected boolean initialised = false;

    protected ResultsInfo info;

    // A map of batch number against a List of ResultsRows
    protected Map<Integer, List<Object>> batches = Collections.synchronizedMap(
            new CacheMap<Integer, List<Object>>("Results batches"));

    /**
     * Construct a new ResultsBatches object. This is generally only called by the Results object.
     *
     * @param query a Query object to retrieve data for
     * @param os an ObjectStore
     * @param sequence the ObjectStore-specific sequence data
     */
    public ResultsBatches(Query query, ObjectStore os, Map<Object, Integer> sequence) {
        this.query = query;
        this.os = os;
        this.sequence = sequence;
    }

    /**
     * Get the Query that produced this ResultsBatches object. Note that due to the ObjectStore's
     * Results cache, this may not be the exact same Query as you passed to ObjectStore.execute. The
     * query will have the same meaning, but may not be .equals() to your original query.
     *
     * @return the Query that produced this ResultsBatches object
     */
    public Query getQuery() {
        return query;
    }

    /**
     * Returns the sequence that this ResultsBatches object was created with
     *
     * @return the ObjectStore-specific object
     */
    public Map<Object, Integer> getSequence() {
        return sequence;
    }

    /**
     * Returns the ObjectStore that this ResultsBatches object will use
     *
     * @return an ObjectStore
     */
    public ObjectStore getObjectStore() {
        return os;
    }

    /**
     * Returns the current maximum number of rows in the results
     *
     * @return an int
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Returns the current minimum number of rows in the results
     *
     * @return an int
     */
    public int getMinSize() {
        return minSize;
    }

    /**
     * Prefetch into memory the batch indicated with the given batch number. If it is already there,
     * do nothing.
     *
     * @param batchNo the batch number
     * @param optimise true if queries should be optimised
     * @param explain true if this method should explain each query first
     */
    public void prefetch(int batchNo, boolean optimise, boolean explain) {
        if (!batches.containsKey(new Integer(batchNo))) {
            PrefetchManager.addRequest(this, batchNo, optimise, explain);
        }
    }

    /**
     * Gets a range of rows from within a batch
     *
     * @param batchNo the batch number
     * @param start the row to start from (based on total rows)
     * @param end the row to end at (based on total rows) - returned List includes this row
     * @param optimise true if queries should be optimised
     * @param explain true if this method should explain each query first
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     * @throws IndexOutOfBoundsException if the batch is off the end of the results
     * @return the rows in the range
     */
    protected List<Object> getRowsFromBatch(int batchNo, int start, int end, boolean optimise,
            boolean explain) throws ObjectStoreException {
        List<Object> batchList = getBatch(batchNo, optimise, explain);

        int startRowInBatch = batchNo * batchSize;
        int endRowInBatch = startRowInBatch + batchSize - 1;

        start = Math.max(start, startRowInBatch);
        end = Math.min(end, endRowInBatch);

        return batchList.subList(start - startRowInBatch, end - startRowInBatch + 1);
    }

    /**
     * Gets a batch by whatever means - maybe batches, maybe the ObjectStore.
     *
     * @param batchNo the batch number to get (zero-indexed)
     * @param optimise true if queries should be optimised
     * @param explain true if this method should explain each query first
     * @return a List which is the batch
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     * @throws IndexOutOfBoundsException if the batch is off the end of the results
     */
    protected List<Object> getBatch(int batchNo, boolean optimise, boolean explain)
        throws ObjectStoreException {
        List<Object> retval = batches.get(new Integer(batchNo));
        if (retval == null) {
            retval = PrefetchManager.doRequest(this, batchNo, optimise, explain);
        }
        return retval;
    }

    /**
     * Gets a batch from the ObjectStore.
     *
     * @param batchNo the batch number to get (zero-indexed)
     * @param optimise true if queries should be optimised
     * @param explain true if this method should explain each query first
     * @return a List which is the batch
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    protected List<Object> fetchBatchFromObjectStore(int batchNo, boolean optimise, boolean explain)
        throws ObjectStoreException {
        int start = batchNo * batchSize;
        int limit = batchSize;
        //int end = start + batchSize - 1;
        initialised = true;
        // We now have 3 possibilities:
        // a) This is a full batch
        // b) This is a partial batch, in which case we have reached the end of the results
        //    and can set size.
        // c) An error has occurred - ie. we have gone beyond the end of the results

        List<Object> rows = null;
        try {
            @SuppressWarnings("unchecked") List<Object> tmpRows =
                (List) os.execute(query, start, limit, optimise, explain, sequence);
            rows = tmpRows;

            synchronized (this) {
                // Now deal with a partial batch, so we can update the maximum size
                if (rows.size() != batchSize) {
                    int size = start + rows.size();
                    maxSize = (maxSize > size ? size : maxSize);
                }
                // Now deal with non-empty batch, so we can update the minimum size
                if (!rows.isEmpty()) {
                    int size = start + rows.size();
                    minSize = (minSize > size ? minSize : size);
                }

                Integer key = new Integer(batchNo);
                batches.put(key, rows);
            }
        } catch (IndexOutOfBoundsException e) {
            synchronized (this) {
                if (rows == null) {
                    maxSize = (maxSize > start ? start : maxSize);
                }
            }
            throw e;
        }

        return rows;
    }

    /**
     * Gets the number of results rows in this Results object.
     *
     * @param optimise true if queries should be optimised
     * @param explain true if this method should explain each query first
     * @return the number of rows in this Results object
     */
    public int size(boolean optimise, boolean explain) {
        if ((minSize == 0) && (maxSize == Integer.MAX_VALUE)) {
            // Fetch the first batch, as it is reasonably likely that it will cover it.
            try {
                getBatch(0, optimise, explain);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("ObjectStore error has occurred in size()", e);
            }
            return size(optimise, explain);
        } else if (minSize < maxSize) {
            // Do a count, because it will probably be a little faster.
            try {
                maxSize = os.count(query, sequence);
            } catch (DataChangedException e) {
                ConcurrentModificationException e2 = new ConcurrentModificationException("Object"
                        + "Store error has occurred (in size) - data changed");
                e2.initCause(e);
                throw e2;
            } catch (ObjectStoreException e) {
                throw new RuntimeException("ObjectStore error has occured (in size)", e);
            }
            minSize = maxSize;
        }
        return maxSize;
    }

    /**
     * Returns true is there are no rows in the results.
     *
     * @param optimise true if queries should be optimised
     * @param explain true if this method should explain each query first
     * @return a boolean
     */
    public boolean isEmpty(boolean optimise, boolean explain) {
        if (minSize > 0) {
            return false;
        }
        if (maxSize <= 0) {
            return true;
        }
        // Okay, we don't know anything. Fetch the first batch.
        try {
            getBatch(0, optimise, explain);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("ObjectStore error has occurred in isEmpty()", e);
        }
        return isEmpty(optimise, explain);
    }

    /**
     * Gets the best current estimate of the characteristics of the query.
     *
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     * @return a ResultsInfo object
     */
    public ResultsInfo getInfo() throws ObjectStoreException {
        if (info == null) {
            info = os.estimate(query);
        }
        return new ResultsInfo(info, minSize, maxSize);
    }

    /**
     * Sets the number of rows requested from the ObjectStore whenever an execute call is made
     *
     * @param size the number of rows
     */
    public synchronized void setBatchSize(int size) {
        if (initialised) {
            throw new IllegalStateException("Cannot set batchSize if rows have been retrieved");
        }
        if (size > os.getMaxLimit()) {
            throw new IllegalArgumentException("Batch size cannot be greater os.query.max-limit"
                    + " property (" + os.getMaxLimit() + ") tried to set to: " + size);
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Batch size must be greater than zero - tried to set"
                    + " to " + size);
        }
        batchSize = size;
    }

    /**
     * Gets the batch size being used.
     *
     * @return an int
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
        return row / batchSize;
    }

    /**
     * Returns true if the results are known to fit entirely within the first batch.
     *
     * @return a boolean
     */
    public boolean isSingleBatch() {
        return maxSize < batchSize;
    }

    /**
     * Returns a new ResultsBatches object with a different batch size with some of the data already
     * filled in.
     *
     * @param newBatchSize the batch size of the new ResultsBatch object
     * @return a new ResultsBatches object
     */
    public ResultsBatches makeWithDifferentBatchSize(int newBatchSize) {
        ResultsBatches retval = new ResultsBatches(query, os, sequence);
        retval.setBatchSize(newBatchSize);
        List<Object> firstBatch = batches.get(new Integer(0));
        if ((firstBatch != null) && (isSingleBatch() || (firstBatch.size() >= newBatchSize))) {
            if (firstBatch.size() > newBatchSize) {
                // Trim batch to size
                List<Object> newFirstBatch = new ArrayList<Object>();
                for (int i = 0; i < newBatchSize; i++) {
                    newFirstBatch.add(firstBatch.get(i));
                }
                firstBatch = newFirstBatch;
            }
            retval.batches.put(new Integer(0), firstBatch);
            retval.minSize = minSize;
            retval.maxSize = maxSize;
            retval.initialised = true;
        }
        return retval;
    }

    /**
     * Returns the given batch, if it is already in the batch cache.
     *
     * @param batchNo the batch number to return
     * @return a batch of rows
     */
    public List<Object> getBatchFromCache(int batchNo) {
        List<Object> retval = batches.get(batchNo);
        if (retval != null) {
            return Collections.unmodifiableList(retval);
        } else {
            return null;
        }
    }
}
