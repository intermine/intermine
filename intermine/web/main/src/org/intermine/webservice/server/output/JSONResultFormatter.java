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
import java.util.Map;

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
    public String formatHeader(Map<String, String> attributes) {
        String superResults = super.formatHeader(attributes);
        StringBuilder sb = new StringBuilder(superResults);
        for (String key : attributes.keySet()) {
            if (KEY_CALLBACK.equals(key)) { continue; }
            sb.append("'" + key + "':");
            String attr = attributes.get(key);
            boolean shouldQuoteAttr = attrNeedsQuotes(attr);
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
    public String formatFooter() {
        StringBuilder sb = new StringBuilder();
        sb.append("],");
        Date now = Calendar.getInstance().getTime();
        DateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm::ss");
        String executionTime = dateFormatter.format(now);
        sb.append("'" + JSONResultFormatter.KEY_TIME + "':'" + executionTime + "'");
        sb.append(super.formatFooter());
        return sb.toString();
    }
}
