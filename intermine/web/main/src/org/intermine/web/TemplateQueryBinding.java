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
 * Convert PathQueries to and from XML
 *
 * @author Mark Woodbridge
 */
public class TemplateQueryBinding
{
    /**
     * Convert a TemplateQuery to XML and write XML to given writer.
     *
     * @param template the TemplateQuery
     * @param writer the XMLStreamWriter to write to
     */
    public static void marshal(TemplateQuery template, XMLStreamWriter writer) {
        try {
            writer.writeStartElement("template");
            writer.writeAttribute("name", template.getName());
            writer.writeAttribute("description", template.getDescription());
            writer.writeAttribute("category", template.getCategory());
            writer.writeAttribute("important", "" + template.isImportant());
            writer.writeAttribute("keywords", template.getKeywords());
            
            PathQueryBinding.marshal(template.getQuery(),
                                     template.getName(),
                                     template.getQuery().getModel().getName(),
                                     writer);
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Convert a TemplateQuery to XML
     *
     * @param template the TemplateQuery
     * @return the corresponding XML String
     */
    public String marshal(TemplateQuery template) {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            marshal(template, writer);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        
        return sw.toString();
    }

    /**
     * Parse TemplateQuerys from XML
     * @param reader the saved templates
     * @return a Map from template name to TemplateQuery
     */
    public Map unmarshal(Reader reader) {
        Map templates = new LinkedHashMap();
        try {
            SAXParser.parse(new InputSource(reader), new TemplateQueryHandler(templates));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return templates;
    }

    /**
     * Extension of PathQueryHandler to handle parsing TemplateQueries
     */
    static class TemplateQueryHandler extends PathQueryHandler
    {
        Map templates;
        String templateName;
        String templateDesc;
        String templateCat;
        String keywords;
        boolean important;

        /**
         * Constructor
         * @param templates Map from template name to TemplateQuery
         */
        public TemplateQueryHandler(Map templates) {
            super(new HashMap());
            this.templates = templates;
        }

        /**
         * @see DefaultHandler#startElement
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
            if (qName.equals("template")) {
                templateName = attrs.getValue("name");
                templateDesc = attrs.getValue("description");
                templateCat = attrs.getValue("category");
                keywords = attrs.getValue("keywords");
                if (keywords == null) {
                    keywords = "";
                }
                important = Boolean.valueOf(attrs.getValue("important")).booleanValue();
            }
            super.startElement(uri, localName, qName, attrs);
        }
        
        /**
         * @see DefaultHandler#endElement
         */
        public void endElement(String uri, String localName, String qName) {
            super.endElement(uri, localName, qName);
            if (qName.equals("template")) {
                templates.put(templateName, new TemplateQuery(templateName,
                                                              templateDesc,
                                                              templateCat,
                                                              query,
                                                              important,
                                                              keywords));
            }
        }
    }
}
