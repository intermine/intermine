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
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;
import java.io.StringWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.util.SAXParser;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.metadata.Model;

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
    public String marshal(PathQuery query, String queryName, String modelName) {
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
    public void marshal(PathQuery query, String queryName, String modelName, 
                                                                XMLStreamWriter writer) {
        try {
            writer.writeStartElement("query");
            writer.writeAttribute("name", queryName);
            writer.writeAttribute("model", modelName);
            writer.writeAttribute("view", StringUtil.join(query.getView(), " "));
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
                    }
                    if (c.getIdentifier() != null) {
                        writer.writeAttribute("identifier", "" + c.getIdentifier());
                    }
                    if (c.isEditable()) {
                        writer.writeAttribute("editable", "true");
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
     */
    public Map unmarshal(Reader reader) {
        Map queries = new LinkedHashMap();
        try {
            SAXParser.parse(new InputSource(reader), new QueryHandler(queries));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return queries;
    }

    /**
     * Extension of DefaultHandler to handle parsing PathQueries
     */
    static class QueryHandler extends DefaultHandler
    {
        Map queries;
        String queryName;
        PathQuery query;
        PathNode node;

        /**
         * Constructor
         * @param queries Map from query name to PathQuery
         */
        public QueryHandler(Map queries) {
            this.queries = queries;
        }

        /**
         * @see DefaultHandler#startElement
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
            if (qName.equals("query")) {
                queryName = attrs.getValue("name");
                Model model;
                try {
                    model = Model.getInstanceByName(attrs.getValue("model"));
                } catch (Exception e) {
                    throw new SAXException(e);
                }
                query = new PathQuery(model);
                if (attrs.getValue("view") != null) {
                    query.setView(StringUtil.tokenize(attrs.getValue("view")));
                }
            }
            if (qName.equals("node")) {
                node = query.addNode(attrs.getValue("path"));
                if (attrs.getValue("type") != null) {
                    node.setType(attrs.getValue("type"));
                }
            }
            if (qName.equals("constraint")) {
                int opIndex = toStrings(ConstraintOp.getValues()).indexOf(attrs.getValue("op"));
                ConstraintOp constraintOp = ConstraintOp.getOpForIndex(new Integer(opIndex));
                Object constraintValue;
                // If we know that the query is not valid, don't resolve the type of
                // the node as it may not resolve correctly
                if (node.isReference() || BagConstraint.VALID_OPS.contains(constraintOp)
                        || !query.isValid()) {
                    constraintValue = attrs.getValue("value");
                } else {
                    constraintValue = TypeUtil.stringToObject(MainHelper.getClass(node.getType()),
                                                              attrs.getValue("value"));
                }
                String editable = attrs.getValue("editable");
                boolean editableFlag = false;
                if (editable != null && editable.equals("true")) {
                    editableFlag = true;
                }
                String description = attrs.getValue("description");
                String identifier = attrs.getValue("identifier");
                node.getConstraints().add(new Constraint(constraintOp, constraintValue,
                                                         editableFlag, description, identifier));
            }
        }
        
        /**
         * @see DefaultHandler#endElement
         */
        public void endElement(String uri, String localName, String qName) {
            if (qName.equals("query")) {
                queries.put(queryName, query);
            }
        }
        
        /**
         * Convert a List of Objects to a List of Strings using toString
         * @param list the Object List
         * @return the String list
         */
        protected List toStrings(List list) {
            List strings = new ArrayList();
            for (Iterator i = list.iterator(); i.hasNext();) {
                strings.add(i.next().toString());
            }
            return strings;
        }
    }
}
