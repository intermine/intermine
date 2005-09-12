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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.util.CollectionUtil;

/**
 * Class to represent a path-based query
 * @author Mark Woodbridge
 */
public class PathQuery
{
    protected Model model;
    protected LinkedHashMap nodes = new LinkedHashMap();
    protected List view = new ArrayList();
    protected ResultsInfo info;
    protected ArrayList problems = new ArrayList();
   
    /**
     * Constructor
     * @param model the Model on which to base this query
     */
    public PathQuery(Model model) {
        this.model = model;
    }

    /**
     * Gets the value of model
     * @return the value of model
     */
    public Model getModel() {
        return model;
    }

    /**
     * Gets the value of nodes
     * @return the value of nodes
     */
    public Map getNodes() {
        return nodes;
    }

    /**
     * Sets the value of view
     * @param view the value of view
     */
    public void setView(List view) {
        this.view = view;
    }

    /**
     * Gets the value of view
     * @return the value of view
     */
    public List getView() {
        return view;
    }

    /**
     * Get info regarding this query
     * @return the info
     */
    public ResultsInfo getInfo() {
        return info;
    }

    /**
     * Set info about this query
     * @param info the info
     */
    public void setInfo(ResultsInfo info) {
        this.info = info;
    }

    /**
     * Provide a list of the names of bags mentioned in the query
     * @return the list of bag names
     */
    public List getBagNames() {
        List bagNames = new ArrayList();
        for (Iterator i = nodes.values().iterator(); i.hasNext();) {
            PathNode node = (PathNode) i.next();
            for (Iterator j = node.getConstraints().iterator(); j.hasNext();) {
                Constraint c = (Constraint) j.next();
                if (BagConstraint.VALID_OPS.contains(c.getOp())) {
                    bagNames.add(c.getValue());
                }
            }
        }
        return bagNames;
    }

    /**
     * Add a node to the query using a path, adding parent nodes if necessary
     * @param path the path for the new Node
     * @return the PathNode that was added to the nodes Map
     */
    public PathNode addNode(String path) {
        PathNode node;

        // the new node will be inserted after this one or at the end if null
        String previousNodePath = null;

        if (path.indexOf(".") == -1) {
            node = new PathNode(path);
            // Check whether starting point exists
            try {
                MainHelper.getQualifiedTypeName(path, model);
            } catch (ClassNotFoundException err) {
                problems.add(err);
            }
        } else {
            String prefix = path.substring(0, path.lastIndexOf("."));
            if (nodes.containsKey(prefix)) {
                Iterator pathsIter = nodes.keySet().iterator();

                while (pathsIter.hasNext()) {
                    String pathFromMap = (String) pathsIter.next(); 
                    if (pathFromMap.startsWith(prefix)) {
                        previousNodePath = pathFromMap;
                    }
                }

                PathNode parent = (PathNode) nodes.get(prefix);
                String fieldName = path.substring(path.lastIndexOf(".") + 1);
                node = new PathNode(parent, fieldName);
                try {
                    node.setModel(model);
                } catch (Exception err) {
                    problems.add(err);
                }
            } else {
                addNode(prefix);
                return addNode(path);
            }
        }

        nodes = CollectionUtil.linkedHashMapAdd(nodes, previousNodePath, path, node);

        return node;
    }
    
    /**
     * Get the exceptions generated while deserialising this path query query.
     * @return exceptions relating to this path query
     */
    public Exception[] getProblems() {
        return (Exception[]) problems.toArray(new Exception[0]);
    }
    
    /**
     * Find out whether the path query is valid against the current model.
     * @return true if query is valid, false if not
     */
    public boolean isValid() {
        return (problems.size() == 0);
    }

    /**
     * Clone this PathQuery
     * @return a PathQuery
     */
    public Object clone() {
        PathQuery query = new PathQuery(model);
        for (Iterator i = nodes.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            query.getNodes().put(entry.getKey(), clone(query, (PathNode) entry.getValue()));
        }
        query.getView().addAll(view);
        return query;
    }

    /**
     * Clone a PathNode
     * @param query PathQuery containing cloned PathNode
     * @param node a PathNode
     * @return a copy of the PathNode
     */
    protected PathNode clone(PathQuery query, PathNode node) {
        PathNode newNode;
        PathNode parent = (PathNode) nodes.get(node.getPrefix());
        if (parent == null) {
            newNode = new PathNode(node.getType());
        } else {
            newNode = new PathNode(parent, node.getFieldName());
            try {
                newNode.setModel(model);
            } catch (IllegalArgumentException err) {
                query.problems.add(err);
            }
            newNode.setType(node.getType());
        }
        for (Iterator i = node.getConstraints().iterator(); i.hasNext();) {
            Constraint constraint = (Constraint) i.next();
            newNode.getConstraints().add(new Constraint(constraint.getOp(), constraint.getValue(),
                    constraint.isEditable(), constraint.getDescription(), constraint.getIdentifier()));
        }
        return newNode;
    }

    /**
     * @see Object#equals
     */
    public boolean equals(Object o) {
        return (o instanceof PathQuery)
            && model.equals(((PathQuery) o).model)
            && nodes.equals(((PathQuery) o).nodes)
            && view.equals(((PathQuery) o).view);
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return 2 * model.hashCode()
            + 3 * nodes.hashCode()
            + 5 * view.hashCode();
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        return "{PathQuery: " + model + ", " + nodes + ", " + view + "}";
    }

    /**
     * Check validity of receiver by trying to create an objectstore Query. If
     * conversion fails, the exception is recorded and isValid will return false.
     */
    protected void checkValidity() {
        try {
            MainHelper.makeQuery(this, new HashMap());
        } catch (Exception err) {
            problems.add(err);
        }
    }
}
