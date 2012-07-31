package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.intermine.objectstore.DataChangedException;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.LazyCollection;

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
public class Results extends AbstractList<Object> implements LazyCollection<Object>
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(Results.class);

    protected ResultsBatches resultsBatches;
    protected boolean optimise = true;
    protected boolean explain = true;
    protected boolean prefetch = true;

    protected boolean immutable = false;

    // Some prefetch stuff.
    protected int lastGet = -1;
    protected int sequential = 0;
    private static final int PREFETCH_SEQUENTIAL_THRESHOLD = 6;
    // Basically, this keeps a tally of how many times in a row accesses have been sequential.
    // If sequential gets above a PREFETCH_SEQUENTIAL_THRESHOLD, then we prefetch the batch after
    // the one we are currently using.

    /**
     * No argument constructor for testing purposes
     *
     */
    protected Results() {
    }

    /**
     * Constructor for a Results object.
     *
     * @param query the Query that produces this Results
     * @param os the ObjectStore that can be used to get results rows from
     * @param sequence a number representing the state of the ObjectStore, which should be quoted
     * back to the ObjectStore when requests are made
     */
    public Results(Query query, ObjectStore os, Map<Object, Integer> sequence) {
        if (query == null) {
            throw new NullPointerException("query must not be null");
        }

        if (os == null) {
            throw new NullPointerException("os must not be null");
        }

        resultsBatches = new ResultsBatches(query, os, sequence);
    }

    /**
     * Constructor for a Results object, given a ResultsBatches object.
     *
     * @param batches a ResultsBatches object that will back this new object
     * @param optimise true if queries should be optimised
     * @param explain true if queries should be explained
     * @param prefetch true to switch on the PrefetchManager
     */
    public Results(ResultsBatches batches, boolean optimise, boolean explain, boolean prefetch) {
        this.resultsBatches = batches;
        this.optimise = optimise;
        this.explain = explain;
        this.prefetch = prefetch;
    }

    /**
     * Sets this Results object to bypass the optimiser.
     */
    public synchronized void setNoOptimise() {
        if (immutable) {
            throw new IllegalArgumentException("Cannot change settings of Results object in cache");
        }
        optimise = false;
    }

    /**
     * Sets this Results object to bypass the explain check in ObjectStore.execute().
     */
    public synchronized void setNoExplain() {
        if (immutable) {
            throw new IllegalArgumentException("Cannot change settings of Results object in cache");
        }
        explain = false;
    }

    /**
     * Tells this Results object to never do any background prefetching.
     * This means that Query cancellation via the ObjectStoreInterMineImpl.cancelRequest()
     * will never leave the database busy with cancelled work.
     */
    public synchronized void setNoPrefetch() {
        if (immutable) {
            throw new IllegalArgumentException("Cannot change settings of Results object in cache");
        }
        prefetch = false;
    }

    /**
     * Tells this Results object that it is being put into a cache, so it needs to be made immutable
     * to prevent threads stomping on each other and changing settings.
     */
    public synchronized void setImmutable() {
        immutable = true;
    }

    /**
     * Get the Query that produced this Results object. Note that due to the ObjectStore's Results
     * cache, this may not be the exact same Query as you passed to ObjectStore.execute. The query
     * will have the same meaning, but may not be .equals() to your original query.
     *
     * @return the Query that produced this Results object
     */
    public Query getQuery() {
        return resultsBatches.getQuery();
    }

    /**
     * Returns the sequence that this Results object was created with
     *
     * @return the ObjectStore-specific object
     */
    public Map<Object, Integer> getSequence() {
        return resultsBatches.getSequence();
    }

    /**
     * Returns the ObjectStore that this Results object will use
     *
     * @return an ObjectStore
     */
    public ObjectStore getObjectStore() {
        return resultsBatches.getObjectStore();
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
    public List<Object> range(int start, int end) throws ObjectStoreException {
        if (start > end + 1) {
            throw new IllegalArgumentException("start=" + start + " > (end + 1)=" + (end + 1));
        }

        // If we know the size of the results (ie. have had a last partial batch), check that
        // the end is within range
        if (end >= resultsBatches.getMaxSize()) {
            throw new IndexOutOfBoundsException("(end + 1)=" + (end + 1) + " > size="
                    + resultsBatches.getMaxSize());
        }

        int startBatch = getBatchNoForRow(start);
        int endBatch = getBatchNoForRow(end);

        List<Object> ret = new ArrayList<Object>();
        for (int i = startBatch; i <= endBatch; i++) {
            ret.addAll(resultsBatches.getRowsFromBatch(i, start, end, optimise, explain));
        }

        if (start - 1 == lastGet) {
            sequential += end - start + 1;
        } else {
            sequential = 0;
        }
        if ((resultsBatches.getObjectStore() != null)
                && prefetch
                && resultsBatches.getObjectStore().isMultiConnection()
                && (sequential > PREFETCH_SEQUENTIAL_THRESHOLD)
                && (getBatchNoForRow(resultsBatches.getMaxSize()) > endBatch)) {
            resultsBatches.prefetch(endBatch + 1, optimise, explain);
        }
        lastGet = end;

        return ret;
    }

    /**
     * {@inheritDoc}
     * @param index of the ResultsRow required
     * @return the relevant ResultsRow as an Object
     */
    @Override
    public Object get(int index) {
        List<Object> resultList = null;
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
     * {@inheritDoc}
     * @param start the index to start from (inclusive)
     * @param end the index to end at (exclusive)
     * @return the sub-list
     */
    @Override
    public List<Object> subList(int start, int end) {
        List<Object> ret = null;
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
     * {@inheritDoc}
     * @return the number of rows in this Results object
     */
    @Override
    public int size() {
        return resultsBatches.size(optimise, explain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return resultsBatches.isEmpty(optimise, explain);
    }

    /**
     * Gets the best current estimate of the characteristics of the query.
     *
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     * @return a ResultsInfo object
     */
    public ResultsInfo getInfo() throws ObjectStoreException {
        return resultsBatches.getInfo();
    }

    /**
     * Sets the number of rows requested from the ObjectStore whenever an execute call is made
     *
     * @param size the number of rows
     */
    public synchronized void setBatchSize(int size) {
        if (immutable) {
            throw new IllegalArgumentException("Cannot change settings of Results object in cache");
        }
        resultsBatches.setBatchSize(size);
    }

    /**
     * Gets the batch size being used.
     *
     * @return an int
     */
    public int getBatchSize() {
        return resultsBatches.getBatchSize();
    }

    /**
     * Gets the batch for a particular row
     *
     * @param row the row to get the batch for
     * @return the batch number
     */
    protected int getBatchNoForRow(int row) {
        return resultsBatches.getBatchNoForRow(row);
    }

    /**
     * {@inheritDoc}
     */
    public List<Object> asList() {
        return this;
    }

    /**
     * Returns true if the results are known to fit entirely within the first batch.
     *
     * @return a boolean
     */
    public boolean isSingleBatch() {
        return resultsBatches.isSingleBatch();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Object> iterator() {
        return new Iter();
    }

    /**
     * Returns an iterator over the List, starting from the given position.
     * This method is mainly useful for testing.
     *
     * @param from the index of the first object to be fetched
     * @return an Interator
     */
    public Iterator<Object> iteratorFrom(int from) {
        Iter iter = new Iter();
        iter.cursor = from;
        return iter;
    }

    private class Iter implements Iterator<Object>
    {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        int cursor = 0;

        Object nextObject = null;
        Object thisBatch, nextBatch;

        public boolean hasNext() {
            //LOG.debug("iterator.hasNext                                      Result "
            //        + query.hashCode() + "         access " + cursor);
            if (cursor < resultsBatches.getMinSize()) {
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
            int currentBatchNo = getBatchNoForRow(cursor);
            thisBatch = resultsBatches.batches.get(currentBatchNo);
            nextBatch = resultsBatches.batches.get(currentBatchNo + 1);
            return retval;
        }

        public void remove() {
            throw (new UnsupportedOperationException());
        }
    }

    /**
     * Returns the underlying ResultsBatches object that this object is using, in order to create
     * more Results objects (with different settings) from it.
     *
     * @return a ResultsBatches object
     */
    public ResultsBatches getResultsBatches() {
        return resultsBatches;
    }
}
