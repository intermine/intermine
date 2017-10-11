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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.bio.dataconversion.ProteinAtlasEntry.Level;
import org.intermine.bio.dataconversion.ProteinAtlasEntry.TissueExpressionData;
import org.intermine.bio.dataconversion.ProteinAtlasEntry.TissueExpressionSummary;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Read Protein Atlas expression data.
 * @author Richard Smith
 */
public class ProteinAtlasConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(ProteinAtlasConverter.class);
    private static final String DATASET_TITLE = "Protein Atlas expression";
    private static final String DATA_SOURCE_NAME = "Protein Atlas";
    private Map<String, String> genes = new HashMap<String, String>();
    private Map<String, Item> tissues = new HashMap<String, Item>();
    private Set<String> storedTissues = new HashSet<String>();
    private int entryCount = 0;
    protected IdResolver rslv;
    private static final String TAXON_ID = "9606";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public ProteinAtlasConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
        if (rslv == null) {
            rslv = IdResolverService.getIdResolverByOrganism(Collections.singleton(TAXON_ID));
        }
    }

    /**
     * Read Protein Atlas normal_tissue.csv file.
     *
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader) throws Exception {
        File currentFile = getCurrentFile();
        if ("normal_tissue.csv".equals(currentFile.getName())) {
            processNormalTissue(reader);
        } else if ("tissue_to_organ.tsv".equals(currentFile.getName())) {
            processTissueToOrgan(reader);
        } else if ("proteinatlas.xml".equals(currentFile.getName())) {
            processAllInOneXML(currentFile);
        } else {
            throw new RuntimeException("Don't know how to process file: " + currentFile.getName());
        }
    }

    private void  processTissueToOrgan(Reader reader) throws ObjectStoreException, IOException {
        // file has two colums:
        // Tissue name <\t> Tissue group

        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        Map<String, Item> tissueGroups = new HashMap<String, Item>();

        // Read all lines into gene records
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            String tissueName = line[0];
            String tissueGroupName = line[1];

            Item tissue = getTissue(tissueName);
            Item tissueGroup = tissueGroups.get(tissueGroupName);
            if (tissueGroup == null) {
                tissueGroup = createItem("TissueGroup");
                tissueGroup.setAttribute("name", tissueGroupName);
                store(tissueGroup);
                tissueGroups.put(tissueGroupName, tissueGroup);
            }
            tissue.setAttribute("name", tissueName);
            tissue.setReference("tissueGroup", tissueGroup);
            store(tissue);
            storedTissues.add(tissueName);
        }

        // Tissue data is homebrew, it has been out of data after protein-atlas v10, a hacky
        // way for the new tissue types
        @SuppressWarnings("unchecked")
        Collection<String> unstoredTissues = CollectionUtils.subtract(
                tissues.keySet(), storedTissues);
        for (String tissueName : unstoredTissues) {
            Item tissue = getTissue(tissueName);
            tissue.setAttribute("name", tissueName);
            store(tissue);
        }
    }

    private void processNormalTissue(Reader reader) throws ObjectStoreException, IOException {
        // data has format
        // "Gene","Tissue","Cell type","Level","Expression type","Reliability"
        // "ENSG00000000003","adrenal gland","glandular cells","Negative","Staining","Supportive"

        // APE - two or more antibodies
        // Staining - one antibody only
        // 0 - very low/ none
        // 1 - low/ not supportive
        // 2 - medium/ unsupportive
        // 3 - high/ supportive

        Iterator<?> lineIter = FormattedTextParser.parseCsvDelimitedReader(reader);
        lineIter.next();  // discard header

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            String geneId = getGeneId(line[0]);
            if (StringUtils.isEmpty(geneId)) {
                continue;
            }
            String capitalisedTissueName = StringUtils.capitalize(line[2]);
            Item tissueId = getTissue(capitalisedTissueName);

            String cellType = line[3];
            String level = line[4];
            String expressionType = line[5];
            String reliability = line[6];

            level = alterLevel(level, expressionType);
            reliability = alterReliability(reliability, expressionType);

            Item expression = createItem("ProteinAtlasExpression");
            expression.setAttribute("cellType", cellType);
            expression.setAttribute("level", level);
            expression.setAttribute("expressionType", alterExpressionType(expressionType));
            expression.setAttribute("reliability", reliability);
            expression.setReference("gene", geneId);
            expression.setReference("tissue", tissueId);
            store(expression);
        }
    }

    private void processAllInOneXML(File file)
        throws SAXException, IOException, ParserConfigurationException, ObjectStoreException {

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();

        NodeList entryList = doc.getElementsByTagName("entry");

        for (int i = 0; i < entryList.getLength(); i++) {
            ProteinAtlasEntry e = new ProteinAtlasEntry();

            Element entry = (Element) entryList.item(i);
            e.setVersion(entry.getAttribute("version"));
            e.setUrl(entry.getAttribute("version"));
            e.setGeneName(entry.getElementsByTagName("name").item(0).getTextContent());

            NodeList synonymList = entry.getElementsByTagName("synonym");
            for (int si = 0; si < synonymList.getLength(); si++) {
                e.getGeneSynonymSet().add(synonymList.item(si).getTextContent());
            }

            e.setGeneId(((Element) entry.getElementsByTagName("identifier").item(0))
                    .getAttribute("id"));

            Element te = (Element) entry.getElementsByTagName("tissueExpression").item(0);
            e.getTissueExpression().setType(te.getAttribute("type"));
            e.getTissueExpression().setTechnology(te.getAttribute("technology"));

            Element tes = (Element) te.getElementsByTagName("summary").item(0);
            TissueExpressionSummary s = e.new TissueExpressionSummary();
            s.setSummaryType(tes.getAttribute("type"));
            s.setSummary(tes.getTextContent());
            e.getTissueExpression().getSummarySet().add(s);

            e.getTissueExpression().setVerificationType(
                    ((Element) te.getElementsByTagName("verification").item(0))
                            .getAttribute("type"));
            e.getTissueExpression().setVerification(
                    te.getElementsByTagName("verification").item(0)
                            .getTextContent());

            NodeList tedList = te.getElementsByTagName("data");
            for (int tedi = 0; tedi < tedList.getLength(); tedi++) {
                Element ted = (Element) tedList.item(tedi);

                TissueExpressionData d = e.new TissueExpressionData();
                d.setTissue(((Element) ted.getElementsByTagName("tissue").item(0))
                        .getTextContent());
                d.setTissueStatus(((Element) ted.getElementsByTagName("tissue").item(0))
                        .getAttribute("status"));

                d.setCellType(ted.getElementsByTagName("cellType").item(0)
                        .getTextContent());

                ted.getElementsByTagName("level").item(0).getTextContent();

                Level l = e.new Level();
                l.setType(((Element) ted.getElementsByTagName("level").item(0))
                        .getAttribute("type"));
                l.setLevel(ted.getElementsByTagName("level").item(0)
                        .getTextContent());
                d.getLevelSet().add(l);

                e.getTissueExpression().getDataSet().add(d);
            }

            // TODO extension to subcellularLocation, rnaExpression, antibody

            processEntry(e);
        }
    }

    // store tells us we have been called with the upper case name from the tissue_to_organ file
    private Item getTissue(String tissueName) {
        Item tissue = tissues.get(tissueName);
        if (tissue == null) {
            tissue = createItem("Tissue");
            tissues.put(tissueName, tissue);
        }
        return tissue;
    }

    private String getGeneId(String primaryIdentifier) throws ObjectStoreException {
        String resolvedIdentifier = resolveGene(primaryIdentifier);
        if (StringUtils.isEmpty(resolvedIdentifier)) {
            return null;
        }
        String geneId = genes.get(resolvedIdentifier);
        if (geneId == null) {
            Item gene = createItem("Gene");
            gene.setAttribute("primaryIdentifier", resolvedIdentifier);
            gene.setReference("organism", getOrganism(TAXON_ID));
            store(gene);
            geneId = gene.getIdentifier();
            genes.put(resolvedIdentifier, geneId);
        }
        return geneId;
    }

    private String resolveGene(String identifier) {
        String id = identifier;
        if (rslv != null && rslv.hasTaxon(TAXON_ID)) {
            int resCount = rslv.countResolutions(TAXON_ID, identifier);
            if (resCount != 1) {
                LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                         + identifier + " count: " + resCount + " Human identifier: "
                         + rslv.resolveId(TAXON_ID, identifier));
                return null;
            }
            id = rslv.resolveId(TAXON_ID, identifier).iterator().next();
        }
        return id;
    }


    private static String alterLevel(String level, String type) {
        if ("staining".equalsIgnoreCase(type)) {
            if ("strong".equalsIgnoreCase(level)) {
                return "High";
            } else if ("moderate".equalsIgnoreCase(level)) {
                return "Medium";
            } else if ("weak".equalsIgnoreCase(level)) {
                return "Low";
            } else if ("negative".equalsIgnoreCase(level)) {
                return "None";
            }
        }
        return level;
    }

    private static String alterReliability(String reliability, String type) {
        if ("staining".equalsIgnoreCase(type)) {
            if ("supportive".equalsIgnoreCase(reliability)) {
                return "High";
            } else if ("uncertain".equalsIgnoreCase(reliability)) {
                return "Low";
            }
        } else if ("ape".equalsIgnoreCase(type)) {
            if ("hi".equalsIgnoreCase(reliability)) {
                return "High";
            } else if ("medium".equalsIgnoreCase(reliability)) {
                return "High";
            }  else if ("low".equalsIgnoreCase(reliability)) {
                return "Low";
            }  else if ("very low".equalsIgnoreCase(reliability)) {
                return "Low";
            }
        }
        return reliability;
    }

    private static String alterExpressionType(String expressionType) {
        if ("APE".equalsIgnoreCase(expressionType)) {
            return "APE - two or more antibodies";
        } else if ("Staining".equalsIgnoreCase(expressionType)) {
            return "Staining - one antibody only";
        } else {
            return expressionType;
        }
    }

    private void processEntry(ProteinAtlasEntry entry) throws ObjectStoreException {
        entryCount++;
        if (entryCount % 10000 == 0) {
            LOG.info("Processed " + entryCount + " entries.");
        }

        processTissueExpression(entry);
    }

    private void processTissueExpression(ProteinAtlasEntry entry)
        throws ObjectStoreException {
        String geneRefId = getGeneId(entry.getGeneId());
        String reliability = entry.getTissueExpression().getVerification();

        for (TissueExpressionData ted : entry.getTissueExpression()
                .getDataSet()) {
            String expressionType = ted.getLevelSet().iterator().next()
                    .getType();
            reliability = alterReliability(reliability, expressionType);

            Item expression = createItem("ProteinAtlasExpression");
            expression.setAttribute("cellType", ted.getCellType());
            expression.setAttribute("level", ted.getLevelSet().iterator()
                    .next().getLevel());
            expression.setAttribute("expressionType",
                    alterExpressionType(expressionType));
            expression.setAttribute("reliability", reliability);
            expression.setReference("gene", geneRefId);
            expression.setReference("tissue", ted.getTissue());
            store(expression);
        }
    }
}
