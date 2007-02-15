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
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.intermine.util.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Convert SavedQuerys to and from XML
 *
 * @author Thomas Riley
 */
public class SavedQueryBinding
{
    /**
     * Convert a SavedQuery to XML and write XML to given writer.
     *
     * @param query the SavedQuery
     * @param writer the XMLStreamWriter to write to
     */
    public static void marshal(SavedQuery query, XMLStreamWriter writer) {
        try {
            writer.writeStartElement("saved-query");
            
            writer.writeAttribute("name", query.getName());
            if (query.getDateCreated() != null) {
                writer.writeAttribute("date-created", "" + query.getDateCreated().getTime());
            }
            PathQueryBinding.marshal(query.getPathQuery(),
                                     query.getName(),
                                     query.getPathQuery().getModel().getName(),
                                     writer);
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Convert a TemplateQuery to XML
     *
     * @param query the SavedQuery
     * @return the corresponding XML String
     */
    public static String marshal(SavedQuery query) {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            marshal(query, writer);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        
        return sw.toString();
    }

    /**
     * Parse TemplateQuerys from XML
     * @param reader the saved templates
     * @return a Map from template name to TemplateQuery
     * @param savedBags Map from bag name to bag
     */
    public static Map unmarshal(Reader reader, Map savedBags, Map classKeys) {
        Map queries = new LinkedHashMap();
        try {
            SAXParser.parse(new InputSource(reader), new SavedQueryHandler(queries, savedBags, classKeys));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return queries;
    }

    /**
     * Extension of PathQueryHandler to handle parsing SavedQuerys
     */
    static class SavedQueryHandler extends PathQueryHandler
    {
        Map queries;
        Date dateExecuted;
        Date dateCreated;
        String queryName;

        /**
         * Constructor
         * @param queries Map from saved query name to SavedQuery
         * @param savedBags Map from bag name to bag
         */
        public SavedQueryHandler(Map queries, Map savedBags, Map classKeys) {
            super(new HashMap(), savedBags, classKeys);
            this.queries = queries;
        }

        /**
         * @see DefaultHandler#startElement
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
            if (qName.equals("saved-query")) {
                queryName = attrs.getValue("name");
                if (attrs.getValue("date-created") != null) {
                    dateCreated = new Date(Long.parseLong(attrs.getValue("date-created")));
                }
            }
            super.startElement(uri, localName, qName, attrs);
        }
        
        /**
         * @see DefaultHandler#endElement
         */
        public void endElement(String uri, String localName, String qName) {
            super.endElement(uri, localName, qName);
            if (qName.equals("saved-query")) {
                queries.put(queryName, new SavedQuery(queryName, dateCreated, query));
                dateCreated = null;
                dateExecuted = null;
            }
        }
    }
}
