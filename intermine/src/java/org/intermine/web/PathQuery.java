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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.intermine.metadata.Model;

/**
 * Class to represent a path-based query
 * @author Mark Woodbridge
 */
public class PathQuery
{
    protected Model model;
    protected Map nodes = new LinkedHashMap();
    protected List view = new ArrayList();
   
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
     * Sets the value of nodes
     * @param nodes the value of nodes
     */
    public void setNodes(Map nodes) {
        this.nodes = nodes;
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
     * Add a node to the query using a path, adding parent nodes if necessary
     * @param path the path for the new Node
     * @return the RightNode that was added to the qNodes Map
     */
    public RightNode addNode(String path) {
        RightNode node;
        if (path.lastIndexOf(".") == -1) {
            node = new RightNode(path);
        } else {
            String prefix = path.substring(0, path.lastIndexOf("."));
            if (nodes.containsKey(prefix)) {
                RightNode parent = (RightNode) nodes.get(prefix);
                String fieldName = path.substring(path.lastIndexOf(".") + 1);
                node = new RightNode(parent, fieldName, model);
            } else {
                addNode(prefix);
                return addNode(path);
            }
        }
        nodes.put(path, node);
        return node;
    }

    /**
     * Clone this PathQuery
     * @return a PathQuery
     */
    public Object clone() {
        PathQuery query = new PathQuery(model);
        for (Iterator i = nodes.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            query.getNodes().put(entry.getKey(), clone((RightNode) entry.getValue()));
        }
        query.getView().addAll(view);
        return query;
    }

    /**
     * Clone a RightNode
     * @param node a RightNode
     * @return a copy of the RightNode
     */
    public RightNode clone(RightNode node) {
        RightNode newNode;
        RightNode parent = (RightNode) nodes.get(node.getPrefix());
        if (parent == null) {
            newNode = new RightNode(node.getType());
        } else {
            newNode = new RightNode(parent, node.getFieldName(), model);
            newNode.setType(node.getType());
        }
        for (Iterator i = node.getConstraints().iterator(); i.hasNext();) {
            Constraint constraint = (Constraint) i.next();
            newNode.getConstraints().add(new Constraint(constraint.getOp(), constraint.getValue()));
        }
        return newNode;
    }
}
