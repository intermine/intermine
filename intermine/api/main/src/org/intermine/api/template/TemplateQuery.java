package org.intermine.api.template;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;
import org.intermine.api.search.WebSearchable;
import org.intermine.api.xml.TemplateQueryBinding;
import org.intermine.model.userprofile.SavedTemplateQuery;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.PathQuery;

/**
 * A template query, which consists of a PathQuery, description, category,
 * short name.
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
 */
public class TemplateQuery extends PathQuery implements WebSearchable
{
    private static final Logger LOG = Logger.getLogger(TemplateQuery.class);

    /** Template query name. */
    protected String name;
    /** Template query title. */
    protected String title;
    /** The private comment for this query. */
    protected String comment;
    /** Map from node to editable constraint list. */
    protected Map constraints = new HashMap();
    /** Edited version of another template. */
    protected boolean edited = false;
    /** SavedTemplateQuery object in the UserProfile database, so we can update summaries. */
    protected SavedTemplateQuery savedTemplateQuery = null;

    /**
     * Construct a new instance of TemplateQuery.
     *
     * @param name the name of the template
     * @param title the short title of this template for showing in list
     * @param description the full template description for showing on the template form
     * @param comment an optional private comment for this template
     * @param query the query itself
     */
    public TemplateQuery(String name, String title, String description, String comment,
                         PathQuery query) {
        super(query.clone());
        if (description != null) {
            this.description = description;
        }
        if (name != null) {
            this.name = name;
        }
        this.title = title;
        this.comment = comment;
    }

    /**
     * For a PathNode with editable constraints, get all the editable
     * constraints as a List.
     *
     * @param node  a PathNode with editable constraints
     * @return      List of Constraints for Node
     */
    public List<Constraint> getEditableConstraints(PathNode node) {
        return getEditableConstraints(node.getPathString());
    }

    /**
     * Return all constraints for a given node or an empty list if none.
     * For a Path with editable constraints, get all the editable
     * constraints as a List.
     *
     * @param path a String of a path with editable constraints
     * @return List of Constraints for the path
     */
    public List<Constraint> getEditableConstraints(String path) {
        if (nodes.get(path) == null) {
            return Collections.EMPTY_LIST;
        }
        List<Constraint> ecs = new ArrayList<Constraint>();
        for (Constraint c : nodes.get(path).getConstraints()) {
            if (c.isEditable()) {
                ecs.add(c);
            }
        }
        return ecs;
    }

    /**
     * Return a clone of this template query with all editable constraints
     * removed - i.e. a query that will return all possible results of executing
     * the template.  The original template is left unaltered.
     *
     * @return a clone of the original tempate without editable constraints.
     */
    public TemplateQuery cloneWithoutEditableConstraints() {
        TemplateQuery clone = (TemplateQuery) this.clone();

        // Find the editable constraints in the query.
        Iterator iter = clone.getNodes().entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            PathNode node = (PathNode) entry.getValue();
            Iterator citer = node.getConstraints().iterator();
            while (citer.hasNext()) {
                Constraint c = (Constraint) citer.next();
                if (c.isEditable()) {
                    if (clone.constraintLogic != null) {
                        try {
                            clone.constraintLogic.removeVariable(c.getCode());
                        } catch (IllegalArgumentException e) {
                            // Logic expression is now empty
                            clone.constraintLogic = null;
                        }
                    }
                    citer.remove();
                }
            }
        }
        return clone;
    }


    /**
     * Return a List of all the Constraints of fields in this template query.
     *
     * @return a List of all the Constraints of fields in this template query
     */
    public List<Constraint> getAllEditableConstraints() {
        List<Constraint> ecs = new ArrayList<Constraint>();
        for (String path : nodes.keySet()) {
            ecs.addAll(getEditableConstraints(path));
        }
        return ecs;
    }

    /**
     * Get the template title.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Get the private comment for this template.
     * @return the description
     */
    public String getComment() {
        return comment;
    }

    /**
     * Get the nodes from the description, in order (eg. {Company.name}).
     *
     * @return the nodes
     */
    public List<PathNode> getEditableNodes() {
        List<PathNode> newEditableNodes = new ArrayList();
        Iterator nodeIter = nodes.values().iterator();
        while (nodeIter.hasNext()) {
            PathNode node = (PathNode) nodeIter.next();
            if (!(getEditableConstraints(node).isEmpty())) {
                newEditableNodes.add(node);
            }
        }
        return newEditableNodes;
    }

    /**
     * Get the query short name.
     *
     * @return the query identifier string
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the saved template query object.
     *
     * @param savedTemplateQuery the database object
     */
    public void setSavedTemplateQuery(SavedTemplateQuery savedTemplateQuery) {
        this.savedTemplateQuery = savedTemplateQuery;
    }

    /**
     * Gets the saved template query object.
     *
     * @return a SavedTemplateQuery object that represents this TemplateQuery in the userprofile
     * database
     */
    public SavedTemplateQuery getSavedTemplateQuery() {
        return savedTemplateQuery;
    }

    /**
     * Convert a template query to XML.
     *
     * @param version the version number of the XML format
     * @return this template query as XML.
     */
    public String toXml(int version) {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            TemplateQueryBinding.marshal(this, writer, version);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

        return sw.toString();
    }

    /**
     * Clone this TemplateQuery.
     *
     * @return a TemplateQuery
     */
    public PathQuery clone() {
        TemplateQuery templateQuery = new TemplateQuery(name, title, description, comment,
                                                        super.clone());
        templateQuery.edited = edited;
        return templateQuery;
    }

    /**
     * Return the PathQuery part of the TemplateQuery.
     *
     * @return a PathQuery
     */
    public PathQuery getPathQuery() {
        return super.clone();
    }

    /**
     * Returns true if the TemplateQuery has been edited by the user and is therefore saved only in
     * the query history.
     *
     * @return a boolean
     */
    public boolean isEdited() {
        return edited;
    }

    /**
     * Set the query as being edited.
     *
     * @param edited whether the TemplateQuery has been modified by the user
     */
    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        return (o instanceof TemplateQuery)
            && super.equals(o)
            && ((TemplateQuery) o).getName().equals(getName())
            && ((PathQuery) o).getDescription().equals(getDescription())
            && ((TemplateQuery) o).getTitle().equals(getTitle())
            && ((TemplateQuery) o).getComment().equals(getComment());
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return 13 * super.hashCode()
            + 3 * name.hashCode()
            + 5 * title.hashCode()
            + 7 * description.hashCode()
            + 11 * comment.hashCode();
    }
}
