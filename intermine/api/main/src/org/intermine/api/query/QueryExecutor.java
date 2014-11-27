package org.intermine.api.query;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;
import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.CacheMap;

/**
 * Common superclass of query executors that holds cache of pathToQueryNode maps per query. This
 * cache is not just for performance, if a query hits the results cache a different pathToQueryNode
 * map needs to be used because QueryNodes are not equals() to one another even if they represent
 * the same class/field.
 *
 * @author Richard Smith
 *
 */
public abstract class QueryExecutor
{
    /**
     * A cache of pathToQueryNode maps that is shared between subclasses of QueryExecutor. The
     * maps are needed to link paths in path queries to objects in the underlying ObjectStore
     * results.
     */
    protected static Map<Query, Map<String, QuerySelectable>> queryToPathToQueryNode =
        Collections.synchronizedMap(new WeakHashMap<Query, Map<String, QuerySelectable>>());
    protected int summaryBatchSize;

    private static final Logger LOG = Logger.getLogger(QueryExecutor.class);
    /**
     * The profile to use to find bags from.
     */
    protected Profile profile;

    protected BagManager bagManager;
    protected BagQueryRunner bagQueryRunner;
    protected ObjectStore os;


    /**
     * Creates a query that returns the summary for a column in a PathQuery.
     **
     * The format of the results is as follows:
     * <ul>
     *  <li> For non-numeric types:</li>
     *    <ol>
     *      <li>The item</li>
     *      <li>The count of occurrences of this item</li>
     *    </ol>
     *   </li>
     *   <li> For numeric types:</li>
     *     <ol>
     *       <li>Minimum Value</li>
     *       <li>Maximum Value</li>
     *       <li>Average</li>
     *       <li>Standard Deviation</li>
     *     </ol>
     *   </li>
     * </ul>
     *
     * @param pathQuery the query to convert
     * @param summaryPath the column to summarise
     * @param asOccurrances If true, will only return the list of values and their counts.
     * @return an IQL Query object
     * @throws ObjectStoreException if there is a problem creating the query
     */
    public Query makeSummaryQuery(PathQuery pathQuery, String summaryPath, boolean asOccurrances)
        throws ObjectStoreException {
        Map<String, QuerySelectable> pathToQueryNode = new HashMap<String, QuerySelectable>();

        Map<String, InterMineBag> allBags = bagManager.getBags(profile);
        Query q = MainHelper.makeSummaryQuery(pathQuery, summaryPath, allBags, pathToQueryNode,
                bagQueryRunner, asOccurrances);
        return q;
    }

    /**
     *
     * @param pathQuery the query to summarise
     * @param summaryPath the column to summarise
     * @return a Results object with varying styles of data
     * @throws ObjectStoreException if there is a problem creating the query
     */
    public Query makeSummaryQuery(PathQuery pathQuery, String summaryPath)
        throws ObjectStoreException {
        return makeSummaryQuery(pathQuery, summaryPath);
    }

    /**
     * Creates a query that returns the summary for a column in a PathQuery, applying a filter at
     * the database level.
     *
     * @param filterTerm what to filter by
     * @param asOccurrances If true, will only return the list of values and their counts.
     * @param pq the query to convert
     * @param summaryPath the column to summarise
     * @return an IQL Query object
     * @throws ObjectStoreException if there is a problem creating the query
     */
    public Query makeSummaryQuery(PathQuery pq, String summaryPath, String filterTerm,
            boolean asOccurrances) throws ObjectStoreException {
        PathQuery clone = pq.clone();
        clone.addConstraint(Constraints.contains(summaryPath, filterTerm));
        Query q = makeSummaryQuery(clone, summaryPath, asOccurrances);
        return q;
    }

