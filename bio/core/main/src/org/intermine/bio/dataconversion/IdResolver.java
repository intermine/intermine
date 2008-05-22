package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Hold data about primary identifiers and synonyms for a particular class in the
 * data model and provide methods to resolved synonms into correseponding
 * primary identifier(s).
 * @author rns
 *
 */
public class IdResolver
{
    private String clsName;
    private Map<String, Map<String, Set<String>>> orgIdMaps = new HashMap();
    private Map<String, Map<String, Set<String>>> orgSynMaps = new HashMap();
    private Map<String, Map<String, Set<String>>> orgMainMaps = new HashMap();

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
     * taxonId.  In many cases the set will have just one element.
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
        return Collections.EMPTY_SET;
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
        if (orgIdMaps.get(taxonId).containsKey(id)) {
            return 1;
        }
        if (orgMainMaps.containsKey(taxonId)
            && orgMainMaps.get(taxonId).containsKey(id)) {
            return orgMainMaps.get(taxonId).get(id).size();
        }
        if (orgSynMaps.containsKey(taxonId)
            && orgSynMaps.get(taxonId).containsKey(id)) {
            return orgSynMaps.get(taxonId).get(id).size();
        }
        return 0;
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
    public void addMainIds(String taxonId, String primaryIdentifier, Set<String> ids) {
        addEntry(taxonId, primaryIdentifier, ids, true);
    }

    /**
     * Add synonyms for a primary identifier to the IdResolver
     * @param taxonId the organism of the identifier
     * @param primaryIdentifier the main identifier
     * @param ids a set synonyms
     */
    public void addSynonyms(String taxonId, String primaryIdentifier, Set<String> ids) {
        addEntry(taxonId, primaryIdentifier, ids, false);
    }

    /**
     * Add an entry to the IdResolver, a primary identifier and any number of synonyms.
     * @param taxonId the organism of the identifier
     * @param primaryIdentifier the main identifier
     * @param synonyms a set of synonyms
     * @param mainId if true these are main ids, otherwise synonms
    */
     private void addEntry(String taxonId, String primaryIdentifier, Set<String> ids,
                           Boolean mainId) {
        Map<String, Set<String>> idMap = orgIdMaps.get(taxonId);
        if (idMap == null) {
            idMap = new HashMap();
            orgIdMaps.put(taxonId, idMap);
        }

        addToMapList(idMap, primaryIdentifier, ids);

        Map<String, Set<String>> map = null;
        if (mainId) {
            map = orgMainMaps.get(taxonId);
            if (map == null) {
                map = new HashMap();
                orgMainMaps.put(taxonId, map);
            }
        } else {
            // these ids are synonyms
            map = orgSynMaps.get(taxonId);
            if (map == null) {
                map = new HashMap();
                orgSynMaps.put(taxonId, map);
            }
        }

        for (String id : ids) {
                addToMapList(map, id, Collections.singleton(primaryIdentifier));
        }
    }

    /**
     * Write IdResolver contents to a flat file
     * @param f the file to write to
     * @throws IOException if fail to write
     */
    public void writeToFile(File f) throws IOException {
        StringBuffer sb = new StringBuffer();
        for (Map<String, Set<String>> idMap : orgIdMaps.values()) {

            for (Map.Entry<String, Set<String>> entry : idMap.entrySet()) {
                sb.append(entry.getKey() + "\t");
                for (String s : entry.getValue()) {
                    sb.append(s + "\t");
                }
                sb.append(System.getProperty("line.separator"));
            }
        }
        FileWriter fw = new FileWriter(f);
        fw.write(sb.toString());
        fw.flush();
        fw.close();
    }

    // check that the given taxon id has some data for it
    private void checkTaxonId(String taxonId) throws IllegalArgumentException {
        if (!orgIdMaps.containsKey(taxonId)) {
            throw new IllegalArgumentException(clsName + " IdResolver has "
                                               + "no data for taxonId: "
                                               + taxonId + ".");
        }
    }

    // add a new list to a map or add elements of set to existing map entry
    private void addToMapList(Map<String, Set<String>> map, String key, Set<String> values) {
        Set<String> set = map.get(key);
        if (set == null) {
            set = new HashSet<String>();
            map.put(key, set);
        }
        set.addAll(values);
    }
}
