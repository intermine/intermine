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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.ProbeSet;
import org.intermine.model.bio.MicroArrayResult;

import org.intermine.bio.util.Constants;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.postprocess.PostProcessor;

/**
 * Fill in the Gene.microArrayResults collection from Gene.probeSets.results
 *
 * @author Thomas Riley
 */
public class FlyAtlasPostProcess extends PostProcessor
{
    private static final Logger LOG = Logger.getLogger(FlyAtlasPostProcess.class);

    /**
     * Create a new instance of FlyAtlasPostProcess.
     *
     * @param osw object store writer
     */
    public FlyAtlasPostProcess(ObjectStoreWriter osw) {
        super(osw);
    }

    /**
     * {@inheritDoc}
     * <br/>
     * Main post-processing routine. Fill in the Gene.microArrayResults collection
     * from Gene.probeSets.results.
     * @throws ObjectStoreException if the objectstore throws an exception
     */
    public void postProcess()
        throws ObjectStoreException {
        Query q = new Query();
        q.setDistinct(false);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);
        q.addToOrderBy(qcGene);

        QueryClass qcProbeSet = new QueryClass(ProbeSet.class);
        q.addFrom(qcProbeSet);
        q.addToSelect(qcProbeSet);
        q.addToOrderBy(qcProbeSet);

        QueryCollectionReference geneProbeSets = new QueryCollectionReference(qcGene, "probeSets");
        cs.addConstraint(new ContainsConstraint(geneProbeSets, ConstraintOp.CONTAINS, qcProbeSet));

        QueryClass qcResult = new QueryClass(MicroArrayResult.class);
        q.addFrom(qcResult);
        q.addToSelect(qcResult);
        q.addToOrderBy(qcResult);

        QueryCollectionReference probeSetResults =
            new QueryCollectionReference(qcProbeSet, "results");
        cs.addConstraint(new ContainsConstraint(probeSetResults, ConstraintOp.CONTAINS, qcResult));

        q.setConstraint(cs);
        ObjectStore os = osw.getObjectStore();

        ((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);
        Results res = os.execute(q, 500, true, true, true);

        int count = 0;
        Gene lastGene = null;
        Set<MicroArrayResult> newCollection = new HashSet<MicroArrayResult>();

        osw.beginTransaction();

        Iterator<?> resIter = res.iterator();
        while (resIter.hasNext()) {
            ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
            Gene thisGene = (Gene) rr.get(0);
            MicroArrayResult maResult = (MicroArrayResult) rr.get(2);

            if (lastGene == null || !thisGene.getId().equals(lastGene.getId())) {
                if (lastGene != null) {
                    // clone so we don't change the ObjectStore cache
                    Gene tempGene;
                    try {
                        tempGene = PostProcessUtil.cloneInterMineObject(lastGene);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Failed to clone InterMineObject: "
                                                   + lastGene, e);
                    }
                    tempGene.setFieldValue("microArrayResults", newCollection);
                    osw.store(tempGene);
                    count++;
                }
                newCollection = new HashSet<MicroArrayResult>();
            }

            newCollection.add(maResult);

            lastGene = thisGene;
        }

        if (lastGene != null) {
            // clone so we don't change the ObjectStore cache
            Gene tempGene;
            try {
                tempGene = PostProcessUtil.cloneInterMineObject(lastGene);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to clone InterMineObject: " + lastGene, e);
            }
            tempGene.setFieldValue("microArrayResults", newCollection);
            osw.store(tempGene);
            count++;
        }
        LOG.info("Created " + count + " Gene.microArrayResults collections");
        osw.commitTransaction();
    }
}
