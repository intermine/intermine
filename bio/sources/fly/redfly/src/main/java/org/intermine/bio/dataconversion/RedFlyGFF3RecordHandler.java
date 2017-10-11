package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.metadata.StringUtil;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;

/**
 * A converter/retriever for the REDfly (http://redfly.ccr.buffalo.edu/) GFF3 files.
 *
 * @author Kim Rutherford
 */

public class RedFlyGFF3RecordHandler extends GFF3RecordHandler
{
    private static final String REDFLY_PREFIX = "REDfly:";
    private Map<String, Item> anatomyMap = new LinkedHashMap<String, Item>();
    private Map<String, Item> geneMap = new HashMap<String, Item>();
    private Map<String, Item> publications = new HashMap<String, Item>();
    private static final String TAXON_FLY = "7227";
    protected IdResolver rslv;

    protected static final Logger LOG = Logger.getLogger(RedFlyGFF3RecordHandler.class);

    /**
     * Create a new RedFlyGFF3RecordHandler for the given target model.
     * @param tgtModel the model for which items will be created
     */
    public RedFlyGFF3RecordHandler (Model tgtModel) {
        super(tgtModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(GFF3Record record) {
        if (rslv == null) {
            rslv = IdResolverService.getFlyIdResolver();
        }

        Item feature = getFeature();

        feature.setClassName("CRM");

        String name = record.getId();

        feature.setAttribute("curated", "true");
        if (record.getAttributes().containsKey("Evidence")) {
            List<String> evidenceList = record.getAttributes().get("Evidence");
            String elementEvidence = evidenceList.get(0);
            feature.setAttribute("evidenceMethod", elementEvidence);
        }

        // unlikely to be more than one but I am leaving this
        List<String> ontologyTerm = record.getAttributes().get("Ontology_term");
        if (ontologyTerm != null) {
            List<String> anatomyItems = new ArrayList<String>();
            // string is a quoted list
            for (String commaListOfIds : ontologyTerm) {
                List<String> ontologyTermIds = new ArrayList<String>(
                        Arrays.asList(StringUtil.split(commaListOfIds, ",")));
                for (String ontologyTermId : ontologyTermIds) {
                    anatomyItems.add(getAnatomy(ontologyTermId).getIdentifier());
                }
            }

            feature.setCollection("anatomyOntology", anatomyItems);
        }

        String geneName = null;
        String pubmedId = null;
        String redflyID = null;

        List<String> dbxrefs = record.getDbxrefs();

        //Format changed. Ref to FlyReg.
        if (dbxrefs != null) {
            Iterator<String> dbxrefsIter = dbxrefs.iterator();

            while (dbxrefsIter.hasNext()) {
                String dbxref = dbxrefsIter.next();

                List<String> refList = new ArrayList<String>(
                        Arrays.asList(StringUtil.split(dbxref, ",")));
                for (String ref : refList) {
                    ref = ref.trim();
                    int colonIndex = ref.indexOf(":");
                    if (colonIndex == -1) {
                        throw new RuntimeException("external reference not understood: " + ref);
                    }

                    if (ref.startsWith("FB:")) {
                        geneName = ref.substring(colonIndex + 1);
                    } else {
                        if (ref.startsWith("PMID:")) {
                            pubmedId = ref.substring(colonIndex + 1);
                        } else {
                            if (ref.startsWith(REDFLY_PREFIX)) {
                                redflyID = ref.substring(colonIndex + 1);
                            } else {
                                throw new RuntimeException("unknown external reference type: "
                                        + ref);
                            }
                        }
                    }
                }

            }
        }

        if (geneName == null) {
            throw new RuntimeException("gene name not found when processing " + name
                    + " found these dbxrefs: " + dbxrefs);
        }
        if (pubmedId == null) {
            throw new RuntimeException("pubmed ID not found when processing " + name
                    + " found these dbxrefs: " + dbxrefs);
        }
        if (redflyID == null) {
            throw new RuntimeException("REDfly ID not found when processing " + name
                    + " found these dbxrefs: " + dbxrefs);
        }

        if (StringUtils.isEmpty(geneName)) {
            geneName = name;
        }

        Item gene = getGene(geneName);
        if (gene != null) {
            feature.setReference("gene", gene);
        }

        if (StringUtils.isNotEmpty(pubmedId)) {
            addPublication(getPublication(pubmedId));
        }

        feature.setAttribute("primaryIdentifier", name);
        feature.setAttribute("secondaryIdentifier", redflyID);
    }

    private Item getGene(String geneId) {
        if (rslv == null || !rslv.hasTaxon(TAXON_FLY)) {
            return null;
        }

        int resCount = rslv.countResolutions(TAXON_FLY, geneId);
        if (resCount != 1) {
            LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                     + geneId + " count: " + resCount + " FBgn: "
                     + rslv.resolveId(TAXON_FLY, geneId));
            return null;
        }
        String primaryIdentifier = rslv.resolveId(TAXON_FLY, geneId).iterator().next();
        Item geneItem = geneMap.get(primaryIdentifier);
        if (geneItem == null) {
            geneItem = converter.createItem("Gene");
            geneItem.setAttribute("primaryIdentifier", primaryIdentifier);
            geneItem.setReference("organism", getOrganism());
            addItem(geneItem);
            geneMap.put(primaryIdentifier, geneItem);
        }
        return geneItem;
    }

    private Item getAnatomy(String ontologyTermId) {
        if (anatomyMap.containsKey(ontologyTermId)) {
            return anatomyMap.get(ontologyTermId);
        }
        Item anatomyItem = converter.createItem("AnatomyTerm");
        anatomyItem.setAttribute("identifier", ontologyTermId);
        addItem(anatomyItem);
        anatomyMap.put(ontologyTermId, anatomyItem);
        return anatomyItem;
    }

    /**
     * Return the publication object for the given PubMed id
     *
     * @param pubmedId the PubMed ID
     * @return the publication
     */
    protected Item getPublication(String pubmedId) {
        if (publications.containsKey(pubmedId)) {
            return publications.get(pubmedId);
        }
        Item publicationItem = converter.createItem("Publication");
        publicationItem.addAttribute(new Attribute("pubMedId", pubmedId));
        addItem(publicationItem);
        publications.put(pubmedId, publicationItem);
        return publicationItem;
    }
}
