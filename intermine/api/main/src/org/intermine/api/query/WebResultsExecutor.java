package org.intermine.api.query;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.config.Constants;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.results.WebResults;
import org.intermine.api.template.TemplateQuery;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;
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
public class WebResultsExecutor
{
    private ObjectStore os;
    private Map<String, List<FieldDescriptor>> classKeys;
    private BagQueryConfig bagQueryConfig;
    private Profile profile;
    private List<TemplateQuery> conversionTemplates;
    private BagManager bagManager;
    private static Map<Query, Map<String, QuerySelectable>> queryToPathToQueryNode
        = Collections.synchronizedMap(new WeakHashMap<Query, Map<String, QuerySelectable>>());


    /**
     * Constructor with necessary objects to generate an ObjectStore query from a PathQuery and
     * execute it.
     *
     * @param os the ObjectStore to run the query in
     * @param classKeys key fields for classes in the data model
     * @param bagQueryConfig bag queries to run when interpreting LOOKUP constraints
     * @param profile the user executing the query - for access to saved lists
     * @param conversionTemplates templates used for converting bag query results between types
     * @param bagManager access to global and user bags
     */
    public WebResultsExecutor(ObjectStore os,
            Map<String, List<FieldDescriptor>> classKeys,
            BagQueryConfig bagQueryConfig,
            Profile profile, List<TemplateQuery> conversionTemplates,
            BagManager bagManager) {
        this.os = os;
        this.classKeys = classKeys;
        this.bagQueryConfig = bagQueryConfig;
        this.profile = profile;
        this.conversionTemplates = conversionTemplates;
        this.bagManager = bagManager;
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

        WebResults webResults = new WebResults(pathQuery, results, os.getModel(),
                pathToQueryNode, classKeys, pathToBagQueryResult);

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
        BagQueryRunner bqr = new BagQueryRunner(os, classKeys, bagQueryConfig,
                conversionTemplates);

        Map<String, InterMineBag> allBags = bagManager.getUserAndGlobalBags(profile);

        Query q = MainHelper.makeQuery(pathQuery, allBags, pathToQueryNode, bqr,
                pathToBagQueryResult, false);
        return q;
    }
}
