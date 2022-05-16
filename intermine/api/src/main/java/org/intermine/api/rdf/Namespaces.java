package org.intermine.api.rdf;

/*
 * Copyright (C) 2002-2022 FlyMine
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
 * Class to load the prefix of the RDF namespaces
 * @author Daniela Butano
 */
public final class Namespaces
{
    private static Namespaces instance;
    private static Map<String, String> namespaces;
    private static final Logger LOG = Logger.getLogger(Namespaces.class);

    private Namespaces() {
        namespaces = new HashMap<>();
        Properties namespaceProp = new Properties();
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("prefix_namespaces.properties");
        if (is != null) {
            try {
                namespaceProp.load(is);
                for (Object key: namespaceProp.keySet()) {
                    String prefix = (String) key;
                    namespaces.put(prefix, namespaceProp.getProperty(prefix));
                }
            } catch (IOException e) {
                LOG.error("Issues reading the prefix_namepsaces.properties", e);
            }
        }
    }

    /**
     * Return all the namespaces configured in the prefix_namespaces.properties file
     * @return the map containing prefix and uri
     */
    public static Map<String, String> getNamespaces() {
        if (instance == null) {
            instance = new Namespaces();
        }
        return namespaces;
    }
}
