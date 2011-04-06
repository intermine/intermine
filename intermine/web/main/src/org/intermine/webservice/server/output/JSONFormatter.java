package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2011 FlyMine
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

import org.apache.commons.lang.StringEscapeUtils;

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

    /**
     * The key for the callback
     */
    public static final String KEY_CALLBACK = "callback";

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
        return sb.toString();
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
        StringBuffer buffer = new StringBuffer(iter.next());
        while (iter.hasNext()) {
            buffer.append(",").append(iter.next());
        }
        return buffer.toString();
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
        StringBuilder sb = new StringBuilder();
        sb.append(",\"wasSuccessful\":");
        if (errorCode != Output.SC_OK) {
        	sb.append("false,\"error\":\"" + StringEscapeUtils.escapeJavaScript(errorMessage) + "\"");
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
