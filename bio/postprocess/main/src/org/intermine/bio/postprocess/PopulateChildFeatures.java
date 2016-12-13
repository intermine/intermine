package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.Model;
import org.intermine.metadata.TypeUtil;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.OntologyTerm;
import org.intermine.model.bio.SOTerm;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

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
    private static final String TARGET_COLLECTION = "childFeatures";
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
    @SuppressWarnings("unchecked")
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
        LOG.info("Stored " + childCount + " child features for " + parentCount
                + " parent features. ");
    }

    // for each collection in this class (e.g. Gene), test if it's a child feature
    @SuppressWarnings("unchecked")
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

        // all intermine collections for gene
        Map<String, Class<?>> childCollections = model.getCollectionsForClass(parentClass);
        Set<CollectionHolder> children = new HashSet<CollectionHolder>();

        // for each collection, see if this is a child class
        for (Map.Entry<String, Class<?>> entry : childCollections.entrySet()) {

            String childCollectionName = entry.getKey();
            String childClassName = entry.getValue().getSimpleName();

            // TODO use same method as in the oboparser
            // is this a child collection? e.g. transcript
            SOTerm childSOTerm = null;

            childSOTerm = lookUpChild(soTerms, childClassName);
            if (childSOTerm == null) {
                // for testing
                continue;
            }
            LOG.debug("CHILD CLASS " + childClassName + " (" + childSOTerm.getName() + ") :"
                    + childCollectionName);

            // is gene in transcript parents collection
            // exon.parents() contains transcript, but we need to match on mRNA which is a
            // subclass of transcript

            // loop through all parents
            for (OntologyTerm parent : childSOTerm.getParents()) {
                if (parent.getName().equals(parentSOTermName)) {
                    CollectionHolder h = new CollectionHolder(childClassName, childCollectionName);
                    children.add(h);
                }
            }
            // check for superclasses too
            ClassDescriptor parentClassDescr = model.getClassDescriptorByName(parentClsName);
            Set<String> parentInterMineClassNames = parentClassDescr.getSuperclassNames();

            for (String superParent : parentInterMineClassNames) {
                if (!superParent.equalsIgnoreCase("SequenceFeature")) {
                    CollectionHolder h = new CollectionHolder(childClassName, childCollectionName);
                    children.add(h);
                }
            }
        }
        if (children.size() > 0) {
            LOG.info("Adding " + children.size() + " children to parent class "
                    + parentSOTermName);
            // don't do it if parent exon or transposon fragment, see Engine.java
            parentToChildren.put(parentSOTermName, children);
        }
    }

    /**
     * @param soTerms  the map of SO terms (name, SOterm)
     * @param childClassName the name of the class
     * @return the SO term object
     *
     * TODO: add utility for the translation className -> so_term
     */
    private SOTerm lookUpChild(Map<String, SOTerm> soTerms,
            String childClassName) {
        // some specific translations
        if (childClassName.contentEquals("CDS")) {
            return soTerms.get(childClassName);
        }
//        if (childClassName.contentEquals("MiRNA")) {
//            return soTerms.get("miRNA");
//        }
        if (childClassName.contentEquals("PseudogenicTranscript")) {
            return soTerms.get("pseudogenic_transcript");
        }
        if (childClassName.contentEquals("PseudogenicExon")) {
            return soTerms.get("pseudogenic_exon");
        }
        if (childClassName.contentEquals("TransposableElement")) {
            return soTerms.get("transposable_element");
        }
        if (childClassName.contentEquals("TransposonFragment")) {
            return soTerms.get("transposon_fragment");
        }
        if (childClassName.contentEquals("FivePrimeUTR")) {
            return soTerms.get("five_prime_UTR");
        }
        if (childClassName.contentEquals("ThreePrimeUTR")) {
            return soTerms.get("three_prime_UTR");
        }
        // don't bother
        if (childClassName.contentEquals("Probe")) {
            return null;
        }
        if (childClassName.contentEquals("Allele")) {
            return null;
        }
        if (childClassName.contentEquals("Genotype")) {
            return null;
        }
        if (childClassName.contentEquals("Intron")) {
            return null;
        }
        // deafult
        return soTerms.get(childClassName.toLowerCase());
    }

    /**
     * @param os object store
     * @return map of name to so term
     * @throws ObjectStoreException if something goes wrong
     */
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
            LOG.debug("Added SO term: " + soTerm.getName());
        }
        return soTerms;
    }

    /**
     * @return query to get all parent so terms
     */
    protected Query getAllParents() {
        Query q = new Query();
        q.setDistinct(false);

        QueryClass qcFeature =
                new QueryClass(model.getClassDescriptorByName("SequenceFeature").getType());

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
    private class CollectionHolder
    {
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
