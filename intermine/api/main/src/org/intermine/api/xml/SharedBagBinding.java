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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.util.SAXParser;
import org.xml.sax.InputSource;

/**
 * Parse the shared bags in XML format
 *
 * @author dbutano
 */
public class SharedBagBinding
{
    /**
     * Convert the shared bags to XML
     * @param profile user's profile
     * @return the corresponding XML String
     */
    public String marshal(Profile profile) {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            marshal(profile, writer);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

        return sw.toString();
    }

    /**
     * Convert the bags shared to the profile given in input and write XML to given writer.
     *
     * @param profile the profile which has been shared some bags
     * @param writer the XMLStreamWriter to write to
     */
    public static void marshal(Profile profile, XMLStreamWriter writer) {
        try {
            writer.writeStartElement("shared-bags");
            Map<String, InterMineBag> sharedBags = profile.getSharedBags();
            for (Map.Entry<String, InterMineBag> entry : sharedBags.entrySet()) {
                writer.writeCharacters("\n");
                writer.writeStartElement("shared-bag");
                writer.writeAttribute("name", entry.getKey());
                Date dateCreated = entry.getValue().getDateCreated();
                if (dateCreated != null) {
                    writer.writeAttribute("date-created", "" + dateCreated.getTime());
                }
                writer.writeEndElement();
            }
            writer.writeCharacters("\n");
            writer.writeEndElement();
            writer.writeCharacters("\n");
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parse saved queries from a Reader.
     *
     * @param reader the saved bags
     * @param userId an Integer
     * @return list of queries
     */
    public static List<Map<String, String>> unmarshal(final Reader reader, Integer userId) {
        final List<Map<String, String>> sharedBags = new ArrayList<Map<String, String>>();
        try {
            SAXParser.parse(new InputSource(reader), new SharedBagHandler(sharedBags));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return sharedBags;
    }
}
