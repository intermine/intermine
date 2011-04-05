package org.intermine.bio.web.model;

/*
 * Copyright (C) 2002-2011 FlyMine
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
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
        } else {
            LOG.info("GENE MODELS cache hit");
        }
        return geneModels;
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
