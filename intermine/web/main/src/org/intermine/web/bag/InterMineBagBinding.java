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

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.SAXParser;
import org.intermine.util.TypeUtil;

import java.io.Reader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;

/**
 * Parse InterMineBags in XML format
 *
 * @author Mark Woodbridge
 */
public class InterMineBagBinding
{
    private static final Logger LOG = Logger.getLogger(InterMineBagBinding.class);

    /**
     * Convert an InterMine bag to XML
     * @param bag the InterMineBag
     * @param bagName the name of the bag
     * @return the corresponding XML String
     */
    public String marshal(InterMineBag bag, String bagName) {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            marshal(bag, bagName, writer);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

        return sw.toString();
    }

    /**
     * Convert a InterMineBag to XML and write XML to given writer.
     *
     * @param bag the InterMineBag
     * @param bagName the bag name to serialise
     * @param writer the XMLStreamWriter to write to
     */
    public static void marshal(InterMineBag bag, String bagName, XMLStreamWriter writer) {
        try {
            writer.writeStartElement("bag");
            writer.writeAttribute("name", bagName);

            for (Iterator j = bag.iterator(); j.hasNext();) {
                Object o = j.next();
                String type, value;
                if (bag instanceof InterMineIdBag) {
                    type = InterMineObject.class.getName();
                    value = o.toString();
                } else {
                    type = o.getClass().getName();
                    value = TypeUtil.objectToString(o);
                }
                writer.writeEmptyElement("element");
                writer.writeAttribute("type", type);
                writer.writeAttribute("value", value);
            }
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parse saved queries from a Reader
     * @param reader the saved bags
     * @param os ObjectStore used to resolve object ids
     * @return a Map from bag name to InterMineBag
     */
    public static Map unmarshal(final Reader reader, final ObjectStore os,
    		IdUpgrader idUpgrader) {
        final Map bags = new LinkedHashMap();
        try {
            SAXParser.parse(new InputSource(reader),
                            new InterMineBagHandler(os, bags, new HashMap(), idUpgrader));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return bags;
    }
}
