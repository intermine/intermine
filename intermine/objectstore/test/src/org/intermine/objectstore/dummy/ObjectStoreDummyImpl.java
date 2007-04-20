package org.intermine.objectstore.dummy;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;
import org.intermine.objectstore.*;
import org.intermine.objectstore.query.*;

/**
 * Generate dummy Results from a query. Used for testing purposes.
 *
 * @author Andrew Varley
 */
public class ObjectStoreDummyImpl extends ObjectStoreAbstractImpl
{
    private List rows = new ArrayList();
    private int resultsSize = 0;
    private int estimatedResultsSize = 0;
    private int executeTime = 10;
    private int executeCalls = 0;
    private int poisonRowNo = -1;
    private Map objects = new HashMap();

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
        Results res = new Results(q, this, 0);
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
     * @param optimise true if the query should be optimised
     * @param explain true if the query should be explained
     * @param sequence an integer that is ignored
     * @return a list of ResultsRows
     * @throws ObjectStoreException if an error occurs during the running of the Query
     */
    public List execute(Query q, int start, int limit, boolean optimise, boolean explain,
            int sequence) throws ObjectStoreException {
        checkStartLimit(start, limit, q);
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
     * @see ObjectStore#cacheObjectById
     */
    public Object cacheObjectById(Integer id, InterMineObject o) {
        objects.put(id, o);
        return o;
    }

    /**
     * @see ObjectStore#getObjectById
     */
    public InterMineObject getObjectById(Integer id) throws ObjectStoreException {
        return (InterMineObject) objects.get(id);
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
        setEstimatedResultsSize(size);
    }

    /**
     * Set the number of rows to be contained in the returned Results object
     *
     * @param size the number of rows in the returned Results object
     */
    public void setEstimatedResultsSize(int size) {
        this.estimatedResultsSize = size;
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
                    Class cls = ((QueryClass) qn).getType();
                    if (cls.isInterface()) {
                        obj = DynamicUtil.createObject(Collections.singleton(cls));
                    } else {
                        obj = ((QueryClass) qn).getType().newInstance();
                    }
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
     * @see ObjectStore#getObjectByExample
     */
    public Object getObjectByExample(Object obj) throws ObjectStoreException {
        throw new ObjectStoreException("getObjectByExample is not supported by "
                                       + this.getClass().getName());
    }

    /**
     * Returns an empty ResultsInfo object
     *
     * @param q the query to estimate rows for
     * @return parsed results of EXPLAIN
     * @throws ObjectStoreException if an error occurs explaining the query
     */
    public ResultsInfo estimate(Query q) throws ObjectStoreException {
        return new ResultsInfo(0, 0, estimatedResultsSize);
    }

    /**
     * return the resultsSize parameter that simulates number of rows returned from query
     *
     * @param q Intermine Query on which to run COUNT(*)
     * @param sequence an integer that is ignored
     * @return the number of rows to be produced by query
     * @throws ObjectStoreException if an error occurs counting the query
     */
    public int count(Query q, int sequence) throws ObjectStoreException {
        return this.resultsSize;
    }

    /**
     * Set the model to be returned by this ObjectStore
     * @param model the Model
     */
    public void setModel(Model model) {
        this.model = model;
    }

    /**
     * @see ObjectStore#isMultiConnection
     */
    public boolean isMultiConnection() {
        return true;
    }

    /**
     * @see ObjectStore#getSequence
     */
    public int getSequence() {
        return 0;
    }

    public Integer getSerial() throws ObjectStoreException {
        throw new ObjectStoreException();
    }
}
