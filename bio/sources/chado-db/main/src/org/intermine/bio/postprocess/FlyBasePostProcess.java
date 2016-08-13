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

import org.intermine.bio.util.Constants;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Intron;
import org.intermine.model.bio.Transcript;
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
 * Copy transcript.introns to gene.introns.
 *
 * We can't do this in the main chado processor because
 * the gene.transcript and transcript.introns relationships are made at the same time.  We can't use
 * the finishedProcessing() method because we don't have an objectstore.  That class is too
 * complicated as it is anyway.  Might should move this to IntronUtil though.
 * @author Julie Sullivan
 */
public class FlyBasePostProcess extends PostProcessor
{

    /**
     * Create a new instance of FlyBasePostProcess
     *
     * @param osw object store writer
     */
    public FlyBasePostProcess(ObjectStoreWriter osw) {
        super(osw);
    }

    /**
     * {@inheritDoc}
     * <br/>
     * Main post-processing routine. Fill in the Gene.introns collection
     * from Gene.introns
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

        QueryClass qcTranscript = new QueryClass(Transcript.class);
        q.addFrom(qcTranscript);
        q.addToSelect(qcTranscript);
        q.addToOrderBy(qcTranscript);

        QueryCollectionReference geneTrans = new QueryCollectionReference(qcGene, "transcripts");
        cs.addConstraint(new ContainsConstraint(geneTrans, ConstraintOp.CONTAINS, qcTranscript));

        QueryClass qcIntrons = new QueryClass(Intron.class);
        q.addFrom(qcIntrons);
        q.addToSelect(qcIntrons);
        q.addToOrderBy(qcIntrons);

        QueryCollectionReference introns = new QueryCollectionReference(qcTranscript, "introns");
        cs.addConstraint(new ContainsConstraint(introns, ConstraintOp.CONTAINS, qcIntrons));

        q.setConstraint(cs);
        ObjectStore os = osw.getObjectStore();

        ((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);
        Results res = os.execute(q, 500, true, true, true);

        int count = 0;
        Gene lastGene = null;
        Set<Intron> newCollection = new HashSet<Intron>();

        osw.beginTransaction();

        Iterator<?> resIter = res.iterator();
        while (resIter.hasNext()) {
            ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
            Gene thisGene = (Gene) rr.get(0);
            Intron intron = (Intron) rr.get(2);

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
                    tempGene.setFieldValue("introns", newCollection);
                    osw.store(tempGene);
                    count++;
                }
                newCollection = new HashSet<Intron>();
            }

            newCollection.add(intron);

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
            tempGene.setFieldValue("introns", newCollection);
            osw.store(tempGene);
            count++;
        }
//        LOG.info("Created " + count + " Gene.introns collections");
        osw.commitTransaction();
    }
}
