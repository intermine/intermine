package org.intermine.api.query;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.profile.BagState;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.pathquery.PathQuery;

/**
 * Executes path query and returns results in form suitable for export or web
 * services.
 *
 * @author Jakub Kulaviak
 * @author Alex Kalderimis
 */
public class PathQueryExecutor extends QueryExecutor
{

    public static final int DEFAULT_BATCH_SIZE = 5000;
    private static final long MAX_WAIT_TIME = 2000;
    private int batchSize = DEFAULT_BATCH_SIZE;

    /**
     * Sets batch size.
     *
     * @param size batch size
     */
    public void setBatchSize(int size) {
        this.batchSize = size;
    }

    /**
     * Constructor with necessary objects.
     *
     * @param os the ObjectStore to run the query in
     * @param classKeys key fields for classes in the data model
     * @param profile the user executing the query - for access to saved lists
     * @param bagQueryRunner for executing bag searches in queries
     * @param bagManager access to global and user bags
     */
    public PathQueryExecutor(
            ObjectStore os,
            Map<String, List<FieldDescriptor>> classKeys,
            Profile profile,
            BagQueryRunner bagQueryRunner,
            BagManager bagManager) {
        this.os = os;
        this.bagQueryRunner = bagQueryRunner;
        this.bagManager = bagManager;
        this.profile = profile;
        this.summaryBatchSize = DEFAULT_BATCH_SIZE;
    }


    /**
     * Executes object store query and returns results as iterator over rows.
     * Every row is a list of result elements.
     *
     * @param pathQuery path query to be executed
     * @return results
     */
    public ExportResultsIterator execute(PathQuery pathQuery) {
        try {
            Map<String, QuerySelectable> pathToQueryNode = new HashMap<String, QuerySelectable>();
            Map<String, BagQueryResult> returnBagQueryResults = new HashMap<String, BagQueryResult>();

            Query q = makeQuery(pathQuery, returnBagQueryResults, pathToQueryNode);
            Results results = os.execute(q, batchSize, true, true, false);

            //Query realQ = results.getQuery();
            //if (realQ == q) {
            //    queryToPathToQueryNode.put(q, pathToQueryNode);
            //} else {
            //    pathToQueryNode = queryToPathToQueryNode.get(realQ);
            //}
            return new ExportResultsIterator(pathQuery, q, results, pathToQueryNode);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Creating export results iterator failed", e);
        }
    }


    /**
     * Executes object store query and returns results as iterator over rows.
     * Every row is a list of result elements.
     *
     * @param pathQuery path query to be executed
     * @param start index of first result which will be retrieved. It can be very slow, it fetches
     * results from database from index 0 and just throws away all before start index.
     * @param limit maximum number of results
     * @return results
     */

    public ExportResultsIterator execute(PathQuery pathQuery, final int start,
            final int limit) {
        try {
            Map<String, QuerySelectable> pathToQueryNode = new HashMap<String, QuerySelectable>();
            Map<String, BagQueryResult> returnBagQueryResults = new HashMap<String, BagQueryResult>();

            Query q = makeQuery(pathQuery, returnBagQueryResults, pathToQueryNode);
            Results results = os.execute(q, batchSize, true, true, false);

            /* 
             * The purpose behind all this is to make sure that the results caching
             * does not interfere with the routine to convertColumns in ExportResultsIterator.
             * Sad, but true.
             * TODO: dispense with these stupid shenanigins! and just pass through the query itself.
            Query realQ = results.getQuery();
            if (realQ == q) {
                queryToPathToQueryNode.put(q, pathToQueryNode);
            } else {
                pathToQueryNode = queryToPathToQueryNode.get(realQ);
            }
            if (pathToQueryNode == null) {
                throw new Error("realQ != q but !queryToPathToQueryNode.contains(realQ)");
            }
            */
            return new ResultIterator(pathQuery, q, results, pathToQueryNode, start, limit);
        } catch (ObjectStoreException e) {
            throw new RuntimeException(
                    "Creating export results iterator failed", e);
        }
    }

    private Query makeQuery(PathQuery pathQuery, Map<String, BagQueryResult> pathToBagQueryResult,
            Map<String, QuerySelectable> pathToQueryNode) throws ObjectStoreException {

        Map<String, InterMineBag> allBags = bagManager.getCurrentBags(profile);

        Query q = MainHelper.makeQuery(pathQuery, allBags, pathToQueryNode, bagQueryRunner,
                pathToBagQueryResult);
        return q;
    }
    
