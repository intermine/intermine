package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Extension of DefaultHandler to handle parsing PathQuery objects
 * @author Mark Woodbridge
 * @author Kim Rutherford
 * @author Thomas Riley
 */
public class PathQueryHandler extends DefaultHandler
{
    Map<String, List<FieldDescriptor>> classKeys;
    private Map<String, PathQuery> queries;
    private String queryName;
    private char gencode;
    private PathNode node;
    protected PathQuery query;
    private Model model = null;
    private List<String> viewStrings = new ArrayList();
    private Map<String, String> pathStringDescriptions = new HashMap<String, String>();
    private Map<String, Boolean> sortOrder = new LinkedHashMap();

    /**
     * Constructor
     * @param queries Map from query name to PathQuery
     * @param classKeys class keys
     */
    public PathQueryHandler(Map<String, PathQuery> queries,
            Map<String, List<FieldDescriptor>> classKeys) {
        this.classKeys = classKeys;
        this.queries = queries;
    }

    /**
     * {@inheritDoc}
     */
    public void startElement(@SuppressWarnings("unused") String uri,
                             @SuppressWarnings("unused") String localName,
                             String qName, Attributes attrs)
    throws SAXException {
        if (qName.equals("query")) {
            // reset things
            gencode = 'A';
            queryName = validateName(attrs.getValue("name"));
            try {
                model = Model.getInstanceByName(attrs.getValue("model"));
            } catch (Exception e) {
                throw new SAXException(e);
            }
            query = new PathQuery(model);
            if (attrs.getValue("view") != null) {
                viewStrings = StringUtil.tokenize(attrs.getValue("view"));
            }

            if (attrs.getValue("sortOrder") != null) {
               String[] s = (attrs.getValue("sortOrder")).split(" ");
               for (int i = 0; i < s.length; i++) {
                   Boolean sortAscending = Boolean.TRUE;
                   String orderByString = s[i];
                   // check if next string bit is a direction string
                   if ((s.length > i + 1) && (s[i + 1].equalsIgnoreCase("desc")
                                                   || s[i + 1].equalsIgnoreCase("asc"))) {
                       if (s[i + 1].equalsIgnoreCase("desc")) {
                           sortAscending = Boolean.FALSE;
                       }
                       i++;
                   }
                   sortOrder.put(orderByString, sortAscending);
               }
            }
            if (attrs.getValue("constraintLogic") != null) {
                query.setConstraintLogic(attrs.getValue("constraintLogic"));
            }
        }
        if (qName.equals("node")) {
            node = query.addNode(attrs.getValue("path"));
            if (attrs.getValue("type") != null) {
                node.setType(attrs.getValue("type"));
            }
        }
        if (qName.equals("constraint")) {
            boolean constrainParent = false;
            int opIndex = toStrings(ConstraintOp.getValues()).indexOf(attrs.getValue("op"));
            ConstraintOp constraintOp = ConstraintOp.getOpForIndex(new Integer(opIndex));
            Object constraintValue = null;
            // If we know that the query is not valid, don't resolve the type of
            // the node as it may not resolve correctly
            if (node.isReference() || !query.isValid()) {
                constraintValue = attrs.getValue("value");
            } else if (BagConstraint.VALID_OPS.contains(constraintOp)) {
                constraintValue = attrs.getValue("value");
                // bag constraints are now only valid on classes.  If this bag
                // constraint is on another field:
                // a) if a key field move it to parent
                // b) otherwise throw an exception to disable query
                if (node.isAttribute()) {
                    if (isKeyField(classKeys, node.getParentType(),
                                                  node.getFieldName())) {
                        constrainParent = true;
                    } else {
                        Exception e = new Exception("Invalid bag constraint - only objects can be"
                                + "constrained to be in bags.");
                        // such complicated because list created by Arrays.asList doesn't
                        // support add method
                        List<Throwable> problems = new ArrayList<Throwable>(Arrays.asList(
                                query.getProblems()));
                        problems.add(e);
                        query.setProblems(problems);
                    }
                }
            } else {
                Class c = null;
                if (model != null && !node.getType().startsWith(model.getPackageName())) {
                    String type = model.getPackageName() + "." + node.getType();
                    try {
                        c = TypeUtil.getClass(type);
                    } catch (RuntimeException e) {
                        // ignore - probably a String/BigDecimal etc.
                    }
                }
                if (c == null) {
                    c = TypeUtil.getClass(node.getType());
                }
                if (constraintOp != ConstraintOp.IS_NULL
                        && constraintOp != ConstraintOp.IS_NOT_NULL) {
                    constraintValue = TypeUtil.stringToObject(c, attrs.getValue("value"));
                }
            }
            String editable = attrs.getValue("editable");
            boolean editableFlag = false;
            if (editable != null && editable.equals("true")) {
                editableFlag = true;
            }
            String description = attrs.getValue("description");
            String identifier = attrs.getValue("identifier");
            String code = attrs.getValue("code");
            if (code == null) {
                code = "" + gencode;
                gencode++;
            }
            String extraValue = attrs.getValue("extraValue");
            if (constrainParent) {
                PathNode parent = (PathNode) node.getParent();
                parent.getConstraints().add(new Constraint(constraintOp, constraintValue,
                            editableFlag, description, code, identifier, extraValue));
            } else {
                node.getConstraints().add(new Constraint(constraintOp, constraintValue,
                            editableFlag, description, code, identifier, extraValue));
            }
        }
        if (qName.equals("pathDescription")) {
            String pathString = attrs.getValue("pathString");
            String description = attrs.getValue("description");
            pathStringDescriptions.put(pathString, description);
        }
    }

