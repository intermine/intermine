package org.intermine.api.fairresolution;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/***
 * A register of FAIR IDs to intermine object IDs
 *
 * @author justincc
 */
public class FairRegistry
{
/*    private Map<String, Map<String, Integer>> registry =
            new HashMap<String, Map<String, Integer>>();
    private long idsCount;

    *//**
     * Add a mapping from FAIR id components to the InterMine internal ID
     *
     * @param prefix
     * @param uniqueId
     * @param intermineId
     *//*
    public void add(String prefix, String uniqueId, int intermineId) {
        prefix = prefix.toLowerCase();

        if (!registry.containsKey(prefix)) {
            registry.put(prefix, new HashMap<String, Integer>());
        }

        if (registry.get(prefix).put(uniqueId, intermineId) == null) {
            idsCount += 1;
        }
    }

    *//**
     * For a given FAIR prefix and local unique identifier, resolve to an
     * internal InterMine object ID.
     *
     * @param prefix prefix of the FAIR identifier
     * @param uniqueId local unique id component
     * @return null if the inputs did not resolve to an internal object ID
     *//*
    public Integer resolve(String prefix, String uniqueId) {
        prefix = prefix.toLowerCase();

        Map<String, Integer> localMap = registry.get(prefix);

        if (localMap == null) {
            return null;
        }

        return localMap.get(uniqueId);
    }

    public long idsSize() {
        return idsCount;
    }

    public long prefixesSize() {
        return registry.size();
    }*/
}
