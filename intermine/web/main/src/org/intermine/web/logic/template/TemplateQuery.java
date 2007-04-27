package org.intermine.web.logic.template;

/*
 * Copyright (C) 2002-2007 FlyMine
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

import org.apache.log4j.Logger;

import org.intermine.model.userprofile.SavedTemplateQuery;
import org.intermine.model.userprofile.TemplateSummary;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.web.logic.query.Constraint;
import org.intermine.web.logic.query.PathNode;
import org.intermine.web.logic.query.PathQuery;

import net.sourceforge.iharder.Base64;

/**
 * A template query, which consists of a PathQuery, description, category,
 * short name.
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
 */
public class TemplateQuery extends PathQuery
{
    private static final Logger LOG = Logger.getLogger(TemplateQuery.class);

    /** Template query name. */
    protected String name;
    /** Template query title. */
    protected String title;
    /** Template query description. */
    protected String description;
    /** The private comment for this query. */
    protected String comment;
    /** Map from node to editable constraint list. */
    protected Map constraints = new HashMap();
    /** Keywords set for this template. */
    protected String keywords = "";
    /** Edited version of another template. */
    protected boolean edited = false;
    /** Map from editable constraint path to Lists of possible values. */
    protected HashMap possibleValues = new HashMap();
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
     * @param keywords keywords for this template
     */
    public TemplateQuery(String name, String title, String description, String comment,
                         PathQuery query, String keywords) {
        super((PathQuery) query.clone());
        if (description != null) {
            this.description = description;
        }
        if (name != null) {
            this.name = name;
        }
        if (keywords != null) {
            this.keywords = keywords;
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
    public List getEditableConstraints(PathNode node) {
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
    public List getEditableConstraints(String path) {
        if (nodes.get(path) == null) {
            return Collections.EMPTY_LIST;
        } else {
            List ecs = new ArrayList();
            Iterator cIter = nodes.get(path).getConstraints().iterator();
            while (cIter.hasNext()) {
                Constraint c = (Constraint) cIter.next();
                if (c.isEditable()) {
                    ecs.add(c);
                }
            }
            return ecs;
        }
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
                        clone.constraintLogic.removeVariable(c.getCode());
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
    public List getAllEditableConstraints() {
        List ecs = new ArrayList();
        Iterator nodeIter = nodes.keySet().iterator();
        while (nodeIter.hasNext()) {
            ecs.addAll(getEditableConstraints((String) nodeIter.next()));
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
     * Get the template description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
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
    public List getEditableNodes() {
        List newEditableNodes = new ArrayList();
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
     * Get the keywords.
     *
     * @return template keywords
     */
    public String getKeywords() {
        return keywords;
    }

    /**
     * Returns a List of possible values for a node.
     *
     * @param node a PathNode
     * @return a List, or null if possible values have not been computed
     */
    public List getPossibleValues(PathNode node) {
        return (List) possibleValues.get(node.getPathString());
    }

    /**
     * Returns the entire possibleValues Map.
     *
     * @return possibleValues
     */
    public Map getPossibleValues() {
        return possibleValues;
    }

    /**
     * Sets the possibleValues Map.
     *
     * @param possibleValues a HashMap
     */
    public void setPossibleValues(HashMap possibleValues) {
        this.possibleValues = possibleValues;
    }

    /**
     * Returns true if there is any possibleValues data at all.
     *
     * @return a boolean
     */
    public boolean isSummarised() {
        return !possibleValues.isEmpty();
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
     * Populates the possibleValues data for this TemplateQuery from the os.
     *
     * @param os the production ObjectStore
     * @param osw the user profile ObjectStoreWriter
     * @throws ObjectStoreException if something goes wrong
     */
    public void summarise(ObjectStore os, ObjectStoreWriter osw) throws ObjectStoreException {
        Iterator iter = getEditableNodes().iterator();
        while (iter.hasNext()) {
            PathNode node = (PathNode) iter.next();
            Query q = TemplateHelper.getPrecomputeQuery(this, null, node);
            LOG.error("Running query: " + q);
            List results = os.execute(q, 0, 20, true, false, os.getSequence());
            if (results.size() < 20) {
                List values = new ArrayList();
                Iterator resIter = results.iterator();
                while (resIter.hasNext()) {
                    values.add(((List) resIter.next()).get(0));
                }
                possibleValues.put(node.getPathString(), values);
            }
        }
        LOG.error("New summary: " + possibleValues);
        // Now write the summary to the user profile database.
        try {
            osw.beginTransaction();
            if (savedTemplateQuery != null) {
                Query q = new Query();
                QueryClass qc = new QueryClass(TemplateSummary.class);
                q.addFrom(qc);
                q.addToSelect(qc);
                q.setConstraint(new ContainsConstraint(new QueryObjectReference(qc, "template"),
                            ConstraintOp.CONTAINS, savedTemplateQuery));
                Iterator oldIter = new SingletonResults(q, osw.getObjectStore(),
                        osw.getObjectStore().getSequence()).iterator();
                while (oldIter.hasNext()) {
                    osw.delete((TemplateSummary) oldIter.next());
                }
            }
            TemplateSummary templateSummary = new TemplateSummary();
            templateSummary.setTemplate(savedTemplateQuery);
            templateSummary.setSummary(Base64.encodeObject(possibleValues));
            osw.store(templateSummary);
        } catch (ObjectStoreException e) {
            if (osw.isInTransaction()) {
                osw.abortTransaction();
            }
            throw e;
        } finally {
            if (osw.isInTransaction()) {
                osw.commitTransaction();
            }
        }
    }

    /**
     * Convert a template query to XML.
     *
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

    /**
     * Clone this TemplateQuery.
     *
     * @return a TemplateQuery
     */
    public PathQuery clone() {
        TemplateQuery templateQuery = new TemplateQuery(name, title, description, comment,
                                                        super.clone(), keywords);
        templateQuery.edited = edited;
        return templateQuery;
    }

    /**
     * Return the PathQuery part of the TemplateQuery.
     *
     * @return a PathQuery
     */
    public PathQuery getPathQuery() {
        return (PathQuery) super.clone();
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
}
