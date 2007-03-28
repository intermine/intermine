package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.flymine.model.genomic.Annotation;
import org.flymine.model.genomic.BlastMatch;
import org.flymine.model.genomic.DataSet;
import org.flymine.model.genomic.Disease;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.Publication;
import org.flymine.model.genomic.Translation;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;
import org.intermine.postprocess.PostProcessor;

/**
 * Annotate drosophila genes with diseases related via homophila. The evidence of the annotations
 * are the Homophila database and the two homophila publications.
 *
 * @author Thomas Riley
 */
public class HomophilaPostProcess extends PostProcessor
{
    private static final Logger LOG = Logger.getLogger(HomophilaPostProcess.class);
    private DataSet homophilaDataSet;
    private Publication pub1, pub2;

    /**
     * Create a new instance of HomophilaPostProcess.
     *
     * @param osw object store writer
     */
    public HomophilaPostProcess(ObjectStoreWriter osw) {
        super(osw);
    }

    /**
     * {@inheritDoc}
     * <br/>
     * Main post-processing routine. Fill in the omimDiseases collection on drosophila genes
     * with diseases related via homophila blast matches. An Annotation object is created for
     * each reference.
     *
     * @throws ObjectStoreException if the objectstore throws an exception
     */
    public void postProcess()
        throws ObjectStoreException {

        pub1 = (Publication) DynamicUtil.createObject(Collections.singleton(Publication.class));
        pub1.setPubMedId("11381037");
        pub1 = (Publication) osw.getObjectByExample(pub1, Collections.singleton("pubMedId"));

        pub2 = (Publication) DynamicUtil.createObject(Collections.singleton(Publication.class));
        pub2.setPubMedId("11752278");
        pub2 = (Publication) osw.getObjectByExample(pub2, Collections.singleton("pubMedId"));

        homophilaDataSet = (DataSet) DynamicUtil.createObject(Collections.singleton(DataSet.class));
        homophilaDataSet.setTitle("Homophila data set");
        homophilaDataSet =
            (DataSet) osw.getObjectByExample(homophilaDataSet, Collections.singleton("title"));

        if (homophilaDataSet == null) {
            LOG.error("Failed to find homophila DataSet object");
        }
        if (pub1 == null) {
            LOG.error("Failed to find publication with id 11381037");
        }
        if (pub2 == null) {
            LOG.error("Failed to find publication with id 11752278");
        }

        Results results = findHomophilaGenesDiseases(osw.getObjectStore());
        Iterator iter = results.iterator();
        int count = 0;

        osw.beginTransaction();
        while (iter.hasNext()) {
            ResultsRow rr = (ResultsRow) iter.next();
            Gene gene = (Gene) rr.get(0);
            Disease disease = (Disease) rr.get(1);
            LOG.debug("gene = " + gene.getIdentifier() + "  disease = " + disease.getOmimId());

            Set newCollection = new HashSet();
            newCollection.add(disease);
            try {
                InterMineObject tempObject = PostProcessUtil.cloneInterMineObject(gene);
                Set oldCollection = (Set) TypeUtil.getFieldValue(tempObject, "omimDiseases");
                newCollection.addAll(oldCollection);
                TypeUtil.setFieldValue(tempObject, "omimDiseases", newCollection);
                osw.store(tempObject);

                // Create annotation
                Annotation annotation =
                    (Annotation) DynamicUtil.createObject(Collections.singleton(Annotation.class));
                annotation.setSubject(gene);
                annotation.setProperty(disease);
                annotation.addEvidence(homophilaDataSet);
                annotation.addEvidence(pub1);
                annotation.addEvidence(pub2);
                osw.store(annotation);

            } catch (IllegalAccessException e) {
                LOG.error("Object with ID: " + gene.getId() + " has no omimDiseases field");
            }

            count++;
        }
        osw.commitTransaction();

        LOG.info("Added " + count + " references to Disease to drosophila Genes");
    }

    /**
     * Run a query that returns the drosophila Genes and associated Diseases matched via
     * homophila BlastMatches.
     * @param os the objectstore
     * @return the Results object
     * @throws ObjectStoreException if there is an error while reading from the ObjectStore
     */
    protected static Results findHomophilaGenesDiseases(ObjectStore os)
        throws ObjectStoreException {
        Query q = new Query();
        QueryClass bmc = new QueryClass(BlastMatch.class);
        QueryClass tc = new QueryClass(Translation.class);
        QueryClass dgc = new QueryClass(Gene.class);
        QueryClass hgc = new QueryClass(Gene.class);
        QueryClass pc = new QueryClass(Protein.class);
        QueryClass dc = new QueryClass(Disease.class);
        q.addFrom(bmc);
        q.addFrom(tc);
        q.addFrom(dgc);
        q.addFrom(hgc);
        q.addFrom(pc);
        q.addFrom(dc);
        q.addToSelect(dgc);
        q.addToSelect(dc);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        // Constraint translation to be on gene
        QueryObjectReference tg = new QueryObjectReference(tc, "gene");
        ContainsConstraint tgc = new ContainsConstraint(tg, ConstraintOp.CONTAINS, dgc);
        cs.addConstraint(tgc);

        // Match object translation
        QueryObjectReference bmo = new QueryObjectReference(bmc, "object");
        ContainsConstraint bmoc = new ContainsConstraint(bmo, ConstraintOp.CONTAINS, tc);
        cs.addConstraint(bmoc);

        // Match subject protein
        QueryObjectReference po = new QueryObjectReference(bmc, "subject");
        ContainsConstraint poc = new ContainsConstraint(po, ConstraintOp.CONTAINS, pc);
        cs.addConstraint(poc);

        // Human gene on protein
        QueryCollectionReference pg = new QueryCollectionReference(pc, "genes");
        ContainsConstraint pgc = new ContainsConstraint(pg, ConstraintOp.CONTAINS, hgc);
        cs.addConstraint(pgc);

        // Disease on human gene
        QueryCollectionReference gd = new QueryCollectionReference(hgc, "omimDiseases");
        ContainsConstraint gdc = new ContainsConstraint(gd, ConstraintOp.CONTAINS, dc);
        cs.addConstraint(gdc);

        q.setConstraint(cs);

        ((ObjectStoreInterMineImpl) os).precompute(q, PostProcessOperationsTask.PRECOMPUTE_CATEGORY);
        Results res = new Results(q, os, os.getSequence());

        return res;
    }
}
