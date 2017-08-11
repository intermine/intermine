package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang.StringUtils;
import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.Model;
import org.intermine.metadata.SAXParser;
import org.intermine.metadata.StringUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Convert PathQueries to and from XML or JSON
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
    private static void marshalPathQueryJoinStyle(PathQuery query, XMLStreamWriter writer)
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
    private static void marshalPathQueryDescriptions(PathQuery query, XMLStreamWriter writer)
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
                    if (value == null) {
                        writer.writeStartElement("nullValue");
                        writer.writeEndElement();
                    } else {
                        if (!value.equals(value.trim())) {
                            throw new XMLStreamException("Value in MultiValue starts or ends with "
                                + "whitespace - this query cannot be represented in XML");
                        }
                        writer.writeStartElement("value");
                        writer.writeCharacters(value);
                        writer.writeEndElement();
                    }
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
        // do stuff
    }

    /**
     * Parse PathQueries from XML.
     * @param reader the saved queries
     * @param version the version of the xml, an attribute on the profile manager
     * @return a Map from query name to PathQuery
     */
    public static Map<String, PathQuery> unmarshalPathQueries(Reader reader, int version) {
        return unmarshalPathQueries(reader, version, null);
    }

    /**
     * Parse PathQueries from XML, declaring which model should be used in preference to the
     * default model retrieved by <code>Model::getInstanceByName</code>.
     * @param reader the saved queries
     * @param version the version of the xml, an attribute on the profile manager
     * @param model The model to use in preference. May be null.
     * @return a Map from query name to PathQuery
     */
    public static Map<String, PathQuery> unmarshalPathQueries(Reader reader, int version,
        Model model) {
        Map<String, PathQuery> queries = new LinkedHashMap<String, PathQuery>();
        try {
            InputSource src = new InputSource(reader);
            DefaultHandler handler = new PathQueryHandler(queries, version, model);
            SAXParser.parse(src, handler);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return queries;
    }

    /**
     * Parse PathQueries from JSON
     * @param model the data model
     * @param jsonString the query in JSON format
     * @return a Map from query name to PathQuery
     * @throws JSONException if poorly formatted JSON
     */
    public static PathQuery unmarshalJSONPathQuery(Model model, String jsonString)
        throws JSONException {
        JSONObject obj = new JSONObject(jsonString);
        PathQuery query = new PathQuery(model);
        String name = "query";
        if (obj.has("name")) {
            name = obj.getString("name");
        }
        query.setTitle(name);

        // SELECT statement
        JSONArray viewPaths = obj.getJSONArray("select");
        for (int i = 0; i < viewPaths.length(); i++) {
            query.addView(viewPaths.getString(i));
        }

        // WHERE statement
        setConstraints(obj, query);

        // outer join status
        if (obj.has("joins")) {
            JSONArray outerJoins = obj.getJSONArray("joins");
            for (int i = 0; i < outerJoins.length(); i++) {
                query.setOuterJoinStatus(outerJoins.getString(i), OuterJoinStatus.OUTER);
            }
        }

        // constraint logic
        if (obj.has("constraintLogic")) {
            String constraintLogic = obj.getString("constraintLogic");
            query.setConstraintLogic(constraintLogic);
        }

        // description
        if (obj.has("description")) {
            String description = obj.getString("description");
            query.setDescription(description);
        }

        // order by
        if (obj.has("orderBy")) {
            JSONArray orderBys = obj.getJSONArray("orderBy");
            for (int i = 0; i < orderBys.length(); i++) {
                JSONObject orderBy = orderBys.getJSONObject(i);
                Iterator keys = orderBy.keys();
                while (keys.hasNext()) {
                    String orderPath = (String) keys.next();
                    String direction = orderBy.getString(orderPath);
                    if ("desc".equalsIgnoreCase(direction)) {
                        query.addOrderBy(orderPath, OrderDirection.DESC);
                    } else {
                        query.addOrderBy(orderPath, OrderDirection.ASC);
                    }
                }
            }
        }

        return query;
    }

    private static void setConstraints(JSONObject obj, PathQuery query) throws JSONException {
        // WHERE statement
        if (obj.has("where")) {
            JSONArray constraints = obj.getJSONArray("where");
            for (int i = 0; i < constraints.length(); i++) {
                JSONObject constraintObj = constraints.getJSONObject(i);
                PathConstraint constraint = null;
                String path = constraintObj.getString("path");
                if (constraintObj.has("op")) {
                    String op = constraintObj.getString("op");
                    ConstraintOp constraintOp = ConstraintOp.getConstraintOp(
                            constraintObj.getString("op"));
                    String code = constraintObj.getString("code");
                    String value = null;
                    JSONArray values = null;
                    JSONArray idArray = null;
                    if (constraintObj.has("value")) {
                        value = constraintObj.getString("value");
                    } else if (constraintObj.has("values"))  {
                        values = constraintObj.getJSONArray("values");
                    } else if (constraintObj.has("ids"))  {
                        // if the constraint doesn't have a list name, it will have a set of ids
                        idArray = constraintObj.getJSONArray("ids");
                    }

                    if ("IN".equals(op) || "NOT IN".equals(op)) {
                        if (StringUtils.isNotEmpty(value)) {
                            constraint = new PathConstraintBag(path, constraintOp, value);
                        } else if (idArray != null) {
                            List<Integer> ids = new ArrayList<Integer>();
                            for (int j = 0; j < idArray.length(); j++) {
                                String id = idArray.getString(j);
                                ids.add(Integer.parseInt(id));
                            }
                            constraint = new PathConstraintIds(path, constraintOp, ids);
                        }
                    } else if ("LOOKUP".equals(op)) {
                        String extraValue = null;
                        if (constraintObj.has("extraValue")) {
                            extraValue = constraintObj.getString("extraValue");
                        }
                        constraint = new PathConstraintLookup(path, value, extraValue);
                    } else if ("ONE OF".equals(op) || "NONE OF".equals(op)) {
                        List<String> oneOfValues = new ArrayList<String>();
                        for (int j = 0; j < values.length(); j++) {
                            oneOfValues.add(values.get(j).toString());
                        }
                        constraint = new PathConstraintMultiValue(path, constraintOp, oneOfValues);
                    } else if ("IS NULL".equals(op) || "IS NOT NULL".equals(op)) {
                        constraint = new PathConstraintNull(path, constraintOp);
                    } else if ("WITHIN".equals(op) || "OVERLAPS".equals(op)
                        || "DOES NOT OVERLAP".equals(op) || "OUTSIDE".equals(op)) {
                        List<String> ranges = new ArrayList<String>();
                        for (int j = 0; j < values.length(); j++) {
                            ranges.add(values.get(j).toString());
                        }
                        constraint = new PathConstraintRange(path, constraintOp, ranges);
                    } else if ("ISA".equals(op) || "ISNT".equals(op)) {
                        List<String> types = new ArrayList<String>();
                        for (int j = 0; j < values.length(); j++) {
                            types.add(values.get(j).toString());
                        }
                        constraint = new PathConstraintMultitype(path, constraintOp, types);
                    } else {
                        if (constraintObj.has("loopPath")) {
                            String loopPath = constraintObj.getString("loopPath");
                            constraint = new PathConstraintLoop(path, constraintOp, loopPath);
                        } else {
                            constraint = new PathConstraintAttribute(path, constraintOp, value);
                        }
                    }
                    query.addConstraint(constraint, code);
                } else if (constraintObj.has("type")) {
                    String type = constraintObj.getString("type");
                    // subclass
                    constraint = new PathConstraintSubclass(path, type);
                    query.addConstraint(constraint);
                }
            }
        }
    }

    /**
     * Parses PathQuery from XML.
     * @param reader reader containing XML
     * @param version the version of the xml, an attribute on the profile manager
     * @return PathQuery
     */
    public static PathQuery unmarshalPathQuery(Reader reader, int version) {
        return unmarshalPathQuery(reader, version, null);
    }

    /**
     * Parses a PathQuery from XML, declaring which model should be used in preference to the
     * default model retrieved by <code>Model::getInstanceByName</code>.
     * @param reader The source of the model XML.
     * @param version The version of the path-query format.
     * @param model The model to use in preference. May be null.
     * @return The path-query, or null if none were found.
     */
    public static PathQuery unmarshalPathQuery(Reader reader, int version, Model model) {
        Map<String, PathQuery> map = unmarshalPathQueries(reader, version, model);
        if (map.size() != 0) {
            return map.values().iterator().next();
        }
        return null;
    }
}
