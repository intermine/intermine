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
 * Defines a BagCreator which takes an input list and
 *
 * @author Richard Smith
 */
public class BagQueryRunner
{
	private ObjectStore os;
	private Model model;
	private Map classKeys;
	
	public BagQueryRunner(ObjectStore os, Map classKeys) {
		this.os = os;
		this.model = os.getModel();
		this.classKeys = classKeys;
	}
	
    public BagQueryResult searchForBag(String type, List input) throws ClassNotFoundException, ObjectStoreException {
    	List unresolved = new ArrayList(input);
    	Map resMap = new HashMap();
    	BagQuery bq = BagQueryHelper.createDefaultBagQuery(type, classKeys, model, new HashSet(input));
    	BagQueryResult bqr = new BagQueryResult();
    	Results res = os.execute(bq.getQuery());
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
    			}
    		}
    	}
 
    	// 
    	Iterator mapIter = resMap.entrySet().iterator();
    	while (mapIter.hasNext()) {
    		Map.Entry entry = (Map.Entry) mapIter.next();
    		Object o = entry.getKey();
    		Set ids = (Set) entry.getValue();
    		System.out.println("ids: " + ids);
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
    	return bqr;
    }
    
    public void addBagQuery(int index, BagQuery query) {
    	
    }
}
