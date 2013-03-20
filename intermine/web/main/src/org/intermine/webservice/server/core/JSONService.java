package org.intermine.webservice.server.core;

/*
 * Copyright (C) 2002-2013 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.metadata.Model;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.output.JSONFormatter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

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
    protected void postInit() {
        output.setHeaderAttributes(getHeaderAttributes());
    }

    protected String getResultsKey() {
    	return null;
    }
    
    /**
     * Get the header attributes to apply to the formatter.
     * @return A map from string to object.
     */
    protected Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = new HashMap<String, Object>();
        String resultsKey = getResultsKey();
        if (resultsKey != null) {
        	attributes.put(JSONFormatter.KEY_INTRO, "\"" + resultsKey + "\":");
        }
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
    
    /**
     * Output a map of names and values as a JSON object.
     * @param mapping the mapping of things to output.
     * @param hasMore Whether there is more to come, and thus a comma is required.
     */
    protected void addResultItem(Map<String, ? extends Object> mapping, boolean hasMore) {
        JSONObject jo = new JSONObject(mapping);
        addResultItemInternal(jo, hasMore);
    }

    protected void addResultValue(String str, boolean hasMore) {
        addResultItemInternal("\"" + String.valueOf(str) + "\"", hasMore);
    }

    protected void addResultValue(Number num, boolean hasMore) {
        addResultValueInternal(String.valueOf(num), hasMore);
    }

    protected void addResultValue(Boolean bool, boolean hasMore) {
        addResultValueInternal(String.valueOf(bool), hasMore);
    }

    private void addResultValueInternal(String val, boolean hasMore) {
        List<String> outputStrings = new ArrayList<String>();
        outputStrings.add(val);
        if (hasMore) outputStrings.add("");
        output.addResultItem(outputStrings);
    }

    /**
     * Output a list of objects as a JSON array.
     * @param listing The list of things to output.
     * @param hasMore Whether there is more to come, and thus a comma is required.
     */
    protected void addResultItem(List<? extends Object> listing, boolean hasMore) {
        JSONArray ja = new JSONArray(listing);
        addResultItemInternal(ja, hasMore);
    }
    
    private void addResultItemInternal(Object obj, boolean hasMore) {
        List<String> outputStrings = new ArrayList<String>();
        outputStrings.add(String.valueOf(obj));
        if (hasMore) {
            outputStrings.add(""); // Dummy value used to get a comma printed.
        }
        output.addResultItem(outputStrings);
    }

    @Override
    protected Format getDefaultFormat() {
        return Format.JSON;
    }
}
