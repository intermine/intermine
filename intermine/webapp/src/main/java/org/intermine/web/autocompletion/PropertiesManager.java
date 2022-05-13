package org.intermine.web.autocompletion;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * A manager class to handle all the configuration properties
 * from objectstoresummary.config.properties file
 *
 * @author arunans23
 */
final class PropertiesManager
{
    private static final Logger LOG = Logger.getLogger(PropertiesManager.class);

    private static PropertiesManager propertiesManager;

    private static final String CONFIG_FILE_NAME = "objectstoresummary.config.properties";

    private Properties properties = null;

    private HashMap<String, String> classFieldMap = new HashMap<String, String>();

    private String solrUrl;

    private PropertiesManager() {
        parseProperties();
    }

    /**
     * static method to get one instance of Properties Manager
     *
     * @return Manager instance
     **/
    public static PropertiesManager getInstance() {
        if (propertiesManager == null) {
            synchronized (PropertiesManager.class) {
                if (propertiesManager == null) {
                    propertiesManager = new PropertiesManager();
                }
            }
        }
        return propertiesManager;
    }

    private synchronized void parseProperties() {
        if (properties != null) {
            return;
        }

        String configFileName = CONFIG_FILE_NAME;
        ClassLoader classLoader = PropertiesManager.class.getClassLoader();
        InputStream configStream = classLoader.getResourceAsStream(configFileName);

        if (configStream != null) {
            properties = new Properties();

            try {
                properties.load(configStream);

                for (Map.Entry<Object, Object> entry: properties.entrySet()) {
                    String key = (String) entry.getKey();
                    String value = (String) entry.getValue();
                    if (key.endsWith(".autocomplete")) {
                        String className = key.substring(0, key.lastIndexOf("."));
                        classFieldMap.put(className, value);
                    } else if ("autocomplete.solrurl".equals(key) && !StringUtils.isBlank(value)) {
                        solrUrl = value;
                    }

                }
            } catch (IOException e) {
                LOG.error("keyword_search.properties: errow while loading file '" + configFileName
                        + "'", e);
            }
        } else {
            LOG.error("objectstore_summary.properties: file '" + configFileName + "' not found!");
        }
    }

    /**
    * @return classFieldMap
    */
    HashMap<String, String> getClassFieldMap() {
        return classFieldMap;
    }

    /**
     * @return solrUrl
     */
    String getSolrUrl() {
        return solrUrl;
    }
}
