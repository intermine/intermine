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
 * Class to map the class name defined in the core.xml with the identifier used to generate the InterMineLUI
 * (e.g. Protein ->primaryAccession,  Publication ->pubMedId). The map is loaded from class_keys.properties
 * where we have set e.g.
 * Protein_URI = primaryAccession
 *
 * @author danielabutano
 */
public final class ClassNameURIIdentifierMapper
{
    private static ClassNameURIIdentifierMapper instance = null;
    private static final String URI_SUFFIX = "_URI";
    private Properties properties = null;
    //map tp cache the identifier associated to a class Name
    private final static Map<String, String> classNameIdentifiersMap = new HashMap();
    private static final Logger LOGGER = Logger.getLogger(ClassNameURIIdentifierMapper.class);

    /**
     * Private constructor called by getMapper (singleton)
     */
    private ClassNameURIIdentifierMapper() {
        properties = new Properties();
        try {
            InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream("class_keys.properties");
            if (inputStream == null) {
                LOGGER.error("File class_keys.properties not found");
                return;
            }
            properties.load(inputStream);
            String key = null;
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                key = (String) entry.getKey();
                if (key.contains(URI_SUFFIX)) {
                    String className = key.replace(URI_SUFFIX, "");
                    classNameIdentifiersMap.put(className, (String) entry.getValue());
                }

            }
        } catch (IOException ex) {
            LOGGER.error("Error loading class_keys.properties file", ex);
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
}
