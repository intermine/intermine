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

import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.lang.StringEscapeUtils;
import org.intermine.webservice.server.StatusDictionary;

/**
 * Formats data to XML format.
 * @author Jakub Kulaviak
 **/
public class XMLFormatter extends Formatter
{

    private final Stack<String> openElements = new Stack<String>();

    /** {@inheritDoc}} **/
    @Override
    public String formatHeader(Map<String, Object> attributes) {
        StringBuilder sb = new  StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        openElements.push("ResultSet");
        sb.append("<ResultSet ");
        if (attributes != null) {
            for (String key : attributes.keySet()) {
                sb.append(key + "=\"" + attributes.get(key) + "\" ");
            }
        }
        sb.append(">");
        return sb.toString();
    }

    /** {@inheritDoc}} **/
    @Override
    public String formatResult(List<String> resultRow) {
        StringBuilder sb = new StringBuilder();
        sb.append("<Result>");
        openElements.push("Result");
        for (String s : resultRow) {
            addElement(sb, "i", s);
        }
        sb.append("</Result>");
        openElements.pop();
        return sb.toString();
    }

    private void addElement(StringBuilder sb, String tag, String contents) {
        sb.append("<" + tag + ">");
        openElements.push(tag);
        sb.append(StringEscapeUtils.escapeXml(contents));
        sb.append("</" + openElements.pop() + ">");
    }

    /** {@inheritDoc}} **/
    @Override
    public String formatFooter(String errorMessage, int errorCode) {

        StringBuilder sb = new StringBuilder();
        // Close all the open tags, except for ResultSet
        if (openElements.isEmpty()) {
            sb.append("<ResultSet>"); //return a result set, even on failure
        }
        while (!openElements.isEmpty()) {
            String openTag = openElements.pop();
            if ("ResultSet".equals(openTag)) {continue;}
            sb.append("</" + openTag + ">");
        }
        if (errorCode != Output.SC_OK) {
            sb.append("<error><message>");
            sb.append(StatusDictionary.getDescription(errorCode));
            sb.append("</message>");
            sb.append("<cause>").append(errorMessage);
            sb.append("</cause></error>");
        }
        sb.append("</ResultSet>");
        return sb.toString();
    }
}
