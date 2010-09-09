package org.intermine.web.logic.template;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.intermine.api.profile.InterMineBag;
import org.intermine.api.template.TemplateQuery;
import org.intermine.api.xml.TemplateQueryBinding;

/**
 * Static helper routines related to templates.
 *
 * @author Thomas Riley
 * @author Richard Smith
 */
public class TemplateHelper
{
    /**
     * Given a Map of TemplateQuerys (mapping from template name to TemplateQuery)
     * return a string containing each template seriaised as XML. The root element
     * will be a <code>template-queries</code> element.
     *
     * @param templates  map from template name to TemplateQuery
     * @param version the version number of the XML format
     * @return  all template queries serialised as XML
     * @see  TemplateQuery
     */
    public static String templateMapToXml(Map<String, TemplateQuery> templates, int version) {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            writer.writeStartElement("template-queries");
            for (TemplateQuery template : templates.values()) {
                TemplateQueryBinding.marshal(template, writer, version);
            }
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        return sw.toString();
    }

    /**
     * Parse templates in XML format and return a map from template name to
     * TemplateQuery.
     *
     * @param xml         the template queries in xml format
     * @param savedBags   Map from bag name to bag
     * @param version the version of the xml format, an attribute on ProfileManager
     * @return            Map from template name to TemplateQuery
     * @throws Exception  when a parse exception occurs (wrapped in a RuntimeException)
     */
    public static Map<String, TemplateQuery> xmlToTemplateMap(String xml,
            Map<String, InterMineBag> savedBags, int version) throws Exception {
        Reader templateQueriesReader = new StringReader(xml);
        return new TemplateQueryBinding().unmarshal(templateQueriesReader, savedBags, version);
    }
}
