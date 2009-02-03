package org.intermine.web.logic.query;

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

import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.BagQueryResult;
import org.intermine.web.logic.bag.BagQueryRunner;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.results.WebResults;
import org.intermine.web.logic.search.SearchRepository;
import org.intermine.web.logic.template.TemplateQuery;

import org.apache.log4j.Logger;

/**
 * Executes a PathQuery and returns a WebResults object, to be used when multi-row
 * style results are required.  
 * @author Richard Smith
 * @author Jakub Kulaviak
 */
public class WebResultsExecutor 
{
    private static final Logger LOG = Logger.getLogger(WebResultsExecutor.class);

    private ObjectStore os;
    private Map<String, List<FieldDescriptor>> classKeys;
    private BagQueryConfig bagQueryConfig;
    private Profile profile;
    private List<TemplateQuery> conversionTemplates;
    private SearchRepository searchRepository;
    private static Map<Query, Map<String, QuerySelectable>> queryToPathToQueryNode
        = Collections.synchronizedMap(new WeakHashMap<Query, Map<String, QuerySelectable>>());

    /**
     * Constructor with necessary objects to generate an ObjectStore query from a PathQuery and
     * execute it.
     * @param os the ObjectStore to run the query in
     * @param classKeys key fields for classes in the data model
     * @param bagQueryConfig bag queries to run when interpreting LOOKUP constraints
     * @param profile the user executing the query - for access to saved lists
     * @param conversionTemplates templates used for converting bag query results between types
     * @param searchRepository global search repository to fetch saved bags from
     */
    public WebResultsExecutor(ObjectStore os,
            Map<String, List<FieldDescriptor>> classKeys,
            BagQueryConfig bagQueryConfig,
            Profile profile, List<TemplateQuery> conversionTemplates, 
            SearchRepository searchRepository) {
        this.os = os;
        this.classKeys = classKeys;
        this.bagQueryConfig = bagQueryConfig;
        this.profile = profile;
        this.conversionTemplates = conversionTemplates;
        this.searchRepository = searchRepository;
    }

    /**
     * Create and ObjectStore query from a PathQuery and execute it, returning results in a format
     * appropriate for displaying a web table.
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
     * @param pathQuery the query to execute
     * @param pathToBagQueryResult will be populated with results from bag queries used in any
     * LOOKUP constraints
     * @return results in a format appropriate for display in a web page table
     * @throws ObjectStoreException if problem running query
     */
    public WebResults execute(PathQuery pathQuery, Map<String,
            BagQueryResult> pathToBagQueryResult) throws ObjectStoreException {
        Map<String, QuerySelectable> pathToQueryNode = new HashMap<String, QuerySelectable>();

        BagQueryRunner bqr = new BagQueryRunner(os, classKeys, bagQueryConfig,
                conversionTemplates);

        Map<String, InterMineBag> allBags = WebUtil.getAllBags(profile.getSavedBags(), 
                searchRepository);
        
        Query q = MainHelper.makeQuery(pathQuery, allBags, pathToQueryNode, bqr,
                pathToBagQueryResult, false);

        Results results = os.execute(q, Constants.BATCH_SIZE, true, true, false);

        Query realQ = results.getQuery();
        if (realQ == q) {
            queryToPathToQueryNode.put(q, pathToQueryNode);
            LOG.error("queries are equal");
            LOG.error("query: " + realQ);
            LOG.error("pathToQueryNode: " + pathToQueryNode);
            LOG.error("queryToPathToQueryNode: " + queryToPathToQueryNode);
        } else {
            pathToQueryNode = queryToPathToQueryNode.get(realQ);
            LOG.error("queries are not equal");
            LOG.error("query: " + realQ);
            LOG.error("pathToQueryNode: " + pathToQueryNode);
            LOG.error("queryToPathToQueryNode: " + queryToPathToQueryNode);
        }

        WebResults webResults = new WebResults(pathQuery, results, os.getModel(),
                pathToQueryNode, classKeys, pathToBagQueryResult);

        return webResults;
    }

//    public ResultsInfo estimate(PathQuery pq) {
//        return null;
//    }
//
//    public int count(PathQuery pq) {
//        return 0;
//
//    }
}