    // copyed from ClassKeyHelper, so this class is independent at it and can be part of client
    private static boolean isKeyField(Map<String, List<FieldDescriptor>> classKeys, String clsName,
            String fieldName) {
        String className = clsName;
        if (clsName.indexOf('.') != -1) {
            className = TypeUtil.unqualifiedName(clsName);
        }
        List<FieldDescriptor> keys = classKeys.get(className);
        if (keys != null) {
            for (FieldDescriptor key : keys) {
                if (key.getName().equals(fieldName) && key.isAttribute()) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public void endElement(@SuppressWarnings("unused") String uri,
                           @SuppressWarnings("unused") String localName, String qName) {
        if (qName.equals("query")) {
            query.syncLogicExpression("and"); // always and for old queries
            query.addView(viewStrings);
            if (query.getView().size() == 0) {
                // query has no valid view paths, which we can't handle at the moment
                return;
            }
            if (sortOrder.isEmpty()) {
                query.setOrderBy(viewStrings.get(0));
            } else {
                setSortOrder();
            }

            for (Map.Entry<String, String> entry: pathStringDescriptions.entrySet()) {
                query.addPathStringDescription(entry.getKey(), entry.getValue());
            }
            queries.put(queryName, query);
            viewStrings = new ArrayList<String>();
            sortOrder = new LinkedHashMap();
            pathStringDescriptions = new HashMap<String, String>();
        }
    }

    /**
     * Convert a List of Objects to a List of Strings using toString
     * @param list the Object List
     * @return the String list
     */
    protected List<String> toStrings(List list) {
        List<String> strings = new ArrayList<String>();
        for (Iterator i = list.iterator(); i.hasNext();) {
            strings.add(i.next().toString());
        }
        return strings;
    }

    /**
     * Checks that the query has a name and that there's no name duplicates
     * and appends a number to the name if there is.
     * @param name the query name
     * @return the validated query name
     */
    protected String validateName(String name) {
        String validatedName = name;
        if (name == null || name.length() == 0) {
            validatedName = "unnamed_query";
        }
        if (queries.containsKey(validatedName)) {
            int i = 1;
            while (true) {
                String testName = validatedName + "_" + i;
                if (!queries.containsKey((testName))) {
                    return testName;
                }
                i++;
            }
        }
        return validatedName;
    }

    private void setSortOrder() {
        Iterator it = sortOrder.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String pathString = (String) entry.getKey();
            Boolean sortAscending = (Boolean) entry.getValue();
            query.addOrderBy(pathString, sortAscending);
        }
    }
}
