package org.intermine.bio.dataconversion;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

/**
 * Data structure to hold information from one row of an NCBI Entrex gene_info file.
 * @author Richard Smith
 *
 */
public class GeneInfoRecord {
    protected final String taxon;
    protected final String entrez;
    protected final String officialSymbol;
    protected final String defaultSymbol;
    protected final String officialName;
    protected final String defaultName;
    protected final String mapLocation;
    protected final Set<String> ensemblIds = new HashSet<String>();;
    protected final Set<String> synonyms = new HashSet<String>();

    /**
     * Construct information from file for one gene.
     * @param taxon the organism
     * @param entrez NCBI gene number
     * @param officialSymbol official symbol if present, e.g. from HGNC
     * @param defaultSymbol the NCBI symbol, may be the same as officialSymbol
     * @param officialName official name if present, e.g. from HGNC
     * @param defaultName NCBI name, may be the same as the officialName
     * @param mapLocation chromosome band of the gene, if known
     */
    public GeneInfoRecord(String taxon, String entrez, String officialSymbol, String defaultSymbol,
            String officialName, String defaultName, String mapLocation) {
        this.taxon = taxon;
        this.entrez = entrez;
        this.officialSymbol = filter(officialSymbol);
        this.officialName = filter(officialName);
        this.defaultSymbol = filter(defaultSymbol);
        this.defaultName = filter(defaultName);
        this.mapLocation = filter(mapLocation);
    }

    /**
     * Return the entrez gene id, official symbol and default symbol (if different).
     * @return the main ids for this gene record
     */
    public Set<String> getMainIds() {
        Set<String> ids = new HashSet<String>();
        ids.add(entrez);
        if (officialSymbol != null) {
            ids.add(officialSymbol);
        }
        if (defaultSymbol != null) {
            ids.add(defaultSymbol);
        }
        return ids;
    }

    private String filter(String s) {
        if (!StringUtils.isBlank(s)) {
            if (!"-".equals(s)) {
                return s;
            }
        }
        return null;
    }
}
