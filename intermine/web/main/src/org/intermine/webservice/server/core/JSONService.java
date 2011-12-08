package org.intermine.webservice.server.core;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.metadata.Model;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.output.JSONFormatter;

/**
 * A Service that has specialisations for supplying JSON.
 * @author Alex Kalderimis
 *
 */
public abstract class JSONService extends WebService
{

    protected final BagManager bagManager;
    protected final Model model;

    private final Map<String, String> kvPairs = new HashMap<String, String>();

    /**
     * Constructor
     * @param im The InterMine configuration object.
     */
    public JSONService(InterMineAPI im) {
        super(im);
        bagManager = im.getBagManager();
        model = im.getObjectStore().getModel();
    }

    @Override
    protected void initState() {
        output.setHeaderAttributes(getHeaderAttributes());
    }

    /**
     * Get the header attributes to apply to the formatter.
     * @return A map from string to object.
     */
    protected Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = new HashMap<String, Object>();
        if (formatIsJSONP()) {
            attributes.put(JSONFormatter.KEY_CALLBACK, getCallback());
        }
        attributes.put(JSONFormatter.KEY_KV_PAIRS, kvPairs);
        return attributes;
    }

    /**
     * Add a key value pair to put in the header of json results.
     * @param key An identifier.
     * @param value Some piece of data.
     */
    protected void addOutputInfo(String key, String value) {
        kvPairs.put(key, value);
    }

    @Override
    protected int getDefaultFormat() {
        if (hasCallback()) {
            return WebService.JSONP_FORMAT;
        } else {
            return WebService.JSON_FORMAT;
        }
    }
}