    /**
     * Returns the results for a summary for a column in a PathQuery.
     *
     * The format of the results is as follows:
     * <ul>
     *  <li> For non-numeric types:</li>
     *    <ol>
     *      <li>The item</li>
     *      <li>The count of occurrences of this item</li>
     *    </ol>
     *   </li>
     *   <li> For numeric types:</li>
     *     <ol>
     *       <li>Minimum Value</li>
     *       <li>Maximum Value</li>
     *       <li>Average</li>
     *       <li>Standard Deviation</li>
     *     </ol>
     *   </li>
     * </ul>
     *
     * @param pathQuery the query to summarise
     * @param summaryPath the column to summarise
     * @param asOccurrances If true, will only return the list of values and their counts.
     * @return a Results object with varying styles of data
     * @throws ObjectStoreException if there is a problem summarising
     */
    public Results summariseQuery(PathQuery pathQuery, String summaryPath, boolean asOccurrances)
        throws ObjectStoreException {
        return os.execute(makeSummaryQuery(pathQuery, summaryPath, asOccurrances), summaryBatchSize,
                true, true, true);
    }

    /**
     *
     * @param pathQuery The query to execute.
     * @param summaryPath The path whose unique column value count we want.
     * @return A set of results.
     * @throws ObjectStoreException If there is a problem making the query.
     */
    public Results summariseQuery(PathQuery pathQuery, String summaryPath)
        throws ObjectStoreException {
        return summariseQuery(pathQuery, summaryPath, false);
    }

    /**
     * Summarise a query.
     * @param pq The query to summarise
     * @param summaryPath The path of the query to focus on.
     * @param filterTerm An optional term to further filter by.
     * @param asOccurrances If true, will only return the list of values and their counts.
     * @return A set of results.
     * @throws ObjectStoreException in case of Ragnarok.
     */
    public Results summariseQuery(
            PathQuery pq,
            String summaryPath,
            String filterTerm,
            boolean asOccurrances) throws ObjectStoreException {
        if (filterTerm == null || filterTerm.isEmpty()) {
            return summariseQuery(pq, summaryPath, asOccurrances);
        }
        return os.execute(makeSummaryQuery(pq, summaryPath, filterTerm, asOccurrances),
                summaryBatchSize, true, true, true);
    }

    /**
     * Take a query and return the results row count.
     *
     * @param pathQuery the query to count
     * @return the number of rows returned
     * @throws ObjectStoreException if there is a problem counting the query
     */
    public int count(PathQuery pathQuery) throws ObjectStoreException {
        Query q = makeQuery(pathQuery);
        return os.count(q, ObjectStore.SEQUENCE_IGNORE);
    }

    private static final Map<String, Integer> COUNT_CACHE = new CacheMap<String, Integer>();
    /**
     * Get the the total number of unique column values for a given path in the
     * context of a given query.
     *
     * eg:
     * <pre>
     *   int count = ex.uniqueColumnValues(pq, "Gene.symbol");
     * </pre>
     *
     * @param pq The query to execute.
     * @param path The path whose unique column value count we want.
     * @return The number of different values this path can have.
     * @throws ObjectStoreException If there is a problem making the query.
     */
    public int uniqueColumnValues(PathQuery pq, String path) throws ObjectStoreException {
        Query q = makeSummaryQuery(pq, path, true);
        String cacheKey = q.toString() + "summary-path: " + path;
        if (COUNT_CACHE.containsKey(cacheKey)) {
            LOG.debug("Count cache hit");
            return COUNT_CACHE.get(cacheKey);
        } else {
            LOG.debug("Count cache miss");
            Results res = os.execute(q, summaryBatchSize, true, true, true);
            int c = res.size();
            COUNT_CACHE.put(cacheKey, c);
            return c;
        }
    }

    /**
     * Make an InterMine Query object from a PathQuery.
     *
     * @param pathQuery The path query.
     * @return The internal Query representation.
     * @throws ObjectStoreException If there is a problem making the query.
     */
    public abstract Query makeQuery(PathQuery pathQuery) throws ObjectStoreException;


}
