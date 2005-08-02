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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;

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
    private Set pseudogeneIds = new HashSet();

    // items that need extra processing that can only be done after all other GFF features have
    // been read
    private Collection finalItems = new ArrayList();

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
        // Gene.transcripts collection set in post-processing
        references.put("MRNA", "gene");
        references.put("NcRNA", "gene");
        references.put("SnRNA", "gene");
        references.put("SnoRNA", "gene");
        references.put("TRNA", "gene");
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

        // set Gene.organismDbId
        // name set in core - if no name symbol is CGxx?
        // create some synonyms
        if ("Gene".equals(clsName)) {
            Iterator fbgnIter = parseFlyBaseId(record.getDbxrefs(), "FBgn").iterator();
            while (fbgnIter.hasNext()) {
                String organismDbId = (String) fbgnIter.next();
                if (!feature.hasAttribute("organismDbId")) {
                    feature.setAttribute("organismDbId", organismDbId);
                }
                addItem(createSynonym(feature, "identifier", organismDbId, "FlyBase"));
            }

            // if no name set for gene then use CGxx (FlyBase symbol rules)
            if (feature.getAttribute("name") == null) {
                feature.setAttribute("name", feature.getAttribute("identifier").getValue());
                addItem(createSynonym(feature, "name", feature.getAttribute("name").getValue(),
                                      "FlyBase"));
            }
        }

        // In FlyBase GFF, pseudogenes are modelled as a gene with a pseudogene feature as a child.
        // We fix this by changing the pseudogene to a transcript and then fixing the gene
        // class names later
        if ("Pseudogene".equals(clsName)) {
            String className = tgtNs + "Transcript";
            // Transcript doesn't have a name attribute
            getFeature().removeAttribute("name");

            getFeature().setClassName(className);

            pseudogeneIds.addAll(record.getParents());
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

        // for CDS create additional Translation object
        if ("CDS".equals(clsName)) {
            // CDSs have identifiers like CG1234-PA - we want this to be the Translation
            // identifier, for the CDS add an _CDS to the end.
            String identifier = feature.getAttribute("identifier").getValue();
            feature.setAttribute("identifier", identifier + "_CDS");

            // create and reference additional Translation object, add to polypeptides collection
            Item translation = getItemFactory().makeItem(null, tgtNs + "Translation", "");
            translation.setReference("organism", getOrganism().getIdentifier());
            translation.setAttribute("identifier", identifier);
            translation.addCollection(new ReferenceList("evidence",
                    Arrays.asList(new Object[] {getSourceIdentifier("FlyBase")})));

            addItem(translation);
            addItem(createSynonym(translation, "identifier", identifier, "FlyBase"));

            feature.addCollection(new ReferenceList("polypeptides",
                new ArrayList(Collections.singleton(translation.getIdentifier()))));

            Iterator fbIter = parseFlyBaseId(record.getDbxrefs(), "FBpp").iterator();
            while (fbIter.hasNext()) {
                String organismDbId = (String) fbIter.next();
                if (!translation.hasAttribute("organismDbId")) {
                    translation.setAttribute("organismDbId", organismDbId);
                }
                addItem(createSynonym(translation, "identifier", organismDbId, "FlyBase"));
            }
            // TODO add GenBank protein identifier as synonym

        }

        // make sure we have a set with all existing Synonyms and those that will
        // be created by GFF3Converter
        Set synonyms = new HashSet();
        if (feature.hasAttribute("identifier")) {
            synonyms.add(feature.getAttribute("identifier").getValue());
        }
        if (feature.hasAttribute("name")) {
            synonyms.add(feature.getAttribute("name").getValue());
        }
        if (feature.hasAttribute("organismDbId")) {
            synonyms.add(feature.getAttribute("organismDbId").getValue());
        }
        Iterator itemIter = getItems().iterator();
        while (itemIter.hasNext()) {
            Item item = (Item) itemIter.next();
            if (item.getClassName().endsWith("Synonym")) {
                synonyms.add(item.getAttribute("value").getValue());
            }
        }
        List combined = new ArrayList();

        List list = (List) record.getAttributes().get("synonym_2nd");
        if (list != null) {
            combined.addAll(list);
        }
        list = (List) record.getAttributes().get("synonym");
        if (list != null) {
            combined.addAll(list);
        }

        Iterator iter = combined.iterator();
        while (iter.hasNext()) {
            String synonym = (String) iter.next();
            if (!synonyms.contains(synonym)) {
                if (synonym.startsWith("CG") || synonym.startsWith("CR")
                    || synonym.startsWith("FB")) {
                    addItem(createSynonym(feature, "identifier", synonym, "FlyBase"));
                } else {
                    addItem(createSynonym(feature, "name", synonym, "FlyBase"));
                }
                synonyms.add(synonym);
            }
        }

        if ("Gene".equals(clsName)) {
            finalItems .add(getFeature());

            // unset the feature in the Item set so that it doesn't get stored automatically
            removeFeature();
        } else {
            // set references from parent relations
            setReferences(references);
        }
    }

    /**
     * Return items that need extra processing that can only be done after all other GFF features
     * have been read.  For ChadoGFF3RecordHandler, the Gene and Pseudogene objects are returned.
     * @return the final Items
     */
    public Collection getFinalItems() {
        Iterator finalItemIter = finalItems.iterator();

        while (finalItemIter.hasNext()) {
            Item thisItem = (Item) finalItemIter.next();

            if (pseudogeneIds.contains(thisItem.getAttribute("identifier").getValue())) {
                thisItem.setClassName(tgtNs + "Pseudogene");
            }
        }

        return finalItems;
    }


    /**
     * Clear the list of final items.
     */
    public void clearFinalItems() {
        finalItems.clear();
    }

    /**
     * Create a synonym Item from the given information.
     */
    private Item createSynonym(Item subject, String type, String value, String sourceName) {
        Item synonym = getItemFactory().makeItem(null, tgtNs + "Synonym", "");
        synonym.setAttribute("type", type);
        synonym.setAttribute("value", value);
        synonym.setReference("subject", subject.getIdentifier());
        synonym.setReference("source", getSourceIdentifier(sourceName));
        return synonym;
    }


    /**
     * Get identifier of InfoSource of given name.
     * @param sourceName title of InfoSource
     * @return the identifier
     */
    protected String getSourceIdentifier(String sourceName) {
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
