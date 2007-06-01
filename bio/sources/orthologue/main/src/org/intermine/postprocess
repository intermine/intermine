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

import java.util.Iterator;

import org.intermine.objectstore.query.*;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.util.TypeUtil;

import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Translation;
import org.flymine.model.genomic.Relation;
import org.flymine.model.genomic.Paralogue;
import org.flymine.model.genomic.Orthologue;

import org.apache.log4j.Logger;

/**
 * Orthologues from INPARANOID are attched to Translations not Genes, this class
 * finds all Orthologues and Paralogues on Translations and creates references to
 * to corresponding Genes, duplicating the [Orth|Para]logues if the Translation
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
     * Find orthologues with objectTranslation and subjectTranslation set and, where possible,
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
     * Insert object/subject from Orthologues/Paralogues to Genes where currently objectTranslation
     * and subjectTranslation references are set and references from Translations to Genes are
     * available. Treats subject and object of Orthologue/Paralogue as separate, should be called
     * for each.
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


        // query for [Ortho|Para]logue, subject/object Translation and Translation.genes
        Query q = new Query();
        q.setDistinct(false);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryClass qcRel = new QueryClass(relationClass);
        q.addFrom(qcRel);
        q.addToSelect(qcRel);

        QueryClass qcTranslation = new QueryClass(Translation.class);
        q.addFrom(qcTranslation);
        q.addToSelect(qcTranslation);
        String translationRef = refType + "Translation";
        QueryObjectReference refTranslation = new QueryObjectReference(qcRel, translationRef);
        ContainsConstraint cc1 = new ContainsConstraint(refTranslation, ConstraintOp.CONTAINS,
                                                        qcTranslation);
        cs.addConstraint(cc1);

        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);
        QueryObjectReference refGene = new QueryObjectReference(qcTranslation, "gene");
        ContainsConstraint cc2 = new ContainsConstraint(refGene, ConstraintOp.CONTAINS, qcGene);
        cs.addConstraint(cc2);
        q.setConstraint(cs);
        q.addToOrderBy(qcRel);
        q.addToOrderBy(qcTranslation);
        q.addToOrderBy(qcGene);

        os = osw.getObjectStore();
        Results res = os.execute(q);
        res.setBatchSize(500);

        Translation lastTranslation = null;
        Gene lastGene = null;
        Relation lastRelation = null;
        int updated = 0;
        int created = 0;

        Iterator resIter = res.iterator();
        osw.beginTransaction();
        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            Relation relation = (Relation) rr.get(0);
            Translation translation = (Translation) rr.get(1);
            Gene gene = (Gene) rr.get(2);

            if (!(translation.equals(lastTranslation) && relation.equals(lastRelation))) {
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
                         + gene.getIdentifier() + ") in genes collection of translation: "
                         + translation.getIdentifier());
            }
            lastTranslation = translation;
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
