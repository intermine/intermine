package org.intermine.api.query;

/*
 * Copyright (C) 2002-2011 FlyMine
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
import org.intermine.api.LinkRedirectManager;
import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.config.Constants;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.results.WebResults;
import org.intermine.api.template.TemplatePrecomputeHelper;
import org.intermine.api.template.TemplateQuery;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.pathquery.PathQuery;

/**
 * Executes a PathQuery and returns a WebResults object, to be used when multi-row
 * style results are required.
 *
 * @author Richard Smith
 * @author Jakub Kulaviak
 */
public class WebResultsExecutor extends QueryExecutor
{
    private ObjectStore os;
    private Profile profile;
    private BagManager bagManager;
    private BagQueryRunner bagQueryRunner;
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
        if (realQ == q) {
            queryToPathToQueryNode.put(q, pathToQueryNode);
        } else {
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

        Map<String, InterMineBag> allBags = bagManager.getUserAndGlobalBags(profile);

        Query q = MainHelper.makeQuery(pathQuery, allBags, pathToQueryNode, bagQueryRunner,
                pathToBagQueryResult);
        return q;
    }

    /**
     * Returns the results for a summary for a column in a PathQuery.
     *
     * @param pathQuery the query to summarise
     * @param summaryPath the column to summarise
     * @return a Results object with varying styles of data
     * @throws ObjectStoreException if there is a problem summarising
     */
    public Results summariseQuery(PathQuery pathQuery,
            String summaryPath) throws ObjectStoreException {
        return os.execute(makeSummaryQuery(pathQuery, summaryPath));
    }

    /**
     * Creates a query that returns the summary for a column in a PathQuery.
     *
     * @param pathQuery the query to convert
     * @param summaryPath the column to summarise
     * @return an IQL Query object
     * @throws ObjectStoreException if there is a problem creating the query
     */
    public Query makeSummaryQuery(PathQuery pathQuery,
            String summaryPath) throws ObjectStoreException {
        Map<String, QuerySelectable> pathToQueryNode = new HashMap<String, QuerySelectable>();

        Map<String, InterMineBag> allBags = bagManager.getUserAndGlobalBags(profile);
        Query q = MainHelper.makeSummaryQuery(pathQuery, summaryPath, allBags, pathToQueryNode,
                bagQueryRunner);
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
