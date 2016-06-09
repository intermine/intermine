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

    /**
     * Set the current tag we are working within.
     * @param tag The tag.
     */
    protected void pushTag(String tag) {
        openElements.push(tag);
    }

    /**
     * Say that we are finished with the current tag.
     * @return What that tag was.
     */
    protected String popTag() {
        return openElements.pop();
    }

    /** @return the root element of the document **/
    protected String getRootElement() {
        return "ResultSet";
    }

    /** @return the name of the tag for each row **/
    protected String getRowElement() {
        return "Result";
    }

    /** @return the name of the tag for each item **/
    protected String getItemElement() {
        return "i";
    }

    /** @return the name of the tag when rendering an error **/
    protected String getErrorElement() {
        return "error";
    }

    /** @return the name of the tag for showing a message **/
    protected String getMessageElement() {
        return "message";
    }

    /** @return the name of the tag for rendering the cause of an error **/
    protected String getCauseElement() {
        return "cause";
    }

    /** @return an XML processing instruction, if any **/
    protected String getProcessingInstruction() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    }

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

    /**
     * Serialise the headers to the current string builder.
     * @param attributes The headers
     * @param sb The string builder.
     */
    protected void handleHeaderAttributes(Map<String, Object> attributes,
            StringBuilder sb) {
        if (attributes != null) {
            for (String key : attributes.keySet()) {
                if (attributes.get(key) instanceof Map) {
                    @SuppressWarnings("rawtypes")
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

    /**
     * @param attr an attribute to escape.
     * @return The escaped representation.
     */
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

    /**
     * Add an element with some content.
     * @param sb The current string builder.
     * @param tag The tag to add.
     * @param contents The content to add.
     */
    protected void addElement(StringBuilder sb, String tag, String contents) {
        sb.append("<" + tag + ">");
        openElements.push(tag);
        sb.append(escapeElementContent(contents));
        sb.append("</" + openElements.pop() + ">");
    }

    /**
     * Escape the content of an element.
     * @param contents The content to escape.
     * @return The escaped representation.
     */
    protected String escapeElementContent(String contents) {
        return StringEscapeUtils.escapeXml(contents);
    }

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
