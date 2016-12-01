package org.intermine.webservice.client.results;

/*
 * Copyright (C) 2002-2016 FlyMine
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
 * Class representing a row of data as a map. This class parses JSON row data in the
 * 8+ webservice API version format (ie. a simple array of values such as
 * <code>[1, "foo", null, true, 1.23]</code>).
 *
 * @author Alex Kalderimis
 *
 */
public class JsonRowMap extends AbstractMap<String, Object>
{
    private final JSONArray data;
    private final List<String> views;

    /**
     * Construct a result-row with a JSONArray as its backing
     * data store.
     * @param ja The source of the data.
     * @param views The column names.
     */
    public JsonRowMap(JSONArray ja, List<String> views) {
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
    public JsonRowMap(String json, List<String> views) {
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
            try {
                Object val = data.get(i);
                entries.add(new RowEntry(key, val));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
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
            return data.get(i);
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
