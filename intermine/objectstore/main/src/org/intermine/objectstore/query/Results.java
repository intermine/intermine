package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import java.lang.ref.SoftReference;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.intermine.objectstore.DataChangedException;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.LazyCollection;
import org.intermine.util.CacheMap;

/**
 * Results representation as a List of ResultRows.
 * Extending AbstractList requires implementation of get(int) and size().
 * In addition subList(int, int) overrides AbstractList implementation for efficiency.
 * Also iterator() and isEmpty() to avoid evaluating the entire collection.
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public class Results extends AbstractList implements LazyCollection
{
    private static final Logger LOG = Logger.getLogger(Results.class);

    protected Query query;
    protected ObjectStore os;
    protected int sequence;
    protected boolean optimise = true;
    protected boolean explain = true;
    protected boolean prefetch = true;

    protected int minSize = 0;
    // TODO: update this to use ObjectStore.getMaxRows().
    protected int maxSize = Integer.MAX_VALUE;
    // -1 stands for "not estimated yet"
    protected int estimatedSize = -1;
    protected int originalMaxSize = maxSize;
    protected int batchSize = 1000;
    protected boolean initialised = false;

    // Some prefetch stuff.
    protected int lastGet = -1;
    protected int sequential = 0;
    private static final int PREFETCH_SEQUENTIAL_THRESHOLD = 6;
    // Basically, this keeps a tally of how many times in a row accesses have been sequential.
    // If sequential gets above a PREFETCH_SEQUENTIAL_THRESHOLD, then we prefetch the batch after
    // the one we are currently using.
    protected SoftReference thisBatchHolder;
    protected SoftReference nextBatchHolder;

    protected int lastGetAtGetInfoBatch = -1;
    protected ResultsInfo info;

    // A map of batch number against a List of ResultsRows
    protected Map batches = Collections.synchronizedMap(new CacheMap("Results batches"));

    /**
     * No argument constructor for testing purposes
     *
     */
    protected Results() {
    }

    /**
     * Constructor for a Results object
     *
     * @param query the Query that produces this Results
     * @param os the ObjectStore that can be used to get results rows from
     * @param sequence a number representing the state of the ObjectStore, which should be quoted
     * back to the ObjectStore when requests are made
     */
     public Results(Query query, ObjectStore os, int sequence) {
         if (query == null) {
             throw new NullPointerException("query must not be null");
         }

         if (os == null) {
             throw new NullPointerException("os must not be null");
         }

         this.query = query;
         this.os = os;
         this.sequence = sequence;
     }

    /**
     * Sets this Results object to bypass the optimiser.
     */
    public void setNoOptimise() {
        optimise = false;
    }

    /**
     * Sets this Results object to bypass the explain check in ObjectStore.execute().
     */
    public void setNoExplain() {
        explain = false;
    }

    /**
     * Tells this Results object to never do any background prefetching.
     * This means that Query cancellation via the ObjectStoreInterMineImpl.cancelRequest()
     * will never leave the database busy with cancelled work.
     */
    public void setNoPrefetch() {
        prefetch = false;
    }

    /**
     * Get the Query that produced this Results object
     *
     * @return the Query that produced this Results object
     */
    public Query getQuery() {
        return query;
    }

    /**
     * Returns the ObjectStore that this Results object will use
     *
     * @return an ObjectStore
     */
    public ObjectStore getObjectStore() {
        return os;
    }

    /**
     * Returns a range of rows of results. Will fetch batches from the
     * underlying ObjectStore if necessary.
     *
     * @param start the start index (inclusive)
     * @param end the end index (inclusive)
     * @return the relevant ResultRows as a List
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     * @throws IndexOutOfBoundsException if end is beyond the number of rows in the results
     * @throws IllegalArgumentException if start &gt; end
     */
    public List range(int start, int end) throws ObjectStoreException {
        if (start > end + 1) {
            throw new IllegalArgumentException("start=" + start + " > (end + 1)=" + (end + 1));
        }

        // If we know the size of the results (ie. have had a last partial batch), check that
        // the end is within range
        if (end >= maxSize) {
            throw new IndexOutOfBoundsException("(end + 1)=" + (end + 1) + " > size=" + maxSize);
        }

        int startBatch = getBatchNoForRow(start);
        int endBatch = getBatchNoForRow(end);

        List ret = new ArrayList();
        for (int i = startBatch; i <= endBatch; i++) {
            ret.addAll(getRowsFromBatch(i, start, end));
        }

        if (start - 1 == lastGet) {
            sequential += end - start + 1;
            //LOG.debug("This access sequential = " + sequential
            //        + "                            Result " + query.hashCode()
            //        + "         access " + start + " - " + end);
        } else {
            sequential = 0;
            //LOG.debug("This access not sequential                            Result "
            //        + query.hashCode() + "         access " + start + " - " + end);
        }
        if ((os != null)
                && prefetch
                && os.isMultiConnection()
                && (sequential > PREFETCH_SEQUENTIAL_THRESHOLD)
                && (getBatchNoForRow(maxSize) > endBatch)
                && !batches.containsKey(new Integer(endBatch + 1))) {
            PrefetchManager.addRequest(this, endBatch + 1);
        }
        synchronized (this) {
            if (sequential > PREFETCH_SEQUENTIAL_THRESHOLD) {
                if (startBatch == getBatchNoForRow(end + 1) - 1) {
                    thisBatchHolder = nextBatchHolder;
                    nextBatchHolder = null;
                }
                if (thisBatchHolder != null) {
                    thisBatchHolder.get();
                }
                if (nextBatchHolder != null) {
                    nextBatchHolder.get();
                }
            } else {
                thisBatchHolder = null;
                nextBatchHolder = null;
            }
        }
        lastGet = end;
        /*
        // Do the loop in reverse, so that we get IndexOutOfBoundsException first thing if we are
        // out of range
        for (int i = endBatch; i >= startBatch; i--) {
            // Only one thread does this test at a time to save on calls to ObjectStore
            synchronized (batches) {
                if (!batches.containsKey(new Integer(i))) {
                    fetchBatchFromObjectStore(i);
                }
            }
        }
        */

        return ret;
    }

    /**
     * Gets a range of rows from within a batch
     *
     * @param batchNo the batch number
     * @param start the row to start from (based on total rows)
     * @param end the row to end at (based on total rows) - returned List includes this row
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     * @throws IndexOutOfBoundsException if the batch is off the end of the results
     * @return the rows in the range
     */
    protected List getRowsFromBatch(int batchNo, int start, int end) throws ObjectStoreException {
        List batchList = getBatch(batchNo);

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
     * @return a List which is the batch
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     * @throws IndexOutOfBoundsException if the batch is off the end of the results
     */
    protected List getBatch(int batchNo) throws ObjectStoreException {
        List retval = (List) batches.get(new Integer(batchNo));
        if (retval == null) {
            retval = PrefetchManager.doRequest(this, batchNo);
        }
        return retval;
    }

    /**
     * Gets a batch from the ObjectStore
     *
     * @param batchNo the batch number to get (zero-indexed)
     * @return a List which is the batch
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    protected List fetchBatchFromObjectStore(int batchNo) throws ObjectStoreException {
        int start = batchNo * batchSize;
        int limit = batchSize;
        //int end = start + batchSize - 1;
        initialised = true;
        // We now have 3 possibilities:
        // a) This is a full batch
        // b) This is a partial batch, in which case we have reached the end of the results
        //    and can set size.
        // c) An error has occurred - ie. we have gone beyond the end of the results

        List rows = null;
        try {
            rows = os.execute(query, start, limit, optimise, explain, sequence);

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
                // Set holders, so our data doesn't go away too quickly
                if (sequential > PREFETCH_SEQUENTIAL_THRESHOLD) {
                    if (batchNo == getBatchNoForRow(lastGet + 1)) {
                        thisBatchHolder = new SoftReference(rows);
                    } else if (batchNo == getBatchNoForRow(lastGet + 1) + 1) {
                        nextBatchHolder = new SoftReference(rows);
                    }
                }

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
     * @see AbstractList#get
     * @param index of the ResultsRow required
     * @return the relevant ResultsRow as an Object
     */
    public Object get(int index) {
        List resultList = null;
        try {
            resultList = range(index, index);
        } catch (DataChangedException e) {
            ConcurrentModificationException e2 = new ConcurrentModificationException("ObjectStore"
                    + " error has occurred (in get) - data changed");
            e2.initCause(e);
            throw e2;
        } catch (ObjectStoreException e) {
            //LOG.debug("get - " + e);
            throw new RuntimeException("ObjectStore error has occurred (in get)", e);
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
        } catch (DataChangedException e) {
            ConcurrentModificationException e2 = new ConcurrentModificationException("ObjectStore"
                    + " error has occurred (in subList) - data changed");
            e2.initCause(e);
            throw e2;
        } catch (ObjectStoreException e) {
            //LOG.debug("subList - " + e);
            throw new RuntimeException("ObjectStore error has occured (in subList)", e);
        }
        return ret;
    }

    /**
     * Gets the number of results rows in this Results object
     *
     * @see AbstractList#size
     * @return the number of rows in this Results object
     */
    public int size() {
        //LOG.debug("size - starting                                       Result "
        //        + query.hashCode() + "         size " + minSize + " - " + maxSize);
        if ((minSize == 0) && (maxSize == Integer.MAX_VALUE)) {
            // Fetch the first batch, as it is reasonably likely that it will cover it.
            try {
                get(0);
            } catch (IndexOutOfBoundsException e) {
                // Ignore - that means there are NO rows in this results object.
            }
            return size();
        } else if (minSize * 2 + batchSize < maxSize) {
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
            //LOG.debug("size - returning                                      Result "
            //        + query.hashCode() + "         size " + maxSize);
        } else {
            int iterations = 0;
            while (minSize < maxSize) {
                try {
                    int toGt = (maxSize == originalMaxSize ? minSize * 2
                            : (minSize + maxSize) / 2);
                    //LOG.debug("size - getting " + toGt
                    //        + "                                   Result "
                    //        + query.hashCode() + "         size " + minSize + " - " + maxSize);
                    get(toGt);
                } catch (IndexOutOfBoundsException e) {
                    // Ignore - this will happen if the end of a batch lies on the
                    // end of the results
                    //LOG.debug("size - Exception caught                               Result "
                    //        + query.hashCode() + "         size " + minSize + " - " + maxSize
                    //        + " " + e);
                }
                iterations++;
            }
            //LOG.debug("size - returning after " + (iterations > 9 ? "" : " ") + iterations
            //        + " iterations                  Result "
            //        + query.hashCode() + "         size " + maxSize);
        }
        return maxSize;
    }

    /**
     * @see List#isEmpty
     */
    public boolean isEmpty() {
        if (minSize > 0) {
            return false;
        }
        if (maxSize <= 0) {
            return true;
        }
        // Okay, we don't know anything. Fetch the first batch.
        try {
            get(0);
        } catch (IndexOutOfBoundsException e) {
            // Ignore - that means there are NO rows in this results object.
        }
        return isEmpty();
    }
        
    /**
     * Gets the best current estimate of the characteristics of the query.
     *
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     * @return a ResultsInfo object
     */
    public ResultsInfo getInfo() throws ObjectStoreException {
        if ((info == null) || ((lastGet % batchSize) != lastGetAtGetInfoBatch)) {
            info = os.estimate(query);
            lastGetAtGetInfoBatch = lastGet % batchSize;
        }
        return new ResultsInfo(info, minSize, maxSize);
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
        if (size > os.getMaxLimit()) {
            throw new IllegalArgumentException("Batch size cannot be greater that max limit");
        }
        batchSize = size;
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
    
    /**
     * @see LazyCollection#asList()
     */
    public List asList() {
        return this;
    }
    
    /**
     * @see AbstractList#iterator
     */
    public Iterator iterator() {
        return new Iter();
    }

    /**
     * Returns an iterator over the List, starting fromthe given position.
     * This method is mainly useful for testing.
     *
     * @param from the index of the first object to be fetched
     * @return an Interator
     */
    public Iterator iteratorFrom(int from) {
        Iter iter = new Iter();
        iter.cursor = from;
        return iter;
    }

    private class Iter implements Iterator
    {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        int cursor = 0;

        Object nextObject = null;

        public boolean hasNext() {
            //LOG.debug("iterator.hasNext                                      Result "
            //        + query.hashCode() + "         access " + cursor);
            if (cursor < minSize) {
                return true;
            }
            if (nextObject != null) {
                return true;
            }
            try {
                nextObject = get(cursor);
                return true;
            } catch (IndexOutOfBoundsException e) {
                // Ignore - it means that we should return false;
            }
            return false;
        }

        public Object next() {
            //LOG.debug("iterator.next                                         Result "
            //        + query.hashCode() + "         access " + cursor);
            Object retval = null;
            if (nextObject != null) {
                retval = nextObject;
                nextObject = null;
            } else {
                try {
                    retval = get(cursor);
                } catch (IndexOutOfBoundsException e) {
                    throw (new NoSuchElementException());
                }
            }
            cursor++;
            return retval;
        }

        public void remove() {
            throw (new UnsupportedOperationException());
        }
    }
}