    /* make this the returned value rather than those stupid maps...
    private class MainHelperResult {
        final Map<String, BagQueryResult> pathToBagQueryResult = new HashMap<String, BagQueryResult>();
        final Map<String, QuerySelectable> pathToQueryNode = new HashMap<String, QuerySelectable>();
        Query query;
        
        MainHelperResult() {
        }
    }
    */

    /**
     * Make the Lower-level Query object to run from the the higher level
     * PathQuery one.
     * @param pq the PathQuery to translate.
     * @return The Query to run.
     * @throws ObjectStoreException if there is a problem making the query.
     */
    public Query makeQuery(PathQuery pq) throws ObjectStoreException {
        Map<String, QuerySelectable> pathToQueryNode = new HashMap<String, QuerySelectable>();
        Map<String, BagQueryResult> returnBagQueryResults =
            new HashMap<String, BagQueryResult>();

        checkListStatus(pq);

        return makeQuery(pq, returnBagQueryResults, pathToQueryNode);
    }

    /**
     * Get the SQl that will be run on the database.
     * @param pathQuery The path-query to run.
     * @return An SQL command.
     * @throws ObjectStoreException If something goes wrong.
     */
    public String makeSql(PathQuery pathQuery) throws ObjectStoreException {
        Query query = makeQuery(pathQuery);
        ObjectStoreInterMineImpl osimi = (ObjectStoreInterMineImpl) os;
        return osimi.generateSql(query);
    }

    /**
     * Check that the lists in the query are all current. Wait up to 20 seconds
     * (or the value of MAX_WAIT_TIME) for them to become current.
     * @param pq The query with the lists to check.
     */
    private void checkListStatus(PathQuery pq) {
        Set<String> listNames = pq.getBagNames();
        Set<InterMineBag> lists = new HashSet<InterMineBag>();
        Map<String, InterMineBag> availableBags = bagManager.getBags(profile);
        for (String listName : listNames) {
            lists.add(availableBags.get(listName));
        }

        Date maximumWaitUntil = new Date(System.currentTimeMillis() + MAX_WAIT_TIME);
        boolean canContinue = false;

    LISTCHECKS:
        while (new Date().before(maximumWaitUntil) && !canContinue) {
            canContinue = true;
            for (InterMineBag list : lists) {
                String status = list.getState();
                if (!BagState.CURRENT.toString().equals(status)) {
                    canContinue = false;
                } else if (BagState.TO_UPGRADE.toString().equals(status)) {
                    break LISTCHECKS;
                }
            }
        }
        Set<String> listsWithIssues = new HashSet<String>();
        for (InterMineBag list : lists) {
            if (!BagState.CURRENT.toString().equals(list.getState())) {
                if (BagState.NOT_CURRENT.toString().equals(list.getState())) {
                    listsWithIssues.add(list.getName() + "[currently being upgraded]");
                } else if (BagState.TO_UPGRADE.toString().equals(list.getState())) {
                    listsWithIssues.add(list.getName() + "[requires manual resolution]");
                }
            }
        }
        if (!listsWithIssues.isEmpty()) {
            throw new RuntimeException("Cannot run this query, "
                    + "as the following lists are not current: "
                    + StringUtils.join(listsWithIssues, ", "));
        }
    }

}

/**
 * Class adapting ExportResultsIterator to be able to get results only in specified range
 * but is very slow, it just throws away all results before the start index.
 *
 * @author Jakub Kulaviak
 */
class ResultIterator extends ExportResultsIterator
{

    private int counter = 0;
    private final int limit;
    private final int start;

    /**
     * Constructor for ExportResultsIterator. This creates a new instance from the given
     * ObjectStore, PathQuery, and other necessary objects.
     *
     * @param pathQuery a PathQuery to run.
     * @param q The object-store query this path-query corresponds to.
     * @param results the results object created when executing the query
     * @param pathToQueryNode a map from path in pathQuery to QuerySelectable in the generated
     * ObjectStore query
     * @param start the first row of results to be returned
     * @param limit the number of result rows to return
     * @throws ObjectStoreException if something goes wrong executing the query
     */
    public ResultIterator(PathQuery pathQuery, Query q, Results results,
            Map<String, QuerySelectable> pathToQueryNode, int start, int limit)
        throws ObjectStoreException {
        super(pathQuery, q, results, pathToQueryNode);
        this.limit = limit;
        this.start = start;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        // throw away results before start index
        while (counter < start) {
            if (super.hasNext()) {
                next();
            } else {
                return false;
            }
        }

        if (counter >= (limit + start)) {
            return false;
        } else {
            return super.hasNext();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ResultElement> next() {
        List<ResultElement> ret = super.next();
        counter++;
        return ret;
    }
}
