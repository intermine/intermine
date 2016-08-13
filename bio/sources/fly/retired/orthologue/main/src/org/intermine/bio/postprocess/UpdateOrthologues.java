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

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Homologue;
import org.intermine.model.bio.Protein;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.postprocess.PostProcessor;

/**
 * Orthologues from INPARANOID are attched to Proteins not Genes, this class
 * finds all Orthologues and Paralogues on Proteins and creates references to
 * to corresponding Genes, duplicating the [Orth|Para]logues if the Protein
 * references multiple Genes.
 *
 * @author Richard Smith
 */
public class UpdateOrthologues extends PostProcessor
{
    private static final Logger LOG = Logger.getLogger(UpdateOrthologues.class);
    protected ObjectStore os;

    /**
     * Create a new UpdateOrthologes object from an ObjectStoreWriter
     * @param osw writer on genomic ObjectStore
     */
    public UpdateOrthologues(ObjectStoreWriter osw) {
        super(osw);
        this.os = osw.getObjectStore();
    }


    /**
     * Find orthologues with protein and homologueProtein set and, where possible,
     * set object and subject to corresponding genes.
     * @throws ObjectStoreException if anything goes wrong
     */
    public void postProcess() throws ObjectStoreException {
        os.flushObjectById();
        LOG.info("Updating Orthologue objects");
        update("protein");
        os.flushObjectById();
        LOG.info("Updating Orthologue subjects");
        update("homologueProtein");
    }

    /**
     * Insert object/subject from Orthologues/Paralogues to Genes where currently protein
     * and homologueProtein references are set and references from Proteins to Genes are
     * available. Treats subject and object of Orthologue/Paralogue as separate, should be called
     * for each.
     * @param relationClass either Orthologue or Paralogue
     * @param refType the relation to set - either "subject" or "object"
     */
    private void update(String refType) throws ObjectStoreException {

        // query for [Ortho|Para]logue, protein/homologueProtein Protein and Protein.genes
        Query q = new Query();
        q.setDistinct(false);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryClass qcRel = new QueryClass(Homologue.class);
        q.addFrom(qcRel);
        q.addToSelect(qcRel);

        QueryClass qcProtein = new QueryClass(Protein.class);
        q.addFrom(qcProtein);
        q.addToSelect(qcProtein);
        QueryObjectReference refProtein = new QueryObjectReference(qcRel, refType);
        ContainsConstraint cc1 = new ContainsConstraint(refProtein, ConstraintOp.CONTAINS,
                                                        qcProtein);
        cs.addConstraint(cc1);

        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);
        QueryCollectionReference colGenes = new QueryCollectionReference(qcProtein, "genes");
        ContainsConstraint cc2 = new ContainsConstraint(colGenes, ConstraintOp.CONTAINS, qcGene);
        cs.addConstraint(cc2);
        q.setConstraint(cs);
        q.addToOrderBy(qcRel);
        q.addToOrderBy(qcProtein);
        q.addToOrderBy(qcGene);

        Results res = os.execute(q, 500, true, true, true);

        Protein lastProtein = null;
        Gene lastGene = null;
        InterMineObject lastObject = null;
        int updated = 0;
        int created = 0;

        // set gene or [orthologue|paralogue]
        String newRef = "protein".equals(refType) ? "gene"
            : refType.substring(0, refType.indexOf('P'));

        Iterator<?> resIter = res.iterator();
        osw.beginTransaction();
        while (resIter.hasNext()) {
            ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
            InterMineObject o = (InterMineObject) rr.get(0);
            Protein protein = (Protein) rr.get(1);
            Gene gene = (Gene) rr.get(2);

            if (!(protein.equals(lastProtein) && o.equals(lastObject))) {
                // clone so we don't change ObjectStore cache
                try {
                    InterMineObject newO = PostProcessUtil.cloneInterMineObject(o);
                    // set reference to Gene
                    newO.setFieldValue(newRef, gene);
                    osw.store(newO);
                    updated++;
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            } else if (!(gene.equals(lastGene) && o.equals(lastObject))) {
                try {
                    // create new [Ortho|Para]logue and set reference to Gene
                    InterMineObject newO = PostProcessUtil.copyInterMineObject(o);
                    newO.setFieldValue(newRef, gene);
                    osw.store(newO);
                    created++;
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            } else {
                LOG.info("Found duplicate Genes (" + lastGene.getSecondaryIdentifier() + ", "
                         + gene.getSecondaryIdentifier() + ") in genes collection of protein: "
                         + protein.getSecondaryIdentifier());
            }
            lastProtein = protein;
            lastGene = gene;
            lastObject = o;

            if ((updated + created) % 1000 == 0) {
                LOG.info("updated: " + updated + " and created: " + created
                         + " [Ortho|Para]logues");
            }
        }
        osw.commitTransaction();
    }
}
