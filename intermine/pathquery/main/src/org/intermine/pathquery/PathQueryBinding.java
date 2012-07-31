package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2011 FlyMine
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
import org.intermine.util.StringUtil;
import org.xml.sax.InputSource;

/**
 * Convert PathQueries to and from XML
 *
 * @author Matthew Wakeling
 */
public class PathQueryBinding
{
    /** A single instance of this class */
    public static final PathQueryBinding INSTANCE = new PathQueryBinding();

    /**
     * Convert a PathQuery to XML.
     *
     * @param query the PathQuery
     * @param queryName the name of the query
     * @param modelName the model name
     * @param version the version number of the xml format, an attribute of the ProfileManager
     * @return the corresponding XML String
     */
    public static String marshal(PathQuery query, String queryName, String modelName, int version) {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            marshal(query, queryName, modelName, writer, version);
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
     * @param version the version number of the xml format, an attribute of the ProfileManager
     */
    public static void marshal(PathQuery query, String queryName, String modelName,
            XMLStreamWriter writer, int version) {
        INSTANCE.doMarshal(query, queryName, modelName, writer, version);
    }

    /**
     * Marshal to an XMLStreamWriter.
     *
     * @param query the PathQuery
     * @param queryName the name of the query
     * @param modelName the model name
     * @param writer the xml stream writer to write to
     * @param version the version number of the xml format, an attribute of the ProfileManager
     */
    public void doMarshal(PathQuery query, String queryName, String modelName,
            XMLStreamWriter writer, int version) {
        try {
            writer.writeStartElement("query");
            writer.writeAttribute("name", queryName);
            writer.writeAttribute("model", modelName);
            writer.writeAttribute("view", StringUtil.join(query.getView(), " "));
            if (query.getDescription() != null) {
                writer.writeAttribute("longDescription", query.getDescription());
            } else {
                writer.writeAttribute("longDescription", "");
            }
            StringBuilder sort = new StringBuilder();
            boolean needComma = false;
            for (OrderElement oe : query.getOrderBy()) {
                if (needComma) {
                    sort.append(" ");
                }
                needComma = true;
                sort.append(oe.getOrderPath() + (oe.getDirection().equals(OrderDirection
                                .ASC) ? " asc" : " desc"));
            }
            String sortString = sort.toString();
            if (!"".equals(sortString)) {
                writer.writeAttribute("sortOrder", sortString);
            }
            String logic = query.getConstraintLogic();
            boolean hasMultipleConstraints = false;
            if ((logic != null) && (logic.length() > 1)) {
                writer.writeAttribute("constraintLogic", query.getConstraintLogic());
                hasMultipleConstraints = true;
            }
            marshalPathQueryJoinStyle(query, writer);
            marshalPathQueryDescriptions(query, writer);
            marshalPathQueryConstraints(query, writer, hasMultipleConstraints);
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create XML for the join style in a PathQuery.
     */
    private void marshalPathQueryJoinStyle(PathQuery query, XMLStreamWriter writer)
        throws XMLStreamException {
        for (Map.Entry<String, OuterJoinStatus> entry : query.getOuterJoinStatus().entrySet()) {
            writer.writeEmptyElement("join");
            writer.writeAttribute("path", entry.getKey());
            writer.writeAttribute("style", entry.getValue().toString());
            //writer.writeEndElement();
        }
    }

    /**
     * Create XML for the path descriptions in a PathQuery.
     */
    private void marshalPathQueryDescriptions(PathQuery query, XMLStreamWriter writer)
        throws XMLStreamException {
        for (Map.Entry<String, String> entry : query.getDescriptions().entrySet()) {
            String path = entry.getKey();
            String description = entry.getValue();
            writer.writeEmptyElement("pathDescription");
            writer.writeAttribute("pathString", path);
            writer.writeAttribute("description", description);
        }
    }

    /**
     * Create XML for the constraints in a PathQuery.
     */
    private void marshalPathQueryConstraints(PathQuery query, XMLStreamWriter writer,
            boolean hasMultipleConstraints)
        throws XMLStreamException {
        for (Map.Entry<PathConstraint, String> constraint : query.getConstraints().entrySet()) {
            boolean emptyElement = true;
            if (constraint.getKey() instanceof PathConstraintMultiValue) {
                emptyElement = false;
            }

            if (emptyElement) {
                writer.writeEmptyElement("constraint");
            } else {
                writer.writeStartElement("constraint");
            }
            writer.writeAttribute("path", constraint.getKey().getPath());
            if ((constraint.getValue() != null) && (hasMultipleConstraints)) {
                writer.writeAttribute("code", constraint.getValue());
            }
            doAdditionalConstraintStuff(query, constraint.getKey(), writer);
            if (constraint.getKey() instanceof PathConstraintAttribute) {
                writer.writeAttribute("op", "" + constraint.getKey().getOp());
                String outputValue = ((PathConstraintAttribute) constraint.getKey()).getValue();
                writer.writeAttribute("value", "" + outputValue);
            } else if (constraint.getKey() instanceof PathConstraintNull) {
                writer.writeAttribute("op", "" + constraint.getKey().getOp());
            } else if (constraint.getKey() instanceof PathConstraintSubclass) {
                writer.writeAttribute("type", ((PathConstraintSubclass) constraint.getKey())
                        .getType());
            } else if (constraint.getKey() instanceof PathConstraintBag) {
                writer.writeAttribute("op", "" + constraint.getKey().getOp());
                writer.writeAttribute("value", ((PathConstraintBag) constraint.getKey()).getBag());
            } else if (constraint.getKey() instanceof PathConstraintIds) {
                writer.writeAttribute("op", "" + constraint.getKey().getOp());
                StringBuilder sb = new StringBuilder();
                boolean needComma = false;
                for (Integer id : ((PathConstraintIds) constraint.getKey()).getIds()) {
                    if (needComma) {
                        sb.append(", ");
                    }
                    needComma = true;
                    sb.append("" + id);
                }
                writer.writeAttribute("ids", sb.toString());
            } else if (constraint.getKey() instanceof PathConstraintMultiValue) {
            	// Includes PathConstraintRange, which is serialised in the exact same manner.
                writer.writeAttribute("op", "" + constraint.getKey().getOp());
                
                for (String value : ((PathConstraintMultiValue) constraint.getKey()).getValues()) {
                    if (!value.equals(value.trim())) {
                        throw new XMLStreamException("Value in MultiValue starts or ends with "
                                + "whitespace - this query cannot be represented in XML");
                    }
                    writer.writeStartElement("value");
                    writer.writeCharacters(value);
                    writer.writeEndElement();
                }
            } else if (constraint.getKey() instanceof PathConstraintLoop) {
                writer.writeAttribute("op", "" + constraint.getKey().getOp());
                writer.writeAttribute("loopPath", ((PathConstraintLoop) constraint.getKey())
                        .getLoopPath());
            } else if (constraint.getKey() instanceof PathConstraintLookup) {
                writer.writeAttribute("op", "" + constraint.getKey().getOp());
                writer.writeAttribute("value", ((PathConstraintLookup) constraint.getKey())
                        .getValue());
                String extraValue = ((PathConstraintLookup) constraint.getKey()).getExtraValue();
                if (extraValue != null) {
                    writer.writeAttribute("extraValue", extraValue);
                }
            } else {
                throw new IllegalStateException("Unrecognised constraint type "
                        + constraint.getKey().getClass().getName());
            }
            if (!emptyElement) {
                writer.writeEndElement();
            }
        }
    }

    /**
     * Adds any extra information for a constraint, like say Template extras.
     *
     * @param query the query
     * @param constraint the constraint being processed
     * @param writer a writer to add data to
     * @throws XMLStreamException if something goes wrong
     */
    public void doAdditionalConstraintStuff(PathQuery query,
            PathConstraint constraint,
            XMLStreamWriter writer) throws XMLStreamException {
    }

    /**
     * Parse PathQueries from XML
     * @param reader the saved queries
     * @param version the version of the xml, an attribute on the profile manager
     * @return a Map from query name to PathQuery
     */
    public static Map<String, PathQuery> unmarshalPathQueries(Reader reader, int version) {
        Map<String, PathQuery> queries = new LinkedHashMap<String, PathQuery>();
        try {
            SAXParser.parse(new InputSource(reader), new PathQueryHandler(queries, version));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return queries;
    }

    /**
     * Parses PathQuery from XML.
     * @param reader reader containing XML
     * @param version the version of the xml, an attribute on the profile manager
     * @return PathQuery
     */
    public static PathQuery unmarshalPathQuery(Reader reader, int version) {
        Map<String, PathQuery> map = unmarshalPathQueries(reader, version);
        if (map.size() != 0) {
            return map.values().iterator().next();
        }
        return null;
    }
}
