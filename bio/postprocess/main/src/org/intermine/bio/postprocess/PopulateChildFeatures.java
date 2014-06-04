package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.OntologyTerm;
import org.intermine.model.bio.SOTerm;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.metadata.TypeUtil;

/**
 * Populate the SequenceFeature.childFeatures() collection for: Gene, Transcript, Exon
 * Only used for JBrowse
 *
 * @author Julie Sullivan
 */
public class PopulateChildFeatures
{
    private static final Logger LOG = Logger.getLogger(PopulateChildFeatures.class);
    protected ObjectStoreWriter osw;
    private Model model;
    private final static String TARGET_COLLECTION = "childFeatures";
    private Map<String, Set<CollectionHolder>> parentToChildren 
    	= new HashMap<String, Set<CollectionHolder>>();
    
    /**
     * Construct with an ObjectStoreWriter, read and write from same ObjectStore
     * @param osw an ObjectStore to write to
     */
    public PopulateChildFeatures(ObjectStoreWriter osw) {
        this.osw = osw;
        this.model = Model.getInstanceByName("genomic");
    }

    /**
     * Populate the SequenceFeature.locatedFeatures() collection for: Gene, Transcript, Exon
     * and CDS
     * @throws Exception if anything goes wrong
     */
    public void populateCollection() throws Exception {
        Map<String, SOTerm> soTerms = populateSOTermMap(osw);
        Query q = getAllParents();
        Results res = osw.getObjectStore().execute(q);
        Iterator<Object> resIter = res.iterator();
        osw.beginTransaction();
        int parentCount = 0;
        int childCount = 0;
       
        while (resIter.hasNext()) {
        	ResultsRow<InterMineObject> rr = (ResultsRow<InterMineObject>) resIter.next();
        	InterMineObject parent = rr.get(0);
        	SOTerm soTerm = (SOTerm) rr.get(1);
        	InterMineObject o = PostProcessUtil.cloneInterMineObject(parent);
        	Set<InterMineObject> newCollection = getChildFeatures(soTerms, soTerm, o);
        	if (newCollection != null && !newCollection.isEmpty()) {
        		o.setFieldValue(TARGET_COLLECTION, newCollection);
        		osw.store(o);
        		parentCount++;
        		childCount += newCollection.size();
        	}
        }
        osw.commitTransaction();
        LOG.info("Stored " + childCount + " child features for " + parentCount + " parent features");
    }

	// for each collection in this class (e.g. Gene), test if it's a child feature
    private Set<InterMineObject> getChildFeatures(Map<String, SOTerm> soTerms, SOTerm soTerm, 
    		InterMineObject o) {

    	// e.g. gene
    	String parentSOTerm = soTerm.getName();
    	
    	// if we have not seen this class before, set relationships
    	if (parentToChildren.get(parentSOTerm) == null) {
    		populateParentChildMap(soTerms, parentSOTerm);
    	}
    	
    	Set<InterMineObject> newCollection = new HashSet<InterMineObject>();
    	
    	Set<CollectionHolder> childHolders = parentToChildren.get(parentSOTerm);
    	if (childHolders == null) {
    		return null;
    	}
    	for (CollectionHolder h : childHolders) {
    		String childCollectionName = h.getCollectionName();
    		String childClassName = h.getClassName();
    		try {
				Set<InterMineObject> childObjects 
					= (Set<InterMineObject>) o.getFieldValue(childCollectionName);
				newCollection.addAll(childObjects);
			} catch (IllegalAccessException e) {
				LOG.error("couldn't set relationship between " + parentSOTerm + " and " 
						+ childClassName);
				return null;
			}
    	}
    	return newCollection;
    }
        
    private void populateParentChildMap(Map<String, SOTerm> soTerms, String parentSOTermName) {
    	String parentClsName = TypeUtil.javaiseClassName(parentSOTermName);
    	ClassDescriptor cd = model.getClassDescriptorByName(parentClsName);
    	if (cd == null) {
    		LOG.error("couldn't find class in model:" + parentClsName);
    		return;
    	}
    	Class<?> parentClass = cd.getType();
    	
    	// all collections for gene
    	Map<String, Class<?>> childCollections = model.getCollectionsForClass(parentClass);

    	Set<CollectionHolder> children = new HashSet<CollectionHolder>();
    	
    	// for each collection, see if this is a child class
    	for(Map.Entry<String, Class<?>> entry : childCollections.entrySet()) {

    		String childCollectionName = entry.getKey();
    		String childClassName = entry.getValue().getSimpleName();
    		
    		// TODO use same method as in the oboparser    		    		
    		// is this a child collection? e.g. transcript
    		SOTerm soterm = soTerms.get(childClassName.toLowerCase());
    		
    		if (soterm == null) {
    			// for testing
    			continue;
    		}
    		
    		// is gene in transcript parents collection
    		for (OntologyTerm parent : soterm.getParents()) {
    			if (parent.getName().equals(parentSOTermName)) {
    				CollectionHolder h = new CollectionHolder(childClassName, childCollectionName);
    				children.add(h);
    			}
    		}
    	}
    	if (children.size() > 0) {
    		parentToChildren.put(parentSOTermName, children);
    	}
    }

    protected Map<String, SOTerm> populateSOTermMap(ObjectStore os) throws ObjectStoreException {
    	Map<String, SOTerm> soTerms = new HashMap<String, SOTerm>();
        Query q = new Query();
        q.setDistinct(false);
        
        QueryClass qcSOTerm = new QueryClass(SOTerm.class);
        q.addToSelect(qcSOTerm);
        q.addFrom(qcSOTerm);
        q.addToOrderBy(qcSOTerm);

        Results res = os.execute(q);

        Iterator it = res.iterator();
        
        while (it.hasNext()) {
        	ResultsRow<InterMineObject> rr = (ResultsRow<InterMineObject>) it.next();
        	SOTerm soTerm = (SOTerm) rr.get(0);
        	soTerms.put(soTerm.getName(), soTerm);
        }
        return soTerms;
    }
    
    protected Query getAllParents() {
        Query q = new Query();
        q.setDistinct(false);
        
        QueryClass qcFeature = new QueryClass(model.getClassDescriptorByName("SequenceFeature")
        		.getType());
        q.addToSelect(qcFeature);
        q.addFrom(qcFeature);        

        QueryClass qcSOTerm = new QueryClass(OntologyTerm.class);
        q.addToSelect(qcSOTerm);
        q.addFrom(qcSOTerm);
        q.addToOrderBy(qcSOTerm);
        
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryObjectReference ref1 = new QueryObjectReference(qcFeature, "sequenceOntologyTerm");
        cs.addConstraint(new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcSOTerm));

        // Set the constraint of the query
        q.setConstraint(cs);
        
        return q;
    }
    
    // holds the class name, e.g. transcript and the collection name, e.g. transcripts.
    // might not be necessary for most collections but matters for MRNAs, etc.
    private class CollectionHolder {
    	private String className;
    	private String collectionName;
    	
    	protected CollectionHolder(String className, String collectionName) {
    		this.className = className;
    		this.collectionName = collectionName;
    	}

    	protected String getClassName() {
			return className;
		}

    	protected String getCollectionName() {
			return collectionName;
		}
    }
}
