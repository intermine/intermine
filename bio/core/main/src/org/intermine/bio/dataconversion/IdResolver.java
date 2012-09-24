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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Hold data about primary identifiers and synonyms for a particular class in the
 * data model and provide methods to resolved synonyms into corresponding
 * primary identifier(s).
 *
 * @author rns
 */
public class IdResolver
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(IdResolver.class);

    private String clsName;
    protected Map<String, Map<String, Set<String>>> orgIdMaps =
        new HashMap<String, Map<String, Set<String>>>();
    protected Map<String, Map<String, Set<String>>> orgSynMaps =
        new HashMap<String, Map<String, Set<String>>>();
    protected Map<String, Map<String, Set<String>>> orgMainMaps =
        new HashMap<String, Map<String, Set<String>>>();
    private Map<String, Map<String, Set<String>>> orgIdMainMaps =
        new HashMap<String, Map<String, Set<String>>>();
    private Map<String, Map<String, Set<String>>> orgIdSynMaps =
        new HashMap<String, Map<String, Set<String>>>();
    /**
     * Construct and empty IdResolver
     * @param clsName the class to resolve identifiers for
     */
    public IdResolver(String clsName) {
        this.clsName = clsName;
    }

    /**
     * Check whether the given id is a primary identifier for this taxonId
     * @param taxonId the organism to look up
     * @param id an identifier
     * @return true if id is a primaryIdentifier
     */
    public boolean isPrimaryIdentifier(String taxonId, String id) {
        checkTaxonId(taxonId);
        return orgIdMaps.get(taxonId).containsKey(id);
    }

    /**
     * For the given id return a set of matching primary identifiers in the given
     * taxonId.  In many cases the set will have just one element. Some will have
     * zero element.
     * @param taxonId the organism to search within
     * @param id the identifier to resolve
     * @return a set of matching primary identifiers
     */
    public Set<String> resolveId(String taxonId, String id) {
        checkTaxonId(taxonId);
        // if this is a primary identifier, just return it
        if (isPrimaryIdentifier(taxonId, id)) {
            return Collections.singleton(id);
        }
        if (orgMainMaps.containsKey(taxonId)
            && orgMainMaps.get(taxonId).containsKey(id)) {
            return orgMainMaps.get(taxonId).get(id);
        }
        if (orgSynMaps.containsKey(taxonId)) {
            return orgSynMaps.get(taxonId).get(id);
        }
        return Collections.emptySet();
    }

    /**
     * For a particular primary identifier fetch a set of synonyms or return
     * null if id is not a primary identifier for the taxonId given.
     * @param taxonId the organism to do a lookup for
     * @param id the primary identifier to look up
     * @return a set of synonyms or null if id is not a primary identifier
     */
    public Set<String> getSynonyms(String taxonId, String id) {
        checkTaxonId(taxonId);
        if (!isPrimaryIdentifier(taxonId, id)) {
            return null;
        }
        return orgIdMaps.get(taxonId).get(id);
    }

    /**
     * Return the count of matching primary identifiers for a particular identifier
     * @param taxonId the organism to check for
     * @param id the identifier to look up
     * @return a count of the resolutions for this identifier
     */
    public int countResolutions(String taxonId, String id) {
        checkTaxonId(taxonId);
        Set<String> resolvedIds = resolveId(taxonId, id);
        return resolvedIds == null ? 0 : resolvedIds.size();
    }

    /**
     * Return true if the idResolver contains information about this taxon id.
     * @param taxonId an organism to check for
     * @return true if data about this taxon id
     */
    public boolean hasTaxon(String taxonId) {
        return orgIdMaps.containsKey(taxonId);
    }

    /**
     * Add alternative main identifiers for a primary identifier to the IdResolver.
     * @param taxonId the organism of the identifier
     * @param primaryIdentifier the main identifier
     * @param ids a set of alternative main identifiers
     */
    protected void addMainIds(String taxonId, String primaryIdentifier, Set<String> ids) {
        addEntry(taxonId, primaryIdentifier, ids, Boolean.TRUE);
    }

    /**
     * Add synonyms for a primary identifier to the IdResolver
     * @param taxonId the organism of the identifier
     * @param primaryIdentifier the main identifier
     * @param ids a set synonyms
     */
    protected void addSynonyms(String taxonId, String primaryIdentifier, Set<String> ids) {
        addEntry(taxonId, primaryIdentifier, ids, Boolean.FALSE);
    }

    /**
     * Add an entry to the IdResolver, a primary identifier and any number of synonyms.
     * @param taxonId the organism of the identifier
     * @param primaryIdentifier the main identifier
     * @param synonyms a set of synonyms
     * @param mainId if true these are main ids, otherwise synonms
    */
    private void addEntry(String taxonId, String primaryIdentifier, Collection<String> ids,
            Boolean mainId) {
        Map<String, Set<String>> idMap = orgIdMaps.get(taxonId);
        if (idMap == null) {
            idMap = new HashMap<String, Set<String>>();
            orgIdMaps.put(taxonId, idMap);
        }

        addToMapList(idMap, primaryIdentifier, ids);

        Map<String, Set<String>> lookupMap = null;
        Map<String, Set<String>> reverseMap = null;
        if (mainId.booleanValue()) {
            lookupMap = orgMainMaps.get(taxonId);
            if (lookupMap == null) {
                lookupMap = new HashMap<String, Set<String>>();
                orgMainMaps.put(taxonId, lookupMap);
            }

            reverseMap = orgIdMainMaps.get(taxonId);
            if (reverseMap == null) {
                reverseMap = new HashMap<String, Set<String>>();
                orgIdMainMaps.put(taxonId, reverseMap);
            }
        } else {
            // these ids are synonyms
            lookupMap = orgSynMaps.get(taxonId);
            if (lookupMap == null) {
                lookupMap = new HashMap<String, Set<String>>();
                orgSynMaps.put(taxonId, lookupMap);
            }

            reverseMap = orgIdSynMaps.get(taxonId);
            if (reverseMap == null) {
                reverseMap = new HashMap<String, Set<String>>();
                orgIdSynMaps.put(taxonId, reverseMap);
            }
        }

        // map from primaryId back to main/synonym ids
        addToMapList(reverseMap, primaryIdentifier, ids);

        for (String id : ids) {
            addToMapList(lookupMap, id, Collections.singleton(primaryIdentifier));
        }
    }

    /**
     * Write IdResolver contents to a flat file
     * @param f the file to write to
     * @throws IOException if fail to write
     */
    public void writeToFile(File f) throws IOException {
        FileWriter fw = new FileWriter(f);
        for (String taxonId : orgIdMaps.keySet()) {

            // get maps for this organism
            Map<String, Set<String>> idMap = orgIdMaps.get(taxonId);
            Map<String, Set<String>> mainIdsMap = orgIdMainMaps.get(taxonId);
            Map<String, Set<String>> synonymMap = orgIdSynMaps.get(taxonId);

            for (Map.Entry<String, Set<String>> idMapEntry : idMap.entrySet()) {
                StringBuffer sb = new StringBuffer();

                String primaryId = idMapEntry.getKey();

                sb.append(taxonId + "\t");  // write taxon id
                sb.append(primaryId + "\t");  // write primary id

                if (mainIdsMap != null && mainIdsMap.containsKey(primaryId)) {
                    boolean first = true;
                    for (String mainId : mainIdsMap.get(primaryId)) {
                        if (!first) {
                            sb.append(",");
                        } else {
                            first = false;
                        }
                        sb.append(mainId);
                    }
                }

                if (synonymMap != null && synonymMap.containsKey(primaryId)) {
                    boolean first = true;
                    sb.append("\t");
                    for (String synonym : synonymMap.get(primaryId)) {
                        if (!first) {
                            sb.append(",");
                        } else {
                            first = false;
                        }
                        sb.append(synonym);
                    }
                }
                sb.append(System.getProperty("line.separator"));
                fw.write(sb.toString());
            }
        }
        fw.flush();
        fw.close();
    }


    /**
     * Read contents of an IdResolver from file, allows for caching during a build.
     * @param f the file to read from
     * @throws IOException if problem reading from file
     */
    public void populateFromFile(File f) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(f));
        String line = null;
        while ((line = reader.readLine()) != null) {
            String[] cols = line.split("\t");
            String taxonId = cols[0];
            String primaryId = cols[1];

            String mainIdsStr = cols[2];
            if (!StringUtils.isBlank(mainIdsStr)) {
                String[] mainIds = mainIdsStr.split(",");
                addEntry(taxonId, primaryId, Arrays.asList(mainIds), Boolean.TRUE);
            }

            // read synonyms if they are present
            if (cols.length >= 4) {
                String synonymsStr = cols[3];
                if (!StringUtils.isBlank(synonymsStr)) {
                    String[] synonyms = synonymsStr.split(",");
                    addEntry(taxonId, primaryId, Arrays.asList(synonyms), Boolean.FALSE);
                }
            }
        }
        reader.close();
    }

    // check that the given taxon id has some data for it
    private void checkTaxonId(String taxonId) {
        if (!orgIdMaps.containsKey(taxonId)) {
            throw new IllegalArgumentException(clsName + " IdResolver has "
                                               + "no data for taxonId: "
                                               + taxonId + ".");
        }
    }

    // add a new list to a map or add elements of set to existing map entry
    private void addToMapList(Map<String, Set<String>> map, String key, Collection<String> values) {
        Set<String> set = map.get(key);
        if (set == null) {
            set = new HashSet<String>();
            map.put(key, set);
        }
        set.addAll(values);
    }
}
