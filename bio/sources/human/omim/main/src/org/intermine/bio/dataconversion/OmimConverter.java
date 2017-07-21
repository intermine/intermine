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
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;

/**
 * Load disease data from OMIM and relationship to genes, publications and SNPs.
 *
 * @author Richard Smith
 */
public class OmimConverter extends BioDirectoryConverter
{

    private static final Logger LOG = Logger.getLogger(OmimConverter.class);

    private static final String DATASET_TITLE = "OMIM diseases";
    private static final String DATA_SOURCE_NAME = "Online Mendelian Inheritance in Man";
    private static final String TAXON_ID = "9606";
    private static final String OMIM_PREFIX = "OMIM:";

    private Map<String, String> genes = new HashMap<String, String>();
    private Map<String, String> pubs = new HashMap<String, String>();
    private Map<String, Item> diseases = new HashMap<String, Item>();

    private String organism;
    private IdResolver rslv;
    private static final String OMIM_TXT_FILE = "mimTitles.txt";
    private static final String MORBIDMAP_FILE = "morbidmap.txt";
    private static final String PUBMED_FILE = "pubmed_cited";
    // An asterisk (*) before an entry number indicates a gene.
    private static final String GENE_ENTRY = "Asterisk";
    private static final String GENE_PHENOTYPE_ENTRY = "Plus";
    private static final String OBSOLETE = "Caret";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public OmimConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
        if (rslv == null) {
            rslv = IdResolverService.getIdResolverByOrganism(Collections.singleton(TAXON_ID));
        }
    }

    @Override
    public void close() throws Exception {
        store(diseases.values());
    }

    /**
     * {@inheritDoc}
     */
    public void process(File dataDir) throws Exception {
        Map<String, File> files = readFilesInDir(dataDir);

        organism = getOrganism(TAXON_ID);

        String[] requiredFiles = new String[] {OMIM_TXT_FILE, MORBIDMAP_FILE, PUBMED_FILE};
        Set<String> missingFiles = new HashSet<String>();
        for (String requiredFile : requiredFiles) {
            if (!files.containsKey(requiredFile)) {
                missingFiles.add(requiredFile);
            }
        }

        if (!missingFiles.isEmpty()) {
            throw new RuntimeException("Not all required files for the OMIM sources were found in: "
                    + dataDir.getAbsolutePath() + ", was missing " + missingFiles);
        }
        // don't change the processing order. or else!
        processOmimTxtFile(new FileReader(files.get(OMIM_TXT_FILE)));
        processMorbidMapFile(new FileReader(files.get(MORBIDMAP_FILE)));
        processPubmedCitedFile(new FileReader(files.get(PUBMED_FILE)));
    }

    private Map<String, File> readFilesInDir(File dir) {
        Map<String, File> files = new HashMap<String, File>();
        for (File file : dir.listFiles()) {
            files.put(file.getName(), file);
        }
        return files;
    }

    private void processOmimTxtFile(Reader reader) throws IOException, ObjectStoreException {
        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            if (line.length < 3) {
                LOG.error("Disease not processed -- only had " + line.length + " columns");
                continue;
            }

            String prefix = line[0].trim();

            // skip header AND genes
            if (prefix.startsWith("#") || GENE_ENTRY.equals(prefix)
                    || GENE_PHENOTYPE_ENTRY.equals(prefix) || OBSOLETE.equals(prefix)) {
                continue;
            }

            String mimId = line[1];
            String preferredTitles = line[2];

            if (StringUtils.isEmpty(mimId)) {
                continue;
            }

            Item disease = getDisease(mimId);
            String[] names = preferredTitles.split(";");
            if (names.length > 0) {
                disease.setAttribute("name", names[0]);
            }
            for (int i = 1; i < names.length; i++) {
                createSynonym(disease.getIdentifier(), names[i], true);
            }

        }
    }

    private void processMorbidMapFile(Reader reader) throws IOException, ObjectStoreException {
        BufferedReader br = new BufferedReader(reader);
        String line = null;
        // find the OMIM ID. OMIM identifiers are 6 digits
        Pattern matchMajorDiseaseNumber = Pattern.compile("(\\d{6})");
        final String delim = "\\(3\\)";
        while ((line = br.readLine()) != null) {
            Matcher diseaseMatcher = matchMajorDiseaseNumber.matcher(line);
            String mimNumber = null;
            if (diseaseMatcher.find()) {
                mimNumber = diseaseMatcher.group(1);
            }
            if (mimNumber == null || mimNumber.isEmpty()) {
                LOG.info("Not processing " + line + ", no OMIM ID");
                continue;
            }
            // only get diseases from already created map. don't create any new diseases
            Item disease = diseases.get(mimNumber);
            if (disease == null) {
                // disease will be NULL if mimNumber belongs to a gene. OMIM assigns genes and
                // phenotypes MIM numbers and does not distinguish the two in this file. We are
                // relying on our diseases map to be populated by the processOmimTxtFile() method
                continue;
            }
            String symbols = null;
            String[] firstBits = line.split(delim);
            if (firstBits.length != 2) {
                // throw exception here?
                continue;
            }
            symbols = firstBits[1].trim();
            // remove the trailing columns
            String[] bits = symbols.split("(\\t)|(\\s\\s)");
            String chompedSymbols = bits[0];
            for (String geneSymbol : chompedSymbols.split(",")) {
                String geneRefId = getGene(geneSymbol.trim());
                if (geneRefId != null) {
                    disease.addToCollection("genes", geneRefId);
                }
            }
        }
    }

    private void processPubmedCitedFile(Reader reader) throws IOException, ObjectStoreException {
        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        String mimNumber = null;
        while (lineIter.hasNext()) {

            String[] bits = lineIter.next();
            if (bits.length == 3) {
                mimNumber = bits[0];
                // String pos = bits[1];
                String pubmedId = bits[2];
                // all the diseases we need are already create from morbidmap file
                if (diseases.containsKey(mimNumber)) {
                    Item disease = getDisease(mimNumber);
                    disease.addToCollection("publications", getPubId(pubmedId));

                }
            }
        }
    }

    private Item getDisease(String mimNumber) {
        Item disease = diseases.get(mimNumber);

        if (disease == null) {
            disease = createItem("Disease");
            disease.setAttribute("identifier", OMIM_PREFIX + mimNumber);
            diseases.put(mimNumber, disease);
        }
        return disease;
    }

    private String getPubId(String pubmed) throws ObjectStoreException {
        String refId = pubs.get(pubmed);
        if (refId == null) {
            Item pub = createItem("Publication");
            pub.setAttribute("pubMedId", pubmed);
            refId = pub.getIdentifier();
            pubs.put(pubmed, refId);
            store(pub);
        }
        return refId;
    }

    private String getGene(String geneSymbol) throws ObjectStoreException {
        String refId = null;
        String entrezGeneNumber = resolveGene(geneSymbol.trim());
        if (StringUtils.isNotEmpty(entrezGeneNumber)) {
            refId = genes.get(entrezGeneNumber);
            if (refId == null) {
                Item gene = createItem("Gene");
                gene.setAttribute("primaryIdentifier", entrezGeneNumber);
                gene.setReference("organism", organism);
                store(gene);
                refId = gene.getIdentifier();
                genes.put(entrezGeneNumber, refId);
            }
        }
        return refId;
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
}
