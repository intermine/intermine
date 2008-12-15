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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.bag.BagConversionHelper;
import org.intermine.web.logic.bag.BagQueryRunner;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.results.ExportResultsIterator;
import org.intermine.web.logic.results.ResultElement;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateQuery;

/**
 * Creates and executes intermine query. Code example: 
 * <pre>
 *   PathQueryExecutor executor = new PathQueryExecutor(request, pathQuery);
 *   Results results = executor.getResults();
 * </pre>
 *  
 * @author Jakub Kulaviak
 **/
public class PathQueryExecutor
{
    
    private static final int DEFAULT_BATCH_SIZE = 5000;
    
    private ExportResultsIterator it;
    
    /**
     * 
     * @param size batch size
     */
    public void setBatchSize(int size) {
        it.setBatchSize(size);
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
        ServletContext context = request.getSession().getServletContext();
        ObjectStore os = SessionMethods.getObjectStore(context);
        BagQueryRunner runner = getBagQueryRunner(context);
        try {
            it = new ExportResultsIterator(os, pathQuery, bags, runner);
            it.setBatchSize(DEFAULT_BATCH_SIZE);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Creating export results iterator failed", e);
        }
    }

    private BagQueryRunner getBagQueryRunner(ServletContext context) {
        ObjectStore os = SessionMethods.getObjectStore(context);
        Map<String, List<FieldDescriptor>> classKeys = SessionMethods.getClassKeys(context);
        List<TemplateQuery> conversionTemplates = BagConversionHelper
            .getConversionTemplates(SessionMethods.getProfileManager(context)
                    .getSuperuserProfile());
        return new BagQueryRunner(os, classKeys, SessionMethods.getBagQueryConfig(context), 
                conversionTemplates);
    }
        
    /**
     * Executes object store query and returns results as iterator over rows. Every row is a list 
     * of result elements. 
     * @return results
     */
    public Iterator<List<ResultElement>> getResults() {
        return getResults(0, 10000000);
    }

    /**
     * Executes object store query and returns results as iterator over rows. Every row is a list 
     * of result elements.
     * @param start start index
     * @param limit maximum number of results 
     * @return results
     */
    public Iterator<List<ResultElement>> getResults(int start, final int limit) {
        final ExportResultsIterator resultIt = this.it;
        Iterator<List<ResultElement>> ret = new Iterator<List<ResultElement>>() {

            private int counter = 0;
            
            public boolean hasNext() {
                if (counter >= limit) {
                    return false;
                } else {
                    return resultIt.hasNext();    
                }
            }

            public List<ResultElement> next() {
                List<ResultElement> ret = (List<ResultElement>) resultIt.next();
                counter++;
                return ret;
            }

            public void remove() {
                resultIt.remove();
            }
        };
        return ret;
    }    
}
