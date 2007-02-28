package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2005 FlyMine
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

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.postprocess.PostProcessor;
import org.intermine.sql.DatabaseUtil;
import org.intermine.util.TypeUtil;

import org.flymine.model.genomic.CDNAClone;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.MicroArrayResult;
import org.flymine.model.genomic.ProbeSet;
import org.flymine.model.genomic.Reporter;

import java.sql.SQLException;

import org.apache.log4j.Logger;

/**
 * Code to fix references in the classes from the mage source.
 *
 * @author Thomas Riley
 */
public class MagePostProcess extends PostProcessor
{
    private static final Logger LOG = Logger.getLogger(MagePostProcess.class);
    private Model model;

    /**
     * Create a new MagePostProcess object.
     *
     * @param osw object store writer
     */
    public MagePostProcess(ObjectStoreWriter osw) {
        super(osw);
        model = Model.getInstanceByName("genomic");
    }

    /**
     * {@inheritDoc}
     * Creates a collection of MicroArrayResult objects on Genes.
     * Creates a collection of MicroArrayResult objects on CDNAClone.
     * Creates a collection of MicroArrayResult objects on CompositeSequence.
     * @throws Exception 
     */
    public void postProcess() {
        try {
            // CDNAClone.results (MicroArrayResult)
            createCDNACloneResultsCollection();
            // CompositeSequence.results (MicroArrayResult)
            // this is commented out to work with FlyMine build
            //createCompositeSeqResultsCollection();
            // Gene.microArrayResults
            createMicroArrayResultsCollection();
        } catch (Exception e) {
            throw new RuntimeException("exception in mage post-processing", e);
        }
    }

    /**
     * Creates a collection of MicroArrayResult objects on Genes.
     * @throws ObjectStoreException 
     * @throws IllegalAccessException 
     * @throws SQLException 
     * @throws Exception if anything goes wrong
     */
    protected void createMicroArrayResultsCollection()
             throws ObjectStoreException, IllegalAccessException, SQLException {
        Query q = new Query();
        q.setDistinct(false);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);
        q.addToOrderBy(qcGene);

        QueryClass qcCDNAClone = new QueryClass(CDNAClone.class);
        q.addFrom(qcCDNAClone);
        q.addToSelect(qcCDNAClone);
        q.addToOrderBy(qcCDNAClone);

        QueryCollectionReference geneClones =
            new QueryCollectionReference(qcGene, "clones");
        ContainsConstraint ccGeneClones =
            new ContainsConstraint(geneClones, ConstraintOp.CONTAINS, qcCDNAClone);
        cs.addConstraint(ccGeneClones);

        QueryClass qcResult = new QueryClass(MicroArrayResult.class);
        q.addFrom(qcResult);
        q.addToSelect(qcResult);
        q.addToOrderBy(qcResult);

        QueryCollectionReference cloneResults =
            new QueryCollectionReference(qcCDNAClone, "results");
        ContainsConstraint ccCloneResults =
            new ContainsConstraint(cloneResults, ConstraintOp.CONTAINS, qcResult);
        cs.addConstraint(ccCloneResults);

        q.setConstraint(cs);
        ObjectStore os = osw.getObjectStore();

        ((ObjectStoreInterMineImpl) os).precompute(q, PostProcessTask.PRECOMPUTE_CATEGORY);
        Results res = new Results(q, os, os.getSequence());
        res.setBatchSize(500);

        int count = 0;
        Gene lastGene = null;
        Set newCollection = new HashSet();

        osw.beginTransaction();

        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            Gene thisGene = (Gene) rr.get(0);
            MicroArrayResult maResult = (MicroArrayResult) rr.get(2);

            if (lastGene == null || !thisGene.getId().equals(lastGene.getId())) {
                if (lastGene != null) {
                    // clone so we don't change the ObjectStore cache
                    Gene tempGene = (Gene) PostProcessUtil.cloneInterMineObject(lastGene);
                    TypeUtil.setFieldValue(tempGene, "microArrayResults", newCollection);
                    osw.store(tempGene);
                    count++;
                }
                newCollection = new HashSet();
            }

            newCollection.add(maResult);

