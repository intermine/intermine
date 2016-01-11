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
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.intermine.api.profile.SavedQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.util.SAXParser;
import org.xml.sax.InputSource;

/**
 * Convert SavedQuerys to and from XML
 *
 * @author Thomas Riley
 */
public final class SavedQueryBinding
{
    private SavedQueryBinding() {
    }

    /**
     * Convert a SavedQuery to XML and write XML to given writer.
     *
     * @param query the SavedQuery
     * @param writer the XMLStreamWriter to write to
     * @param version the version number of the XML format, an attribute of the ProfileManager
     */
    public static void marshal(SavedQuery query, XMLStreamWriter writer, int version) {
        try {
            writer.writeCharacters("\n");
            writer.writeStartElement("saved-query");

            writer.writeAttribute("name", query.getName());
            if (query.getDateCreated() != null) {
                writer.writeAttribute("date-created", "" + query.getDateCreated().getTime());
            }
            PathQueryBinding.marshal(query.getPathQuery(),
                                     query.getName(),
                                     query.getPathQuery().getModel().getName(),
                                     writer, version);
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convert a TemplateQuery to XML
     *
     * @param query the SavedQuery
     * @param version the version number of the XML format, an attribute of the ProfileManager
     * @return the corresponding XML String
     */
    public static String marshal(SavedQuery query, int version) {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            marshal(query, writer, version);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

        return sw.toString();
    }

    /**
     * Parse TemplateQuerys from XML
     * @param reader the saved queries
     * @param savedBags Map from bag name to bag
     * @param version the version of the XML, an attribute on the profile manager
     * @return a Map from query name to SavedQuery
     */
    public static Map<String, SavedQuery> unmarshal(
            Reader reader, @SuppressWarnings("rawtypes") Map savedBags, int version) {
        Map<String, SavedQuery> queries = new LinkedHashMap<String, SavedQuery>();
        try {
            SAXParser.parse(new InputSource(reader), new SavedQueryHandler(queries, savedBags,
                        version));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return queries;
    }
}
