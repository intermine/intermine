package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.apache.commons.lang.StringEscapeUtils;
import org.intermine.webservice.server.exceptions.InternalErrorException;

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
     * The key for the execution time
     */
    public static final String KEY_TIME = "executionTime";


    /**
     * Starts the result set object, and sets the attributes given into it
     * @see org.intermine.webservice.server.output.Formatter#formatHeader(java.util.Map)
     * @return the header
     * @param attributes the attributes passed in from the containing output
     */
    @Override
    public String formatHeader(Map<String, Object> attributes) {
        String superResults = super.formatHeader(attributes);
        StringBuilder sb = new StringBuilder(superResults);
        if (attributes != null) {
            for (String key : attributes.keySet()) {
                if (KEY_CALLBACK.equals(key)) { continue; }
                sb.append("\"" + key + "\":");
                // Format lists as arrays
                if (attributes.get(key) instanceof List) {
                        JSONArray ja = new JSONArray((List) attributes.get(key));
                        sb.append(ja.toString());
                } else {
                    // Format as attribute
                    String attr = (attributes.get(key) == null) ? null : attributes.get(key).toString();
                    sb.append(quote(StringEscapeUtils.escapeJavaScript(attr)));
                }
                sb.append(",");
            }
        }
        sb.append("\"results\":[");
        return sb.toString();
    }

    private String quote(Object o) {
        if (o == null) {
            return "null";
        }
        String sth = o.toString();
        if (attrNeedsQuotes(sth)) {
            return "\"" + sth + "\"";
        } else {
            return sth;
        }
    }

    private boolean attrNeedsQuotes(String attr) {
        if (attr == null) {
            return false;
        }
        return !attr.startsWith("{") // it is a javascript object
                && !attr.startsWith("[") // it is a javascript array
                && !attr.matches("[-+]?\\d+(\\.\\d+)?"); // it is numeric
    }

    /**
     * Closes the remaining open brackets (the root class array, and the
     * overall result set object), and adds an execution time property.
     * @see org.intermine.webservice.server.output.Formatter#formatFooter()
     * @return The formatted footer string.
     */
    @Override
    public String formatFooter(String errorMessage, int errorCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("],");
        Date now = Calendar.getInstance().getTime();
        DateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm::ss");
        String executionTime = dateFormatter.format(now);
        sb.append("\"" + JSONResultFormatter.KEY_TIME + "\":\"" + executionTime + "\"");
        declarePrinted();
        sb.append(super.formatFooter(errorMessage, errorCode));
        return sb.toString();
    }
}
