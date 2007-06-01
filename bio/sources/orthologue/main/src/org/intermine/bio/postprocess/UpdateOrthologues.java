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

import org.apache.log4j.Logger;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Orthologue;
import org.flymine.model.genomic.Paralogue;
import org.flymine.model.genomic.Translation;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.postprocess.PostProcessor;
import org.intermine.util.TypeUtil;

/**
 * Orthologues from INPARANOID are attched to Translations not Genes, this class
 * finds all Orthologues and Paralogues on Translations and creates references to
 * to corresponding Genes, duplicating the [Orth|Para]logues if the Translation
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
     * Find orthologues with objectTranslation and subjectTranslation set and, where possible,
     * set object and subject to corresponding genes.
     * @throws Exception if anything goes wrong
     */
    public void postProcess() throws ObjectStoreException {
        os.flushObjectById();
        LOG.info("Updating Orthologue objects");
        update(Orthologue.class, "translation");
        os.flushObjectById();
        LOG.info("Updating Orthologue subjects");
        update(Orthologue.class, "orthologueTranslation");
        os.flushObjectById();
        LOG.info("Updating Paralogue objects");
        update(Paralogue.class, "translation");
        os.flushObjectById();
        LOG.info("Updating Paralogue subjects");
        update(Paralogue.class, "paralogueTranslation");
    }

    /**
     * Insert object/subject from Orthologues/Paralogues to Genes where currently objectTranslation
     * and subjectTranslation references are set and references from Translations to Genes are
     * available. Treats subject and object of Orthologue/Paralogue as separate, should be called
     * for each.
     * @param relationClass either Orthologue or Paralogue
     * @param refType the relation to set - either "subject" or "object"
     */
    private void update(Class relationClass, String refType) throws ObjectStoreException {
        String clsName = TypeUtil.unqualifiedName(relationClass.getName());
        if (!(clsName.equals("Orthologue") || (clsName.equals("Paralogue")))) {
            throw new IllegalArgumentException("relationClass was '" + clsName + "'"
                                               + " but must be 'Orthologue' or 'Paralogue'");
        }
        if (!(refType.equals("translation") || refType.equals("orthologueTranslation")
                        || refType.equals("paralogueTranslation"))) {
            throw new IllegalArgumentException("refType was '" + refType + "'"
                                               + " but must be 'translation', "
                                               + "'orthologueTranslation or " 
                                               + "'paralogueTranslation'");
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
        QueryObjectReference refTranslation = new QueryObjectReference(qcRel, refType);
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
        
        Results res = os.execute(q);
        res.setBatchSize(500);

        Translation lastTranslation = null;
        Gene lastGene = null;
        InterMineObject lastObject = null;
        int updated = 0;
        int created = 0;

        // set gene or [orthologue|paralogue]
        String newRef = refType.equals("translation") ? "gene"
            : refType.substring(0, refType.indexOf('T'));

        Iterator resIter = res.iterator();
        osw.beginTransaction();
        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            InterMineObject o = (InterMineObject) rr.get(0);
            Translation translation = (Translation) rr.get(1);
            Gene gene = (Gene) rr.get(2);

            if (!(translation.equals(lastTranslation) && o.equals(lastObject))) {
                // clone so we don't change ObjectStore cache
                try {
                    InterMineObject newO = (InterMineObject) PostProcessUtil
                    .cloneInterMineObject(o);
                    // set reference to Gene
                    TypeUtil.setFieldValue(newO, newRef, gene);
                    osw.store(newO);
                    updated++;
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            } else if (!(gene.equals(lastGene) && o.equals(lastObject))) {
                try {
                    // create new [Ortho|Para]logue and set reference to Gene
                    InterMineObject newO = (InterMineObject) PostProcessUtil.copyInterMineObject(o);
                    TypeUtil.setFieldValue(newO, newRef, gene);
                    osw.store(newO);
                    created++;
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            } else {
                LOG.info("Found duplicate Genes (" + lastGene.getIdentifier() + ", "
                         + gene.getIdentifier() + ") in genes collection of translation: "
                         + translation.getIdentifier());
            }
            lastTranslation = translation;
            lastGene = gene;
            lastObject = o;

            if ((updated + created) % 100 == 0) {
                LOG.info("updated: " + updated + " and created: " + created
                         + " [Ortho|Para]logues");
            }
        }
        osw.commitTransaction();
    }
}
