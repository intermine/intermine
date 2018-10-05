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
 * Class to map the class name defined in the core.xml with the identifier used by the data source
 * providers (e.g. Protein ->primaryAccession,  Publication ->pubMedId)
 *
 * @author danielabutano
 */
public final class ClassNameURIIdentifierMapper
{
    private static ClassNameURIIdentifierMapper instance = null;
    private Properties properties = null;
    //map tp cache the identifier associated to a class Name
    private Map<String, String> classNameIdentifiersMap = null;
    private static final Logger LOGGER = Logger.getLogger(ClassNameURIIdentifierMapper.class);

    /**
     * Private constructor called by getMapper (singleton)
     */
    private ClassNameURIIdentifierMapper() {
        properties = new Properties();
        classNameIdentifiersMap = new HashMap();
        try {
            InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream("uri_identifiers.properties");
            if (inputStream == null) {
                LOGGER.error("File uri_identifiers.properties not found");
                return;
            }
            properties.load(inputStream);
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                classNameIdentifiersMap.put((String) entry.getKey(), (String) entry.getValue());
            }
        } catch (IOException ex) {
            LOGGER.error("Error loading uri_identifiers.properties file", ex);
            return;
        }
    }

    /**
     * Static method to create the instance of ClassNameURIIdentifierMapper class
     * @return the ClassNameURIIdentifierMapper instance
     */
    public static ClassNameURIIdentifierMapper getMapper() {
        if (instance == null) {
            instance = new ClassNameURIIdentifierMapper();
        }
        return instance;
    }

    /**
     * Given the className, returns the identifier (e.g. primaryAccession, pubMedId,
     * primaryIdentifier) associated to it; data source providers have different keys
     * @param className the className
     * @return the identifier assigned to the className
     */
    public String getIdentifier(String className) {
        if (classNameIdentifiersMap != null) {
            return classNameIdentifiersMap.get(className);
        }
        return null;
    }

/*    *//**
     * This method is used to convert the LUI(local unique identifier) into the value stored
     * by InterMine which, in some case, might be different. Some example:
     * go uses 0000186 as LUI which is stored in intermine as GO:0000186
     * doid(disease ontology) uses 0001816 as LUI which is stored in intermine as DOID:0001816
     * uniprot uses P81928 which is stored in intermine with no alteration as P81928
     * @param curie the compact uri
     * @return the value used by Intermine to represent the localUniqueId
     *//*
    public String getInterMineAdaptedLUI(PermanentURI curie) {
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
    }*/
}
