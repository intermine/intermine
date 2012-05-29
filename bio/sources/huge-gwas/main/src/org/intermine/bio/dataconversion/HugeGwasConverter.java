package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;

/**
 * Read the HuGE GWAS flat file and create GWAS items and GWASResults.
 * @author Richard Smith
 */
public class HugeGwasConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "HuGE GWAS Integrator";
    private static final String DATA_SOURCE_NAME = "HuGE Navigator";

    private String headerStart = "rs Number(region location)";
    private Map<String, String> genes = new HashMap<String, String>();
    private Map<String, String> pubs = new HashMap<String, String>();
    private Map<String, String> snps = new HashMap<String, String>();
    private Map<String, String> studies = new HashMap<String, String>();

    private static final String HUMAN_TAXON = "9606";
    private List<String> invalidGenes = Arrays.asList(new String[] {"nr", "intergenic"});

    // approximately the minimum permitted double value in postgres
    private static final double MIN_POSTGRES_DOUBLE = 1.0E-307;

    private static final Logger LOG = Logger.getLogger(HugeGwasConverter.class);

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public HugeGwasConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        boolean doneHeader = false;

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            LOG.info("Line: " + line);

            if (line[0].startsWith(headerStart)) {
                doneHeader = true;
                continue;
            }

            if (!doneHeader) {
                continue;
            }

            if (line.length <= 1) {
                continue;
            }

            Set<String> rsNumbers = parseSnpRsNumbers(line[0]);
            if (rsNumbers.isEmpty()) {
                continue;
            }

            Set<String> geneIdentifiers = getGenes(line[1]);
            String phenotype = line[3];
            String firstAuthor = line[4];
            String year = line[6];
            String pubIdentifier = getPub(line[7]);
            Map<String, String> samples = parseSamples(line[8]);
            String riskAlleleStr = line[9];
            Double pValue = parsePValue(line[11]);

            // There may be multiple SNPs in one line, create a GWASResult per SNP.
            for (String rsNumber : rsNumbers) {
                Item result = createItem("GWASResult");
                result.setReference("SNP", getSnpIdentifier(rsNumber));
                if (!geneIdentifiers.isEmpty()) {
                    result.setCollection("associatedGenes", new ArrayList<String>(geneIdentifiers));
                }
                result.setAttribute("phenotype", phenotype);
                if (pValue != null) {
                    result.setAttribute("pValue", pValue.toString());
                }
                String studyIdentifier = getStudy(pubIdentifier, firstAuthor, year, phenotype,
                        samples);
                result.setReference("study", studyIdentifier);

                // set risk allele details
                String[] alleleParts = riskAlleleStr.split("\\[");
                String alleleStr = alleleParts[0];
                if (alleleStr.startsWith("rs")) {
                    if (alleleStr.indexOf('-') >= 0) {
                        String riskSnp = alleleStr.substring(0, alleleStr.indexOf('-'));
                        if (riskSnp.equals(rsNumber)) {
                            String allele = alleleStr.substring(alleleStr.indexOf('-') + 1);
                            result.setAttribute("associatedVariantRiskAllele", allele);
                        }
                    } else {
                        LOG.warn("ALLELE: no allele found in '" + alleleStr + "'.");
                    }
                } else {
                    result.setAttribute("associatedVariantRiskAllele", alleleParts[0]);
                }
                if (alleleParts.length > 1) {
                    String freqStr = alleleParts[1];
                    try {
                        Float freq = Float.parseFloat(freqStr.substring(0, freqStr.indexOf(']')));
                        result.setAttribute("riskAlleleFreqInControls", freq.toString());
                    } catch (NumberFormatException e) {
                        // wasn't a valid float, probably "NR"
                    }
                }
                store(result);
            }
        }
    }

    /**
     * Read a p-value from a String of the format 5x10-6.
     * @param s the input string, e.g. 5x10-6
     * @return the extracted double or null if failed to parse
     */
    protected Double parsePValue(String s) {
        s = s.replace("x10", "E");
        try {
            double pValue = Double.parseDouble(s);

            // Postgres JDBC driver is allowing double values outside the permitted range to be
            // stored which are then unusable.  This a hack to prevent it.
            if (pValue < MIN_POSTGRES_DOUBLE) {
                pValue = 0.0;
            }

            return pValue;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<String, String> parseSamples(String fromFile) {
        Map<String, String> samples = new HashMap<String, String>();
        String[] parts = fromFile.split("/");
        samples.put("initial", parts[0]);
        if (parts.length == 1 || "NR".equals(parts[1])) {
            samples.put("replicate", "No replicate");
        } else {
            samples.put("replicate", parts[1]);
        }
        return samples;
    }

    /**
     * Extract a SNP rs id from a string of the format rs11099864(4q31.3).
     * @param s the string read from the file
     * @return the SNP rs number
     */
    protected String parseSnp(String s) {
        if (s.indexOf('(') > 0) {
            return s.substring(0, s.indexOf('(')).trim();
        }
        return s.trim();
    }

    private String getStudy(String pubIdentifier, String firstAuthor, String year, String phenotype,
            Map<String, String> samples)
        throws ObjectStoreException {
        String studyIdentifier = studies.get(pubIdentifier);
        if (studyIdentifier == null) {
            Item gwas = createItem("GWAS");
            gwas.setAttribute("firstAuthor", firstAuthor);
            gwas.setAttribute("year", year);
            gwas.setAttribute("name", phenotype);
            gwas.setAttribute("initialSample", samples.get("initial"));
            gwas.setAttribute("replicateSample", samples.get("replicate"));
            gwas.setReference("publication", pubIdentifier);
            store(gwas);

            studyIdentifier = gwas.getIdentifier();
            studies.put(pubIdentifier, studyIdentifier);
        }
        return studyIdentifier;
    }

    private String getPub(String pubMedId) throws ObjectStoreException {
        String pubIdentifier = pubs.get(pubMedId);
        if (pubIdentifier == null) {
            Item pub = createItem("Publication");
            pub.setAttribute("pubMedId", pubMedId);
            store(pub);

            pubIdentifier = pub.getIdentifier();
            pubs.put(pubMedId, pubIdentifier);
        }
        return pubIdentifier;
    }



    private Set<String> parseSnpRsNumbers(String fromFile) {
        Set<String> rsNumbers = new HashSet<String>();
        for (String s : fromFile.split(",")) {
            String rsNumber = parseSnp(s);
            if (rsNumber.startsWith("rs")) {
                rsNumbers.add(rsNumber);
            }
        }
        return rsNumbers;
    }

    private String getSnpIdentifier(String rsNumber) throws ObjectStoreException {
        if (!snps.containsKey(rsNumber)) {
            Item snp = createItem("SNP");
            snp.setAttribute("primaryIdentifier", rsNumber);
            snp.setReference("organism", getOrganism(HUMAN_TAXON));
            store(snp);
            snps.put(rsNumber, snp.getIdentifier());
        }
        return snps.get(rsNumber);
    }

    private Set<String> getGenes(String s) throws ObjectStoreException {
        Set<String> geneIdentifiers = new HashSet<String>();
        for (String symbol : s.split(",")) {
            symbol = symbol.trim();
            if (invalidGenes.contains(symbol.toLowerCase())) {
                continue;
            }
            String geneIdentifier = genes.get(symbol);
            if (geneIdentifier == null) {
                Item gene = createItem("Gene");
                gene.setAttribute("symbol", symbol);
                gene.setReference("organism", getOrganism(HUMAN_TAXON));
                geneIdentifier = gene.getIdentifier();

                store(gene);
                genes.put(symbol, geneIdentifier);
            }
            geneIdentifiers.add(geneIdentifier);
        }
        return geneIdentifiers;
    }
}
