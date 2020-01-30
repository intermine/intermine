package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2020 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;

/**
 * DataConverter creating items from PubMed data file
 *
 * @author Jakub Kulaviak
 */
public class PubMedGeneConverter extends BioFileConverter
{
    protected static final Logger LOG = Logger.getLogger(PubMedGeneConverter.class);
    private Set<String> taxonIds = new HashSet<String>();
    private Map<String, String> publications = new HashMap<String, String>();
    protected IdResolver rslv;
    private Map<String, Item> genes = new HashMap<String, Item>();

    /**
     * @param writer item writer
     * @param model model
     * @throws ObjectStoreException if can't store the datasource/dataset
     */
    public PubMedGeneConverter(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model, "NCBI", "PubMed to gene mapping");
    }

    /**
     * Sets the list of taxonIds that should be imported
     *
     * @param taxonIds a space-separated list of taxonIds
     */
    public void setPubmedOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtils.split(taxonIds, " ")));
    }

    /**
     * Process references file and gene information file. Implementation based on the fact
     * that both files are sorted by the organism id. This is checked and if it is not true
     * an exception is thrown.
     * {@inheritDoc}
     */
    public void process(Reader reader)
        throws Exception {

        if (rslv == null) {
            rslv = IdResolverService.getIdResolverByOrganism(taxonIds);
        }

        Iterator it = FormattedTextParser.parseTabDelimitedReader(reader);
        while (it.hasNext()) {
            String[] line = (String[]) it.next();
            if (line.length != 3 || line[0].startsWith("#")) {
                // comment or bad line
                continue;
            }

            String originalTaxonId = line[0];
            String taxonId = replaceSubspecies(originalTaxonId);
            String geneId = line[1];
            String pubmedId = line[2];

            if (!taxonIds.isEmpty() && !taxonIds.contains(taxonId)) {
                continue;
            }

            String pubRefId = getPublication(pubmedId);
            Item gene = getGene(geneId, taxonId);
            if (gene != null) {
                gene.addToCollection("publications", pubRefId);
            }
        }
        for (Item gene : genes.values()) {
            store(gene);
        }
    }

    private static final Map<String, String> SUBSPECIES = new HashMap<String, String>();

    // TODO put this in config file
    static {
        SUBSPECIES.put("559292", "4932");
        SUBSPECIES.put("46245", "7237");
    }

    private String replaceSubspecies(String originalTaxonId) {
        if (SUBSPECIES.containsKey(originalTaxonId)) {
            return SUBSPECIES.get(originalTaxonId);
        }
        return originalTaxonId;
    }


    private String getPublication(String pubMedId) throws ObjectStoreException {
        String refId = publications.get(pubMedId);
        if (refId == null) {
            Item publication = createItem("Publication");
            publication.setAttribute("pubMedId", pubMedId);
            store(publication);
            refId = publication.getIdentifier();
            publications.put(pubMedId, refId);
        }
        return refId;
    }

    private Item getGene(String identifier, String taxonId) {
        String resolvedIdentifier = resolveGene(taxonId, identifier);
        if (resolvedIdentifier == null) {
            return null;
        }
        Item gene = genes.get(resolvedIdentifier);
        if (gene == null) {
            gene = createItem("Gene");
            gene.setAttribute("primaryIdentifier", resolvedIdentifier);
            gene.setReference("organism", getOrganism(taxonId));
            genes.put(resolvedIdentifier, gene);
        }
        return gene;
    }

    private String resolveGene(String taxonId, String identifier) {
        if (rslv == null || !rslv.hasTaxon(taxonId)) {
            return identifier;
        }
        int resCount = rslv.countResolutions(taxonId, identifier);

        if (resCount != 1) {
            LOG.warn("Skipping gene, could not resolve identifier: " + identifier);
            return null;
        }
        return rslv.resolveId(taxonId, identifier).iterator().next();
    }
}
