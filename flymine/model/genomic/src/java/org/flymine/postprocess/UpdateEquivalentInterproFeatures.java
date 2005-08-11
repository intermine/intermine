package org.flymine.postprocess;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.*;
import org.flymine.model.genomic.ProteinFeature;

import java.util.*;


/**
 * Sets equivalent Interpro Features to point to one another after dataloading has occured.
 *
 * @author Peter Mclaren
 */
public class UpdateEquivalentInterproFeatures {

    private static final Logger LOG = Logger.getLogger(UpdateEquivalentInterproFeatures.class);

    protected ObjectStoreWriter osw;
    protected ObjectStore os;

    public UpdateEquivalentInterproFeatures(ObjectStoreWriter osw) {
        this.osw = osw;
        this.os = osw.getObjectStore();
    }

    /***/
    public void updateEquivalentFeatures() throws ObjectStoreException {

        Query pdq = new Query();
        pdq.setDistinct(false);

        QueryClass pd1QC = new QueryClass(ProteinFeature.class);
        pdq.addToSelect(pd1QC);
        pdq.addFrom(pd1QC);
        pdq.addToOrderBy(pd1QC);

        QueryClass pd2QC = new QueryClass(ProteinFeature.class);
        pdq.addToSelect(pd2QC);
        pdq.addFrom(pd2QC);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryField pd1InterproIdQF = new QueryField(pd1QC, "interproId");
        QueryField pd2InterproIdQF = new QueryField(pd2QC, "interproId");
        SimpleConstraint equalIproIdSC = new SimpleConstraint(pd1InterproIdQF, ConstraintOp.EQUALS, pd2InterproIdQF);
        cs.addConstraint(equalIproIdSC);

        QueryField pd1IdQF = new QueryField(pd1QC, "id");
        QueryField pd2IdQF = new QueryField(pd2QC, "id");
        SimpleConstraint notEqualIntermineIdSC = new SimpleConstraint(pd1IdQF, ConstraintOp.NOT_EQUALS, pd2IdQF);
        cs.addConstraint(notEqualIntermineIdSC);

        pdq.setConstraint(cs);

        //keep a map of interpro id's to sets of their child ProteinFeature objects...
        LOG.info("UpdateEquivalentInterproFeatures - BUILDING FEATURE PAIRS");
        Map ProteinFeaturePairsMappedByInterproId = null;
        try {
            ProteinFeaturePairsMappedByInterproId = buildInterproIdIndexedMapFromQuery(pdq);
        } catch (IllegalAccessException e) {
            throw new ObjectStoreException(e);
        }

        //ok, now we need to map the domains to each other - but NOT to themselves!!!
        LOG.info("UpdateEquivalentInterproFeatures - SETTING EQUIVALENT FEATURE MAPPINGS");
        setEquivalentProteinFeatureMappings(ProteinFeaturePairsMappedByInterproId);

        //ok, now we should store any data...
        LOG.info("UpdateEquivalentInterproFeatures - STORING RESULTS");
        storeEquivalentFeatures(ProteinFeaturePairsMappedByInterproId);
    }

    private Map buildInterproIdIndexedMapFromQuery(Query pdq) throws ObjectStoreException, IllegalAccessException {

        Results results = os.execute(pdq);
        Iterator resIter = results.iterator();

        Map pdMap = new HashMap();
        Collection mappedFeaturePairs = null;
        String currentInterproId = null;

        while (resIter.hasNext()) {

            ResultsRow rr = (ResultsRow) resIter.next();
            ProteinFeature pd1 = (ProteinFeature) rr.get(0);
            ProteinFeature pd2 = (ProteinFeature) rr.get(1);

            currentInterproId = pd1.getInterproId();

            if (currentInterproId.equalsIgnoreCase(pd2.getInterproId())) {

                if (pdMap.containsKey(currentInterproId)) {

                    mappedFeaturePairs = (Collection) pdMap.get(currentInterproId);

                    if (mappedFeaturePairs.contains(pd2)) {
                        LOG.warn("SKIPPING A ProteinFeature ALREADY MAPPED TO INTERPROID:" + pd1.getInterproId());
                    } else {
                        LOG.info("MAPPING* INTERPRO_ID:" + pd1.getInterproId() + " ID1:" + pd1.getId() + " TO ID2:" + pd2.getId());

                        mappedFeaturePairs.add(new ProteinFeaturePair((ProteinFeature) PostProcessUtil.cloneInterMineObject(pd1),
                                (ProteinFeature) PostProcessUtil.cloneInterMineObject(pd2)));

                        //mappedFeaturePairs.add(new ProteinFeaturePair(pd1,pd2));
                    }
                } else {
                    Collection newFeaturePairs = new ArrayList();
                    LOG.info("MAPPING@ INTERPRO_ID:" + pd1.getInterproId() + " ID1:" + pd1.getId() + " TO ID2:" + pd2.getId());

                    newFeaturePairs.add(new ProteinFeaturePair((ProteinFeature) PostProcessUtil.cloneInterMineObject(pd1),
                            (ProteinFeature) PostProcessUtil.cloneInterMineObject(pd2)));

                    //newFeaturePairs.add(new ProteinFeaturePair(pd1,pd2));
                    pdMap.put(currentInterproId, newFeaturePairs);
                }
            } else {
                throw new ObjectStoreException("UEIM - CANT PROCESS PAIRS WITH MISMATCHING INTERPROIDS!");
            }
        }

        for (Iterator ipidit = pdMap.keySet().iterator(); ipidit.hasNext();) {

            Object nextIpId = ipidit.next();
            LOG.info("INTERPROID:" + nextIpId.toString() + " HAS THIS MANY FEATURE PAIRS:" + ((Collection) pdMap.get(nextIpId)).size());
        }

        return pdMap;
    }

