package org.intermine.web.logic.bag;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.io.StringWriter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.util.SAXParser;
import org.xml.sax.InputSource;

/**
 * Parse InterMineIdBags in XML format
 *
 * @author Mark Woodbridge
 */
public class InterMineBagBinding
{
    /**
     * Convert an InterMine bag to XML
     * @param bag the InterMineIdBag
     * @return the corresponding XML String
     */
    public String marshal(InterMineBag bag) {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            marshal(bag, writer);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

        return sw.toString();
    }

    /**
     * Convert a InterMineIdBag to XML and write XML to given writer.
     *
     * @param bag the InterMineIdBag
     * @param writer the XMLStreamWriter to write to
     */
    public static void marshal(InterMineBag bag, XMLStreamWriter writer) {
        try {
            writer.writeStartElement("bag");
            writer.writeAttribute("name", bag.getName());
            writer.writeAttribute("type", bag.getType());
            if (bag.getDescription() != null) {
                writer.writeAttribute("description", bag.getDescription());
            }

            List<Integer> ids = (List<Integer>) bag.getContentsAsIds();
            for (Integer id : ids) {
                writer.writeEmptyElement("bagElement");
                writer.writeAttribute("id", id.toString());
            }
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parse saved queries from a Reader
     * @param reader the saved bags
     * @param uosw UserProfile ObjectStoreWriter
     * @param osw ObjectStoreWriter used to resolve object ids and write to ObjectStoreBags
     * @param idUpgrader bag object id upgrader
     * @param userId an Integer
     * @return a Map from bag name to InterMineIdBag
     */
    public static Map unmarshal(final Reader reader, final ObjectStoreWriter uosw,
            final ObjectStoreWriter osw, IdUpgrader idUpgrader, Integer userId) {
        final Map bags = new LinkedHashMap();
        try {
            SAXParser.parse(new InputSource(reader), new InterMineBagHandler(uosw, osw, bags,
                        userId, new HashMap(), idUpgrader.ERROR_UPGRADER));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return bags;
    }
}
