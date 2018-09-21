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
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;

/**
 * A registry for all the prefixes set in the prefixes.properties file
 *
 * @author danielabutano
 */
public final class PrefixRegistry
{
    private static PrefixRegistry instance = null;
    private Properties prefixProperties = null;
    private Set<String> prefixes = null;
    private Map<String, List<String>> prefixClassNamesMap = null;
    private static final Logger LOGGER = Logger.getLogger(PrefixRegistry.class);

    /**
     * Private constructor
     */
    private PrefixRegistry() {
        prefixProperties = new Properties();
        try {
            InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream("prefixes.properties");
            if (inputStream == null) {
                LOGGER.error("File prefixes.properties not found");
                return;
            }
            prefixProperties.load(inputStream);
        } catch (IOException ex) {
            LOGGER.error("Error reading prefixes.properties file", ex);
            return;
        }
        prefixes = new HashSet<>();
        prefixClassNamesMap = new HashMap<>();
        for (Map.Entry<Object, Object> entry : prefixProperties.entrySet()) {
            String prefixesAsString = entry.getValue().toString();
            List<String> prefixList = Arrays.asList(prefixesAsString.split(",(\\s){0,}"));
            prefixes.addAll(prefixList);
            for (String prefix : prefixList) {
                if (!prefixClassNamesMap.containsKey(prefix)) {
                    prefixClassNamesMap.put(prefix,
                            new ArrayList<>(Arrays.asList(entry.getKey().toString())));
                } else {
                    prefixClassNamesMap.get(prefix).add(entry.getKey().toString());
                }
            }
        }
    }

    /**
     * Static method to create the instance of PrefixRegistry class
     * @return the PrefixRegistry instance
     */
    public static PrefixRegistry getRegistry() {
        if (instance == null) {
            instance = new PrefixRegistry();
        }
        return instance;
    }

    /**
     * Get all the prefixes registered
     * @return the set of prefixes register
     */
    public Set<String> getPrefixes() {
        return prefixes;
    }

    /**
     * Get the prefixes which can be associated to the class name given in input.
     * Flybase (with prefix fb) can provide, for example, proteins which are not provided
     * by uniprot so given in input 'Protein' as class name the list of prefixes returned
     * might be: uniprot, fb.
     * @param className the class name, e.g. Protein
     * @return the list of prefixes
     */
    public List<String> getPrefixes(String className) {
        String prefixesAsString = prefixProperties.getProperty(className);
        if (prefixesAsString != null && !prefixesAsString.isEmpty()) {
            return Arrays.asList(prefixesAsString.split(","));
        }
        return null;
    }

    /**
     * Return the list of class names assigned to a prefix
     * @param prefix the prefix
     * @return the list of class names assigned to a prefix given in input
     */
    public List<String> getClassNames(String prefix) {
        if (prefixClassNamesMap != null) {
            return prefixClassNamesMap.get(prefix);
        }
        return null;
    }
}