    private void setEquivalentProteinFeatureMappings(Map interproIdIndexedMapOfProtenFeatures) {

        //Iterate over each collection of FeaturePairs that relate to a given interproid.
        for (Iterator ipidit = interproIdIndexedMapOfProtenFeatures.keySet().iterator(); ipidit.hasNext();) {

            Object nextIpId = ipidit.next();
            Collection nextFeaturePairCollection = (Collection) interproIdIndexedMapOfProtenFeatures.get(nextIpId);

            Map a2bSetMap = new HashMap();

            ProteinFeaturePair nextFeaturePair = null;
            ProteinFeature nextFeatureA = null;
            ProteinFeature nextFeatureB = null;

            //Loop over all the pairs and build up complete sets for each feature of its related equivalent features.
            for (Iterator nmpcit = nextFeaturePairCollection.iterator(); nmpcit.hasNext();) {

                nextFeaturePair = (ProteinFeaturePair) nmpcit.next();
                nextFeatureA = nextFeaturePair.getProteinFeatureA();
                nextFeatureB = nextFeaturePair.getProteinFeatureB();

                if (nextFeatureA.getId() != nextFeatureB.getId()) {

                    //have we seen this item A before???
                    if (a2bSetMap.containsKey(nextFeatureA)) {
                        Set a2bSet = (Set) a2bSetMap.get(nextFeatureA);
                        a2bSet.add(nextFeatureB);
                    } else {
                        Set a2bSetNew = new HashSet();
                        a2bSetNew.add(nextFeatureB);
                        a2bSetMap.put(nextFeatureA, a2bSetNew);
                    }

                } else {
                    LOG.info("SKIPPING SETTING A SELF REFERENCE IN PROTEINFEATURE.EQUIVALENTFEATURES!");
                }
            }

            //loop over each features mapped set and use the feature.setEquivalent method
            // - since the add feature one by one approach is buggy - for now at least...
            for (Iterator a2bMapIt = a2bSetMap.keySet().iterator(); a2bMapIt.hasNext();) {

                ProteinFeature theKey = (ProteinFeature)a2bMapIt.next();
                Set theSet = (Set)a2bSetMap.get(theKey);
                theKey.setEquivalentFeatures(theSet);
            }
        }
    }


    private void storeEquivalentFeatures(Map domainsWithEquivalentsSet) throws ObjectStoreException {

        Collection domainPairCollectionsToStore = domainsWithEquivalentsSet.values();
        Collection nextBunchOfPairs = null;

        ProteinFeaturePair nextProteinFeaturePair = null;
        ProteinFeature nextProteinFeatureA = null;
        ProteinFeature nextProteinFeatureB = null;

        //need to store the results...
        osw.beginTransaction();

        for (Iterator nextCollectionIt = domainPairCollectionsToStore.iterator(); nextCollectionIt.hasNext();) {

            nextBunchOfPairs = (Collection) nextCollectionIt.next();

            for (Iterator pairIt = nextBunchOfPairs.iterator(); pairIt.hasNext();) {

                nextProteinFeaturePair = (ProteinFeaturePair) pairIt.next();
                nextProteinFeatureA = nextProteinFeaturePair.getProteinFeatureA();
                nextProteinFeatureB = nextProteinFeaturePair.getProteinFeatureB();

                osw.store(nextProteinFeatureA);
                osw.store(nextProteinFeatureB);
            }
        }
        osw.commitTransaction();
    }


    //Simple little bean to make my life easier...
    private class ProteinFeaturePair {

        private ProteinFeature ProteinFeatureA = null;
        private ProteinFeature ProteinFeatureB = null;

        private ProteinFeaturePair() {
        }

        public ProteinFeaturePair(ProteinFeature ProteinFeatureA, ProteinFeature ProteinFeatureB) {
            this();
            this.ProteinFeatureA = ProteinFeatureA;
            this.ProteinFeatureB = ProteinFeatureB;
        }

        public ProteinFeature getProteinFeatureA() {
            return ProteinFeatureA;
        }

        public ProteinFeature getProteinFeatureB() {
            return ProteinFeatureB;
        }
    }

}
