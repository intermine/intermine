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
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Class to manage the keys used by the data source providers (e.g. primaryAccession for uniprot,
 * pubMedId for pubmed) and the possible prefixes (e.g. 'GO:', 'DOID:') applied to the
 * local unique identifiers values by InterMine.
 * An example: go uses as LUI 0000186 which is stored in intermine as GO:0000186.
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

    /**
     * Private constructor called by getProperties (singleton)
     */
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
            for (Object value : prefixKeysProperties.values()) {
                if (!((String) value).matches(regex)) {
                    LOGGER.error("In the prefix-keys.properties file, the key " + value.toString()
                            + " has a value which does not match the regular expression");
                    return;
                }
            }
        } catch (IOException ex) {
            LOGGER.error("Error loading prefix-keys.properties file", ex);
            return;
        }
    }

    /**
     * Static method to create the instance of PrefixKeysProperties class
     * @return the PrefixKeysProperties instance
     */
    public static PrefixKeysProperties getProperties() {
        if (instance == null) {
            instance = new PrefixKeysProperties();
        }
        return instance;
    }

    /**
     * Given the prefix, returns the key (e.g. primaryAccession, pubmedid, primaryIdentifier)
     * associated to it; data source providers have different keys
     * @param prefix the prefix
     * @return the key assigned to the prefix
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
     * This methos is used to convert the LUI(local unique identifier) into the value stored
     * by InterMine which, in some case, might be different. Some example:
     * go uses 0000186 as LUI which is stored in intermine as GO:0000186
     * doid(disease ontology) uses 0001816 as LUI which is stored in intermine as DOID:0001816
     * uniprot uses P81928 which is stored in intermine with no alteration as P81928
     * @param curie the compact uri
     * @return the value used by Intermine to represent the localUniqueId
     */
    public String getInterMineValue(CURIE curie) {
        if (prefixKeysProperties != null) {
            String prefix = curie.getPrefix();
            String propertyValue = prefixKeysProperties.getProperty(prefix);
            if (!propertyValue.matches(regex)) {
                LOGGER.error("In the prefix-keys.properties file, the key " + prefix
                        + " has a value which does not match the regular expression");
                return null;
            }
            String interMineValue = null;
            String localUniqueId = curie.getLocalUniqueId();
            if (propertyValue.startsWith("{")) {
                interMineValue = localUniqueId;
            } else {
                int index = propertyValue.indexOf("{");
                if (propertyValue.startsWith("+")) {
                    String prefixToRemove = propertyValue.substring(1, index);
                    interMineValue = localUniqueId.replace(prefixToRemove, "");
                } else if (propertyValue.startsWith("-")) {
                    String prefixToAdd = propertyValue.substring(1, index);
                    interMineValue = prefixToAdd + localUniqueId;
                }
            }
            return interMineValue;
        }
        return null;
    }
}
