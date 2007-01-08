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

import java.io.Reader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.SAXParser;
import org.xml.sax.InputSource;

/**
 * Parse InterMineIdBags in XML format
 *
 * @author Mark Woodbridge
 */
public class InterMineBagBinding
{
    private static final Logger LOG = Logger.getLogger(InterMineBagBinding.class);

    /**
     * Convert an InterMine bag to XML
     * @param bag the InterMineIdBag
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
     * Convert a InterMineIdBag to XML and write XML to given writer.
     *
     * @param bag the InterMineIdBag
     * @param bagName the bag name to serialise
     * @param writer the XMLStreamWriter to write to
     */
    public static void marshal(InterMineBag bag, String bagName, XMLStreamWriter writer) {
        try {
            writer.writeStartElement("bag");
            writer.writeAttribute("name", bagName);
            writer.writeAttribute("type", bag.getType());

            if (bag.width() == 1) {
                for (Iterator j = bag.iterator(); j.hasNext();) {
//                    writer.writeStartElement("row");
                    writeOneBagElement((BagElement) j.next(), writer);
//                    writer.writeEndElement();
                }
            } else {
                List listOfLists = bag.asListOfLists();
                Iterator columnIter = listOfLists.iterator();
                while (columnIter.hasNext()) {
                    List row = (List) columnIter.next();
                    Iterator rowIter = row.iterator();
//                    writer.writeStartElement("row");
                    while (rowIter.hasNext()) {
                        BagElement o = (BagElement) rowIter.next();
                        writeOneBagElement(o, writer);
                    }
//                    writer.writeEndElement();
                }
            }
            writer.writeEndElement();

        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeOneBagElement(BagElement bagElement,
                                           XMLStreamWriter writer) throws XMLStreamException {
        //        String type = thisObject.getClass().getName();
        writer.writeEmptyElement("bagElement");
        writer.writeAttribute("type", bagElement.getType());
        writer.writeAttribute("id", bagElement.getId().toString());
    }
        
    /**
     * Parse saved queries from a Reader
     * @param reader the saved bags
     * @param os ObjectStore used to resolve object ids
     * @param idUpgrader bag object id upgrader
     * @param userId an Integer
     * @return a Map from bag name to InterMineIdBag
     */
    public static Map unmarshal(final Reader reader, final ObjectStore uos, final ObjectStore os,
                                IdUpgrader idUpgrader,
            Integer userId) {
        final Map bags = new LinkedHashMap();
        try {
            SAXParser.parse(new InputSource(reader),
                    new InterMineBagHandler(uos, os, bags, userId));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return bags;
    }
}
