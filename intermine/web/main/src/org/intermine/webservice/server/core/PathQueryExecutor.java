package org.intermine.webservice.server.core;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.flatouterjoins.ObjectStoreFlatOuterJoinsImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.webservice.server.exceptions.InternalErrorException;


/**
 * Creates and executes intermine query. Code example: 
 * <pre>
 *   PathQueryExecutor executor = new PathQueryExecutor(request, pathQuery);
 *   Results results = executor.getResults();
 * </pre> 
 * @author Jakub Kulaviak
 **/
public class PathQueryExecutor
{

    private HttpServletRequest request;

    private HashMap<String, QuerySelectable> pathToQueryNode;
    
    private Query query;
    
    private PathQuery pathQuery;
    
    private Map<String, InterMineBag> bags;
    
    /**
     * Returns object store query.
     * @return query
     */
    public Query getQuery() {
        return query;
    }
    
    /**
     * Returns path query.
     * @return query
     */
    public PathQuery getPathQuery() {
        return pathQuery;
    }

    /**
     * Constructor.. 
     * @param request request
     * @param pathQuery query
     */
    public PathQueryExecutor(HttpServletRequest request, PathQuery pathQuery) {
        init(request, pathQuery, new HashMap<String, InterMineBag>());        
    }

    /**
     * Constructor.. 
     * @param request request
     * @param pathQuery query
     * @param bags required bags for executing query
     */
    public PathQueryExecutor(HttpServletRequest request, PathQuery pathQuery, 
            Map<String, InterMineBag> bags) {
        init(request, pathQuery, bags);        
    }

    private void init(HttpServletRequest request, PathQuery pathQuery, 
            Map<String, InterMineBag> bags) {
        this.pathQuery = pathQuery;
        this.request = request;
        this.bags = bags;
        ServletContext servletContext = request.getSession()
        .getServletContext();
        pathToQueryNode = new HashMap();
        try {
            // makeQuery initializes pathToQueryNode
            query = MainHelper.makeQuery(pathQuery, getBags(), pathToQueryNode,
                    servletContext, null);
        } catch (ObjectStoreException ex) {
            throw new InternalErrorException(ex);
        }
    }

    /**
     * Returns mapping that is used for parsing of Results object to get required fields
     * that are specified in view part of pathQuery. 
     * @return mapping
     * @see org.intermine.web.logic.results.WebResults#getPathToIndex(Query, Map)
     */
    public HashMap<String, QuerySelectable> getPathToQueryNode() {
        return pathToQueryNode;
    }
    
    private ObjectStore getObjectStore() {
        return (ObjectStore) request.getSession().getServletContext().
            getAttribute(Constants.OBJECTSTORE);
    }
    
    /**
     * Executes object store query and returns results.
     * @param query
     * @return results
     */
    public Results getResults() {
        return (new ObjectStoreFlatOuterJoinsImpl(getObjectStore())).execute(query);
    }
    
    private Map<String, InterMineBag> getBags() {
        return bags;
    }
}
