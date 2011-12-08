package org.intermine.webservice.server;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * @author Jakub Kulaviak
 **/
public class XMLResultHandler extends DefaultHandler
{
    private List<String> currentResult;
    private StringBuffer currentResultItem;
    private List<List<String>> results;
    private Attributes rootAttributes;

    public Attributes getRootAttributes() {
        return rootAttributes;
    }

    @Override
    public void startElement(String uri, String localName, String name,
            Attributes attributes) throws SAXException {
        if ("ResultSet".equals(name)) {
            results = new ArrayList<List<String>>();
            rootAttributes = attributes;
        } else if ("Result".equals(name)) {
            currentResult = new ArrayList<String>();
        } else if ("i".equals(name)) {
            currentResultItem = new StringBuffer();
        }
    }

    @Override
    public void endElement(String uri, String localName, String name)
            throws SAXException {
        if ("Result".equals(name)) {
            results.add(currentResult);
        } else if ("i".equals(name)) {
            currentResult.add(currentResultItem.toString().trim());
            currentResultItem = null;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (currentResultItem != null) {
            currentResultItem.append(ch, start, length);
        }
    }

    public List<List<String>> getResults() {
        return results;
    }
}
