package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;

import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;
import org.intermine.util.XmlUtil;

import org.flymine.io.gff3.GFF3Record;

import org.apache.log4j.Logger;

/**
 * A converter/retriever for FlyBase GFF3 files.
 *
 * @author Richard Smith
 */

public class ChadoGFF3RecordHandler extends GFF3RecordHandler
{
    private Map references;
    private static final Logger LOG = Logger.getLogger(ChadoGFF3RecordHandler.class);
    private String tgtNs;
    private Map sources = new HashMap();

    /**
     * Create a new FlyBaseGFF3RecordHandler for the given target model.
     * @param tgtModel the model for which items will be created
     */
    public ChadoGFF3RecordHandler(Model tgtModel) {
        super(tgtModel);

        // create a map of classname to reference name for parent references
        // this will add the parents of any SimpleRelations from getParents() to the
        // given collection
        references = new HashMap();
        references.put("Enhancer", "gene");
        references.put("Exon", "transcripts");
        references.put("InsertionSite", "genes");
        references.put("Intron", "transcripts");
        // release 4.0 gff3 is inconsistent with parents of RNAs
        //references.put("MRNA", "gene");
        //references.put("NcRNA", "gene");
        //references.put("SnRNA", "gene");
        //references.put("SnoRNA", "gene");
        //references.put("TRNA", "gene");
        references.put("PointMutation", "gene");
        references.put("PolyASite", "processedTranscripts");
        // Region is inherited by loads of things, causes conflicts
        //references.put("Region", "gene");
        references.put("RegulatoryRegion", "gene");
        references.put("SequenceVariant", "gene");
        references.put("FivePrimeUTR", "MRNAs");
        references.put("ThreePrimeUTR", "MRNAs");
        references.put("CDS", "MRNAs");

        tgtNs = tgtModel.getNameSpace().toString();
    }

