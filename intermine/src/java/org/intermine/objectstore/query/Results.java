package org.flymine.objectstore.query;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import java.util.List;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Iterator;
import java.util.WeakHashMap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.flymine.FlyMineException;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.ObjectStoreLimitReachedException;
import org.flymine.objectstore.proxy.LazyCollection;
import org.flymine.objectstore.proxy.LazyReference;
import org.flymine.util.TypeUtil;

/**
 * Results representation as a List of ResultRows
 * Extending AbstractList requires implementation of get(int) and size()
 * In addition subList(int, int) overrides AbstractList implementation for efficiency
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public class Results extends AbstractList
{
    protected Query query;
    protected ObjectStore os;
    protected int minSize = 0;
    // TODO: update this to use ObjectStore.getMaxRows().
    protected int maxSize = Integer.MAX_VALUE;
    protected int originalMaxSize = maxSize;
    protected int batchSize = 100;
    protected boolean initialised = false;
    protected boolean doPrefetch = false;

    // Some prefetch stuff.
    protected int lastGet = -1;
    protected int sequential = 0;
    private static final int PREFETCH_SEQUENTIAL_THRESHOLD = 6;
    // Basically, this keeps a tally of how many times in a row accesses have been sequential.
    // If sequential gets above a PREFETCH_SEQUENTIAL_THRESHOLD, then we prefetch the batch after
    // the one we are currently using.

    protected static final Logger LOG = Logger.getLogger(Results.class);

    // A map of batch number against a List of ResultsRows
    protected Map batches = Collections.synchronizedMap(new WeakHashMap());

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
     * @throws IndexOutOfBoundsException if end is beyond the number of rows in the results
     * @throws IllegalArgumentException if start &gt; end
     * @throws FlyMineException if an error occurs promoting proxies
     */
    public List range(int start, int end) throws ObjectStoreException, FlyMineException {
        if (start > end) {
            throw new IllegalArgumentException();
        }

        int startBatch = getBatchNoForRow(start);
        int endBatch = getBatchNoForRow(end);

        // If we know the size of the results (ie. have had a last partial batch), check that
        // the end is within range
        if ((minSize == maxSize) && (end >= maxSize)) {
            throw new IndexOutOfBoundsException("End = " + end + ", size = " + maxSize);
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
        if ((sequential > PREFETCH_SEQUENTIAL_THRESHOLD) && (getBatchNoForRow(maxSize) > endBatch)
                && (!batches.containsKey(new Integer(endBatch + 1)))) {
            PrefetchManager.addRequest(this, endBatch + 1);
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
        return localRange(start, end);
    }

    /**
     * Returns a combined list of objects from the batches Map
     *
     * @param start the start row
     * @param end the end row
     * @return a List of ResultsRows made up of the ResultsRows in the individual batches
     * @throws FlyMineException if an error occurs promoting proxies
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     * @throws IndexOutOfBoundsException if the batch is off the end of the results
     */
    protected List localRange(int start, int end) throws FlyMineException, ObjectStoreException {
        List ret = new ArrayList();
        int startBatch = getBatchNoForRow(start);
        int endBatch = getBatchNoForRow(end);

        for (int i = startBatch; i <= endBatch; i++) {
            List rows = getRowsFromBatch(i, start, end);
            promoteProxies(rows);
            ret.addAll(rows);
        }
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
            rows = os.execute(query, start, limit);

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
            }
        } catch (ObjectStoreLimitReachedException e) {
            throw e;
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
        } catch (ObjectStoreException e) {
            LOG.info("get - " + e);
            throw new RuntimeException("ObjectStore error has occured (in get)", e);
        } catch (FlyMineException e) {
            LOG.info("get - " + e);
            throw new RuntimeException("FlyMineException occurred (in get)", e);
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
            LOG.info("subList - " + e);
            throw new RuntimeException("ObjectStore error has occured (in subList)", e);
        } catch (FlyMineException e) {
            LOG.info("subList - " + e);
            throw new RuntimeException("FlyMineException occurred (in subList)", e);
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
        if (minSize * 2 + batchSize < maxSize) {
            // Do a count, because it will probably be a little faster.
            maxSize = os.count(query);
            minSize = maxSize;
            LOG.info("size - returning                                      Result "
                    + query.hashCode() + "         size " + maxSize);
        } else {
            int iterations = 0;
            while (minSize < maxSize) {
                try {
                    int toGt = (maxSize == originalMaxSize ? minSize * 2
                            : (minSize + maxSize) / 2);
                    LOG.info("size - getting " + toGt + "                                   Result "
                            + query.hashCode() + "         size " + minSize + " - " + maxSize);
                    get(toGt);
                } catch (ObjectStoreLimitReachedException e) {
                    throw e;
                } catch (IndexOutOfBoundsException e) {
                    // Ignore - this will happen if the end of a batch lies on the
                    // end of the results
                    //LOG.debug("size - Exception caught                               Result "
                    //        + query.hashCode() + "         size " + minSize + " - " + maxSize
                    //        + " " + e);
                }
                iterations++;
            }
            LOG.info("size - returning after " + (iterations > 9 ? "" : " ") + iterations
                    + " iterations                  Result "
                    + query.hashCode() + "         size " + maxSize);
        }
        return maxSize;
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


    /**
     * Iterate through a list of ResultsRows converting any objects' LazyCollection fields
     * to Results objects.  The LazyCollection wraps a Query, these need to be converted to
     * SingletonResults so that they have an ObjectStore reference and are able to run themselves.
     * Instances of LazyReference have an ObjectStore set.
     * @param rows a list of rows (Lists) to search through
     * @throws FlyMineException if errors occur accessing object fields
     */
    protected void promoteProxies(List rows) throws FlyMineException {
        try {
            Iterator listIter = rows.iterator();
            while (listIter.hasNext()) {
                List rr = (List) listIter.next();
                Iterator rowIter = rr.iterator();
                while (rowIter.hasNext()) {
                    Object obj = (Object) rowIter.next();
                    Class objClass = obj.getClass();

                    Map fieldToGetter = TypeUtil.getFieldToGetter(objClass);
                    Map fieldToSetter = TypeUtil.getFieldToSetter(objClass);
                    Iterator fields = fieldToGetter.entrySet().iterator();
                    while (fields.hasNext()) {
                        Map.Entry entry = (Map.Entry) fields.next();
                        Field field = (Field) entry.getKey();
                        Method getter = (Method) entry.getValue();
                        Object fieldVal = getter.invoke(obj, new Object[] {});

                        if (fieldVal instanceof LazyCollection) {
                            Query query = ((LazyCollection) fieldVal).getQuery();
                            Object singletonResult = new SingletonResults(query, os);
                            try {
                                Method setter = (Method) fieldToSetter.get(field);
                                setter.invoke(obj, new Object[] {new SingletonResults(query, os)});
                            } catch (IllegalArgumentException e) {
                                throw new IllegalArgumentException("Error setting field "
                                        + field.getDeclaringClass().getName() + "."
                                        + field.getName() + " ( a " + field.getType().getName()
                                        + ") to object " + singletonResult + " (a "
                                        + singletonResult.getClass().getName() + ")");
                            }
                        } else if (fieldVal instanceof LazyReference) {
                            ((LazyReference) fieldVal).setObjectStore(os);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new FlyMineException(e);
        }
    }

    /**
     * @see AbstractList#iterator
     */
    public Iterator iterator() {
        return new Iter();
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
            } catch (ObjectStoreLimitReachedException e) {
                throw e;
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

    private List columnAliases = null;
    private List columnTypes = null;

    /**
     * Returns a list of aliases, where each alias corresponds to each element of the SELECT list
     * of the Query object. This is effectively a list of column headings for the results object.
     *
     * @return a List of Strings, each of which is the alias of the column
     */
    public List getColumnAliases() {
        if (columnAliases == null) {
            setupColumns();
        }
        return columnAliases;
    }

    /**
     * Returns a list of Class objects, where each object corresponds to the type of each element
     * of the SELECT list of the Query object. This is effectively a list of column types for the
     * results object.
     *
     * @return a List of Class objects
     */
    public List getColumnTypes() {
        if (columnTypes == null) {
            setupColumns();
        }
        return columnTypes;
    }

    private void setupColumns() {
        columnAliases = new ArrayList();
        columnTypes = new ArrayList();
        Iterator selectIter = query.getSelect().iterator();
        while (selectIter.hasNext()) {
            QueryNode node = (QueryNode) selectIter.next();
            String alias = (String) query.getAliases().get(node);
            Class type = node.getType();
            columnAliases.add(alias);
            columnTypes.add(type);
        }
    }
}
