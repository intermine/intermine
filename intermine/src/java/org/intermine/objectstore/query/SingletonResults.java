package org.flymine.objectstore.query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.flymine.FlyMineException;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreException;

/**
 * This class is equivalent to a Result object with ResultRows consisting only of single items
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class SingletonResults extends Results
{

    /**
     * Constructor for a SingletonResults object
     *
     * @param q the Query that produces this Results
     * @param os the ObjectStore that can be used to get results rows from
     * @throws IllegalArgumentException if q does not return a single column of type QueryClass
     */
     public SingletonResults(Query q, ObjectStore os) {
         super(q, os);

         // Test that this Query only returns a single column of results, and that that column
         // is a QueryClass

         if (q.getSelect().size() != 1) {
             throw new IllegalArgumentException("Query must return a single column");
         }
         if (!(q.getSelect().get(0) instanceof QueryClass)) {
             throw new IllegalArgumentException("Query must select a QueryClass");
         }
     }


    /**
     * Returns a range of objects. Will fetch batches from the
     * underlying ObjectStore if necessary.
     *
     * @param start the start index
     * @param end the end index
     * @return the relevant Objects as a List
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     * @throws IndexOutOfBoundsException if end is beyond the number of rows in the results
     * @throws IllegalArgumentException if start > end
     * @throws FlyMineException if an error occurs promoting proxies
     * @see Results#range
     */
    public List range(int start, int end) throws ObjectStoreException, FlyMineException {
        List rows = super.range(start, end);
        List ret = new ArrayList();
        Iterator rowsIter = rows.iterator();
        while (rowsIter.hasNext()) {
            ret.add(((ResultsRow) rowsIter.next()).get(0));
        }
        return ret;
    }
}
