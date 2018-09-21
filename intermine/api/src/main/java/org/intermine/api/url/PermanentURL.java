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
 * Permamanent ULR pattern: domain/context/prefix:external_local_id
 * (e.g. humanmine.org/humanmine/uniprot:P31946)
 *
 * @author danielabutano
 */
public class PermanentURL
{
    private String prefix;
    private static final String LOCAL_ID_SEPARATOR = ":";
    private String externalLocalId;
    private static final Logger LOGGER = Logger.getLogger(PermanentURL.class);

    /**
     * Constructor. Build a PermanentURL after verifying the prefix
     * @param permanentURI URI as humanmine/uniprot:P31946
    */
    public PermanentURL(String permanentURI) throws InvalidPermanentURLException {
        int locadIdSeparatorPosition = permanentURI.lastIndexOf(LOCAL_ID_SEPARATOR);
        if (locadIdSeparatorPosition == -1) {
            throw new InvalidPermanentURLException();
        }
        try {
            String prefixFromPermanentURI = permanentURI.substring(permanentURI.lastIndexOf("/") + 1,
                    locadIdSeparatorPosition);
            Set<String> prefixes = PrefixRegistry.getRegistry().getPrefixes();
            boolean validURL = false;
            if (prefixes != null) {
                for (String tmpPrefix : prefixes) {
                    if (prefixFromPermanentURI.equals(tmpPrefix)) {
                        this.prefix = prefixFromPermanentURI;
                        externalLocalId = permanentURI.substring(locadIdSeparatorPosition + 1);
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

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getExternalLocalId() {
        return externalLocalId;
    }

    public void setExternalLocalId(String externalLocalId) {
        this.externalLocalId = externalLocalId;
    }

    public String toString() {
        return prefix + LOCAL_ID_SEPARATOR + externalLocalId;
    }
}
