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
import java.util.Map;
import java.util.TreeMap;
import java.util.Collection;
import java.util.LinkedHashMap;

import org.intermine.metadata.AttributeDescriptor;
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
        Map nodes = new TreeMap();
        nodes.put(className, new LeftNode(className));
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
    public static void makeNodes(ClassDescriptor cld, String path, String currentPath, Map nodes) {
        for (Iterator i = cld.getAllFieldDescriptors().iterator(); i.hasNext();) {
            FieldDescriptor fd = (FieldDescriptor) i.next();
            String fieldName = fd.getName();

            if (fieldName.equals("id")) {
                return;
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

            LeftNode parent = (LeftNode) nodes.get(currentPath);
            LeftNode node = new LeftNode(parent, fieldName, cld.getModel(), button);
            nodes.put(node.getPath(), node);
            if (fieldName.equals(head)) {
                ClassDescriptor refCld = ((ReferenceDescriptor) fd).getReferencedClassDescriptor();
                makeNodes(refCld, tail, currentPath + "." + head, nodes);
            }
        }
    }

    /**
     * Method to return a String representing the unqualified type of the item referenced by a path
     * @param path the path
     * @param model the metadata used to resolve the types of fields in the path
     * @return the type
     */
    public static String getType(String path, Model model) {
        if (path.indexOf(".") == -1) {
            return path;
        }

        FieldDescriptor fd = getFieldDescriptor(path, model);
        if (fd.isAttribute()) {
            return TypeUtil.unqualifiedName(((AttributeDescriptor) fd).getType());
        } else {
            return TypeUtil.unqualifiedName(((ReferenceDescriptor) fd).
                                            getReferencedClassDescriptor().getType().getName());
        }
    }

    /**
     * Return the metadata for a field identified by a path into a model (eg Gene.organism.name)
     * @param path the path
     * @param model the model
     * @return the metadata for the field
     *
     */
    public static FieldDescriptor getFieldDescriptor(String path, Model model) {
        if (path.indexOf(".") == -1) {
            return null;
        }
        String className = path.substring(0, path.indexOf("."));
        path = path.substring(path.indexOf(".") + 1);
        ClassDescriptor cld = getClassDescriptor(className, model);
        FieldDescriptor fd;
        for (;;) {
            if (path.indexOf(".") == -1) {
                fd = cld.getFieldDescriptorByName(path);
                break;
            } else {
                fd = cld.getFieldDescriptorByName(path.substring(0, path.indexOf(".")));
                cld = ((ReferenceDescriptor) fd).getReferencedClassDescriptor();
                path = path.substring(path.indexOf(".") + 1);
            }
        }
        return fd;
    }

    /**
     * Add a node to the query using a path, adding parent nodes if necessary
     * @param qNodes the current Node map (from path to Node)
     * @param path the path for the new Node
     * @param model the model
     */
    public static void addNode(Map qNodes, String path, Model model) {
        String prefix = path.substring(0, path.lastIndexOf("."));
        if (qNodes.containsKey(prefix)) {
            RightNode parent = (RightNode) qNodes.get(prefix);
            String fieldName = path.substring(path.lastIndexOf(".") + 1);
            qNodes.put(path, new RightNode(parent, fieldName, model));
        } else {
            addNode(qNodes, prefix, model);
            addNode(qNodes, path, model);
        }
    }

    /**
     * Make an InterMine query from a query represented as a Node map (from path to Node)
     * @param qNodes the Node map
     * @param view a list of paths (that may not appear in the query) to use as the results view
     * @param model the relevant metadata
     * @param savedBags the current saved bags map
     * @return an InterMine Query
     */
    public static Query makeQuery(Map qNodes, List view, Model model, Map savedBags) {
        Map qNodes2 = new TreeMap(qNodes);
        for (Iterator i = view.iterator(); i.hasNext();) {
            String path = (String) i.next();
            if (path.indexOf(".") != -1 && !qNodes.containsKey(path)) {
                addNode(qNodes2, path, model);
            }
        }

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        Query q = new Query();
        q.setConstraint(cs);

        Map queryBits = new HashMap();

        for (Iterator i = qNodes2.values().iterator(); i.hasNext();) {
            RightNode node = (RightNode) i.next();
            String path = node.getPath();
            
            if (path.indexOf(".") == -1) {
                QueryClass qc = new QueryClass(getClass(node.getType(), model));
                q.addFrom(qc);
                queryBits.put(path, qc);
                for (Iterator j = node.getConstraints().iterator(); j.hasNext();) {
                    Constraint c = (Constraint) j.next();
                    cs.addConstraint(new BagConstraint(qc,
                                                       c.getOp(), 
                                                       (Collection) savedBags.get(c.getValue())));
                }
                continue;
            }
            
            String fieldName = path.substring(path.lastIndexOf(".") + 1);
            String prefix = path.substring(0, path.lastIndexOf("."));
            QueryClass parentQc = (QueryClass) queryBits.get(prefix);

            FieldDescriptor fd = getFieldDescriptor(path, model);
            if (fd.isAttribute()) {
                QueryField qf = new QueryField(parentQc, fieldName);
                queryBits.put(path, qf);
                for (Iterator j = node.getConstraints().iterator(); j.hasNext();) {
                    Constraint c = (Constraint) j.next();
                    cs.addConstraint(new SimpleConstraint(qf,
                                                          c.getOp(), 
                                                          new QueryValue(c.getValue())));
                }
            } else {
                QueryReference qr = null;
                if (fd.isReference()) {
                    qr = new QueryObjectReference(parentQc, fieldName);
                } else {
                    qr = new QueryCollectionReference(parentQc, fieldName);
                }
                QueryClass qc = new QueryClass(getClass(node.getType(), model));
                cs.addConstraint(new ContainsConstraint(qr, ConstraintOp.CONTAINS, qc));
                q.addFrom(qc);
                queryBits.put(path, qc);
                for (Iterator j = node.getConstraints().iterator(); j.hasNext();) {
                    Constraint c = (Constraint) j.next();
                    cs.addConstraint(new BagConstraint(qc,
                                                       c.getOp(), 
                                                       (Collection) savedBags.get(c.getValue())));
                }
            }
        }
        for (Iterator i = view.iterator(); i.hasNext();) {
            String path = (String) i.next();
            q.addToSelect((QueryNode) queryBits.get(path));
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
    protected static Map mapOps(Collection ops) {
        Map opString = new LinkedHashMap();
        for (Iterator iter = ops.iterator(); iter.hasNext();) {
            ConstraintOp op = (ConstraintOp) iter.next();
            opString.put(op.getIndex(), op.toString());
        }
        return opString;
    }
}
