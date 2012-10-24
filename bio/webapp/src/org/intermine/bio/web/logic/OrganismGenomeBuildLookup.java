package org.intermine.bio.web.logic;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.intermine.util.PropertiesUtil;

/**
 * An util class to help looking up genome build by a given organism id.
 *
 * @author Fengyuan Hu
 *
 */
public final class OrganismGenomeBuildLookup
{
    private static Map<Integer, String> taxonMap = new HashMap<Integer, String>();
    private static Map<String, String> abbreviationMap = new HashMap<String, String>();
    private static Map<String, String> fullnameMap = new HashMap<String, String>();

    // TODO how genome build can be integrated to database rather than written in a file?

    private OrganismGenomeBuildLookup() {
        //disable external instantiation
    }

    private static void prepareData() {
        if (taxonMap.size() == 0 || abbreviationMap.size() == 0 || fullnameMap.size() == 0) {
            Properties props = PropertiesUtil.getProperties();

            // Manual work...
            String flyGB = props.getProperty("genomeVersion.fly");
            String wormGB = props.getProperty("genomeVersion.worm");
            String humanGB = props.getProperty("genomeVersion.human");
            String mouseGB = props.getProperty("genomeVersion.mouse");

            if (flyGB != null) {
                taxonMap.put(7227, flyGB);
                abbreviationMap.put("D. melanogaster", flyGB);
                fullnameMap.put("Drosophila melanogaster", flyGB);
            }

            if (wormGB != null) {
                taxonMap.put(6239, wormGB);
                abbreviationMap.put("C. elegans", wormGB);
                fullnameMap.put("Caenorhabditis elegans", wormGB);
            }

            if (humanGB != null) {
                taxonMap.put(9606, humanGB);
                abbreviationMap.put("H. sapiens", humanGB);
                fullnameMap.put("Homo sapiens", humanGB);
            }

            if (mouseGB != null) {
                taxonMap.put(10090, mouseGB);
                abbreviationMap.put("M. musculus", mouseGB);
                fullnameMap.put("Mus musculus", mouseGB);
            }
        }
    }
    /**
     * Get genome build by organism full name such as "Drosophila melanogaster"
     * @param fn full name of an organism
     * @return genome build
     */
    public static String getGenomeBuildbyOrgansimFullName(String fn) {
        prepareData();
        return fullnameMap.get(fn);
    }

    /**
     * Get genome build by organism short name such as "D. melanogaster"
     * @param abbr short name of an organism
     * @return genome build
     */
    public static String getGenomeBuildbyOrgansimAbbreviation(String abbr) {
        prepareData();
        return abbreviationMap.get(abbr);
    }

    /**
     * Get genome build by organism taxon such as 7227
     * @param taxon taxon of an organism
     * @return genome build
     */
    public static String getGenomeBuildbyOrgansimTaxon(Integer taxon) {
        prepareData();
        return taxonMap.get(taxon);
    }

    /**
     * Get genome build by any id
     * @param id id of an organism
     * @return genome build
     */
    public static String getGenomeBuildbyOrgansimId(String id) {
        prepareData();

        if (id.contains(". ")) {
            return abbreviationMap.get(id);
        } else if (Pattern.matches("^\\d+$", id)) {
            return taxonMap.get(id);
        } else {
            return fullnameMap.get(id);
        }

    }

    /**
     * Get genome build by a collection of ids
     * @param c a collection of organism ids
     * @return a collection genome builds
     */
    public static Collection<String> getGenomeBuildByOrgansimCollection(Collection<String> c) {
        prepareData();

        Collection<String> gbc = new LinkedHashSet<String>();

        for (String id : c) {

            if (id.contains(". ")) {
                gbc.add(abbreviationMap.get(id));
            } else if (Pattern.matches("^\\d+$", id)) {
                gbc.add(taxonMap.get(id));
            } else {
                gbc.add(fullnameMap.get(id));
            }
        }

        return gbc;
    }
}
