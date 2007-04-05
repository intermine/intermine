package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;

/**
 * A converter/retriever for the REDfly (http://redfly.ccr.buffalo.edu/) GFF3 files.
 *
 * @author Kim Rutherford
 */

public class RedFlyGFF3RecordHandler extends GFF3RecordHandler
{
    private String tgtNs;
    private static final String REDFLY_PREFIX = "REDfly:";
    private Map anatomyMap = new LinkedHashMap();
    private Map geneMap = new HashMap();
    private Map publications = new HashMap();

    /**
     * Create a new RedFlyGFF3RecordHandler for the given target model.
     * @param tgtModel the model for which items will be created
     */
    public RedFlyGFF3RecordHandler (Model tgtModel) {
        super(tgtModel);
        tgtNs = tgtModel.getNameSpace().toString();
    }

    /**
     * @see GFF3RecordHandler#process(GFF3Record)
     */
    public void process(GFF3Record record) {
        Item feature = getFeature();

        feature.setClassName(tgtNs + "TFmodule");

        String name = record.getId();

        feature.addAttribute(new Attribute("curated", "true"));
        if (record.getAttributes().containsKey("Evidence")) {
            List evidenceList = (List) record.getAttributes().get("Evidence");
            String elementEvidence = (String) evidenceList.get(0);
            feature.addAttribute(new Attribute("elementEvidence", elementEvidence));
        }

        List ontologyTermIds = (List) record.getAttributes().get("Ontology_term");

        if (ontologyTermIds != null) {
            Iterator ontologyTermIdsIter = ontologyTermIds.iterator();
            List anatomyItems = new ArrayList();

            while (ontologyTermIdsIter.hasNext()) {
                String ontologyTermId = (String) ontologyTermIdsIter.next();
                anatomyItems.add(getAnatomy(ontologyTermId).getIdentifier());
            }

            feature.setCollection("anatomyOntology", anatomyItems);
        }

        String geneName = null;
        String pubmedId = null;
        String redflyID = null;

        List dbxrefs = record.getDbxrefs();

        if (dbxrefs != null) {
            Iterator dbxrefsIter = dbxrefs.iterator();
            while (dbxrefsIter.hasNext()) {
                String dbxref = (String) dbxrefsIter.next();

                int colonIndex = dbxref.indexOf(":");
                if (colonIndex == -1) {
                    throw new RuntimeException("external reference not understood: " + dbxref);
                }

                if (dbxref.startsWith("Flybase:")) {
                    geneName = dbxref.substring(colonIndex + 1);
                } else {
                    if (dbxref.startsWith("PMID:")) {
                        pubmedId = dbxref.substring(colonIndex + 1);
                    } else {
                        if (dbxref.startsWith(REDFLY_PREFIX)) {
                            redflyID = dbxref.substring(colonIndex + 1);
                        } else {
                            throw new RuntimeException("unknown external reference type: "
                                                       + dbxref);
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

        feature.setReference("gene", getGene(geneName));

        addEvidence(getPublication(pubmedId));

        feature.setAttribute("accession", REDFLY_PREFIX + redflyID);
        feature.setAttribute("identifier", name);
    }

    private Item getGene(String geneOrganismDbId) {
        if (geneMap.containsKey(geneOrganismDbId)) {
            return (Item) geneMap.get(geneOrganismDbId);
        }

        Item geneItem = getItemFactory().makeItem(null, tgtNs + "Gene", "");
        geneItem.addAttribute(new Attribute("organismDbId", geneOrganismDbId));
        geneItem.setReference("organism", getOrganism());
        addItem(geneItem);
        geneMap.put(geneOrganismDbId, geneItem);
        return geneItem;
    }

    private Item getAnatomy(String ontologyTermId) {
        if (anatomyMap.containsKey(ontologyTermId)) {
            return (Item) anatomyMap.get(ontologyTermId);
        }

        Item anatomyItem = getItemFactory().makeItem(null, tgtNs + "AnatomyTerm", "");
        anatomyItem.addAttribute(new Attribute("identifier", ontologyTermId));
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
            return (Item) publications.get(pubmedId);
        }

        Item publicationItem = getItemFactory().makeItem(null, tgtNs + "Publication", "");
        publicationItem.addAttribute(new Attribute("pubMedId", pubmedId));
        addItem(publicationItem);
        publications.put(pubmedId, publicationItem);
        return publicationItem;
    }
}