            lastGene = thisGene;
        }

        if (lastGene != null) {
            // clone so we don't change the ObjectStore cache
            Gene tempGene = (Gene) PostProcessUtil.cloneInterMineObject(lastGene);
            TypeUtil.setFieldValue(tempGene, "microArrayResults", newCollection);
            osw.store(tempGene);
            count++;
        }
        LOG.info("Created " + count + " Gene.microArrayResults collections");
        osw.commitTransaction();


        // now ANALYSE tables relating to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName(Gene.class.getName());
            DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld, false);
        }
    }


    /**
     * Creates a collection of MicroArrayResult objects on CDNAClone.
     * @throws Exception if anything goes wrong
     */
    protected void createCDNACloneResultsCollection() throws Exception {
        Query q = new Query();
        q.setDistinct(false);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryClass qcCDNAClone = new QueryClass(CDNAClone.class);
        q.addFrom(qcCDNAClone);
        q.addToSelect(qcCDNAClone);
        q.addToOrderBy(qcCDNAClone);

        QueryClass qcReporter = new QueryClass(Reporter.class);
        q.addFrom(qcReporter);
        q.addToSelect(qcReporter);
        q.addToOrderBy(qcReporter);

        QueryObjectReference reporterMaterial =
            new QueryObjectReference(qcReporter, "material");
        ContainsConstraint ccReporterMaterial =
            new ContainsConstraint(reporterMaterial, ConstraintOp.CONTAINS, qcCDNAClone);
        cs.addConstraint(ccReporterMaterial);

        QueryClass qcResult = new QueryClass(MicroArrayResult.class);
        q.addFrom(qcResult);
        q.addToSelect(qcResult);
        q.addToOrderBy(qcResult);

        QueryCollectionReference reporterResults =
            new QueryCollectionReference(qcReporter, "results");
        ContainsConstraint ccReporterResults =
            new ContainsConstraint(reporterResults, ConstraintOp.CONTAINS, qcResult);
        cs.addConstraint(ccReporterResults);

        q.setConstraint(cs);
        ObjectStore os = osw.getObjectStore();

        ((ObjectStoreInterMineImpl) os).precompute(q, PostProcessTask.PRECOMPUTE_CATEGORY);
        Results res = new Results(q, os, os.getSequence());
        res.setBatchSize(500);

        int count = 0;
        CDNAClone lastClone = null;
        Set newCollection = new HashSet();

        osw.beginTransaction();

        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            CDNAClone thisClone = (CDNAClone) rr.get(0);
            MicroArrayResult maResult = (MicroArrayResult) rr.get(2);

            if (lastClone == null || !thisClone.getId().equals(lastClone.getId())) {
                if (lastClone != null) {
                    // clone so we don't change the ObjectStore cache
                    CDNAClone tempClone = (CDNAClone) PostProcessUtil
                        .cloneInterMineObject(lastClone);
                    TypeUtil.setFieldValue(tempClone, "results", newCollection);
                    osw.store(tempClone);
                    count++;
                }
                newCollection = new HashSet();
            }

            newCollection.add(maResult);

            lastClone = thisClone;
        }

        if (lastClone != null) {
            // clone so we don't change the ObjectStore cache
            CDNAClone tempClone = (CDNAClone) PostProcessUtil.cloneInterMineObject(lastClone);
            TypeUtil.setFieldValue(tempClone, "results", newCollection);
            osw.store(tempClone);
            count++;
        }
        LOG.info("Created " + count + " CDNAClone.results collections");
        osw.commitTransaction();

        // now ANALYSE tables relating to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName(CDNAClone.class.getName());
            DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld, false);
        }
    }



    /**
     * Creates a collection of MicroArrayResult objects on CompositeSequence.
     * @throws Exception if anything goes wrong
     */
    protected void createCompositeSeqResultsCollection() throws Exception {
        Query q = new Query();
        q.setDistinct(false);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryClass qcCompositeSeq = new QueryClass(ProbeSet.class);
        q.addFrom(qcCompositeSeq);
        q.addToSelect(qcCompositeSeq);
        q.addToOrderBy(qcCompositeSeq);

        QueryClass qcReporter = new QueryClass(Reporter.class);
        q.addFrom(qcReporter);
        q.addToSelect(qcReporter);
        q.addToOrderBy(qcReporter);

        QueryObjectReference reporterMaterial =
            new QueryObjectReference(qcReporter, "material");
        ContainsConstraint ccReporterMaterial =
            new ContainsConstraint(reporterMaterial, ConstraintOp.CONTAINS, qcCompositeSeq);
        cs.addConstraint(ccReporterMaterial);

        QueryClass qcResult = new QueryClass(MicroArrayResult.class);
        q.addFrom(qcResult);
        q.addToSelect(qcResult);
        q.addToOrderBy(qcResult);

        QueryCollectionReference reporterResults =
            new QueryCollectionReference(qcReporter, "results");
        ContainsConstraint ccReporterResults =
            new ContainsConstraint(reporterResults, ConstraintOp.CONTAINS, qcResult);
        cs.addConstraint(ccReporterResults);

        q.setConstraint(cs);
        ObjectStore os = osw.getObjectStore();

        ((ObjectStoreInterMineImpl) os).precompute(q, PostProcessTask.PRECOMPUTE_CATEGORY);
        Results res = new Results(q, os, os.getSequence());
        res.setBatchSize(500);

        int count = 0;
        ProbeSet lastComSeq = null;
        Set newCollection = new HashSet();

        osw.beginTransaction();

        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            ProbeSet thisComSeq = (ProbeSet) rr.get(0);
            MicroArrayResult maResult = (MicroArrayResult) rr.get(2);

            if (lastComSeq == null || !thisComSeq.getId().equals(lastComSeq.getId())) {
                if (lastComSeq != null) {
                    // clone so we don't change the ObjectStore cache
                    ProbeSet tempProbeSet = (ProbeSet) PostProcessUtil
                        .cloneInterMineObject(lastComSeq);
                    TypeUtil.setFieldValue(tempProbeSet, "results", newCollection);
                    osw.store(tempProbeSet);
                    count++;
                }
                newCollection = new HashSet();
            }

            newCollection.add(maResult);

            lastComSeq = thisComSeq;
        }

        if (lastComSeq != null) {
            // clone so we don't change the ObjectStore cache
            ProbeSet tempProbeSet = (ProbeSet) PostProcessUtil.cloneInterMineObject(lastComSeq);
            TypeUtil.setFieldValue(tempProbeSet, "results", newCollection);
            osw.store(tempProbeSet);
            count++;
        }
        LOG.info("Created " + count + " ProbeSet.results collections");
        osw.commitTransaction();

        // now ANALYSE tables relating to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName(ProbeSet.class.getName());
            DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld, false);
        }
    }
}
