package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

/**
 * Data structure to hold information from one row of an NCBI Entrez gene_info file.
 *
 * @author Richard Smith
 *
 */
public class GeneInfoRecord
{
    protected final String taxon;
    protected final String entrez;
    protected final String officialSymbol;
    protected final String defaultSymbol;
    protected final String officialName;
    protected final String defaultName;
    protected final String mapLocation;
    protected final String geneType;
    protected final String locusTag;
    protected final Set<String> ensemblIds = new HashSet<String>();
    // xrefs: key - DB name, e.g. FLYABSE; value - set of id, e.g. FBgn1234567890
    protected final Map<String, Set<String>> xrefs = new HashMap<String, Set<String>>();
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
     * @param geneType e.g. protein-coding, ncrna, etc.
     * @param locusTag treefam uses this field to identify yeast
     */
    public GeneInfoRecord(String taxon, String entrez, String officialSymbol,
            String defaultSymbol, String officialName, String defaultName,
            String mapLocation, String geneType, String locusTag) {
        this.taxon = taxon;
        this.entrez = entrez;
        this.officialSymbol = filter(officialSymbol);
        this.officialName = filter(officialName);
        this.defaultSymbol = filter(defaultSymbol);
        this.defaultName = filter(defaultName);
        this.mapLocation = filter(mapLocation);
        this.geneType = filter(geneType);
        this.locusTag = filter(locusTag);
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
        if (locusTag != null) {
            ids.add(locusTag);
        }
        return ids;
    }

    private static String filter(String s) {
        if (!StringUtils.isBlank(s)) {
            if (!"-".equals(s)) {
                return s;
            }
        }
        return null;
    }
}
