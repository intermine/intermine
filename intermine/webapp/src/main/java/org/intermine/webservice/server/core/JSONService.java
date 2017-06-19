package org.intermine.webservice.server.core;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.metadata.Model;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.output.JSONFormatter;
import org.json.JSONArray;
import org.json.JSONObject;

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

    /**
     * @return The key for the results property.
     */
    protected String getResultsKey() {
        return null;
    }

    /**
     * @return Whether to treat this as a lazy list.
     */
    protected boolean lazyList() {
        return false;
    }

    /**
     * Get the header attributes to apply to the formatter.
     * @return A map from string to object.
     */
    protected Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = new HashMap<String, Object>();
        String resultsKey = getResultsKey();
        if (resultsKey != null) {
            String intro = "\"" + resultsKey + "\":";
            if (lazyList()) {
                intro += "[";
                attributes.put(JSONFormatter.KEY_OUTRO, "]");
            }
            attributes.put(JSONFormatter.KEY_INTRO, intro);
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

    /**
     * Output a char-sequence as a JSON value.
     * @param str The character sequence.
     * @param hasMore Whether there are more to come.
     */
    protected void addResultValue(CharSequence str, boolean hasMore) {
        addResultItemInternal("\"" + String.valueOf(str) + "\"", hasMore);
    }

    /**
     * Output a number as a JSON value.
     * @param num The number.
     * @param hasMore Whether there are more to come.
     */
    protected void addResultValue(Number num, boolean hasMore) {
        addResultValueInternal(String.valueOf(num), hasMore);
    }

    /**
     * Output a bool as a JSON value.
     * @param bool The boolean.
     * @param hasMore Whether there are more.
     */
    protected void addResultValue(Boolean bool, boolean hasMore) {
        addResultValueInternal(String.valueOf(bool), hasMore);
    }

    private void addResultValueInternal(String val, boolean hasMore) {
        List<String> outputStrings = new ArrayList<String>();
        outputStrings.add(val);
        if (hasMore) {
            outputStrings.add("");
        }
        output.addResultItem(outputStrings);
    }

    /**
     * @param entries The entries to output
     */
    protected void addResultEntries(
            Collection<Map.Entry<String, Object>> entries) {
        addResultEntries(entries, false);
    }

    /**
     * Output a single entry.
     * @param key The key
     * @param value The value
     * @param hasMore Whether there are more to come.
     */
    protected void addResultEntry(String key, Object value, boolean hasMore) {
        addResultEntry(new Pair<String, Object>(key, value), hasMore);
    }

    /**
     * Output a single entry.
     * @param entry The entry
     * @param hasMore Whether there are more to come.
     */
    protected void addResultEntry(Map.Entry<String, Object> entry, boolean hasMore) {
        addResultEntries(Collections.singleton(entry), hasMore);
    }

    /**
     * Output a bunch of entries.
     * @param entries The entries
     * @param hasMore Whether there are more of them to come.
     */
    @SuppressWarnings("rawtypes")
    protected void addResultEntries(
            Collection<Map.Entry<String, Object>> entries, boolean hasMore) {
        List<String> outputStrings = new ArrayList<String>();
        for (Map.Entry<String, Object> entry: entries) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String valStr = null;

            if (value == null) {
                valStr = "null";
            } else if (value instanceof Map) {
                valStr = new JSONObject((Map) value).toString();
            } else if (value instanceof List) {
                valStr = new JSONArray((List) value).toString();
            } else if (value instanceof CharSequence) {
                valStr = String.format("\"%s\"",
                        StringEscapeUtils.escapeJava(String.valueOf(value)));
            } else if (value instanceof Number || value instanceof Boolean) {
                valStr = String.valueOf(value);
            }
            outputStrings.add(String.format("\"%s\":%s", key, valStr));
        }
        if (hasMore) {
            outputStrings.add("");
        }
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
