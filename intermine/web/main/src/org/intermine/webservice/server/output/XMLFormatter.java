package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2014 FlyMine
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

    protected void pushTag(String tag) {
        openElements.push(tag);
    }

    protected String popTag() {
        return openElements.pop();
    }

    protected String getRootElement() {
        return "ResultSet";
    }

    protected String getRowElement() {
        return "Result";
    }

    protected String getItemElement() {
        return "i";
    }
    
    protected String getErrorElement() {
        return "error";
    }

    protected String getMessageElement() {
        return "message";
    }

    protected String getCauseElement() {
        return "cause";
    }

    protected String getProcessingInstruction() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    }

    /** {@inheritDoc}} **/
    @SuppressWarnings("rawtypes")
    @Override
    public String formatHeader(Map<String, Object> attributes) {
        StringBuilder sb = new  StringBuilder();
        sb.append(getProcessingInstruction());
        String elem = getRootElement();
        openElements.push(elem);
        sb.append("\n<" + elem + " ");
        handleHeaderAttributes(attributes, sb);
        return sb.toString();
    }

    protected void handleHeaderAttributes(Map<String, Object> attributes,
            StringBuilder sb) {
        if (attributes != null) {
            for (String key : attributes.keySet()) {
                if (attributes.get(key) instanceof Map) {
                    Map obj = (Map) attributes.get(key);
                    for (Object subK: obj.keySet()) {
                        sb.append(subK + "=\"" + escapeAttribute(obj.get(subK)) + "\" ");
                    }
                } else {
                    sb.append(key + "=\"" + escapeAttribute(attributes.get(key)) + "\" ");
                }
            }
        }
        sb.append(">");
    }

    protected String escapeAttribute(Object attr) {
        return StringEscapeUtils.escapeXml(String.valueOf(attr));
    }

    /** {@inheritDoc}} **/
    @Override
    public String formatResult(List<String> resultRow) {
        StringBuilder sb = new StringBuilder();
        String elem = getRowElement();
        sb.append("<" + elem + ">");
        openElements.push(elem);
        for (String s : resultRow) {
            addElement(sb, getItemElement(), s);
        }
        sb.append("</" + elem + ">");
        openElements.pop();
        return sb.toString();
    }

    protected void addElement(StringBuilder sb, String tag, String contents) {
        sb.append("<" + tag + ">");
        openElements.push(tag);
        sb.append(escapeElementContent(contents));
        sb.append("</" + openElements.pop() + ">");
    }

    protected String escapeElementContent(String contents) {
        return StringEscapeUtils.escapeXml(contents);
    }

    /** {@inheritDoc}} **/
    @Override
    public String formatFooter(String errorMessage, int errorCode) {

        StringBuilder sb = new StringBuilder();
        // Close all the open tags, except for ResultSet
        if (openElements.isEmpty()) {
            sb.append("<" + getRootElement() + ">"); //return a result set, even on failure
        }
        while (!openElements.isEmpty()) {
            String openTag = openElements.pop();
            if (getRootElement().equals(openTag)) {
                continue;
            }
            sb.append("</" + openTag + ">");
        }
        if (errorCode != Output.SC_OK) {
            sb.append("<" + getErrorElement() + ">");
            addElement(sb, getMessageElement(), StatusDictionary.getDescription(errorCode));
            addElement(sb, getCauseElement(), errorMessage);
            sb.append("</" + getErrorElement() + ">");
        }
        sb.append("</" + getRootElement() + ">");
        return sb.toString();
    }
}
