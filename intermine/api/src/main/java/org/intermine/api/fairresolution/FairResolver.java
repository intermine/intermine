package org.intermine.api.fairresolution;

import java.util.HashMap;
import java.util.Map;

/***
 * Resolve our FAIR unique and persistent URIs to internal InterMine object information.
 */
public class FairResolver {
    private Map<String, Map<String, Integer>> registry = new HashMap<String, Map<String, Integer>>();

    /**
     * Add a mapping from FAIR id components to the InterMine internal ID
     *
     * @param prefix
     * @param localUniqueIdentifier
     * @param intermineOsId
     */
    public void addMapping(String prefix, String localUniqueIdentifier, int intermineOsId) {
        prefix = prefix.toLowerCase();

        if (!registry.containsKey(prefix)) {
            registry.put(prefix, new HashMap<String, Integer>());
        }

        registry.get(prefix).put(localUniqueIdentifier, intermineOsId);
    }

    /**
     * For a given FAIR prefix and local unique identifier, resolve to an internal InterMine object ID.
     *
     * @param prefix prefix of the FAIR identifier
     * @param localUniqueIdentifier local unique id component
     * @return null if the inputs did not resolve to an internal object ID
     */
    public Integer resolve(String prefix, String localUniqueIdentifier) {
        prefix = prefix.toLowerCase();

        Map<String, Integer> localMap = registry.get(prefix);

        if (localMap == null) {
            return null;
        }

        return localMap.get(localUniqueIdentifier);

        /*
        if (prefix.equals("ensembl") && localUniqueIdentifier.equals("ENSG00000092054")) {
            return 1;
        }

        return null;
        */
    }
}
