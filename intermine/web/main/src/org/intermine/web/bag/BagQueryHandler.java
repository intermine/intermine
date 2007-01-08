package org.intermine.web.bag;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.metadata.Model;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class BagQueryHandler extends DefaultHandler
{
    private List queryList;
    private Map bagQueries = new HashMap();
    private String type, message, queryString;
    private Boolean matchesAreIssues;
    private Model model;
    private StringBuffer sb;

    public BagQueryHandler(Model model) {
        super();
        this.model = model;
    }

    public Map getBagQueries() {
        return bagQueries;
    }

    /**
     * @see DefaultHandler#startElement
     */
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
        if (qName.equals("bag-type")) {
            type = attrs.getValue("type");
            if (!model.hasClassDescriptor(model.getPackageName() + "." + type)) {
                throw new SAXException("Type was not found in model: " + type);
            }
            queryList = new ArrayList();
            if (bagQueries.containsKey(type)) {
                throw new SAXException("Duplicate query lists defined for type: " + type);
            }

        }
        if (qName.equals("query")) {
            message = attrs.getValue("message");
            matchesAreIssues = Boolean.valueOf(attrs.getValue("matchesAreIssues"));
            sb = new StringBuffer();
        }
    }

    /**
     * @see DefaultHandler#endElement
     */
    public void characters(char[] ch, int start, int length)
        throws SAXException {
        // DefaultHandler may call this method more than once for a single
        // attribute content -> hold text & create attribute in endElement
        while (length > 0) {
            boolean whitespace = false;
            switch(ch[start]) {
            case ' ':
            case '\r':
            case '\n':
            case '\t':
                whitespace = true;
                break;
            default:
                break;
            }
            if (!whitespace) {
                break;
            }
            ++start;
            --length;
        }

        if (length > 0) {
            sb.append(ch, start, length);
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("query")) {
            queryString = sb.toString();
            if (queryString != null && message != null && matchesAreIssues != null) {
                BagQuery bq = new BagQuery(queryString, message, model.getPackageName(),
                                           matchesAreIssues.booleanValue());
                queryList.add(bq);
            }
            queryString = null;
            matchesAreIssues = null;
            message = null;
        }
        if (qName.equals("bag-type")) {
            bagQueries.put(type, queryList);
        }
    }
}

