package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.LinkedHashMap;
import java.io.Reader;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.SAXParser;
import org.intermine.util.TypeUtil;

/**
 * Parse InterMineBags in XML format
 *
 * @author Mark Woodbridge
 */
public class SavedBagParser
{
    protected ObjectStore os;
    protected Map savedBags = new LinkedHashMap();

    /**
     * Construct a SavedBagParser
     * @param os the ObjectStore used to reload objects by id
     */
    public SavedBagParser(ObjectStore os) {
        this.os = os;
    }

    /**
     * Parse saved queries from a Reader
     * @param reader the saved bags
     * @return a Map from bag name to InterMineBag
     */
    public Map process(Reader reader) {
        try {
            SAXParser.parse(new InputSource(reader), new BagHandler());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return savedBags;
    }

    /**
     * Extension of DefaultHandler to handle metadata file
     */
    class BagHandler extends DefaultHandler
    {
        String bagName;
        InterMineBag bag;

        /**
         * @see DefaultHandler#startElement
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
            try {
                if (qName.equals("bag")) {
                    bagName = attrs.getValue("name");
                    bag = new InterMineBag();
                }
                if (qName.equals("element")) {
                    String type = attrs.getValue("type");
                    String value = attrs.getValue("value");
                    if (type.equals(InterMineObject.class.getName())) {
                        bag.add(os.getObjectById(Integer.valueOf(value)));
                    } else {
                        bag.add(TypeUtil.stringToObject(Class.forName(type), value));
                    }
                }
            } catch (Exception e) {
                throw new SAXException(e);
            }
        }

        /**
         * @see DefaultHandler#endElement
         */
        public void endElement(String uri, String localName, String qName) {
            if (qName.equals("bag")) {
                savedBags.put(bagName, bag);
            }
        }
    }
}
