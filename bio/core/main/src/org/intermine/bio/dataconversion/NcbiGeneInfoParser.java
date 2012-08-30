package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.intermine.util.FormattedTextParser;
import org.intermine.util.Util;


/**
 * Parse NCBI Entrez gene_info file and return data structure for each row for later processing.
 * @author Richard Smith
 *
 */
public class NcbiGeneInfoParser
{
    private Map<String, Set<GeneInfoRecord>> recordMap = new HashMap<String, Set<GeneInfoRecord>>();
    private Map<String, Set<String>> duplicateEnsemblIds = new HashMap<String, Set<String>>();
    private Map<String, Set<String>> duplicateSymbols = new HashMap<String, Set<String>>();

    /**
     * Construct the parser with the file to read, the input file can be for a single taxon or for
     * multiple.
     * @param reader a reader for the gene_info file to be parsed
     * @throws IOException if problems reading file
     */
    public NcbiGeneInfoParser(Reader reader) throws IOException {
        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        while (lineIter.hasNext()) {
            String[] line = lineIter.next();

            String taxonId = line[0].trim();
            String entrez = line[1].trim();
            String defaultSymbol = line[2].trim();
            String synonyms = line[4].trim();
            String xrefs = line[5].trim(); // dbIdentifier
            String mapLocation = line[7].trim();
            String defaultName = line[8].trim();
            String geneType = line[9].trim();
            String officialSymbol = line[10].trim();
            String officialName = line[11].trim();

            GeneInfoRecord record = new GeneInfoRecord(taxonId, entrez, officialSymbol,
                    defaultSymbol, officialName, defaultName, mapLocation, geneType);

            record.ensemblIds.addAll(parseXrefs(xrefs, "Ensembl"));
            record.xrefs.putAll(parseXrefs(xrefs));

            if (!"-".equals(synonyms)) {
                for (String synonym : synonyms.split("\\|")) {
                    record.synonyms.add(synonym);
                }
            }

            Set<GeneInfoRecord> taxonRecords = recordMap.get(taxonId);
            if (taxonRecords == null) {
                taxonRecords = new HashSet<GeneInfoRecord>();
                recordMap.put(taxonId, taxonRecords);
            }
            taxonRecords.add(record);
        }
    }

    /**
     * Return information read from the file, for each taxon id there is one GeneInfoRecord per row
     * in the file.
     * @return a map from taxon id to gene information records
     */
    public Map<String, Set<GeneInfoRecord>> getGeneInfoRecords() {
        return recordMap;
    }

    /**
     * Return true if the given Ensembl identifier is assigned to only one gene for the taxon
     * specified.
     * @param taxonId the taxon of the gene being looked up
     * @param ensemblId an ensembl gene identifier
     * @return true if this ensembl id is only mapped to one NCBI gene
     */
    public boolean isUniquelyMappedEnsemblId(String taxonId, String ensemblId) {
        Set<String> taxonDuplicates = duplicateEnsemblIds.get(taxonId);
        if (taxonDuplicates == null) {
            taxonDuplicates = findDuplicateEnsemblIds(taxonId);
            duplicateEnsemblIds.put(taxonId, taxonDuplicates);
        }
        return !taxonDuplicates.contains(ensemblId);
    }

    private Set<String> findDuplicateEnsemblIds(String taxonId) {
        Set<String> duplicates = new HashSet<String>();
        if (recordMap.containsKey(taxonId)) {
            Set<String> seenEnsembl = new HashSet<String>();
            for (GeneInfoRecord record : recordMap.get(taxonId)) {
                for (String ensembl : record.ensemblIds) {
                    if (seenEnsembl.contains(ensembl)) {
                        duplicates.add(ensembl);
                    } else {
                        seenEnsembl.add(ensembl);
                    }
                }
            }
        }
        return duplicates;
    }

    /**
     * Return true if the given symbol is is assigned to only one gene for the taxon specified.
     * @param taxonId the taxon of the gene being looked up
     * @param symbol a gene symbl to check
     * @return true if this true id is only assigned to one NCBI gene
     */
    public boolean isUniqueSymbol(String taxonId, String symbol) {
        Set<String> taxonDuplicates = duplicateSymbols.get(taxonId);
        if (taxonDuplicates == null) {
            taxonDuplicates = findDuplicateSymbols(taxonId);
            duplicateSymbols.put(taxonId, taxonDuplicates);
        }
        return !taxonDuplicates.contains(symbol);
    }

    /**
     * @param taxonId taxon ID for organism of interest
     * @return set of symbols that are duplicated
     */
    public Set<String> findDuplicateSymbols(String taxonId) {
        Set<String> duplicates = new HashSet<String>();
        if (recordMap.containsKey(taxonId)) {
            Set<String> seenSymbols = new HashSet<String>();
            for (GeneInfoRecord record : recordMap.get(taxonId)) {
                for (String symbol : new String[] {record.officialSymbol, record.defaultSymbol}) {
                    if (seenSymbols.contains(symbol)) {
                        duplicates.add(symbol);
                    } else {
                        seenSymbols.add(symbol);
                    }
                }
            }
        }
        return duplicates;
    }

    private Set<String> parseXrefs(String xrefs, String prefix) {
        String newPrefix = prefix;
        if (!prefix.endsWith(":")) {
            newPrefix = prefix + ":";
        }
        Set<String> matched = new HashSet<String>();
        for (String xref : xrefs.split("\\|")) {
            if (xref.startsWith(prefix)) {
                matched.add(xref.substring(newPrefix.length()));
            }
        }
        return matched;
    }

    /**
     * Parse all xref, some gene will have multiple id from same source
     * e.g. P2RX5 HGNC:8536|MIM:602836|Ensembl:ENSG00000083454|Ensembl:ENSG00000257950|HPRD:09110|Vega:OTTHUMG00000090700|Vega:OTTHUMG00000169623
     * @param xrefs a "|" separated string
     * @return a map of xrefs
     */
    private Map<String, Set<String>> parseXrefs(String xrefs) {
        Map<String, Set<String>> xrefMap = new HashMap<String, Set<String>>();
        for (String xref : xrefs.split("\\|")) {
            Util.addToSetMap(xrefMap, xref.split(":")[0], xref.split(":")[1]);
        }
        return xrefMap;
    }
}
