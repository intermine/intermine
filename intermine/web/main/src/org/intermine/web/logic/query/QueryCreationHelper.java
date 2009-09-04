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

import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.bag.TypeConverterHelper;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.query.MainHelper;
import org.intermine.api.template.TemplateQuery;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.Constants;

public class QueryCreationHelper 
{

    /**
     * Make an InterMine query from a path query
     * @param query the PathQuery
     * @param savedBags the current saved bags map
     * @param servletContext the current servlet context
     * @param returnBagQueryResults optional parameter in which any BagQueryResult objects can be
     * returned
     * @return an InterMine Query
     * @throws ObjectStoreException if something goes wrong
     */
    public static Query makeQuery(PathQuery query, Map<String, InterMineBag> savedBags,
            ServletContext servletContext,
            Map returnBagQueryResults) throws ObjectStoreException {
        return QueryCreationHelper.makeQuery(query, savedBags, null, 
                (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER),
                returnBagQueryResults, false,
                (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE),
                (Map) servletContext.getAttribute(Constants.CLASS_KEYS),
                (BagQueryConfig) servletContext.getAttribute(Constants.BAG_QUERY_CONFIG));
    }

    /**
     * Make an InterMine query from a path query
     * @param pathQueryOrig the PathQuery
     * @param savedBags the current saved bags map
     * @param pathToQueryNode optional parameter in which path to QueryNode map can be returned
     * @param pm the ProfileManager to fetch the superuser profile
     * @param returnBagQueryResults optional parameter in which any BagQueryResult objects can be
     * @param checkOnly we're only checking the validity of the query, optimised to take less time
     * returned
     * @param os the ObjectStore that this will be run on
     * @param classKeys the class keys
     * @param bagQueryConfig the BagQueryConfig
     * @return an InterMine Query
     * @throws ObjectStoreException if something goes wrong
     */
    public static Query makeQuery(PathQuery pathQueryOrig, Map<String, InterMineBag> savedBags,
            Map<String, QuerySelectable> pathToQueryNode, ProfileManager pm,
            Map returnBagQueryResults, boolean checkOnly, ObjectStore os,
            Map<String, List<FieldDescriptor>> classKeys,
            BagQueryConfig bagQueryConfig) throws ObjectStoreException {
        List<TemplateQuery> conversionTemplates = 
            TypeConverterHelper.getConversionTemplates(pm.getSuperuserProfile());
        BagQueryRunner bagQueryRunner = null;
        if (os != null) {
            bagQueryRunner = new BagQueryRunner(os, classKeys, bagQueryConfig, conversionTemplates);
        }
        return MainHelper.makeQuery(pathQueryOrig, savedBags, pathToQueryNode, bagQueryRunner,
                returnBagQueryResults, checkOnly);
    }

    
    /**
     * Generate a query from a PathQuery, to summarise a particular column of results.
     *
     * @param pathQuery the PathQuery
     * @param savedBags the current saved bags map
     * @param pathToQueryNode Map, into which columns to display will be placed
     * @param summaryPath a String path of the column to summarise
     * @param servletContext a ServletContext
     * @return an InterMine Query
     */
    public static Query makeSummaryQuery(PathQuery pathQuery,
            Map<String, InterMineBag> savedBags,
            Map<String, QuerySelectable> pathToQueryNode,
            String summaryPath,
            ServletContext servletContext) {
        return MainHelper.makeSummaryQuery(pathQuery, savedBags, pathToQueryNode, summaryPath,
                (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE),
                (Map) servletContext.getAttribute(Constants.CLASS_KEYS),
                (BagQueryConfig) servletContext.getAttribute(Constants.BAG_QUERY_CONFIG),
                (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER));
    }
    
}
