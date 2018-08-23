package org.intermine.api.fairresolution;

/***
 * Resolve our FAIR unique and persistent URIs to internal InterMine object information.
 */
public class FairResolver {
    /**
     * For a given FAIR prefix and local unique identifier, resolve to an internal InterMine object ID.
     *
     * @param prefix prefix of the FAIR identifier
     * @param localUniqueIdentifier local unique id component
     * @return null if the inputs did not resolve to an internal object ID
     */
    public Integer resolve(String prefix, String localUniqueIdentifier) {
        prefix = prefix.toLowerCase();

        if (prefix.equals("ensembl") && localUniqueIdentifier.equals("ENSG00000092054")) {
            return 1;
        }

        return null;
    }
}
