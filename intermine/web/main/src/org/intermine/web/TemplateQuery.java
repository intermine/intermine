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

import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

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
    /** Entire query */
    protected PathQuery query;
    /** Nodes with templated constraints */
    protected List nodes = new ArrayList();
    /** Map from node to editable constraint list */
    protected Map constraints = new HashMap();
    /** True if template is considered 'important' for a related class. */
    protected boolean important = false;
    /** Keywords set for this template. */
    protected String keywords = "";

    /**
     * Construct a new instance of TemplateQuery.
     *
     * @param name the name of the template
     * @param description the template description
     * @param query the query itself
     * @param important true if template is important
     * @param keywords keywords for this template
     */
    public TemplateQuery(String name, String description, PathQuery query,
                         boolean important, String keywords) {
        if (description != null) {
            this.description = description;
        }
        if (name != null) {
            this.name = name;
        }
        if (keywords != null) {
            this.keywords = keywords;
        }
        this.query = query;
        this.important = important;
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
        if (constraints.get(node) == null) {
            return Collections.EMPTY_LIST;
        } else {
            return (List) constraints.get(node);
        }
    }


    /**
     * Return a clone of this template query with all editable constraints
     * removed - i.e. a query that will return all possible results of executing
     * the template.  The original template is left unaltered.
     * @return a clone of the original tempate without editable constraints.
     */
    public TemplateQuery cloneWithoutEditableConstraints() {
        TemplateQuery clone = TemplateHelper.cloneTemplate(this);

        PathQuery queryClone = clone.getQuery();

        // Find the editable constraints in the query.
        Iterator iter = queryClone.getNodes().entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            PathNode node = (PathNode) entry.getValue();
            Iterator citer = node.getConstraints().iterator();
            while (citer.hasNext()) {
                Constraint c = (Constraint) citer.next();
                if (c.isEditable()) {
                    citer.remove();
                }
            }
        }
        clone.constraints.clear();
        clone.nodes.clear();

        return clone;
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
     * @return true if template is important
     */
    public boolean isImportant() {
        return important;
    }

    /**
     * Find out whether the template is valid against the current model.
     * @return true if template is valid, false if not
     */
    public boolean isValid() {
        return query.isValid();
    }

    /**
     * Get the exceptions generated while deserialising this template query.
     * @return exceptions relating to this template query
     */
    public Exception[] getProblems() {
        return query.getProblems();
    }

    /**
     * Get the keywords.
     * @return template keywords
     */
    public String getKeywords() {
        return keywords;
    }

    /**
     * Convert a template query to XML.
     * @return this template query as XML.
     */
    public String toXml() {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            TemplateQueryBinding.marshal(this, writer);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

        return sw.toString();
    }

}
