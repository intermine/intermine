package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.Collection;
import java.util.Date;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ClassConstraint;
import org.intermine.util.TypeUtil;

/**
 * Helper methods for main controller and main action
 * @author Mark Woodbridge
 */
public class MainHelper
{
    /**
     * Given a path, render a set of metadata Nodes to the relevant depth
     * @param path of form Gene.organism.name
     * @param model the model used to resolve class names
     * @return an ordered Set of nodes
     */
    public static Collection makeNodes(String path, Model model) {
        String className, subPath;
        if (path.indexOf(".") == -1) {
            className = path;
            subPath = "";
        } else {
            className = path.substring(0, path.indexOf("."));
            subPath = path.substring(path.indexOf(".") + 1);
        }
        Map nodes = new LinkedHashMap();
        nodes.put(className, new MetadataNode(className));
        makeNodes(getClassDescriptor(className, model), subPath, className, nodes);
        return nodes.values();
    }

    /**
     * Recursive method used to add nodes to a set representing a path from a given ClassDescriptor
     * @param cld the root ClassDescriptor
     * @param path current path prefix (eg Gene)
     * @param currentPath current path suffix (eg organism.name)
     * @param nodes the current Node set
     */
    protected static void makeNodes(ClassDescriptor cld, String path, String currentPath,
                                    Map nodes) {
        List sortedNodes = new ArrayList();

        // compare FieldDescriptors by name
        Comparator comparator = new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((FieldDescriptor) o1).getName().compareTo(((FieldDescriptor) o2).getName());
            }
        };

        Set attributeNodes = new TreeSet(comparator);
        Set referenceAndCollectionNodes = new TreeSet(comparator);
        for (Iterator i = cld.getAllFieldDescriptors().iterator(); i.hasNext();) {
            FieldDescriptor fd = (FieldDescriptor) i.next();
            if (!fd.isReference() && !fd.isCollection()) {
                attributeNodes.add(fd);
            } else {
                referenceAndCollectionNodes.add(fd);
            }
        }

        sortedNodes.addAll(attributeNodes);
        sortedNodes.addAll(referenceAndCollectionNodes);

        for (Iterator i = sortedNodes.iterator(); i.hasNext();) {
            FieldDescriptor fd = (FieldDescriptor) i.next();
            String fieldName = fd.getName();

            if (fieldName.equals("id")) {
                continue;
            }

            String head, tail;
            if (path.indexOf(".") != -1) {
                head = path.substring(0, path.indexOf("."));
                tail = path.substring(path.indexOf(".") + 1);
            } else {
                head = path;
                tail = "";
            }

            String button;
            if (fieldName.equals(head)) {
                button = "-";
            } else if (fd.isReference() || fd.isCollection()) {
                button = "+";
            } else {
                button = " ";
            }

            MetadataNode parent = (MetadataNode) nodes.get(currentPath);
            MetadataNode node = new MetadataNode(parent, fieldName, cld.getModel(), button);

            nodes.put(node.getPath(), node);
            if (fieldName.equals(head)) {
                ClassDescriptor refCld = ((ReferenceDescriptor) fd).getReferencedClassDescriptor();
                makeNodes(refCld, tail, currentPath + "." + head, nodes);
            }
        }
    }

    /**
     * Make an InterMine query from a path query
     * @param query the PathQuery
     * @param savedBags the current saved bags map
     * @return an InterMine Query
     */
    public static Query makeQuery(PathQuery query, Map savedBags) {
        query = (PathQuery) query.clone();
        Map qNodes = query.getNodes();
        List view = query.getView();
        Model model = query.getModel();

        //first merge the query and the view
        for (Iterator i = view.iterator(); i.hasNext();) {
            String path = (String) i.next();
            if (!qNodes.containsKey(path)) {
                query.addNode(path);
            }
        }

        //create the real query
        Query q = new Query();
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        q.setConstraint(cs);

        Map queryBits = new HashMap();

        //build the FROM and WHERE clauses
        for (Iterator i = query.getNodes().values().iterator(); i.hasNext();) {
            PathNode node = (PathNode) i.next();
            String path = node.getPath();

            if (path.indexOf(".") == -1) {
                QueryClass qc = new QueryClass(getClass(node.getType(), model));
                q.addFrom(qc);
                queryBits.put(path, qc);
            } else {
                String fieldName = node.getFieldName();
                QueryClass parentQc = (QueryClass) queryBits.get(node.getPrefix());

                if (node.isAttribute()) {
                    QueryField qf = new QueryField(parentQc, fieldName);
                    queryBits.put(path, qf);
                } else {
                    QueryReference qr = null;
                    if (node.isReference()) {
                        qr = new QueryObjectReference(parentQc, fieldName);
                    } else {
                        qr = new QueryCollectionReference(parentQc, fieldName);
                    }
                    QueryClass qc = new QueryClass(getClass(node.getType(), model));
                    cs.addConstraint(new ContainsConstraint(qr, ConstraintOp.CONTAINS, qc));
                    q.addFrom(qc);
                    queryBits.put(path, qc);
                }
            }

            QueryNode qn = (QueryNode) queryBits.get(path);
            for (Iterator j = node.getConstraints().iterator(); j.hasNext();) {
                Constraint c = (Constraint) j.next();
                if (BagConstraint.VALID_OPS.contains(c.getOp())) {
                    cs.addConstraint(new BagConstraint(qn,
                                                       c.getOp(),
                                                       (Collection) savedBags.get(c.getValue())));
                } else if (node.isAttribute()) { //assume, for now, that it's a SimpleConstraint
                    cs.addConstraint(new SimpleConstraint((QueryField) qn,
                                                          c.getOp(),
                                                          new QueryValue(c.getValue())));
                } else if (node.isReference()) {
                    QueryClass refQc = (QueryClass) queryBits.get(c.getValue());
                    cs.addConstraint(new ClassConstraint((QueryClass) qn, c.getOp(), refQc));
                }
            }
        }

        //build the SELECT list
        for (Iterator i = view.iterator(); i.hasNext();) {
            q.addToSelect((QueryNode) queryBits.get((String) i.next()));
        }

        return q;
    }

    /**
     * Instantiate a class by unqualified name
     * The name should be "InterMineObject" or the name of class in the model provided
     * @param className the name of the class
     * @param model the Model used to resolve class names
     * @return the relevant Class
     */
    public static Class getClass(String className, Model model) {
        if ("InterMineObject".equals(className)) {
            className = "org.intermine.model.InterMineObject";
        } else {
            className = model.getPackageName() + "." + className;
        }
        try {
            return Class.forName(className);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Instantiate a class by unqualified name
     * The name should be "Date" or that of a primitive container class such as "Integer"
     * @param className the name of the class
     * @return the relevant Class
     */
    public static Class getClass(String className) {
        Class cls = TypeUtil.instantiate(className);
        if (cls == null) {
            if ("Date".equals(className)) {
                cls = Date.class;
            } else {
                try {
                    cls = Class.forName("java.lang." + className);
                } catch (Exception e) {
                }
            }
        }
        return cls;
    }

    /**
     * Get the metadata for a class by unqualified name
     * The name is looked up in the provided model
     * @param className the name of the class
     * @param model the Model used to resolve class names
     * @return the relevant ClassDescriptor
     */
    public static ClassDescriptor getClassDescriptor(String className, Model model) {
        return model.getClassDescriptorByName(getClass(className, model).getName());
    }

    /**
     * Take a Collection of ConstraintOps and builds a map from ConstraintOp.getIndex() to
     * ConstraintOp.toString() for each
     * @param ops a Collection of ConstraintOps
     * @return the Map from index to string
     */
    public static Map mapOps(Collection ops) {
        Map opString = new LinkedHashMap();
        for (Iterator iter = ops.iterator(); iter.hasNext();) {
            ConstraintOp op = (ConstraintOp) iter.next();
            opString.put(op.getIndex(), op.toString());
        }
        return opString;
    }
    
    /**
     * Create constraint values for display. Returns a Map from Constraint to String
     * for each Constraint in the path query.
     *
     * @param pathquery  the PathQuery to look at
     * @return           Map from Constraint to displat value
     */
    public static Map makeConstraintDisplayMap(PathQuery pathquery) {
        Map map = new HashMap();
        Iterator iter = pathquery.getNodes().values().iterator();
        while (iter.hasNext()) {
            PathNode node = (PathNode) iter.next();
            Iterator citer = node.getConstraints().iterator();
            while (citer.hasNext()) {
                Constraint con = (Constraint) citer.next();
                ConstraintOp op = con.getOp();
                
                map.put(con, con.getDisplayValue(node));
            }
        }
        return map;
    }
}
