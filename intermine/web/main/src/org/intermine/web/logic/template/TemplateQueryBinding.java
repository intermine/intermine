package org.intermine.web.logic.template;

/*
 * Copyright (C) 2002-2009 FlyMine
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

import org.intermine.pathquery.PathQueryBinding;
import org.intermine.util.SAXParser;
import org.intermine.web.logic.bag.InterMineBag;
import org.xml.sax.InputSource;

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
     * @param version the version number of the XML format
     */
    public static void marshal(TemplateQuery template, XMLStreamWriter writer, int version) {
        try {
            writer.writeStartElement("template");
            writer.writeAttribute("name", template.getName());
            writer.writeAttribute("title", template.getTitle());
            if (template.getDescription() == null) {
                writer.writeAttribute("longDescription", "");
            } else {
                writer.writeAttribute("longDescription", template.getDescription());
            }
            if (template.getComment() == null) {
                writer.writeAttribute("comment", "");
            } else {
                writer.writeAttribute("comment", template.getComment());
            }

            PathQueryBinding.marshal(template, template.getName(), template.getModel()
                    .getName(), writer, version);
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
     * @param version the version number of the XML format
     */
    public String marshal(TemplateQuery template, int version) {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            marshal(template, writer, version);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

        return sw.toString();
    }

    /**
     * Parse TemplateQuerys from XML
     * @param reader the saved templates
     * @param savedBags Map from bag name to bag
     * @param version the version of the xml format, an attribute of the ProfileManager
     * @return a Map from template name to TemplateQuery
     */
    public Map<String, TemplateQuery> unmarshal(Reader reader, 
            Map<String, InterMineBag> savedBags, int version) {
        Map<String, TemplateQuery> templates = new LinkedHashMap();

        try {
            SAXParser.parse(new InputSource(reader), new TemplateQueryHandler(templates,
                    savedBags, version));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return templates;
    }
}
