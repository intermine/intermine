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

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to manage the keys used by the data source providers (e.g. primaryAccession for uniprot,
 * pubMedId for pubmed) and the possible prefixes (e.g. 'GO:', 'DOID:') applied to the
 * localExternalId values by InterMine.
 * An example: go uses local ID as 0000186 which is stored in intermine as GO:0000186.
 * The class extracts all the info from the prefix-keys.properties file.
 *
 * @author danielabutano
 */
public final class PrefixKeysProperties
{
    private static PrefixKeysProperties instance = null;
    private Properties prefixKeysProperties = null;
    private String regex = "(((-([a-zA-Z])+)|(\\+([a-zA-Z])+)):?)?\\{([a-zA-Z])+\\}";
    private static final Logger LOGGER = Logger.getLogger(PrefixKeysProperties.class);

    private PrefixKeysProperties() {
        prefixKeysProperties = new Properties();
        try {
            InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream("prefix-keys.properties");
            if (inputStream == null) {
                LOGGER.error("File prefix-keys.properties not found");
                return;
            }
            prefixKeysProperties.load(inputStream);
        } catch (IOException ex) {
            LOGGER.error("Error loading prefix-keys.properties file", ex);
            return;
        }
    }

    public static PrefixKeysProperties getProperties() {
        if (instance == null) {
            instance = new PrefixKeysProperties();
        }
        return instance;
    }

    /**
     * Given the prefix, returns the key (e.g. primaryAccession, pubmedid, primaryIdentifier)
     * associated to it, because not all the data source providers have the same keys
     */
    public String getPrefixKey(String prefix) {
        if (prefixKeysProperties != null) {
            String propertyValue = prefixKeysProperties.getProperty(prefix);
            LOGGER.debug("PrefixKeysProperites: the propertyValues is " + propertyValue);
            if (propertyValue != null) {
                if (!propertyValue.matches(regex)) {
                    LOGGER.error("In the prefix-keys.properties file, the key " + prefix
                            + " has a value which does not match the regular expression");
                    return null;
                }
                Pattern pattern = Pattern.compile(regex);
                int start = propertyValue.indexOf("{");
                int end = propertyValue.indexOf("}");
                return propertyValue.substring(start + 1, end);
            }
        }
        return null;
    }

    /**
     * This methos is used to convert the localExternalID into the value stored by InterMine which,
     * in some case, might be different. Some example:
     * go uses local ID as 0000186 which is stored in intermine as GO:0000186
     * doid(disease ontology) uses local ID as 0001816 which is stored in intermine as DOID:0001816
     * uniprot uses local ID as P81928 which is stored in intermine with no alteration as P81928
     * @param prefix the prefix
     * @param localExternalId the localEXternalId (e.g. 0000186 for go)
     * @return the value used by Intermine to represent the externalId
     */
    public String getInterMineExternalIdValue(String prefix, String localExternalId) {
        if (prefixKeysProperties != null) {
            String propertyValue = prefixKeysProperties.getProperty(prefix);
            if (!propertyValue.matches(regex)) {
                LOGGER.error("In the prefix-keys.properties file, the key " + prefix
                        + " has a value which does not match the regular expression");
                return null;
            }
            String interMineExternalIdValue = null;
            if (propertyValue.startsWith("{")) {
                interMineExternalIdValue = localExternalId;
            } else {
                int index = propertyValue.indexOf("{");
                if (propertyValue.startsWith("+")) {
                    String prefixToRemove = propertyValue.substring(1, index);
                    interMineExternalIdValue = localExternalId.replace(prefixToRemove, "");
                } else if (propertyValue.startsWith("-")) {
                    String prefixToAdd = propertyValue.substring(1, index);
                    interMineExternalIdValue = prefixToAdd + localExternalId;
                }
            }
            return interMineExternalIdValue;
        }
        return null;
    }
}
