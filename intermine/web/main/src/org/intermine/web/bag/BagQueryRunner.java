package org.intermine.web.bag;

/*
 * Copyright (C) 2002-2006 FlyMine
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

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

/**
 * For a given list of input strings search for objects using default and configured
 * queries for a particular type.
 *
 * @author Richard Smith
 */
public class BagQueryRunner
{
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
    public BagQueryResult searchForBag(String type, List input) throws ClassNotFoundException, ObjectStoreException {
    	List queries = getBagQueriesForType(bagQueries, type, input);
    	List unresolved = new ArrayList(input);
    	Iterator qIter = queries.iterator();
		BagQueryResult bqr = new BagQueryResult();
    	while (qIter.hasNext()) {
    		BagQuery bq = (BagQuery) qIter.next();
    		Map resMap = new HashMap();
    		Results res = os.execute(bq.getQuery(unresolved));
    		Iterator resIter = res.iterator();
    		while (resIter.hasNext()) {
    			ResultsRow row = (ResultsRow) resIter.next();
    			Integer id = (Integer) row.get(0);
    			for (int i = 1; i < row.size(); i++) {
    				Object o = row.get(i);
    				if (o != null && input.contains(o)) {
    					Set ids = (Set) resMap.get(o);
    					if (ids == null) {
    						ids = new HashSet();
    						resMap.put(o, ids);
    					}
    					ids.add(id);
    					unresolved.remove(o);
    				}
    			}
    		}
    		addResults(resMap, unresolved, bqr, bq);
    	}
    	return bqr;
    }
    
    /**
     * Add results from resMap to a a BagQueryResults object.
     */
    private void addResults(Map resMap, List unresolved, BagQueryResult bqr, BagQuery bq) 
    	throws ObjectStoreException {
    	Iterator mapIter = resMap.entrySet().iterator();
    	while (mapIter.hasNext()) {
    		Map.Entry entry = (Map.Entry) mapIter.next();
    		Object o = entry.getKey();
    		Set ids = (Set) entry.getValue();
    		if (ids.size() == 1) {
    			if (!bq.matchesAreIssues()) {
    				bqr.addMatch((String) o, (Integer) ids.iterator().next());
    			} else {
    				Set objs = new HashSet();
    				Iterator objIter = os.getObjectsByIds(ids).iterator();
    				while (objIter.hasNext()) {
    					objs.add(((List) objIter.next()).get(0));
    				}
    				bqr.addIssue(BagQueryResult.OTHER, bq.getMessage(), (String) o, 
    						objs);
    			}
    		} else {
    			Set objs = new HashSet();
				Iterator objIter = os.getObjectsByIds(ids).iterator();
				while (objIter.hasNext()) {
					objs.add(((List) objIter.next()).get(0));
				}
    			bqr.addIssue(BagQueryResult.DUPLICATE, bq.getMessage(), (String) entry.getKey(), 
    					objs);
    		}
    		unresolved.remove(o);
    	}
    	bqr.setUnresolved(unresolved);
    }
    
    // temporary method - will be replaced by BagQueryHelper method
    private List getBagQueriesForType(Map bagQueries, String type, List input) throws ClassNotFoundException {
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
