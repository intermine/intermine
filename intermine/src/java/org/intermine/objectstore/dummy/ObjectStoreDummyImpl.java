package org.flymine.objectstore.dummy;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import org.flymine.objectstore.*;
import org.flymine.objectstore.query.*;

/**
 * Generate dummy Results from a query. Used for testing purposes.
 *
 * @author Andrew Varley
 */
public class ObjectStoreDummyImpl implements ObjectStore
{
    private List rows = new ArrayList();

    /**
     * Construct an ObjectStoreDummyImpl
     */
    public ObjectStoreDummyImpl() {
    }

    /**
     * Execute a Query on this ObjectStore
     *
     * @param q the Query to execute
     * @return the results of the Query
     * @throws ObjectStoreException if an error occurs during the running of the Query
     */
    public Results execute(Query q) throws ObjectStoreException {
        return new Results(q, this);
    }

    /**
     * Execute a Query on this ObjectStore, asking for a certain range of rows to be returned.
     * This will usually only be called by the Results object returned from
     * <code>execute(Query q)</code>.
     *
     * @param q the Query to execute
     * @param start the start row
     * @param end the end row
     * @return a list of ResultsRows
     * @throws ObjectStoreException if an error occurs during the running of the Query
     */
    public List execute(Query q, int start, int end) throws ObjectStoreException {

        List results = new ArrayList();

        for (int i = start; i <= end; i++) {
            results.add(getRow(q));
        }

        return results;

    }

    /**
     * Adds a row to be returned
     *
     * @param row a row to be returned
     */
    public void addRow(ResultsRow row) {
        rows.add(row);
    }

    /**
     * Gets the next row to be returned. Will make one up if the ones set up have been exhausted.
     *
     * @param q the Query to return results for
     * @throws ObjectStoreException if a class cannot be instantiated
     */
    private ResultsRow getRow(Query q) throws ObjectStoreException {

        ResultsRow row;
        if (rows.size() > 0) {
            // Get a row from the ones already set up
            row = (ResultsRow) rows.get(0);
            rows.remove(0);
        } else {
            row = new ResultsRow();
            List classes = q.getSelect();

            Iterator i = classes.iterator();

            while (i.hasNext()) {
                QueryNode qn = (QueryNode) i.next();
                Object obj = null;
                if (qn instanceof QueryClass) {
                    try {
                        obj = ((QueryClass) qn).getType().newInstance();
                    } catch (Exception e) {
                        throw new ObjectStoreException("Cannot instantiate class "
                                                       + ((QueryClass) qn).getType().getName(), e);
                    }
                } else {
                    // Either a function, expression or Field
                    obj = new Integer(1);
                }
                row.add(obj);
            }
        }
        return row;
    }

}
