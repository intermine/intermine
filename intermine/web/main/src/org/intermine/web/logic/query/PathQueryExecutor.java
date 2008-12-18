package org.intermine.web.logic.query;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.BagQueryRunner;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.results.ExportResultsIterator;
import org.intermine.web.logic.results.ResultElement;
import org.intermine.web.logic.search.SearchRepository;
import org.intermine.web.logic.template.TemplateQuery;

/**
 * Executes path query and returns results in form suitable for export or web services.
 * 
 * @author Jakub Kulaviak
 */
public class PathQueryExecutor
{
    
    private static final int DEFAULT_BATCH_SIZE = 5000;

    private static final int INFINITE_RESULTS_SIZE = 1000000000;
    
    private Map<String, InterMineBag> allBags;

    private BagQueryRunner runner;

    private ObjectStore os;

    private int batchSize = DEFAULT_BATCH_SIZE;
    
    /**
     * Sets batch size.
     * @param size batch size
     */
    public void setBatchSize(int size) {
        this.batchSize = size;
    }
        
    /**
     * Constructor with necessary objects.
     * @param os the ObjectStore to run the query in
     * @param classKeys key fields for classes in the data model
     * @param bagQueryConfig bag queries to run when interpreting LOOKUP constraints
     * @param profile the user executing the query - for access to saved lists
     * @param conversionTemplates templates used for converting bag query results between types
     * @param searchRepository global search repository to fetch saved bags from
     */
    public PathQueryExecutor(ObjectStore os,
            Map<String, List<FieldDescriptor>> classKeys,
            BagQueryConfig bagQueryConfig,
            Profile profile, List<TemplateQuery> conversionTemplates, 
            SearchRepository searchRepository) {
        this.os = os;
        this.runner = new BagQueryRunner(os, classKeys, bagQueryConfig,
                conversionTemplates);
        this.allBags = WebUtil.getAllBags(profile.getSavedBags(), 
                searchRepository);    
    }
            
    /**
     * Executes object store query and returns results as iterator over rows. Every row is a list 
     * of result elements.
     * @param pathQuery path query to be executed 
     * @return results
     */
    public Iterator<List<ResultElement>> execute(PathQuery pathQuery) {
        return execute(pathQuery, INFINITE_RESULTS_SIZE);
    }

    /**
     * Executes object store query and returns results as iterator over rows. Every row is a list 
     * of result elements.
     * @param pathQuery path query to be executed
     * @param limit maximum number of results 
     * @return results
     */
    public Iterator<List<ResultElement>> execute(PathQuery pathQuery, final int limit) {
        final ExportResultsIterator resultIt;
        try {
            resultIt = new ExportResultsIterator(os, pathQuery, allBags, runner);
            resultIt.setBatchSize(batchSize);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Creating export results iterator failed", e);
        }
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
