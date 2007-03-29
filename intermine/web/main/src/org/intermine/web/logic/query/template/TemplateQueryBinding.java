package org.intermine.web.logic.template;

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
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.intermine.util.SAXParser;
import org.intermine.web.logic.query.PathQueryBinding;


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
     */
    public static void marshal(TemplateQuery template, XMLStreamWriter writer) {
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
            writer.writeAttribute("important", "" + template.isImportant());
            // writer.writeAttribute("keywords", template.getKeywords());

            PathQueryBinding.marshal(template, template.getName(), template.getModel()
                    .getName(), writer);
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
     * @param savedBags Map from bag name to bag
     * @param classKeys class key fields for the model
     */
    public Map unmarshal(Reader reader, Map savedBags, Map classKeys) {
        Map templates = new LinkedHashMap();
        try {
            SAXParser.parse(new InputSource(reader), 
                            new TemplateQueryHandler(templates, savedBags, classKeys));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return templates;
    }
}
