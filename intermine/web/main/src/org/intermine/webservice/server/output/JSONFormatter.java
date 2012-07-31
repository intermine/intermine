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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

/**
 * Base class for formatters that process JSON data. The
 * following basic structure is assumed: The result set
 * is a single JavaScript object literal, with an optional
 * callback provided by the user, and some result
 * status meta-data to round it off (see formatFooter).

 * @author Alex Kalderimis
 *
 */
public class JSONFormatter extends Formatter {

    private boolean hasCallback = false;
    private String outro = "";
    private boolean shouldQuote = false;
    private boolean hasPrintedSomething = false;
    private boolean isExpectingPrimitive = false;
    private String header = null;

    /**
     * The key for the callback
     */
    public static final String KEY_CALLBACK = "callback";
    public static final String KEY_INTRO = "intro";
    public static final String KEY_OUTRO = "outro";
    public static final String KEY_QUOTE = "should_quote";
    public static final String KEY_HEADER_OBJS = "headerObjs";
    /**
     * A map of optional key value pairs that should go in the header of the object.
     * The map should be of type Map<String, String> - woe betide you if you violate
     * this stern imprecation.
     */
    public static final String KEY_KV_PAIRS = "key_value_pairs";

    /**
     * Constructor
     */
    public JSONFormatter() {
        //empty constructor
    }

    /**
     * Add the opening brace, and a call-back if any
     * @see org.intermine.webservice.server.output.Formatter#formatHeader(java.util.Map)
     * @return the header
     * @param attributes the attributes passed in from the containing output
     */
    @Override
    public String formatHeader(Map<String, Object> attributes) {
        StringBuilder sb = new StringBuilder();
        if (attributes != null && attributes.get(KEY_CALLBACK) != null) {
            hasCallback = true;
            sb.append(attributes.get(KEY_CALLBACK)).append("(");
        }
        sb.append("{");
        if (attributes != null && attributes.containsKey(KEY_KV_PAIRS)) {
            @SuppressWarnings("unchecked")
            Map<String, String> kvPairs = (Map<String, String>) attributes.get(KEY_KV_PAIRS);
            for (Entry<String, String> pair: kvPairs.entrySet()) {
                sb.append("\""
                        + StringEscapeUtils.escapeJava(pair.getKey())
                        + "\":"
                        + quoteValue(StringEscapeUtils.escapeJava(pair.getValue()))
                        + ",");
            }
        }
        if (attributes != null && attributes.containsKey(KEY_HEADER_OBJS)) {
            @SuppressWarnings("rawtypes")
            Map<String, Map> headerObjs = (Map<String, Map>) attributes.get(KEY_HEADER_OBJS);
            for (@SuppressWarnings("rawtypes") Entry<String, Map> pair: headerObjs.entrySet()) {
                sb.append("\"" + StringEscapeUtils.escapeJava(pair.getKey()) + "\":");
                JSONObject ho = new JSONObject(pair.getValue());
                sb.append(ho.toString());
                sb.append(",");
            }
        }
        if (attributes != null && attributes.get(KEY_INTRO) != null) {
            sb.append(attributes.get(KEY_INTRO));
            if (attributes.containsKey(KEY_OUTRO)) {
                outro = attributes.get(KEY_OUTRO).toString();
            } else {
                isExpectingPrimitive = true;
            }
        }
        if (attributes != null && attributes.containsKey(KEY_QUOTE)) {
            shouldQuote = (Boolean) attributes.get(KEY_QUOTE);
        }
        header = sb.toString();
        return sb.toString();
    }

    private String quoteValue(String val) {
        if (val == null) {
            return "null";
        }
        if ("null".equals(val) || "true".equals(val) || "false".equals(val)
                || StringUtils.isNumeric(val)) {
            return val;
        }
        return "\"" + val + "\"";
    }

    /**
     * In normal cases a list with a single JSON string item is expected.
     * But just in case, this formatter will simply join any strings
     * it gets given, delimiting with a comma. It is the responsibility of
     * whoever is feeding me these lines to add any necessary commas between
     * them.
     * @see org.intermine.webservice.server.output.Formatter#formatResult(java.util.List)
     * @param resultRow the row as a list of strings
     * @return A formatted result line, or the empty string if the row is empty
     */
    @Override
    public String formatResult(List<String> resultRow) {
        if (resultRow.isEmpty()) { return ""; }
        Iterator<String> iter = resultRow.iterator();
        String first = iter.next();
        if (shouldQuote && !"".equals(first)) {
            first = quoteValue(first);
        }

        StringBuffer buffer = new StringBuffer(first == null ? "null" : first);
        while (iter.hasNext()) {
            String next = iter.next();
            if (shouldQuote && !"".equals(next)) {
                next = quoteValue(next);
            }
            buffer.append(",").append(next);
        }
        declarePrinted();
        return buffer.toString();
    }

    protected void declarePrinted() {
        hasPrintedSomething = true;
    }


    /**
     * Put on the final brace, and close the call-back bracket if needed.
     * If an error has been reported, format that nicely,
     * escaping problematic JavaScript characters appropriately
     * in the message portion.
     *
     * @param errorMessage The message reporting the problem encountered
     *      in processing this request, or null if there was none
     * @param errorCode The status code for the request (200 on success)
     *
     * @see org.intermine.webservice.server.output.Formatter#formatFooter()
     * @return The formatted footer string.
     */
    @Override
    public String formatFooter(String errorMessage, int errorCode) {
        StringBuilder sb = new StringBuilder(outro);
        if (!hasPrintedSomething && isExpectingPrimitive) {
            sb.append("null");
        }
        if ((header != null) && !hasPrintedSomething
                && (header.endsWith("{") || header.endsWith(","))) {
            // That's fine
        } else {
            sb.append(',');
        }

        sb.append("\"wasSuccessful\":");
        if (errorCode != Output.SC_OK) {
            sb.append("false,\"error\":\"" + StringEscapeUtils.escapeJava(errorMessage) + "\"");
        } else {
            sb.append("true,\"error\":null");
        }
        sb.append(",\"statusCode\":" + errorCode);
        sb.append("}");
        if (hasCallback) {
            sb.append(");");
        }
        return sb.toString();
    }

}
