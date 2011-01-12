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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Alexis Kalderimis
 *
 */
public abstract class JSONFormatter extends Formatter
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
     * The key for the callback
     */
    public static final String KEY_CALLBACK = "callback";

    private boolean hasCallback = false;

    /**
     * Starts the result set object, and sets the attributes given into it
     * @see org.intermine.webservice.server.output.Formatter#formatHeader(java.util.Map)
     * @return the header
     * @param attributes the attributes passed in from the containing output
     */
    @Override
    public String formatHeader(Map<String, String> attributes) {
        StringBuilder sb = new StringBuilder();
        if (attributes.get(KEY_CALLBACK) != null) {
            hasCallback = true;
            sb.append(attributes.get(KEY_CALLBACK)).append("(");
        }
        sb.append("{");
        for (String key : attributes.keySet()) {
            if (KEY_CALLBACK.equals(key)) { continue; }
            sb.append("'" + key + "':");
            String attr = attributes.get(key);
            boolean shouldQuoteAttr = !attr.startsWith("{") && !attr.startsWith("[");
            if (shouldQuoteAttr) {
                sb.append("'");
            }
            sb.append(attr);
            if (shouldQuoteAttr) {
                sb.append("'");
            }
            sb.append(",");
        }
        sb.append("'results':[");
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
     * Closes the remaining open brackets (the root class array, and the
     * overall result set object), and adds an execution time property.
     * @see org.intermine.webservice.server.output.Formatter#formatFooter()
     * @return The formatted footer string.
     */
    @Override
    public String formatFooter() {
        StringBuilder sb = new StringBuilder();
        sb.append("],");
        Date now = Calendar.getInstance().getTime();
        DateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm::ss");
        String executionTime = dateFormatter.format(now);
        sb.append("'" + JSONFormatter.KEY_TIME + "':'" + executionTime + "'");
        sb.append("}");
        if (hasCallback) {
            sb.append(");");
        }
        return sb.toString();
    }
}
