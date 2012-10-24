package org.intermine.bio.util;

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
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.log4j.Logger;

/**
 * A class to hold information about organisms.
 * @author Kim Rutherford
 */
public final class OrganismRepository
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(OrganismRepository.class);
    private static OrganismRepository or = null;
    private Map<Integer, OrganismData> taxonMap = new HashMap<Integer, OrganismData>();
    private Map<String, OrganismData> abbreviationMap = new HashMap<String, OrganismData>();
    private Map<String, OrganismData> shortNameMap = new HashMap<String, OrganismData>();
    private Map<MultiKey, OrganismData> genusSpeciesMap = new HashMap<MultiKey, OrganismData>();
    private Map<Integer, OrganismData> strainMap = new HashMap<Integer, OrganismData>();
    private static Map<String, OrganismData> uniprotToTaxon = new HashMap<String, OrganismData>();

    private static final String PROP_FILE = "organism_config.properties";
    private static final String PREFIX = "taxon";

    private static final String ABBREVIATION = "abbreviation";
    private static final String GENUS = "genus";
    private static final String SPECIES = "species";
    private static final String STRAINS = "strains";
    private static final String ENSEMBL = "ensemblPrefix";
    private static final String UNIPROT = "uniprot";

    private static final String REGULAR_EXPRESSION =
        PREFIX + "\\.(\\d+)\\.(" + SPECIES + "|" + GENUS + "|" + ABBREVIATION + "|" + STRAINS
        + "|" + ENSEMBL + "|" + UNIPROT + ")";

    private OrganismRepository() {
      //disable external instantiation
    }

    /**
     * Return an OrganismRepository created from a properties file in the class path.
     * @return the OrganismRepository
     */
    @SuppressWarnings("unchecked")
    public static OrganismRepository getOrganismRepository() {
        if (or == null) {
            Properties props = new Properties();
            try {
                InputStream propsResource =
                    OrganismRepository.class.getClassLoader().getResourceAsStream(PROP_FILE);
                if (propsResource == null) {
                    throw new RuntimeException("can't find " + PROP_FILE + " in class path");
                }
                props.load(propsResource);

            } catch (IOException e) {
                throw new RuntimeException("Problem loading properties '" + PROP_FILE + "'", e);
            }

            or = new OrganismRepository();

            Enumeration<String> propNames = (Enumeration<String>) props.propertyNames();

            Pattern pattern = Pattern.compile(REGULAR_EXPRESSION);

            while (propNames.hasMoreElements()) {
                String name = propNames.nextElement();
                if (name.startsWith(PREFIX)) {
                    Matcher matcher = pattern.matcher(name);
                    if (matcher.matches()) {
                        String taxonIdString = matcher.group(1);
                        int taxonId = Integer.valueOf(taxonIdString).intValue();
                        String fieldName = matcher.group(2);
                        OrganismData od = or.getOrganismDataByTaxonInternal(taxonId);
                        final String attributeValue = props.getProperty(name);
                        if (fieldName.equals(ABBREVIATION)) {
                            od.setAbbreviation(attributeValue);
                            or.abbreviationMap.put(attributeValue.toLowerCase(), od);
                        } else if (fieldName.equals(STRAINS)) {
                            String[] strains = attributeValue.split(" ");
                            for (String strain : strains) {
                                try {
                                    or.strainMap.put(Integer.valueOf(strain), od);
                                } catch (NumberFormatException e) {
                                    throw new NumberFormatException("taxon ID must be a number");
                                }
                            }
                        } else if (fieldName.equals(ENSEMBL)) {
                            od.setEnsemblPrefix(attributeValue);
                        } else if (fieldName.equals(UNIPROT)) {
                            od.setUniprot(attributeValue);
                            uniprotToTaxon.put(attributeValue, od);
                        } else {
                            if (fieldName.equals(SPECIES)) {
                                od.setSpecies(attributeValue);
                            } else {
                                if (fieldName.equals(GENUS)) {
                                    od.setGenus(attributeValue);
                                } else {
                                    throw new RuntimeException("internal error didn't match: "
                                                               + fieldName);
                                }
                            }
                        }
                    } else {
                        throw new RuntimeException("unable to parse organism property key: "
                                                   + name);
                    }
                } else {
                    throw new RuntimeException("properties in " + PROP_FILE + " must start with "
                                               + PREFIX + ".");
                }
            }

            for (OrganismData od: or.taxonMap.values()) {
                or.genusSpeciesMap.put(new MultiKey(od.getGenus(), od.getSpecies()), od);
                // we have some organisms from uniprot that don't have a short name
                if (od.getShortName() != null) {
                    or.shortNameMap.put(od.getShortName(), od);
                }
            }
        }

        return or;
    }

    /**
     * Look up OrganismData objects by taxon id.  Create and return a new OrganismData object if
     * there is no existing one.
     * @param taxonId the taxon id
     * @return the OrganismData
     */
    public OrganismData getOrganismDataByTaxonInternal(int taxonId) {
        OrganismData od = taxonMap.get(new Integer(taxonId));
        if (od == null) {
            od = new OrganismData();
            od.setTaxonId(taxonId);
            taxonMap.put(new Integer(taxonId), od);
        }
        return od;
    }

    /**
     * Look up OrganismData objects by taxon id.  If there is no taxon, look in strains.  Return
     * null if there is no such organism.
     *
     * @param taxonId the taxon id
     * @return the OrganismData
     */
    public OrganismData getOrganismDataByTaxon(int taxonId) {
        OrganismData od = taxonMap.get(new Integer(taxonId));
        if (od == null) {
            od = strainMap.get(taxonId);
        }
        return od;
    }

    /**
     * Look up OrganismData objects by abbreviation, abbreviations are not case sensitive.
     * Return null if there is no such organism.
     * @param abbreviation the abbreviation
     * @return the OrganismData
     */
    public OrganismData getOrganismDataByAbbreviation(String abbreviation) {
        if (abbreviation == null) {
            return null;
        }
        return abbreviationMap.get(abbreviation.toLowerCase());
    }

    /**
     * Look up OrganismData objects by shortName, short names are case sensitive.
     * Return null if there is no such organism.
     * @param shortName the short name. e.g. "H. sapiens"
     * @return the OrganismData
     */
    public OrganismData getOrganismDataByShortName(String shortName) {
        if (shortName == null) {
            return null;
        }
        return shortNameMap.get(shortName);
    }

    /**
     * Look up OrganismData objects by genus and species - both must match.  Returns null if there
     * is no OrganismData in this OrganismRepository that matches.
     * @param genus the genus
     * @param species the species
     * @return the OrganismData
     */
    public OrganismData getOrganismDataByGenusSpecies(String genus, String species) {
        MultiKey key = new MultiKey(genus, species);
        return genusSpeciesMap.get(key);
    }

    /**
     * Look up OrganismData objects by Uniprot abbreviation, eg HUMAN or DROME.
     * Returns null if there is no OrganismData in this OrganismRepository that matches.
     * @param abbreviation the UniProt abbreviation, eg. HUMAN or DROME
     * @return the OrganismData
     */
    public OrganismData getOrganismDataByUniprot(String abbreviation) {
        return uniprotToTaxon.get(abbreviation);
    }

    /**
     * Look up OrganismData objects by a full name that is genus <space> species.  Returns null if
     * there is no OrganismData in this OrganismRepository that matches.
     * @param fullName the genus and species separated by a space
     * @return the OrganismData
     */
    public OrganismData getOrganismDataByFullName(String fullName) {
        if (fullName.indexOf(" ") == -1) {
            return null;
        }
        String genus = fullName.split(" ", 2)[0];
        String species = fullName.split(" ", 2)[1];
        return getOrganismDataByGenusSpecies(genus, species);
    }

}
