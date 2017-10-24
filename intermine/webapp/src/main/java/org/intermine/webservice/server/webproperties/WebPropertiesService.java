package org.intermine.webservice.server.webproperties;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;

import org.intermine.api.InterMineAPI;
import org.intermine.util.PropertiesUtil;
import org.intermine.webservice.server.core.JSONService;


/**
 * Exports selected web.properties.
 *
 * @author Julie
 */
public class WebPropertiesService extends JSONService
{
    //private static final Logger LOG = Logger.getLogger(WebPropertiesService.class);
    // if there is a parent property with an additional child value, we need a key
    private static final String DEFAULT_PATH = "default";

    /** @param im The InterMine state object. **/
    public WebPropertiesService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        Map<String, Object> webPropertiesMap = new HashMap<String, Object>();

        // region search
        appendProperties(webPropertiesMap, "genomicRegionSearch");

        // example identifiers for list upload
        appendProperties(webPropertiesMap, "bag");

        // example identifiers for quickSearch
        appendProperties(webPropertiesMap, "quickSearch");

        // mine name, citation
        appendProperties(webPropertiesMap, "project");

        // list params, delimiters
        appendProperties(webPropertiesMap, "list");

        // defaults for iodocs, only setting default query right now
        appendProperties(webPropertiesMap, "services");

        addResultItem(webPropertiesMap, false);
    }

    @Override
    public String getResultsKey() {
        return "web-properties";
    }

    private void appendProperties(final Map<String, Object> webPropertiesMap,
            final String startingPath) {
        Properties props = PropertiesUtil.getPropertiesStartingWith(startingPath, webProperties);
        Map<String, Object> thisPropertyMap = new HashMap<String, Object>();
        for (Object key: props.keySet()) {
            String keyString = String.valueOf(key);
            String[] keyParts = keyString.split("\\.");
            Queue<String> path = new LinkedList<String>();
            for (int i = 1; i < keyParts.length; i++) {
                path.add(keyParts[i]);
            }
            setProperty(thisPropertyMap, path, props.getProperty(keyString));
        }
        webPropertiesMap.put(startingPath, thisPropertyMap);
    }

    private void setProperty(
            final Map<String, Object> propertyMap,
            final Queue<String> path,
            final String value) {
        String key = path.remove();
        if (path.isEmpty()) {
            // make sure that there are no children for this attribute
            // eg. bag.examples = "eve"
            //     bag.examples.protein = "EVE_DROME"
            Map<String, Object> thisLevel = (Map<String, Object>) propertyMap.get(key);
            if (thisLevel == null || thisLevel.isEmpty()) {
                propertyMap.put(key, value);
            } else {
                // there are other children at this level, so just call this value "default"
                // see #1631
                setProperty(thisLevel, new LinkedList<String>(Arrays.asList(DEFAULT_PATH)), value);
            }
        } else {
            Map<String, Object> thisLevel;
            if (!propertyMap.containsKey(key)) {
                thisLevel = new HashMap<String, Object>();
                propertyMap.put(key, thisLevel);
            } else {
                thisLevel = (Map<String, Object>) propertyMap.get(key);
            }
            setProperty(thisLevel, path, value);
        }
    }

}
