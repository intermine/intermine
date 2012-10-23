package org.intermine.webservice.client.results;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * A class that represents a row of a result set as a map from column names
 * to values.
 * @author Alex Kalderimis
 *
 */
public class ResultRowMap extends AbstractMap<String, Object>
{

    private final JSONArray data;
    private final List<String> views;

    /**
     * Construct a result-row with a JSONArray as its backing
     * data store.
     * @param ja The source of the data.
     * @param views The column names.
     */
    public ResultRowMap(JSONArray ja, List<String> views) {
        this.data = ja;
        this.views = views;
        verify();
    }

    /**
     * Construct a result-row with a String as its backing
     * data store.
     * @param json The source of the data.
     * @param views The column names.
     */
    public ResultRowMap(String json, List<String> views) {
        try {
            this.data = new JSONArray(json);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
        this.views = views;
        verify();
    }

    private void verify() {
        if (data == null || views == null || data.length() != views.size()) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        Set<Entry<String, Object>> entries = new LinkedHashSet<Entry<String, Object>>();
        for (int i = 0; i < data.length(); i++) {
            String key = views.get(i);
            Object val;
            try {
                if (data.getJSONObject(i).isNull("value")) {
                    val = null;
                } else {
                    val = data.getJSONObject(i).get("value");
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            entries.add(new RowEntry(key, val));
        }
        return entries;
    }

    private String getRoot() {
        return views.get(0).split("\\.")[0];
    }

    @Override
    public Object get(Object key) {
        if (!views.contains(key)) {
            String root = getRoot();
            key = root + "." + key;
            if (!views.contains(key)) {
                return null;
            }
        }
        int i = views.indexOf(key);
        try {
            Object val;
            if (data.getJSONObject(i).isNull("value")) {
                val = null;
            } else {
                val = data.getJSONObject(i).get("value");
            }
            return val;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private static class RowEntry extends SimpleImmutableEntry<String, Object>
    {

        /**
         * Generated serial id.
         */
        private static final long serialVersionUID = -8770402781429939453L;

        public RowEntry(String k, Object v) {
            super(k, v);
        }

    }

}
