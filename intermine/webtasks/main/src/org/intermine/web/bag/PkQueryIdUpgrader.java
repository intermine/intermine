package org.intermine.web.bag;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.intermine.dataloader.DataLoaderHelper;
import org.intermine.metadata.MetaDataException;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.IntToIntMap;

public class PkQueryIdUpgrader implements IdUpgrader {
		/**
	     * For the given object from an old ObjectStore, find the corresponding InterMineObjects in a
	     * new ObjectStore.  Primary keys are used to find the objects.
	     * @param oldObject the template object
	     * @param os ObjectStore used to resolve objects
	     * @return the set of new InterMineObjects
	     */
	    public Set getNewIds(InterMineObject oldObject, ObjectStore os) {
	        Query query;
	        try {
	            query = DataLoaderHelper.createPKQuery(os.getModel(), oldObject, null,
	                                                   new IntToIntMap(), false);
	        } catch (MetaDataException e) {
	           throw new RuntimeException("Unable to create query for new object", e);
	        }

	        SingletonResults results = new SingletonResults(query, os, os.getSequence());

	        // faster just to execute the query immediately:
	        results.setNoOptimise();
	        results.setNoExplain();

	        int size = results.size();

	        if (size != 1) {
	            throw new RuntimeException("createPKQuery() query didn't return 1 result for: "
	                                       + oldObject.getId() + " (size was " + size + ")");
	        } else {
	            Set returnSet = new HashSet();

	            Iterator iter = results.iterator();

	            while (iter.hasNext()) {
	                InterMineObject newObject = (InterMineObject) iter.next();

	                returnSet.add(newObject.getId());
	            }
	            return returnSet;
	        }
	    }
	}


