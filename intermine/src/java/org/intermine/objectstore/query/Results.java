package org.flymine.objectstore.query;

import java.util.List;
import java.util.AbstractList;
import java.util.ArrayList;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreException;

/**
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class Results extends AbstractList
{
    protected Query query;
    protected ObjectStore os;
    protected int batchsize;
    protected int lookahead;
    protected List resultsRows = new ArrayList(); // @element-type ResultRow

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
     * Returns a range of rows of results
     *
     * @param start the start index
     * @param end the end index
     * @return the relevant ResultRows as a List
     */
    public List range(int start, int end) {
        return null;
    }

    /**
     * Fetch rows from the underlying ObjectStore
     *
     * @param start the start index
     * @param end the end index
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    protected void fetchRows(int start, int end) throws ObjectStoreException {
        resultsRows.add(os.execute(query, start, end));
    }

    /**
     * @param index of the object required
     * @return the relevant object
     */
    public Object get(int index) {
        return null;
    }

    /**
     * @return the number of rows in this results item
     */
    public int size() {
        return 0;
    }
}
