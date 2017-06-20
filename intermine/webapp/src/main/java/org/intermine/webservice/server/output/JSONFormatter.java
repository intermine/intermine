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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import static org.apache.commons.lang.StringEscapeUtils.escapeJava;

/**
 * Base class for formatters that process JSON data. The
 * following basic structure is assumed: The result set
 * is a single JavaScript object literal, with an optional
 * callback provided by the user, and some result
 * status meta-data to round it off (see formatFooter).

 * @author Alex Kalderimis
 *
 */
public class JSONFormatter extends Formatter
{

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
    /** the key for the result object. **/
    public static final String KEY_INTRO = "intro";
    /** The way to wrap up after the result. **/
    public static final String KEY_OUTRO = "outro";
    /** Whether we should quote the result item. **/
    public static final String KEY_QUOTE = "should_quote";
    /** Things that should go into the header **/
    public static final String KEY_HEADER_OBJS = "headerObjs";
    /**
     * A map of optional key value pairs that should go in the header of the object.
     * The map should be of type Map<String, String> - woe betide you if you violate
     * this stern imprecation.
     */
    public static final String KEY_KV_PAIRS = "key_value_pairs";
    /**
     * The key for the execution time
     */
    public static final String KEY_TIME = "executionTime";

    /** keys which you aren't allowed to set. **/
    public static final Set<String> RESERVED_KEYS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(
                KEY_CALLBACK,
                KEY_INTRO,
                KEY_OUTRO,
                KEY_QUOTE,
                KEY_HEADER_OBJS,
                KEY_KV_PAIRS,
                KEY_TIME)));

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
        if (attributes == null) {
            attributes = new HashMap<String, Object>();
        }
        StringBuilder sb = new StringBuilder();
        Object callback = attributes.get(KEY_CALLBACK);
        if (callback != null) {
            hasCallback = true;
            sb.append(callback).append("(");
        }
        sb.append("{");
        formatAttributes(attributes, sb);
        header = sb.toString();
        return header;
    }

    /**
     * Format the header attributes.
     * @param attributes The header attributes.
     * @param sb Where to format them to.
     */
    protected void formatAttributes(Map<String, Object> attributes, StringBuilder sb) {
        if (attributes == null) {
            return;
        }
        if (sb == null) {
            throw new NullPointerException("sb must not be null");
        }

        if (attributes.containsKey(KEY_KV_PAIRS)) {
            @SuppressWarnings("unchecked")
            Map<String, String> kvPairs = (Map<String, String>) attributes.get(KEY_KV_PAIRS);
            for (Entry<String, String> pair: kvPairs.entrySet()) {
                sb.append("\"")
                    .append(escapeJava(pair.getKey()))
                    .append("\":")
                    .append(quoteValue(escapeJava(pair.getValue())))
                    .append(",");
            }
        }
        // Add any complex objects as json-objects to the headers.
        if (attributes.containsKey(KEY_HEADER_OBJS)) {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Map<String, Map> headerObjs = (Map<String, Map>) attributes.get(KEY_HEADER_OBJS);
            for (@SuppressWarnings("rawtypes") Entry<String, Map> pair: headerObjs.entrySet()) {
                sb.append("\"")
                    .append(escapeJava(pair.getKey()))
                    .append("\":")
                    .append(new JSONObject(pair.getValue()))
                    .append(",");
            }
        }
        if (attributes.get(KEY_INTRO) != null) {
            sb.append(attributes.get(KEY_INTRO));
            if (attributes.containsKey(KEY_OUTRO)) {
                outro = attributes.get(KEY_OUTRO).toString();
            } else {
                isExpectingPrimitive = true;
            }
        }
        if (attributes.containsKey(KEY_QUOTE)) {
            shouldQuote = (Boolean) attributes.get(KEY_QUOTE);
        }
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
        if (resultRow.isEmpty()) {
            return "";
        }
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

    /** Signal that we have started printing results and that it isn't safe to print headers. **/
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

        Date now = Calendar.getInstance().getTime();
        DateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm::ss");
        String executionTime = dateFormatter.format(now);
        sb.append("\"" + KEY_TIME + "\":\"" + executionTime + "\",");

        sb.append("\"wasSuccessful\":");
        if (errorCode >= 400) {
            sb.append("false,\"error\":\"" + escapeJava(errorMessage) + "\"");
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
