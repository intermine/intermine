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
public class ObjectStoreDummyImpl extends ObjectStoreAbstractImpl
{
    private List rows = new ArrayList();
    private int resultsSize = 0;
    private int executeTime = 10;
    private int executeCalls = 0;
    private int poisonRowNo = -1;

    /**
     * Construct an ObjectStoreDummyImpl
     */
    public ObjectStoreDummyImpl() {
    }

    /**
     * Set the max offset allowed
     * @param offset the max offset allowed in queries
     */
    public void setMaxOffset(int offset) {
        this.maxOffset = offset;
    }

    /**
     * Set the max limit allowed
     * @param limit the max limit allowed in queries
     */
    public void setMaxLimit(int limit) {
        this.maxLimit = limit;
    }

    /**
     * Set the max time allowed
     * @param time the max time allowed for queries
     */
    public void setMaxTime(int time) {
        this.maxTime = time;
    }

    /**
     * Sets a row number to throw an ObjectStoreException on.
     *
     * @param row the row which, if accessed will throw an ObjectStoreException
     */
    public void setPoisonRowNo(int row) {
        poisonRowNo = row;
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
        return res;
    }

    /**
     * Execute a Query on this ObjectStore, asking for a certain range of rows to be returned.
     * This will usually only be called by the Results object returned from
     * <code>execute(Query q)</code>.
     *
     * @param q the Query to execute
     * @param start the start row
     * @param limit the maximum numberof rows to be returned
     * @return a list of ResultsRows
     * @throws ObjectStoreException if an error occurs during the running of the Query
     */
    public List execute(Query q, int start, int limit) throws ObjectStoreException {
        checkStartLimit(start, limit);
        if (executeTime > maxTime) {
            throw new ObjectStoreException("Query will take longer than " + maxTime);
        }

        List results = new ArrayList();

        // If we are asking for rows completely outside resultsSize, throw ObjectStoreException
        if (start > resultsSize) {
            return new ArrayList();
            //throw new ArrayIndexOutOfBoundsException("Start row outside results size");
        }

        for (int i = start; ((i <= (start + limit - 1)) && (i < resultsSize)); i++) {
            if (i == poisonRowNo) {
                throw new ObjectStoreException("Poison row number " + i + " reached");
            }
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
     * Set the time it will take to do an execute
     *
     * @param time the time to do an execute
     */
    public void setExecuteTime(int time) {
        this.executeTime = time;
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
     * @param limit the maximum number of rows to be returned
     * @return parsed results of EXPLAIN
     * @throws ObjectStoreException if an error occurs explining the query
     */
    public ExplainResult estimate(Query q, int start, int limit) throws ObjectStoreException {
        return new ExplainResult();
    }

    /**
     * return the resultsSize parameter that simulates number of rows returned from query
     *
     * @param q Flymine Query on which to run COUNT(*)
     * @return the number of rows to be produced by query
     */
    public int count(Query q) {
        return this.resultsSize;
    }

}
