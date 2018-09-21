package org.intermine.api.url;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import java.util.Set;

/**
 * Class to represent the permanentURL pattern adopted in InterMine.
 * Permanent ULR pattern: domain/context/CURIE where CURIE: prefix:external_local_id
 * (e.g. humanmine.org/humanmine/uniprot:P31946-> CURIE=uniprot:P31946)
 *
 * @author danielabutano
 */
public class CURIE
{
    private String prefix;
    private static final String LOCAL_ID_SEPARATOR = ":";
    private String localUniqueId;
    private static final Logger LOGGER = Logger.getLogger(CURIE.class);

    /**
     * Constructor. Build a CURIE after verifying the prefix
     * @param permanentURI URI as humanmine/uniprot:P31946
     * @throws InvalidPermanentURLException if the permanentURI in input is not an permanent URI
    */
    public CURIE(String permanentURI) throws InvalidPermanentURLException {
        int locadIdSeparatorPosition = permanentURI.lastIndexOf(LOCAL_ID_SEPARATOR);
        if (locadIdSeparatorPosition == -1) {
            throw new InvalidPermanentURLException();
        }
        try {
            String prefixFromPermanentURI = permanentURI.substring(
                    permanentURI.lastIndexOf("/") + 1, locadIdSeparatorPosition);
            Set<String> prefixes = PrefixRegistry.getRegistry().getPrefixes();
            boolean validURL = false;
            if (prefixes != null) {
                for (String tmpPrefix : prefixes) {
                    if (prefixFromPermanentURI.equals(tmpPrefix)) {
                        this.prefix = prefixFromPermanentURI;
                        localUniqueId = permanentURI.substring(locadIdSeparatorPosition + 1);
                        validURL = true;
                    }
                }
                if (!validURL) {
                    throw new InvalidPermanentURLException();
                }
            } else {
                throw new InvalidPermanentURLException();
            }
        } catch (IndexOutOfBoundsException ex) {
            throw new InvalidPermanentURLException();
        }

    }

    /**
     * Get the prefix
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Set the prefix
     * @param prefix the value to set
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Get the LUI (local unique identifier)
     * @return the LUI
     */
    public String getLocalUniqueId() {
        return localUniqueId;
    }

    /**
     * Set the LUI (local unique identifier)
     * @param localUniqueId the LUI
     */
    public void setLocalUniqueId(String localUniqueId) {
        this.localUniqueId = localUniqueId;
    }

    /**
     * Returns the String which represents the CURIE -> prefix:localUniqueId
     * @return the string in the format prefix:localUniqueId
     */
    public String toString() {
        return prefix + LOCAL_ID_SEPARATOR + localUniqueId;
    }
}
