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

import java.util.List;
import java.util.Map;

/**
 * A formatter that presents query results as HTML.
 * @author Alex Kalderimis
 *
 */
public class HTMLTableFormatter extends XMLFormatter
{

    /** The key for setting headers **/
    public static final String KEY_COLUMN_HEADERS = "headers";

    @Override
    protected String getRootElement() {
        return "table";
    }

    @Override
    protected String getRowElement() {
        return "tr";
    }

    @Override
    protected String getItemElement() {
        return "td";
    }

    @Override
    protected String getErrorElement() {
        return "div";
    }

    @Override
    protected String getMessageElement() {
        return "h3";
    }

    @Override
    protected String getCauseElement() {
        return "p";
    }

    @Override
    protected String getProcessingInstruction() {
        return "";
    }

    @Override
    protected void handleHeaderAttributes(Map<String, Object> attributes, StringBuilder sb) {
        sb.append(">");
        if (attributes != null && attributes.containsKey(KEY_COLUMN_HEADERS)) {
            @SuppressWarnings("unchecked")
            List<String> headers = (List<String>) attributes.get(KEY_COLUMN_HEADERS);
            sb.append("<thead><tr>");
            for (String header: headers) {
                addElement(sb, "th", header);
            }
            sb.append("</tr></thead>");
        }
    }
}
