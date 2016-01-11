package org.intermine.bio.web.model;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStore;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.util.CacheMap;
import org.intermine.util.DynamicUtil;

/**
 * A cache for gene models by Gene object id.  A GeneModel represetnts a transcripts with exons,
 * introns, UTRs and CDSs where data are available.  Lookups can be done by any component of the
 * gene model and the corresponding gene will be found first.  The implementation uses a CacheMap
 * to allow garbage collection.
 * @author Richard Smith
 *
 */
public final class GeneModelCache
{
    private static CacheMap<Integer, List<GeneModel>> cache =
        new CacheMap<Integer, List<GeneModel>>();
    protected static final Logger LOG = Logger.getLogger(GeneModelCache.class);
    private static Map<String, GeneModelSettings> organismSettings =
        new HashMap<String, GeneModelSettings>();

    private GeneModelCache() {
    }

    /**
     * Fetch a list of gene models for a gene.  The method can be called with an component of a gene
     * model (Transcript, Exon, Intron, UTR, CDS) and corresponding gene will be returned first.
     * e.g. if called with an exon, the related gene will be found and all gene models created for
     * all transcripts of that gene regardless of whether they contain that exon.  If called with an
     * object that isn't a gene model component an empty list is returned.
     * @param object a the gene to get gene models for, or a component of a gene model
     * @param model the data model
     * @return a list of GeneModels, one per transcript or an empty list
     */
    public static List<GeneModel> getGeneModels(InterMineObject object, Model model) {
        String clsName = DynamicUtil.getSimpleClass(object).getSimpleName();

        // TODO make this deal with inheritance (transcripts and UTRs in on statement)
        // TODO handle UTRs better
        // TODO look up gene model components from GeneModel.TYPES
        Gene gene = null;
        if ("Gene".equals(clsName)) {
            gene = (Gene) object;
        } else if ("Transcript".equals(clsName) || "MRNA".equals(clsName)
                || "Exon".equals(clsName) || "UTR".equals(clsName) || "FivePrimeUTR".equals(clsName)
                || "ThreePrimeUTR".equals(clsName)) {

            try {
                gene = (Gene) object.getFieldValue("gene");
            } catch (IllegalAccessException e) {
                LOG.warn("Failed to get gene from " + clsName + ": " + object.getId());
            }
        }
        return fetchGeneModels(gene, model);
    }

    /**
     * Look up gene models for a particular gene, either fetch from cache or create new gene models
     * for each transcript.
     * @param gene the gene to fetch gene models for
     * @param model the data model
     * @return a list of gene models or an empty list
     */
    @SuppressWarnings("unchecked")
    protected static synchronized List<GeneModel> fetchGeneModels(Gene gene, Model model) {
        if (gene == null) {
            return Collections.EMPTY_LIST;
        }
        List<GeneModel> geneModels = cache.get(gene.getId());
        if (geneModels == null) {
            geneModels = new ArrayList<GeneModel>();

            try {
                Collection<InterMineObject> transcripts =
                    (Collection<InterMineObject>) gene.getFieldValue("transcripts");
                for (InterMineObject transcript : transcripts) {
                    geneModels.add(new GeneModel(model, gene, transcript));
                }
                cache.put(gene.getId(), geneModels);
            } catch (IllegalAccessException e) {
                LOG.error("Error accessing transcripts collection for gene: "
                        + gene.getPrimaryIdentifier() + ", " + gene.getId());
            }
        }
        return geneModels;
    }

    /**
     *
     * @param organismName org name
     * @param os ObjectStore
     * @return GeneModelSettings
     */
    public static GeneModelSettings getGeneModelOrganismSettings(String organismName,
            ObjectStore os) {
        if (!organismSettings.containsKey(organismName)) {
            GeneModelSettings settings = determineOrganismSettings(organismName, os);
            organismSettings.put(organismName, settings);
        }
        return organismSettings.get(organismName);

    }

