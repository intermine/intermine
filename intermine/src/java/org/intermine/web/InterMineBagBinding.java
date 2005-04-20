package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;

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
public class InterMineBagBinding
{
    /**
     * Convert an InterMine bag to XML
     * @param bag the InterMineBag
     * @param bagName the name of the bag
     * @return the corresponding XML String
     */
    public String marshal(InterMineBag bag, String bagName) {
        StringBuffer sb = new StringBuffer();
        sb.append("<bag name='" + bagName + "'>");
        for (Iterator j = bag.iterator(); j.hasNext();) {
            Object o = j.next();
            String type, value;
            if (o instanceof InterMineObject) {
                type = InterMineObject.class.getName();
                value = ((InterMineObject) o).getId().toString();
            } else {
                type = o.getClass().getName();
                value = TypeUtil.objectToString(o);
            }
            sb.append("<element type='" + type + "' value='" + value + "'/>");
        }
        sb.append("</bag>");
        return sb.toString();
    }

    /**
     * Parse saved queries from a Reader
     * @param reader the saved bags
     * @param os ObjectStore used to resolve object ids
     * @return a Map from bag name to InterMineBag
     */
    public Map unmarshal(Reader reader, ObjectStore os) {
        Map bags = new LinkedHashMap();
        try {
            SAXParser.parse(new InputSource(reader), new BagHandler(os, bags));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return bags;
    }

    /**
     * Extension of DefaultHandler to handle metadata file
     */
    class BagHandler extends DefaultHandler
    {
        ObjectStore os;
        Map bags;
        String bagName;
        InterMineBag bag;
        
        /**
         * Constructor
         * @param os ObjectStore used to resolve object ids
         * @param bags Map from bag name to InterMineBag
         */
        public BagHandler(ObjectStore os, Map bags) {
            this.os = os;
            this.bags = bags;
        }

        /**
         * @see DefaultHandler#startElement
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
            try {
                if (qName.equals("bag")) {
                    bagName = attrs.getValue("name");
                    bag = new InterMineBag(os);
                }
                if (qName.equals("element")) {
                    String type = attrs.getValue("type");
                    String value = attrs.getValue("value");
                    if (type.equals(InterMineObject.class.getName())) {
                        //bag.add(os.getObjectById(Integer.valueOf(value)));
                        bag.addId(Integer.valueOf(value));
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
                bags.put(bagName, bag);
            }
        }
    }
}
