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

import java.util.Iterator;

import org.intermine.objectstore.query.*;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.util.TypeUtil;

import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.Relation;
import org.flymine.model.genomic.Paralogue;
import org.flymine.model.genomic.Orthologue;

import org.apache.log4j.Logger;

/**
 * Orthologues from INPARANOID are attched to Proteins not Genes, this class
 * finds all Orthologues and Paralogues on Proteins and creates references to
 * to corresponding Genes, duplicating the [Orth|Para]logues if the Protein
 * references multiple Genes.
 *
 * @author Richard Smith
 */
public class UpdateOrthologues
{
    private static final Logger LOG = Logger.getLogger(UpdateOrthologues.class);

    protected ObjectStoreWriter osw;
    protected ObjectStore os;

    /**
     * Create a new UpdateOrthologes object from an ObjectStoreWriter
     * @param osw writer on genomic ObjectStore
     */
    public UpdateOrthologues(ObjectStoreWriter osw) {
        this.osw = osw;
        this.os = osw.getObjectStore();
    }


    /**
     * Find orthologues with objectProtein and subjectProtein set and, where possible,
     * set object and subject to corresponding genes.
     * @throws Exception if anything goes wrong
     */
    public void process() throws Exception {
        os.flushObjectById();
        LOG.info("Updating Orthologue objects");
        update(Orthologue.class, "object");
        os.flushObjectById();
        LOG.info("Updating Orthologue subjects");
        update(Orthologue.class, "subject");
        os.flushObjectById();
        LOG.info("Updating Paralogue objects");
        update(Paralogue.class, "object");
        os.flushObjectById();
        LOG.info("Updating Paralogue subjects");
        update(Paralogue.class, "subject");
    }

    /**
     * Insert object/subject from Orthologues/Paralogues to Genes where currently objectProtein
     * and subjectProtein references are set and references from Proteins to Genes are available.
     * Treats subject and object of Orthologue/Paralogue as separate, should be called for each.
     * @param relationClass either Orthologue or Paralogue
     * @param refType the relation to set - either "subject" or "object"
     */
    private void update(Class relationClass, String refType) throws Exception {
        String clsName = TypeUtil.unqualifiedName(relationClass.getName());
        if (!(clsName.equals("Orthologue") || (clsName.equals("Paralogue")))) {
            throw new IllegalArgumentException("relationClass was '" + clsName + "'"
                                               + " but must be 'Orthologue' or 'Paralogue'");
        }
        if (!(refType.equals("object") || refType.equals("subject"))) {
            throw new IllegalArgumentException("refType was '" + refType + "'"
                                               + " but must be 'object' or 'subject'");
        }


        // query for [Ortho|Para]logue, subject/object Protein and Protein.genes
        Query q = new Query();
        q.setDistinct(false);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryClass qcRel = new QueryClass(relationClass);
        q.addFrom(qcRel);
        q.addToSelect(qcRel);

        QueryClass qcProtein = new QueryClass(Protein.class);
        q.addFrom(qcProtein);
        q.addToSelect(qcProtein);
        String proteinRef = refType + "Protein";
        QueryObjectReference refProtein = new QueryObjectReference(qcRel, proteinRef);
        ContainsConstraint cc1 = new ContainsConstraint(refProtein, ConstraintOp.CONTAINS,
                                                        qcProtein);
        cs.addConstraint(cc1);

        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);
        QueryCollectionReference refGenes = new QueryCollectionReference(qcProtein, "genes");
        ContainsConstraint cc2 = new ContainsConstraint(refGenes, ConstraintOp.CONTAINS, qcGene);
        cs.addConstraint(cc2);
        q.setConstraint(cs);
        q.addToOrderBy(qcRel);
        q.addToOrderBy(qcProtein);
        q.addToOrderBy(qcGene);

        os = osw.getObjectStore();
        Results res = new Results(q, os, os.getSequence());
        res.setBatchSize(500);

        Protein lastProtein = null;
        Gene lastGene = null;
        Relation lastRelation = null;
        int updated = 0;
        int created = 0;

        Iterator resIter = res.iterator();
        osw.beginTransaction();
        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            Relation relation = (Relation) rr.get(0);
            Protein protein = (Protein) rr.get(1);
            Gene gene = (Gene) rr.get(2);

            if (!(protein.equals(lastProtein) && relation.equals(lastRelation))) {
                // clone so we don't change ObjectStore cache
                Relation newRelation = (Relation) PostProcessUtil.cloneInterMineObject(relation);
                // set reference to Gene
                TypeUtil.setFieldValue(newRelation, refType, gene);
                osw.store(newRelation);
                updated++;
            } else if (!(gene.equals(lastGene) && relation.equals(lastRelation))) {
                // create new [Ortho|Para]logue and set reference to Gene
                Relation newRelation = (Relation) PostProcessUtil.copyInterMineObject(relation);
                TypeUtil.setFieldValue(newRelation, refType, gene);
                osw.store(newRelation);
                created++;
            } else {
                LOG.info("Found duplicate Genes (" + lastGene.getIdentifier() + ", "
                         + gene.getIdentifier() + ") in genes collection of protein: "
                         + protein.getIdentifier());
            }
            lastProtein = protein;
            lastGene = gene;
            lastRelation = relation;

            if ((updated + created) % 100 == 0) {
                LOG.info("updated: " + updated + " and created: " + created
                         + " [Ortho|Para]logues");
            }
        }
        osw.commitTransaction();
    }
}