    /**
     * @see GFF3RecordHandler#process()
     */
    public void process(GFF3Record record) {

        Item feature = getFeature();
        String clsName = XmlUtil.getFragmentFromURI(feature.getClassName());
        String tgtNs = getTargetModel().getNameSpace().toString();

        // TODO get alternative ids from dbxref_2nd
        // TODO get synonyms - store if different
        // TODO get synonyms_2nd - store if different


        // set Gene.organismDbId
        // name set in core - if no name symbol is CGxx?
        // create some synonyms
        if ("Gene".equals(clsName) || "Pseudogene".equals(clsName)) {
            Iterator fbgnIter = parseFlyBaseId(record.getDbxrefs(), "FBgn").iterator();
            while (fbgnIter.hasNext()) {
                String organismDbId = (String) fbgnIter.next();
                if (!feature.hasAttribute("organismDbId")) {
                    feature.setAttribute("organismDbId", organismDbId);
                }
                addItem(createSynonym(feature, "identifier", organismDbId, "FlyBase"));
            }
            if (!feature.hasAttribute("organismDbId")) {
                LOG.warn("FlyBase gene with no FBgn: "
                         + feature.getAttribute("identifier").getValue());
            }

            // if no name set for gene then use CGxx (FlyBase symbol rules)
            if (feature.getAttribute("name") == null) {
                // TODO create additional synonym with type "name"??
                feature.setAttribute("name", feature.getAttribute("identifier").getValue());
            }

        }

        // release 4.0 GFF3 is incorrect for some insertion_sites - have Parent reference
        // to an unknown integer istead of a CG/CR number
        if (record.getParents() != null) {
            Iterator parentIter = record.getParents().iterator();
            while (parentIter.hasNext()) {
                String parentId = (String) parentIter.next();
                if (!parentId.startsWith("C")) {
                    LOG.warn("removing parent relation for " + clsName
                             + " (" + feature.getIdentifier() + ")"
                             + " because it has Parent=" + parentId);
                    Iterator relIter = getParentRelations().iterator();
                    while (relIter.hasNext()) {
                        Item relation = (Item) relIter.next();
                        items.remove("_relation" + relation.getIdentifier());
                    }
                }
            }
        }


        // set MRNA.organismDbId
        if ("MRNA".equals(clsName)) {
            // FlyBase GFF3 release 4.0 has non-coding RNAs modelled badly (fixed in 4.1)

            Iterator fbIter = parseFlyBaseId(record.getDbxrefs(), "FBtr").iterator();
            while (fbIter.hasNext()) {
                String organismDbId = (String) fbIter.next();
                if (!feature.hasAttribute("organismDbId")) {
                    feature.setAttribute("organismDbId", organismDbId);
                }
                addItem(createSynonym(feature, "identifier", organismDbId, "FlyBase"));
            }
        }

        // set TransposableElement.organismDbId
        if ("TransposableElement".equals(clsName)) {
            Iterator fbIter = parseFlyBaseId(record.getDbxrefs(), "FBti").iterator();
            while (fbIter.hasNext()) {
                String organismDbId = (String) fbIter.next();
                if (!feature.hasAttribute("organismDbId")) {
                    feature.setAttribute("organismDbId", organismDbId);
                }
                addItem(createSynonym(feature, "identifier", organismDbId, "FlyBase"));
            }
        }

        // create additional referenced gene - this is parent of
        if ("SnRNA".equals(clsName) || "NcRNA".equals(clsName) || "SnoRNA".equals(clsName)
            || "TRNA".equals(clsName)) {
            Item gene = getItemFactory().makeItem(null, tgtNs + "Gene", "");

            Iterator fbIter = parseFlyBaseId(record.getDbxrefs(), "FBgn").iterator();
            while (fbIter.hasNext()) {
                String organismDbId = (String) fbIter.next();
                if (!gene.hasAttribute("organismDbId")) {
                    gene.setAttribute("organismDbId", organismDbId);
                    addItem(gene);
                    feature.setReference("gene", gene.getIdentifier());
                }
                addItem(createSynonym(gene, "identifier", organismDbId, "FlyBase"));

                // look in synonyms for a CG identifier for gene
                List list = (List) record.getAttributes().get("synonym_2nd");
                if (list != null) {
                    Iterator iter = list.iterator();
                    while (iter.hasNext()) {
                        String synonym = (String) iter.next();
                        if (synonym.startsWith("CG") && Character.isDigit(synonym.charAt(2))) {
                            // first CG as identifier, rest as synonyms
                            if (!gene.hasAttribute("identifier")) {
                                gene.setAttribute("identifier", synonym);
                            }
                            addItem(createSynonym(gene, "identifier", organismDbId, "FlyBase"));
                        }
                    }
                }
            }
        }

        // for CDS create additional Translation object
        if ("CDS".equals(clsName)) {
            // CDSs have identifiers like CG1234-PA - we want this to be the Translation
            // identifier, for the CDS add an _CDS to the end.
            String identifier = feature.getAttribute("identifier").getValue();
            feature.setAttribute("identifier", identifier + "_CDS");

            // create and reference additional Translation object, add to polypeptides collection
            Item translation = getItemFactory().makeItem(null, tgtNs + "Translation", "");
            translation.setAttribute("identifier", identifier);

            addItem(translation);

            feature.addCollection(new ReferenceList("polypeptides",
                new ArrayList(Collections.singleton(translation.getIdentifier()))));

            Iterator fbIter = parseFlyBaseId(record.getDbxrefs(), "FBpp").iterator();
            while (fbIter.hasNext()) {
                String organismDbId = (String) fbIter.next();
                if (!feature.hasAttribute("organismDbId")) {
                    feature.setAttribute("organismDbId", organismDbId);
                }
                addItem(createSynonym(feature, "identifier", organismDbId, "FlyBase"));
            }
            // TODO add GenBank protein identifier as synonym

        }


        setReferences(references);
    }


    private Item createSynonym(Item subject, String type, String value, String sourceName) {
        Item synonym = getItemFactory().makeItem(null, tgtNs + "Synonym", "");
        synonym.setAttribute("type", type);
        synonym.setAttribute("value", value);
        synonym.setReference("subject", subject.getIdentifier());
        synonym.setReference("source", getSourceIdentifier(sourceName));
        return synonym;
    }


    private String getSourceIdentifier(String sourceName) {
        String sourceId = null;
        if (sourceName.equals("FlyBase")) {
            sourceId = getInfoSource().getIdentifier();
        } else {
            sourceId = (String) sources.get(sourceName);
            if (sourceId == null) {
                Item infoSource = getItemFactory().makeItem(null, tgtNs + "InfoSource", "");
                infoSource.setAttribute("title", sourceName);
                // store once
                addItem(infoSource);
                sources.put(sourceName, infoSource.getIdentifier());
                sourceId = infoSource.getIdentifier();
            }
        }
        return sourceId;
    }


    /**
     * Given a list of dbxrefs parse for a single FlyBase identifier with the given
     * prefix.  It is an error if more than identifier is found.
     * @param dbxrefs a list of dbxref strings retrieved from GFF3
     * @param prefix the prefix of a FlyBase identifier type - e.g. FBgn
     * @return the identifier or null if none found
     */
    protected List parseFlyBaseId(List dbxrefs, String prefix) {
        List fbs = new ArrayList();
        if (dbxrefs != null) {
            Iterator iter = dbxrefs.iterator();
            while (iter.hasNext()) {
                String s = (String) iter.next();
                if (s.startsWith("FlyBase:" + prefix)) {
                    fbs.add(s.substring(s.indexOf(":") + 1));
                }
            }
        }
        return fbs;
    }
}
