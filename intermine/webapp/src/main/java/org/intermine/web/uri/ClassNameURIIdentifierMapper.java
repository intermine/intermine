package org.intermine.web.uri;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.HashSet;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;

/**
 * Class to map the class name defined in the core.xml with the identifier used to generate the
 * InterMineLUI (e.g. Protein ->primaryAccession,  Publication ->pubMedId). The map is loaded
 * from uri_keys.properties where the identifiers for the core model classes
 * (e.g. Protein_URI = primaryAccession) have been set and from class_keys.properties where the
 * admninistrator can override or add new identifiers
 * @author danielabutano
 */
public final class ClassNameURIIdentifierMapper
{
    private static ClassNameURIIdentifierMapper instance = null;
    private static final String URI_SUFFIX = "_URI";
    private Properties properties = null;
    //map tp cache the identifier associated to a class Name
    private static Map<String, String> classNameIdentifiersMap = new HashMap();
    private static final Logger LOGGER = Logger.getLogger(ClassNameURIIdentifierMapper.class);

    /**
     * Private constructor called by getMapper (singleton)
     */
    private ClassNameURIIdentifierMapper() {
        properties = new Properties();
        try {
            InputStream inputStream = null;
            try {
                inputStream = getClass().getClassLoader()
                        .getResourceAsStream("uri_keys.properties");
                if (inputStream == null) {
                    LOGGER.error("File uri_keys.properties not found");
                    return;
                }
                properties.load(inputStream);
                inputStream = getClass().getClassLoader()
                        .getResourceAsStream("class_keys.properties");
                if (inputStream == null) {
                    LOGGER.error("File class_keys.properties not found");
                    return;
                }
                properties.load(inputStream);
                String key = null;
                Set<String> subClassNames = null;
                for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                    key = (String) entry.getKey();
                    if (key.endsWith(URI_SUFFIX)) {
                        String className = key.replace(URI_SUFFIX, "");
                        classNameIdentifiersMap.put(className, (String) entry.getValue());
                        subClassNames = getSubClassNames(className);
                        for (String subClassName : subClassNames) {
                            classNameIdentifiersMap.put(subClassName, (String) entry.getValue());
                        }
                    }
                }
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (IOException ex) {
            LOGGER.error("Error loading uri_keys.properties/class_keys.properties file", ex);
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

    private Set<String> getSubClassNames(String className) {
        Set<String> subClassNames = new HashSet<String>();
        Model model = Model.getInstanceByName("genomic");
        ClassDescriptor cl = model.getClassDescriptorByName(className);
        if (cl != null) {
            Set<ClassDescriptor> subDescriptors = cl.getSubDescriptors();
            for (ClassDescriptor descriptor : subDescriptors) {
                subClassNames.add(descriptor.getSimpleName());
            }
        }
        return subClassNames;
    }
}
