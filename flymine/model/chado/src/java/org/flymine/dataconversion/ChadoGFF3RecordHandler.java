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

import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;
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
        references.put("FivePrimeUTR", "MRNAs");
        references.put("InsertionSite", "gene");
        references.put("Intron", "transcripts");
        references.put("MRNA", "gene");
        references.put("NcRNA", "gene");
        references.put("SnRNA", "gene");
        references.put("SnoRNA", "gene");
        references.put("TRNA", "gene");
        references.put("PointMutation", "gene");
        references.put("PolyASite", "processedTranscripts");
        references.put("Region", "gene");
        references.put("RegulatoryRegion", "gene");
        references.put("SequenceVariant", "gene");
        references.put("FivePrimeUTR", "MRNAs");
    }

    /**
     * @see GFF3RecordHandler#process()
     */
    public void process(GFF3Record record) {
        setReferences(references);

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
            String organismDbId = parseFlyBaseId(record.getDbxrefs(), "FBgn");
            if (organismDbId != null) {
                feature.setAttribute("organismDbId", organismDbId);
            } else {
                LOG.warn("FlyBase gene with no FBgn: "
                         + feature.getAttribute("identifier").getValue());
            }

            // if no name set for gene then use CGxx (FlyBase symbol rules)
            if (feature.getAttribute("name") == null) {
                feature.setAttribute("name", feature.getAttribute("identifier").getValue());
            }
        }

        // set MRNA.organismDbId
        if ("MRNA".equals(clsName)) {
            String organismDbId = parseFlyBaseId(record.getDbxrefs(), "FBtr");
            if (organismDbId != null) {
                feature.setAttribute("organismDbId", organismDbId);
            }
        }

        // set TransposableElement.organismDbId
        if ("TransposableElement".equals(clsName)) {
            String organismDbId = parseFlyBaseId(record.getDbxrefs(), "FBti");
            if (organismDbId != null) {
                feature.setAttribute("organismDbId", organismDbId);
            }
        }

        // create additional referenced gene
        if ("SnRNA".equals(clsName) || "NcRNA".equals(clsName) || "SnoRNA".equals(clsName)
            || "TRNA".equals(clsName)) {
            Item gene = getItemFactory().makeItem(null, tgtNs + "Gene", "");
            String organismDbId = parseFlyBaseId(record.getDbxrefs(), "FBgn");
            if (organismDbId != null) {
                gene.setAttribute("organismDbId", organismDbId);
                addItem(gene);
                feature.setReference("gene", gene.getIdentifier());
                List list = (List) record.getAttributes().get("synonym_2nd");
                if (list != null) {
                    Iterator iter = list.iterator();
                    while (iter.hasNext()) {
                        String synonym = (String) iter.next();
                        if (synonym.startsWith("CG") && Character.isDigit(synonym.charAt(2))) {
                            if (gene.hasAttribute("identifier")) {
                                throw new RuntimeException("multiple CG identifiers found for"
                                                           + "reference gene: " + organismDbId);
                            }
                            gene.setAttribute("identifier", synonym);
                        }
                    }
                }
            }
        }
    }




    /**
     * Given a list of dbxrefs parse for a single FlyBase identifier with the given
     * prefix.  It is an error if more than identifier is found.
     * @param dbxrefs a list of dbxref strings retrieved from GFF3
     * @param prefix the prefix of a FlyBase identifier type - e.g. FBgn
     * @return the identifier or null if none found
     */
    protected String parseFlyBaseId(List dbxrefs, String prefix) {
        String fb = null;
        if (dbxrefs != null) {
            Iterator iter = dbxrefs.iterator();
            while (iter.hasNext()) {
                String s = (String) iter.next();
                if (s.startsWith("FlyBase:" + prefix)) {
                    if (fb != null) {
                        throw new RuntimeException("Multiple " + prefix + " ids found in dbxref: "
                                                   + dbxrefs);
                    }
                    fb = s.substring(s.indexOf(":") + 1);
                }
            }
        }
        return fb;
    }

}