    private static GeneModelSettings determineOrganismSettings(String organism, ObjectStore os) {
        GeneModelSettings settings = new GeneModelSettings(organism);

        Query query = new Query();
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        query.setConstraint(cs);
        QueryClass qcGene = new QueryClass(Gene.class);
        query.addFrom(qcGene);
        QueryClass qcOrganism = new QueryClass(Organism.class);
        query.addFrom(qcOrganism);
        QueryObjectReference orgRef = new QueryObjectReference(qcGene, "organism");
        QueryField qfOrgName = new QueryField(qcOrganism, "name");
        cs.addConstraint(new SimpleConstraint(qfOrgName, ConstraintOp.EQUALS,
                new QueryValue(organism)));
        cs.addConstraint(new ContainsConstraint(orgRef, ConstraintOp.CONTAINS, qcOrganism));

        query.addToSelect(new QueryValue("1"));

        settings.hasGenes = returnsResults(query, os);

        Model model = os.getModel();
        QueryClass qcTranscript =
            new QueryClass(model.getClassDescriptorByName("Transcript").getType());
        query.addFrom(qcTranscript);
        QueryCollectionReference transcriptsCol =
            new QueryCollectionReference(qcGene, "transcripts");
        cs.addConstraint(new ContainsConstraint(transcriptsCol,
                ConstraintOp.CONTAINS, qcTranscript));

        settings.hasTranscripts = returnsResults(query, os);

        // each call modifies the main query then returns it to the original state
        settings.hasExons = doesTranscriptHave("Exon", "exons", qcTranscript, query, os);
        settings.hasIntrons = doesTranscriptHave("Intron", "introns", qcTranscript, query, os);
        settings.hasThreePrimeUTRs = doesTranscriptHave("ThreePrimeUTR", "threePrimeUTR",
                qcTranscript, query, os);
        settings.hasFivePrimeUTRs = doesTranscriptHave("FivePrimeUTR", "fivePrimeUTR",
                qcTranscript, query, os);
        settings.hasCDSs = doesTranscriptHave("CDS", "CDSs", qcTranscript, query, os);

        return settings;
    }

    private static boolean doesTranscriptHave(String clsName, String fieldName,
            QueryClass qcTranscript, Query q, ObjectStore os) {
        Model model = os.getModel();
        if (os.getModel().hasClassDescriptor(clsName)) {
            ClassDescriptor cldTranscript =
                model.getClassDescriptorByName(qcTranscript.getType().getName());
            FieldDescriptor fld = cldTranscript.getFieldDescriptorByName(fieldName);
            if (fld != null && !fld.isAttribute()) {
                QueryClass qcTarget =
                    new QueryClass(model.getClassDescriptorByName(clsName).getType());
                q.addFrom(qcTarget);
                ConstraintSet cs = (ConstraintSet) q.getConstraint();
                ContainsConstraint cc = null;
                if (fld.isReference()) {
                    QueryObjectReference ref = new QueryObjectReference(qcTranscript, fieldName);
                    cc = new ContainsConstraint(ref, ConstraintOp.CONTAINS, qcTarget);
                    cs.addConstraint(cc);
                } else if (fld.isCollection()) {
                    QueryCollectionReference col = new QueryCollectionReference(qcTranscript,
                            fieldName);
                    cc = new ContainsConstraint(col, ConstraintOp.CONTAINS, qcTarget);
                    ((ConstraintSet) q.getConstraint()).addConstraint(cc);
                }

                boolean hasResults = returnsResults(q, os);

                // clean up the query again
                q.setConstraint(cs.removeConstraint(cc));
                q.deleteFrom(qcTarget);

                return hasResults;
            }
        }
        return false;
    }

    private static boolean returnsResults(Query q, ObjectStore os) {
        Results res = os.execute(q, 1, true, false, false);
        return res.iterator().hasNext();
    }


    /**
     * Look up the gene models for a given gene or gene model component and return the ids of all
     * objects involved.  If no gene model is found or object is not a gene model component and
     * empty set is returned.
     * @param object a gene or gene model component to look up
     * @param model the data model
     * @return the ids of all objects in the gene model or an empty set
     */
    public static Set<Integer> getGeneModelIds(InterMineObject object, Model model) {
        Set<Integer> geneModelIds = new HashSet<Integer>();
        for (GeneModel geneModel : getGeneModels(object, model)) {
            geneModelIds.addAll(geneModel.getIds());
        }
        return geneModelIds;
    }
}
