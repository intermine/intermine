package org.intermine.template.xml;

/*
 * Copyright (C) 2002-2012 FlyMine
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

import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.template.SwitchOffAbility;
import org.intermine.template.TemplateQuery;
import org.intermine.util.SAXParser;
import org.xml.sax.InputSource;

/**
 * Convert PathQueries to and from XML
 *
 * @author Mark Woodbridge
 */
public class TemplateQueryBinding extends PathQueryBinding
{
    /** A single instance of this class */
    public static final TemplateQueryBinding INSTANCE = new TemplateQueryBinding();

    /**
     * Convert a TemplateQuery to XML and write XML to given writer.
     *
     * @param template the TemplateQuery
     * @param writer the XMLStreamWriter to write to
     * @param version the version number of the XML format
     */
    public static void marshal(TemplateQuery template, XMLStreamWriter writer, int version) {
        INSTANCE.doMarshal(template, writer, version);
    }

    /**
     * Convert a TemplateQuery to XML and write XML to given writer.
     *
     * @param template the TemplateQuery
     * @param writer the XMLStreamWriter to write to
     * @param version the version number of the XML format
     */
    public void doMarshal(TemplateQuery template, XMLStreamWriter writer, int version) {
        if (template == null) {
            throw new NullPointerException("template must not be null");
        }
        if (writer == null) {
            throw new NullPointerException("writer must not be null");
        }
        try {
            writer.writeCharacters("\n");
            writer.writeStartElement("template");
            writer.writeAttribute("name", template.getName());
            if (template.getTitle() != null) {
                writer.writeAttribute("title", template.getTitle());
            }
            if (template.getComment() == null) {
                writer.writeAttribute("comment", "");
            } else {
                writer.writeAttribute("comment", template.getComment());
            }

            doMarshal(template, template.getName(), template.getModel()
                    .getName(), writer, version);
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doAdditionalConstraintStuff(PathQuery query, PathConstraint constraint,
            XMLStreamWriter writer) throws XMLStreamException {
        TemplateQuery template = (TemplateQuery) query;
        if (template.isEditable(constraint)) {
            writer.writeAttribute("editable", "true");
        } else {
            writer.writeAttribute("editable", "false");
        }
        String description = template.getConstraintDescription(constraint);
        if (description != null) {
            writer.writeAttribute("description", description);
        }
        SwitchOffAbility switchOffAbility = template.getSwitchOffAbility(constraint);
        if (SwitchOffAbility.ON.equals(switchOffAbility)) {
            writer.writeAttribute("switchable", "on");
        } else if (SwitchOffAbility.OFF.equals(switchOffAbility)) {
            writer.writeAttribute("switchable", "off");
        }
    }

    /**
     * Convert a TemplateQuery to XML
     *
     * @param template the TemplateQuery
     * @return the corresponding XML String
     * @param version the version number of the XML format
     */
    public static String marshal(TemplateQuery template, int version) {
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
     * Parse TemplateQueries from XML.
     *
     * @param reader the saved templates
     * @param version the version of the xml format, an attribute of the ProfileManager
     * @return a Map from template name to TemplateQuery
     */
    public static Map<String, TemplateQuery> unmarshalTemplates(Reader reader, int version) {
        Map<String, TemplateQuery> templates = new LinkedHashMap<String, TemplateQuery>();
        try {
            SAXParser.parse(new InputSource(reader), new TemplateQueryHandler(templates, version));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return templates;
    }
}
