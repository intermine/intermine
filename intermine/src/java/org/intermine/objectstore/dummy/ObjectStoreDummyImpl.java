package org.flymine.objectstore.dummy;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import org.flymine.objectstore.*;
import org.flymine.objectstore.query.*;
import org.flymine.sql.query.ExplainResult;

/**
 * Generate dummy Results from a query. Used for testing purposes.
 *
 * @author Andrew Varley
 */
public class ObjectStoreDummyImpl implements ObjectStore
{
    private List rows = new ArrayList();
    private int resultsSize = 0;
    private int executeCalls = 0;

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
        Results res = new Results(q, this);
        res.setSize(resultsSize);
        return res;
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
            if (i < rows.size()) {
                results.add(rows.get(i));
            } else {
                results.add(getRow(q));
            }
        }
        executeCalls++;
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
     * Set the number of rows to be contained in the returned Results object
     *
     * @param size the number of rows in the returned Results object
     */
    public void setResultsSize(int size) {
        this.resultsSize = size;
    }

    /**
     * Gets the number of execute calls made to this ObjectStore
     *
     * @return the number of execute calls made (not including the initial no-argument call)
     */
    public int getExecuteCalls() {
        return executeCalls;
    }

    /**
     * Gets the next row to be returned.
     *
     * @param q the Query to return results for
     * @throws ObjectStoreException if a class cannot be instantiated
     */
    private ResultsRow getRow(Query q) throws ObjectStoreException {

        ResultsRow row;
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
        return row;
    }


    /**
     * Returns an empty ExplainResult object
     *
     * @param q the query to estimate rows for
     * @return parsed results of EXPLAIN
     * @throws ObjectStoreException if an error occurs explining the query
     */
    public ExplainResult estimate(Query q) throws ObjectStoreException {
        return new ExplainResult();
    }

    /**
     * returns an empty ExplainResult object
     *
     * @param q the query to explain
     * @param start first row required, numbered from zero
     * @param end the number of the last row required, numbered from zero
     * @return parsed results of EXPLAIN
     * @throws ObjectStoreException if an error occurs explining the query
     */
    public ExplainResult estimate(Query q, int start, int end) throws ObjectStoreException {
        return new ExplainResult();
    }

}
