package org.flymine.objectstore.query;

import java.util.List;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.lang.reflect.Field;

import org.flymine.FlyMineException;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.proxy.LazyCollection;
import org.flymine.objectstore.proxy.LazyReference;
import org.flymine.util.TypeUtil;

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
    protected int size = -1;
    protected int batchSize = 1;
    protected int maxBatchNo = -1;
    protected boolean initialised = false;
    protected boolean doPrefetch = false;

    // A map of batch number against a List of ResultsRows
    protected Map batches = Collections.synchronizedMap(new HashMap());

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
        if ((size != -1) && (end >= size)) {
            throw new IndexOutOfBoundsException("End = " + end + ", size = " + size);
        }

        // Do the loop in reverse, so that we get IndexOutOfBoundsException first thing if we are
        // out of range
        for (int i = endBatch; i >= startBatch; i--) {
            // Only one thread does this test at a time to save on calls to ObjectStore
            synchronized (batches) {
                if (!batches.containsKey(new Integer(i))) {
                    fetchBatchFromObjectStore(i);
                    if (i > maxBatchNo) {
                        maxBatchNo = i;
                    }
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
     * @throws FlyMineException if an error occurs promoting proxies
     */
    protected List localRange(int start, int end) throws FlyMineException {
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
     * @return the rows in the range
     */
    protected List getRowsFromBatch(int batchNo, int start, int end) {
        List batchList = (List) batches.get(new Integer(batchNo));
        if (batchList == null) {
            throw new NullPointerException("Batch " + batchNo + " is not in the batches list");
        }

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
     * @throws IndexOutOfBoundsException if the batch is off the end of the results
     */
    protected void fetchBatchFromObjectStore(int batchNo) throws ObjectStoreException {
        int start = batchNo * batchSize;
        int end = start + batchSize - 1;
        initialised = true;
        // We now have 3 possibilities:
        // a) This is a full batch
        // b) This is a partial batch, in which case we have reached the end of the results
        //    and can set size.
        // c) An error has occurred - ie. we have gone beyond the end of the results

        List rows = os.execute(query, start, end);
        batches.put(new Integer(batchNo), rows);

        // Now deal with a partial batch
        if (rows.size() != batchSize) {
            size = start + rows.size();
        }

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
        } catch (FlyMineException e) {
            throw new RuntimeException("FlyMineException occurred. " + e.getMessage());
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
        } catch (FlyMineException e) {
            throw new RuntimeException("FlyMineException occurred. " + e.getMessage());
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
        // If we have got the last batch, then we know the size. Else, fetch batches
        // in turn until we have got the last one.
        // TODO: probably want to be cleverer about how we do this.
        if (size == -1) {
            // Find the largest batch number we already have
            int batchNo = maxBatchNo + 1;
            while (size == -1) {
                try {
                    get(batchNo * batchSize);
                } catch (IndexOutOfBoundsException e) {
                    // Ignore - this will happen if the end of a batch lies on the
                    // end of the results
                }
                batchNo++;
            }
        }
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


    /**
     * Iterate through a list of ResultsRows converting any objects' LazyCollection fields
     * to Results objects.  The LazyCollection wraps a Query, these need to be converted to
     * Results so that they have an ObjectStore reference and are able to run themselves.
     *
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
                    Iterator fields = TypeUtil.getFields(objClass).iterator();
                    while (fields.hasNext()) {
                        Field field = (Field) fields.next();
                        field.setAccessible(true);
                        Object fieldVal = field.get(obj);
                        if (fieldVal instanceof LazyCollection) {
                            Query query = ((LazyCollection) fieldVal).getQuery();
                            field.set(obj, new SingletonResults(query, os));
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
}
