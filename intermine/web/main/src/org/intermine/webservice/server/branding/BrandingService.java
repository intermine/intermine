package org.intermine.webservice.server.branding;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.util.PropertiesUtil;
import org.intermine.webservice.server.core.JSONService;

/**
 * Serve branding information so that a client can provide a branded
 * visual experience.
 *
 * This service should be used to provide a customised interaction with a service.
 * @author Alex Kalderimis
 *
 */
public class BrandingService extends JSONService
{

    private static final String PROPERTIES_NEED_2_SECTIONS
        = "Branding properties should contain at least two sections. Skipping ";
    private static final String PREFIX = "branding.";
    private static final Logger LOG = Logger.getLogger(BrandingService.class);

    /** @param im The InterMine state object. **/
    public BrandingService(InterMineAPI im) {
        super(im);
    }

    private Queue<String> toPath(String key) {
        String[] keyParts = key.split("\\.");
        if (keyParts.length < 2) {
            LOG.warn(PROPERTIES_NEED_2_SECTIONS + key);
            return null;
        }
        Queue<String> path = new LinkedList<String>();
        for (int i = 1; i < keyParts.length; i++) {
            path.add(keyParts[i]);
        }
        return path;
    }

    @Override
    protected void execute() throws Exception {
        Properties props = PropertiesUtil.getPropertiesStartingWith(PREFIX, webProperties);
        Map<String, Object> branding = new HashMap<String, Object>();
        for (Object key: props.keySet()) {
            String keyString = String.valueOf(key);
            Queue<String> path = toPath(keyString);
            if (path != null) {
                setProperty(branding, path, props.getProperty(keyString));
            }
        }
        addResultItem(branding, false);
    }

    @Override
    public String getResultsKey() {
        return "properties";
    }

    // Recursively build up a nested object.
    @SuppressWarnings("unchecked")
    private void setProperty(
            final Map<String, Object> branding,
            final Queue<String> path,
            final String value) {
        String key = path.remove();
        if (path.isEmpty()) {
            branding.put(key, value);
        } else {
            Map<String, Object> thisLevel;
            if (!branding.containsKey(key)) {
                thisLevel = new HashMap<String, Object>();
                branding.put(key, thisLevel);
            } else {
                thisLevel = (Map<String, Object>) branding.get(key);
            }
            setProperty(thisLevel, path, value);
        }
    }

}
