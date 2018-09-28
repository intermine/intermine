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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Class to map the key used by the data source providers (e.g. primaryAccession for uniprot,
 * pubMedId for pubmed) and the possible prefixes (e.g. 'GO:', 'DOID:') applied to the
 * local unique identifiers values by InterMine.
 * An example: go uses as LUI 0000186 which is stored in intermine as GO:0000186.
 * The class extracts all the info from the prefix-keys.properties file.
 *
 * @author danielabutano
 */
public final class PrefixKeyMapper
{
    private static PrefixKeyMapper instance = null;
    private Properties properties = null;
    //map tp cache the key associated to a prefix
    private Map<String, String> prefixKeyMap = null;
    private String regex = "(((-([a-zA-Z])+)|(\\+([a-zA-Z])+)):?)?\\{([a-zA-Z])+\\}";
    private static final Logger LOGGER = Logger.getLogger(PrefixKeyMapper.class);

    /**
     * Private constructor called by getMapper (singleton)
     */
    private PrefixKeyMapper() {
        properties = new Properties();
        prefixKeyMap = new HashMap();
        try {
            InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream("prefix-key.properties");
            if (inputStream == null) {
                LOGGER.error("File prefix-key.properties not found");
                return;
            }
            properties.load(inputStream);
            String propertyValue;
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                propertyValue = (String) entry.getValue();
                if (!propertyValue.matches(regex)) {
                    LOGGER.error("In the prefix-key.properties file, the key " + entry.getKey().toString()
                            + " has a value which does not match the regular expression");
                    return;
                } else {
                    int start = propertyValue.indexOf("{");
                    int end = propertyValue.indexOf("}");
                    prefixKeyMap.put((String) entry.getKey(), propertyValue.substring(start + 1, end));
                }
            }
        } catch (IOException ex) {
            LOGGER.error("Error loading prefix-key.properties file", ex);
            return;
        }
    }

    /**
     * Static method to create the instance of PrefixKeyMapper class
     * @return the PrefixKeyMapper instance
     */
    public static PrefixKeyMapper getMapper() {
        if (instance == null) {
            instance = new PrefixKeyMapper();
        }
        return instance;
    }

    /**
     * Given the prefix, returns the key (e.g. primaryAccession, pubmedid, primaryIdentifier)
     * associated to it; data source providers have different keys
     * @param prefix the prefix
     * @return the key assigned to the prefix
     */
    public String getKey(String prefix) {
        if (prefixKeyMap != null) {
            return prefixKeyMap.get(prefix);
        }
        return null;
    }

    /**
     * This method is used to convert the LUI(local unique identifier) into the value stored
     * by InterMine which, in some case, might be different. Some example:
     * go uses 0000186 as LUI which is stored in intermine as GO:0000186
     * doid(disease ontology) uses 0001816 as LUI which is stored in intermine as DOID:0001816
     * uniprot uses P81928 which is stored in intermine with no alteration as P81928
     * @param curie the compact uri
     * @return the value used by Intermine to represent the localUniqueId
     */
    public String getInterMineAdaptedLUI(CURIE curie) {
        if (properties != null) {
            String prefix = curie.getPrefix();
            String propertyValue = properties.getProperty(prefix);
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

    public String getOriginalLUI(String prefix, String intermineAdaptedLUI) {
        if (properties != null) {
            String propertyValue = properties.getProperty(prefix);
            String originalValue = null;
            if (propertyValue.startsWith("{")) {
                originalValue = intermineAdaptedLUI;
            } else {
                int index = propertyValue.indexOf("{");
                if (propertyValue.startsWith("+")) {
                    String prefixToAdd = propertyValue.substring(1, index);
                    originalValue = prefixToAdd + intermineAdaptedLUI;
                } else if (propertyValue.startsWith("-")) {
                    String prefixToRemove = propertyValue.substring(1, index);
                    originalValue = intermineAdaptedLUI.replace(prefixToRemove, "");
                }
            }
            return originalValue;
        }
        return null;
    }
}
