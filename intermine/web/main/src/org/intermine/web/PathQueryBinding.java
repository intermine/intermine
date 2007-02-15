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

import java.util.LinkedHashMap;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.StringWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.InputSource;

import org.intermine.util.SAXParser;
import org.intermine.util.StringUtil;

/**
 * Convert PathQueries to and from XML
 *
 * @author Mark Woodbridge
 */
public class PathQueryBinding
{
    /**
     * Convert a PathQuery to XML
     * @param query the PathQuery
     * @param queryName the name of the query
     * @param modelName the model name
     * @return the corresponding XML String
     */
    public static String marshal(PathQuery query, String queryName, String modelName) {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            marshal(query, queryName, modelName, writer);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

        return sw.toString();
    }
    
    /**
     * Marshal to an XMLStreamWriter.
     *
     * @param query the PathQuery
     * @param queryName the name of the query
     * @param modelName the model name
     * @param writer the xml stream writer to write to
     */
    public static void marshal(PathQuery query, String queryName, String modelName, 
                               XMLStreamWriter writer) {
        try {
            writer.writeStartElement("query");
            writer.writeAttribute("name", queryName);
            writer.writeAttribute("model", modelName);
            writer.writeAttribute("view", StringUtil.join(query.getView(), " "));
            if (query.getConstraintLogic() != null) {
                writer.writeAttribute("constraintLogic", query.getConstraintLogic());
            }
            for (Iterator j = query.getAlternativeViews().entrySet().iterator(); j.hasNext();) {
                Map.Entry entry = (Map.Entry) j.next();
                writer.writeStartElement("alternative-view");
                writer.writeAttribute("name", entry.getKey().toString());
                writer.writeAttribute("view", StringUtil.join((List) entry.getValue(), " "));
                writer.writeEndElement();
            }
            for (Iterator j = query.getNodes().values().iterator(); j.hasNext();) {
                PathNode node = (PathNode) j.next();
                writer.writeStartElement("node");
                writer.writeAttribute("path", node.getPath());
                if (node.getType() != null) {
                    writer.writeAttribute("type", node.getType());
                }
                for (Iterator k = node.getConstraints().iterator(); k.hasNext();) {
                    Constraint c = (Constraint) k.next();
                    writer.writeStartElement("constraint");
                    writer.writeAttribute("op", "" + c.getOp());
                    writer.writeAttribute("value", "" + c.getValue());
                    if (c.getDescription() != null) {
                        writer.writeAttribute("description", "" + c.getDescription());
                    } else {
                        writer.writeAttribute("description", "");
                    }
                    if (c.getIdentifier() != null) {
                        writer.writeAttribute("identifier", "" + c.getIdentifier());
                    } else {
                        writer.writeAttribute("identifier", "");                        
                    }
                    if (c.isEditable()) {
                        writer.writeAttribute("editable", "true");
                    }
                    if (c.getCode() != null) {
                        writer.writeAttribute("code", c.getCode());
                    } else {
                        writer.writeAttribute("code", "");                        
                    }
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parse PathQueries from XML
     * @param reader the saved queries
     * @return a Map from query name to PathQuery
     * @param savedBags Map from bag name to bag
     */
    public static Map unmarshal(Reader reader, Map savedBags) {
        Map queries = new LinkedHashMap();
        try {
            SAXParser.parse(new InputSource(reader), new PathQueryHandler(queries, savedBags));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return queries;
    }

    
}

