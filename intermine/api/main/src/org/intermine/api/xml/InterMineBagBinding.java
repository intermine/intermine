package org.intermine.api.xml;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.intermine.api.profile.BagValue;
import org.intermine.api.profile.InterMineBag;
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
            writer.writeCharacters("\n");
            writer.writeStartElement("bag");
            writer.writeAttribute("name", bag.getName());
            writer.writeAttribute("type", bag.getType());
            if (bag.getDateCreated() != null) {
                writer.writeAttribute("date-created", "" + bag.getDateCreated().getTime());
            }
            if (bag.getDescription() != null) {
                writer.writeAttribute("description", bag.getDescription());
            }
            writer.writeAttribute("status", bag.getState());
            List<BagValue> keyFieldValues = bag.getContents();
            for (BagValue bagValues : keyFieldValues) {
                writer.writeEmptyElement("bagValue");
                writer.writeAttribute("value", bagValues.getValue());
                writer.writeAttribute("extra", bagValues.getExtra());
            }
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parse saved queries from a Reader
     * @param reader the saved bags
     * @param uosw UserProfile ObjectStoreWriter
     * @param osw ObjectStoreWriter used to resolve object id's and write to ObjectStoreBags
     * @param userId an Integer
     * @return map of queries read from XML
     */
    public static Map unmarshal(final Reader reader, final ObjectStoreWriter uosw,
            final ObjectStoreWriter osw, Integer userId) {
        final Map bags = new LinkedHashMap();
        final Map bagsValues = new LinkedHashMap();
        final Map invalidBags = new LinkedHashMap();
        try {
            SAXParser.parse(new InputSource(reader), new InterMineBagHandler(uosw, osw, bags,
                    invalidBags, bagsValues, userId));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return bags;
    }
}
