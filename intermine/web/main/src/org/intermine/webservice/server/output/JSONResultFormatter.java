package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Alexis Kalderimis
 *
 */
public abstract class JSONResultFormatter extends JSONFormatter
{
    /**
     * The key for the views
     */
    public static final String KEY_VIEWS = "views";
    /**
     * The key for the root class
     */
    public static final String KEY_ROOT_CLASS = "rootClass";
    /**
     * The key for the model name
     */
    public static final String KEY_MODEL_NAME = "modelName";

    /**
     * This differs from the JSONFormatter implementation in that it will output
     * a representation of all keys in the attributes map.
     * @param attributes the attributes passed in from the containing output
     * @param sb The buffer containing the header so far.
     */
    @Override
    protected void formatAttributes(Map<String, Object> attributes, StringBuilder sb) {
        if (sb         == null) {
            throw new NullPointerException("sb must not be null");
        }
        if (attributes == null) {
            attributes = new HashMap<String, Object>();
        } else {
            attributes = new LinkedHashMap<String, Object>(attributes);
        }
        if (!attributes.containsKey(KEY_INTRO)) {
            attributes.put(KEY_INTRO, "\"results\":[");
        }
        if (!attributes.containsKey(KEY_OUTRO)) {
            attributes.put(KEY_OUTRO, "]");
        }

        // Handle all non-reserved keys in the attribute map.
        for (String key : attributes.keySet()) {
            if (RESERVED_KEYS.contains(key)) {
                continue;
            }
            Object val = attributes.get(key);
            sb.append("\"" + key + "\":");

            if (val instanceof List) {
                // Format lists as arrays
                @SuppressWarnings("rawtypes")
                JSONArray ja = new JSONArray((List) val);
                sb.append(ja.toString());
            } else if (val instanceof Map) {
                // Format maps as objects
                @SuppressWarnings("rawtypes")
                JSONObject jo = new JSONObject((Map) val);
                sb.append(jo.toString());
            } else {
                // Format as value (string, number, boolean, null)
                sb.append(quote(val));
            }
            sb.append(",");
        }
        super.formatAttributes(attributes, sb);
    }

    private String quote(Object o) {
        String sth = StringEscapeUtils.escapeJava(String.valueOf(o).trim());
        if (attrNeedsQuotes(o, sth)) {
            return "\"" + sth + "\"";
        } else {
            return sth;
        }
    }

    private boolean attrNeedsQuotes(Object attr, String asString) {
        if (attr == null) {
            return false;
        }
        if (attr instanceof Number) {
            return false;
        }
        // Very ugly, but a js literal may have been placed here as a string.
        // it would be nice to remove this.
        return !asString.startsWith("{") // it is a javascript object
                && !asString.startsWith("[") // it is a javascript array
                && !asString.matches("[-+]?\\d+(\\.\\d+)?"); // it is numeric
    }
}
