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
import java.util.ArrayList;
import java.util.Map;

/**
 * A template query, which consists of a PathQuery, description, category,
 * short name.
 * 
 * @author Mark Woodbridge
 * @author Thomas Riley
 */
public class TemplateQuery
{
    /** Template query name. */
    protected String name;
    /** Template query description. */
    protected String description;
    /** Template query category. */
    protected String category;
    /** Entire query */
    protected PathQuery query;
    /** Nodes with templated constraints */
    protected List nodes = new ArrayList();
    /** Map from node to editable constraint list */
    protected Map constraints = new HashMap();

    /**
     * Construct a new instance of TemplateQuery.
     *
     * @param name the name of the template
     * @param category name of category that this query falls under
     * @param description the template description
     * @param query the query itself
     */
    public TemplateQuery(String name, String description, String category, PathQuery query) {
        if (description != null) {
            this.description = description;
        }
        if (category != null) {
            this.category = category;
        }
        if (name != null) {
            this.name = name;
        }
        this.query = query;
        
        // Find the editable constraints in the query.
        Iterator iter = query.getNodes().entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            PathNode node = (PathNode) entry.getValue();
            Iterator citer = node.getConstraints().iterator();
            while (citer.hasNext()) {
                Constraint c = (Constraint) citer.next();
                if (c.isEditable()) {
                    List ecs = (List) constraints.get(node);
                    if (ecs == null) {
                        ecs = new ArrayList();
                        nodes.add(node);
                        constraints.put(node, ecs);
                    }
                    ecs.add(c);
                }
            }
        }
    }
    
    /**
     * For a PathNode with editable constraints, get all the editable
     * constraints as a List.
     *
     * @param node  a PathNode with editable constraints
     * @return      List of Constraints for Node
     */
    public List getConstraints(PathNode node) {
        return (List) constraints.get(node);
    }

    /**
     * Return a List of all the Constraints of fields in this template query.
     * @return a List of all the Constraints of fields in this template query
     */
    public List getAllConstraints() {
        List returnList = new ArrayList();

        Iterator iter = constraints.values().iterator();

        while (iter.hasNext()) {
            returnList.addAll((List) iter.next());
        }

        return returnList;
    }

    /**
     * Get the tempalte description.
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the query (eg. select c from Company as c where c.name = 'CompanyA')
     * @return the query
     */
    public PathQuery getQuery() {
        return query;
    }

    /**
     * Get the nodes from the description, in order (eg. {Company.name})
     * @return the nodes
     */
    public List getNodes() {
        return nodes;
    }
    
    /**
     * Get the query short name.
     * @return the query identifier string
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the category that this template belongs to.
     * @return category for template
     */
    public String getCategory() {
        return category;
    }
}