package org.intermine.web.bag;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.CollectionUtil;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.InitialiserPlugin;

/**
 * For a given list of input strings search for objects using default and configured
 * queries for a particular type.
 *
 * @author Richard Smith
 */
public class BagQueryRunner
{
	private static final Logger LOG = Logger.getLogger(BagQueryRunner.class);

	private ObjectStore os;
    private Model model;
    private Map classKeys, bagQueries;

    /**
     * Construct with configured bag queries and a map of type -> key fields.
     * @param os
     * @param classKeys
     * @param bagQueries
     */
    public BagQueryRunner(ObjectStore os, Map classKeys, Map bagQueries) {
        this.os = os;
        this.model = os.getModel();
        this.classKeys = classKeys;
        this.bagQueries = bagQueries;
    }


    /**
     * Given an input list of string identifiers search for corresponding
     * objects.  First run a default query then any queries configured for
     * the speified type.
     * @param type an unqualified class name to search for objects
     * @param input a list of strings to query
     * @return the matches, issues and unresolved input
     * @throws ClassNotFoundException
     * @throws ObjectStoreException
     */
    public BagQueryResult searchForBag(String type, List input)
        throws ClassNotFoundException, ObjectStoreException {
    	
    	// TODO tidy up using type String and Class
    	
    	// TODO BagQueryResult.getUnresolved() needs to return a map from input
    	// to null (if not found) or a set of objects.
    	// or just leave as a list of identifiers and objects of the qrong type
    	// CollectionUtil.groupByClass will sort out the strings and types
    	Class typeCls = Class.forName(model.getPackageName() + "." + type);
        List queries = getBagQueriesForType(bagQueries, type, input);
        List unresolved = new ArrayList(input);
        Iterator qIter = queries.iterator();
        BagQueryResult bqr = new BagQueryResult();
        while (qIter.hasNext() && !unresolved.isEmpty()) {
            BagQuery bq = (BagQuery) qIter.next();
            Map resMap = new HashMap();
            // run the next query on identifiers not yet resolved
            Results res = os.execute(bq.getQuery(unresolved));
            Iterator resIter = res.iterator();
            while (resIter.hasNext()) {
                ResultsRow row = (ResultsRow) resIter.next();
                Integer id = (Integer) row.get(0);
                for (int i = 1; i < row.size(); i++) {
                    Object field = row.get(i);
                    if (field != null && input.contains(field)) {
                        Set ids = (Set) resMap.get(field);
                        if (ids == null) {
                            ids = new HashSet();
                            resMap.put(field, ids);
                        }
                        // obj is an Integer
                        ids.add(id);
                        // remove any identifiers that are now resolved
                        unresolved.remove(field);
                    }
                }
            }
            addResults(resMap, unresolved, bqr, bq, typeCls);
        }
        return bqr;
    }

    /**
     * Add results from resMap to a a BagQueryResults object.
     */
    private void addResults(Map resMap, List unresolved, BagQueryResult bqr, BagQuery bq, Class type)
        throws ObjectStoreException {
    	Set objsOfWrongType = new HashSet();
        Iterator mapIter = resMap.entrySet().iterator();
        while (mapIter.hasNext()) {
            Map.Entry entry = (Map.Entry) mapIter.next();
            String input = (String) entry.getKey();
            Set ids = (Set) entry.getValue();
            boolean resolved = true;
            
            if (!bq.matchesAreIssues()) {
            	
            	// if matches are not issues then each entry will be a match or a duplicate
            	if (ids.size() == 1) {
            		bqr.addMatch((String) input, (Integer) ids.iterator().next());
            	} else {
            		List objs = new ArrayList();
            		Iterator objIter = os.getObjectsByIds(ids).iterator();
            		while (objIter.hasNext()) {
            			objs.add(((List) objIter.next()).get(0));
            		}
            		bqr.addIssue(BagQueryResult.DUPLICATE, bq.getMessage(), (String) entry.getKey(),
            				objs);                
            	}
            } else {
            	List objs = new ArrayList();
            	List localObjsOfWrongType = new ArrayList();
            	Iterator objIter = os.getObjectsByIds(ids).iterator();
            	
            	// we have a list of objects that result from some query, divide into any that
            	// match the type of the bag to be created and candidates for conversion
            	while (objIter.hasNext()) {
            		Object obj = ((List) objIter.next()).get(0);
            		
            		// TODO this won't cope with dynamic classes
                	Class c = (Class) DynamicUtil.decomposeClass(obj.getClass()).iterator().next();
                	if (type.isAssignableFrom(c)) {
                		objs.add(obj);
                	} else {
                		localObjsOfWrongType.add(obj);
                	}
            	}
            	
            	if (!objs.isEmpty()) {
            		// we have a list of objects, if any match the type then add to bqr as an issue
            		// discard objects that matched a different type
            		bqr.addIssue(BagQueryResult.OTHER, bq.getMessage(), (String) input, objs);
            	} else {
            		// all wrong, allow conversion attempts
            		resolved = false;
            		objsOfWrongType.addAll(localObjsOfWrongType);
            	}
            }
            if (resolved) {
            	unresolved.remove(input);
            }
        }
        
        // now objsOfWrongType contains all wrong types found for this query, try converting
        if (!objsOfWrongType.isEmpty()) {
        	// group objects by class
            Map objTypes = CollectionUtil.groupByClass(objsOfWrongType, true);
            LOG.info("objTypes = " + objTypes);
            // objects of the correct type are issues (for whatever reason)

            // find type converters for type - this will find the most specific first
            // I think TypeConverter needs a new method that will list all classes that
            // can be converted to the target type, then just object of those types
            // out objTypes maps
            
            // try to convert objects to target type
            
            // match up objects with original input identifiers
            
            // add with message = ' found by converting from x'
            //bqr.addIssue(BagQueryResult.OTHER, bq.getMessage(), (String) o,
            //			objs);
            
            
            // remove identifiers from unresolved list
           }

        // unresolved list will be used for next query
        bqr.setUnresolved(unresolved);
    }

    // temporary method - will be replaced by BagQueryHelper method
    private List getBagQueriesForType(Map bagQueries, String type, List input)
        throws ClassNotFoundException {
        List queries = new ArrayList();
        // create the default query and put it first in the list
        BagQuery defaultQuery = BagQueryHelper.createDefaultBagQuery(type, classKeys, model, input);
        if (defaultQuery != null) {
            queries.add(defaultQuery);
        }

        
        // add any queries that are configured for this type
        List bqs = (List) bagQueries.get(type);
        if (bqs != null) {
            queries.addAll(bqs);
        }
        return queries;
    }
}
