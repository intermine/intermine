package org.intermine.api.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.config.Constants;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.results.WebResults;
import org.intermine.api.template.TemplatePrecomputeHelper;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;

/**
 * Executes a PathQuery and returns a WebResults object, to be used when multi-row
 * style results are required.
 *
 * @author Richard Smith
 * @author Jakub Kulaviak
 * @author Alex Kalderimis
 */
public class WebResultsExecutor extends QueryExecutor
{

    private Map<PathQuery, ResultsInfo> infoCache = Collections.synchronizedMap(
            new IdentityHashMap<PathQuery, ResultsInfo>());
    private InterMineAPI im;

    /**
     * Constructor with necessary objects to generate an ObjectStore query from a PathQuery and
     * execute it.
     *
     * @param im intermine API
     * @param profile the user executing the query - for access to saved lists
     */
    public WebResultsExecutor(InterMineAPI im, Profile profile) {
        os = im.getObjectStore();
        bagQueryRunner = im.getBagQueryRunner();
        this.profile = profile;
        this.im = im;
        bagManager = im.getBagManager();
        this.summaryBatchSize = Constants.BATCH_SIZE;
    }

    /**
     * Create and ObjectStore query from a PathQuery and execute it, returning results in a format
     * appropriate for displaying a web table.
     *
     * @param pathQuery the query to execute
     * @return results in a format appropriate for display in a web page table
     * @throws ObjectStoreException if problem running query
     */
    public WebResults execute(PathQuery pathQuery) throws ObjectStoreException {
        return execute(pathQuery, new HashMap<String, BagQueryResult>());
    }

    /**
     * Create and ObjectStore query from a PathQuery and execute it, returning results in a format
     * appropriate for displaying a web table.
     *
     * @param pathQuery the query to execute
     * @param pathToBagQueryResult will be populated with results from bag queries used in any
     * LOOKUP constraints
     * @return results in a format appropriate for display in a web page table
     * @throws ObjectStoreException if problem running query
     */
    public WebResults execute(PathQuery pathQuery, Map<String,
            BagQueryResult> pathToBagQueryResult) throws ObjectStoreException {
        Map<String, QuerySelectable> pathToQueryNode = new HashMap<String, QuerySelectable>();

        Query q = makeQuery(pathQuery, pathToBagQueryResult, pathToQueryNode);

        Results results = os.execute(q, Constants.BATCH_SIZE, true, true, false);

        Query realQ = results.getQuery();
        // If realQ = q this means that the query has never executed before.
        // We store pathToQueryNode for next time we call the same query
        // (when the query will no been re-executed)
        if (realQ == q) {
            queryToPathToQueryNode.put(q, pathToQueryNode);
        } else {
            // We've executed this query before, so the realQ is the query cached before
            // We have to take to pathToQueryNode matching the real query because in WebResult
            // we use the realQ and its pathToQueryNode to build the pathToIndex
            pathToQueryNode = queryToPathToQueryNode.get(realQ);
        }

        WebResults webResults = new WebResults(im, pathQuery, results, pathToQueryNode,
                pathToBagQueryResult);

        return webResults;
    }

    /**
     * Explain a query and return estimates of execution time and row count.
     *
     * @param pathQuery the query to explain
     * @return a ResultsInfo object
     * @throws ObjectStoreException if there is a problem explaining the query
     */
    public ResultsInfo explain(PathQuery pathQuery) throws ObjectStoreException {
        Query q = makeQuery(pathQuery);
        return os.estimate(q);
    }

    /**
     * Creates an SQL query from a PathQuery.
     *
     * @param pathQuery the query to convert
     * @return an SQL String
     * @throws ObjectStoreException if problem creating query
     */
    public String makeSql(PathQuery pathQuery) throws ObjectStoreException {
        Query query = makeQuery(pathQuery);
        ObjectStoreInterMineImpl osimi = (ObjectStoreInterMineImpl) os;
        return osimi.generateSql(query);
    }

    /**
     * Creates an IQL query from a PathQuery.
     *
     * @param pathQuery the query to convert
     * @return an IQL Query object
     * @throws ObjectStoreException if problem creating query
     */
    public Query makeQuery(PathQuery pathQuery) throws ObjectStoreException {
        Map<String, BagQueryResult> pathToBagQueryResult = new HashMap<String, BagQueryResult>();
        Map<String, QuerySelectable> pathToQueryNode = new HashMap<String, QuerySelectable>();
        return makeQuery(pathQuery, pathToBagQueryResult, pathToQueryNode);
    }

    /**
     * Creates an IQL query from a PathQuery.
     *
     * @param pathQuery the query to convert
     * @param pathToBagQueryResult will be populated with results from bag queries used in any
     * LOOKUP constraints
     * @param pathToQueryNode a Map from String path in the PathQuery to QuerySelectable in the
     * resulting IQL Query
     * @return an IQL Query object
     * @throws ObjectStoreException if problem creating query
     */
    public Query makeQuery(PathQuery pathQuery, Map<String, BagQueryResult> pathToBagQueryResult,
            Map<String, QuerySelectable> pathToQueryNode) throws ObjectStoreException {

        Map<String, InterMineBag> allBags = bagManager.getBags(profile);

        Query q = MainHelper.makeQuery(pathQuery, allBags, pathToQueryNode, bagQueryRunner,
                pathToBagQueryResult);
        return q;
    }

    /**
     * Precomputes a template query if it is not already precomputed, returning whether precomputing
     * was necessary.
     *
     * @param t the TemplateQuery to precompute
     * @return true if the template was not already precomputed
     * @throws ObjectStoreException if there is a problem precomputing
     */
    public boolean precomputeTemplate(TemplateQuery t) throws ObjectStoreException {
        List<QueryNode> indexes = new ArrayList<QueryNode>();
        Query q = TemplatePrecomputeHelper.getPrecomputeQuery(t, indexes);
        ObjectStoreInterMineImpl osimi = (ObjectStoreInterMineImpl) os;
        if (!osimi.isPrecomputed(q, "template")) {
            osimi.precompute(q, indexes, "template");
            return true;
        }
        return false;
    }

    /**
     * Sets a ResultsInfo entry in the infoCache for a given PathQuery. This info can then be used
     * later on the MyMine page to see a preview of how many rows are in the results.
     *
     * @param query a PathQuery object
     * @param info a ResultsInfo object corresponding to the query
     */
    public void setQueryInfo(PathQuery query, ResultsInfo info) {
        infoCache.put(query, info);
    }

    /**
     * Retrieve an entry from the infoCache.
     *
     * @param query a PathQuery object
     * @return a ResultsInfo object, or null if none is present in the cache
     */
    public ResultsInfo getQueryInfo(PathQuery query) {
        return infoCache.get(query);
    }

    /**
     * Returns the entire infoCache, which is an unmodifiable Map from PathQuery (by identity) to
     * ResultsInfo.
     *
     * @return a Map
     */
    public Map<PathQuery, ResultsInfo> getInfoCache() {
        return Collections.unmodifiableMap(infoCache);
    }
}
