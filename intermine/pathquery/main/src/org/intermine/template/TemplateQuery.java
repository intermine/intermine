package org.intermine.template;

/*
 * Copyright (C) 2002-2015 FlyMine
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
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.intermine.pathquery.LogicExpression;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathConstraintLoop;
import org.intermine.pathquery.PathConstraintSubclass;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.xml.TemplateQueryBinding;

/**
 * A template query, which consists of a PathQuery, description, category,
 * short name.
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
 * @author Alex Kalderimis
 */
public class TemplateQuery extends PathQuery
{
    /** Template query name. */
    protected String name;
    /** The private comment for this query. */
    protected String comment;
    /** Whether this is an edited version of another template. */
    protected boolean edited = false;

    /** List of those Constraints that are editable */
    protected List<PathConstraint> editableConstraints = new ArrayList<PathConstraint>();
    /** Descriptions of constraints */
    protected Map<PathConstraint, String> constraintDescriptions =
        new HashMap<PathConstraint, String>();
    /** Configuration for switch-off-ability of constraints */
    protected Map<PathConstraint, SwitchOffAbility> constraintSwitchOffAbility =
        new HashMap<PathConstraint, SwitchOffAbility>();

    /**
     * Construct a new instance of TemplateQuery.
     *
     * @param name the name of the template
     * @param title the short title of this template for showing in list
     * @param comment an optional private comment for this template
     * @param query the query itself
     */
    public TemplateQuery(String name, String title, String comment, PathQuery query) {
        super(query);
        this.name = name;
        this.comment = comment;
        setTitle(title);
    }

    /**
     * Copy constructor.
     * Construct a new template that is the same in all respects as the one
     * passed to the constructor.
     * @param prototype The template to copy.
     */
    public TemplateQuery(TemplateQuery prototype) {
        super(prototype);
        setTitle(prototype.getTitle());
        this.name = prototype.name;
        this.comment = prototype.comment;
        this.edited = prototype.edited;
        this.editableConstraints
            = new ArrayList<PathConstraint>(prototype.editableConstraints);
        this.constraintDescriptions
            = new HashMap<PathConstraint, String>(prototype.constraintDescriptions);
        this.constraintSwitchOffAbility =
            new HashMap<PathConstraint, SwitchOffAbility>(prototype.constraintSwitchOffAbility);
    }

    /**
     * Clone this TemplateQuery.
     *
     * @return a TemplateQuery
     */
    @Override
    public synchronized TemplateQuery clone() {
        super.clone();
        return new TemplateQuery(this);
    }

    /**
     * Fetch the PathQuery to execute for this template.  The returned query excludes any optional
     * constraints that have been sbitched off before the template is executed.
     * @return the PathQuery that should be executed for this query
     */
    @Override
    public PathQuery getQueryToExecute() {
        TemplateQuery queryToExecute = this.clone();
        for (PathConstraint con : queryToExecute.getEditableConstraints()) {
            if (SwitchOffAbility.OFF.equals(getSwitchOffAbility(con))) {
                queryToExecute.removeConstraint(con);
            }
        }
        return queryToExecute;
    }

    /**
     * Sets a constraint to be editable or not. If setting a constraint to be editable, and it is
     * not already editable, then the constraint will be added to the end of the editable
     * constraints list.
     *
     * @param constraint the PathConstraint to mark
     * @param editable whether the constraint should be editable
     * @throws NullPointerException if constraint is null
     * @throws NoSuchElementException if the constraint is not in the query
     */
    public synchronized void setEditable(PathConstraint constraint, boolean editable) {
        if (constraint == null) {
            throw new NullPointerException("Cannot set null constraint to be editable");
        }
        if (!getConstraints().containsKey(constraint)) {
            throw new NoSuchElementException("Constraint " + constraint + " is not in the query");
        }
        if (editable) {
            if (!editableConstraints.contains(constraint)) {
                editableConstraints.add(constraint);
            }
        } else {
            editableConstraints.remove(constraint);
        }
    }

    /**
     * Returns whether a constraint is editable.
     *
     * @param constraint the PathConstraint to check
     * @return true if the constraint is editable
     * @throws NullPointerException if constraint is null
     * @throws NoSuchElementException if constraint is not in the query at all
     */
    public synchronized boolean isEditable(PathConstraint constraint) {
        if (constraint == null) {
            throw new NullPointerException("Cannot fetch editable status of null constraint");
        }
        if (!getConstraints().containsKey(constraint)) {
            throw new NoSuchElementException("Constraint " + constraint + " is not in the query");
        }
        return editableConstraints.contains(constraint);
    }

    /**
     * Returns whether a constraint is optional. This is the logical inverse of isRequired()
     *
     * @param constraint the PathConstraint to check
     * @return true if the constraint is optional
     * @throws NullPointerException if the constraint is null
     * @throws NoSuchElementException if constraint is not in the query at all
     */
    public synchronized boolean isOptional(PathConstraint constraint) {
        return !isRequired(constraint);
    }

    /**
     * Returns whether a constraint is required. This is the logical inverse of isOptional()
     *
     * @param constraint the PathConstraint to check
     * @return true if the constraint is required
     * @throws NullPointerException if the constraint is null
     * @throws NoSuchElementException if constraint is not in the query at all
     */
    public synchronized boolean isRequired(PathConstraint constraint) {
        if (constraint == null) {
            throw new NullPointerException("Cannot fetch editable status of null constraint");
        }
        if (!getConstraints().containsKey(constraint)) {
            throw new NoSuchElementException("Constraint " + constraint + " is not in the query");
        }
        boolean isRequired = SwitchOffAbility.LOCKED.equals(getSwitchOffAbility(constraint));
        return isRequired;
    }

    /**
     * Sets the list of editable constraints to exactly that provided, in the given order.
     * Previously-editable constraints are discarded.
     *
     * @param editable a List of editable constraints to replace the existing list
     * @throws NoSuchElementException if the argument contains a constraint that is not in the query
     */
    public synchronized void setEditableConstraints(List<PathConstraint> editable) {
        for (PathConstraint constraint : editable) {
            if (!getConstraints().containsKey(constraint)) {
                throw new NoSuchElementException("Constraint " + constraint
                        + " is not in the query");
            }
        }
        sortConstraints(editable);
        editableConstraints = new ArrayList<PathConstraint>(editable);
    }

    /**
     * For a path with editable constraints, get all the editable constraints as a List, or the
     * empty list if there are no editable constraints on that path.
     *
     * @param path a String of a path
     * @return List of editable constraints for the path
     */
    public synchronized List<PathConstraint> getEditableConstraints(String path) {
        List<PathConstraint> ecs = new ArrayList<PathConstraint>();
        for (PathConstraint constraint : editableConstraints) {
            if (path.equals(constraint.getPath())) {
                ecs.add(constraint);
            }
        }
        return ecs;
    }

    /**
     * Returns the constraint SwitchOffAbility for this query. The return value of this method is an
     * unmodifiable copy of the data in this query, so it will not change to reflect changes in this
     * query.
     *
     * @return a Map of constraintSwitchOffAbility
     */
    public synchronized Map<PathConstraint, SwitchOffAbility> getConstraintSwitchOffAbility() {
        return Collections.unmodifiableMap(new HashMap<PathConstraint, SwitchOffAbility>(
                constraintSwitchOffAbility));
    }

    /**
     * Sets the description for a constraint. To remove a description, call this method with
     * a null description.
     *
     * @param constraint the constraint to attach the description to
     * @param description a String
     * @throws NullPointerException if the constraint is null
     * @throws NoSuchElementException if the constraint is not in the query
     */
    public synchronized void setConstraintDescription(PathConstraint constraint,
            String description) {
        if (constraint == null) {
            throw new NullPointerException("Cannot set description on null constraint");
        }
        if (!getConstraints().containsKey(constraint)) {
            throw new NoSuchElementException("Constraint " + constraint + " is not in the query");
        }
        if (description == null) {
            constraintDescriptions.remove(constraint);
        } else {
            constraintDescriptions.put(constraint, description);
        }
    }

    /**
     * Returns the description attached to the given constraint. Returns null if no description is
     * present.
     *
     * @param constraint the constraint to fetch the description of
     * @return a String description
     * @throws NullPointerException is the constraint is null
     * @throws NoSuchElementException if the constraint is not in the query
     */
    public synchronized String getConstraintDescription(PathConstraint constraint) {
        if (constraint == null) {
            throw new NullPointerException("Cannot set description on null constraint");
        }
        if (!getConstraints().containsKey(constraint)) {
            throw new NoSuchElementException("Constraint " + constraint + " is not in the query");
        }
        return constraintDescriptions.get(constraint);
    }

    /**
     * Returns the constraint descriptions for this query. The return value of this method is an
     * unmodifiable copy of the data in this query, so it will not change to reflect changes in this
     * query.
     *
     * @return a Map from PathConstraint to String description
     */
    public synchronized Map<PathConstraint, String> getConstraintDescriptions() {
        return Collections.unmodifiableMap(new HashMap<PathConstraint, String>(
                    constraintDescriptions));
    }

    /**
     * Sets the sbitch-off-ability of a constraint.
     *
     * @param constraint the constraint to set the switch-off-ability on
     * @param sbitchOffAbility a SwitchOffAbility instance
     * @throws NullPointerException if the constraint or sbitchOffAbility is null
     * @throws NoSuchElementException if the constraint is not in the query
     */
    public synchronized void setSwitchOffAbility(PathConstraint constraint,
            SwitchOffAbility sbitchOffAbility) {
        if (constraint == null) {
            throw new NullPointerException("Cannot set sbitch-off-ability on null constraint");
        }
        if (sbitchOffAbility == null) {
            throw new NullPointerException("Cannot set null sbitch-off-ability on constraint "
                    + constraint);
        }
        if (!getConstraints().containsKey(constraint)) {
            throw new NoSuchElementException("Constraint " + constraint + " is not in the query");
        }
        constraintSwitchOffAbility.put(constraint, sbitchOffAbility);
    }

    /**
     * Gets the sbitch-off-ability of a constraint.
     *
     * @param constraint the constraint to get the sbitch-off-ability for
     * @return a SwitchOffAbility instance
     * @throws NullPointerException is the constraint is null
     * @throws NoSuchElementException if the constraint is not in the query
     */
    public synchronized SwitchOffAbility getSwitchOffAbility(PathConstraint constraint) {
        if (constraint == null) {
            throw new NullPointerException("Cannot set sbitch-off-ability on null constraint");
        }
        if (!getConstraints().containsKey(constraint)) {
            throw new NoSuchElementException("Constraint " + constraint + " is not in the query");
        }
        SwitchOffAbility sbitchOffAbility = constraintSwitchOffAbility.get(constraint);
        if (sbitchOffAbility == null) {
            return SwitchOffAbility.LOCKED;
        }
        return sbitchOffAbility;
    }

    /**
     * Returns the list of all editable constraints. The return value of this method is an
     * unmodifiable copy of the data in this query, so it will not change to reflect changes in this
     * query.
     *
     * @return a List of PathConstraint
     */
    public synchronized List<PathConstraint> getEditableConstraints() {
        return Collections.unmodifiableList(new ArrayList<PathConstraint>(editableConstraints));
    }

    /**
     * Returns the list of all editable constraints. The return value of this method is a
     * copy of the data in this query, so it will not change to reflect changes in this
     * query. Please only use this method if you are going to resubmit the list to the
     * setEditableConstraints() method, as any changes made in this list will not be otherwise
     * copied to this TemplateQuery object. For other uses, use the getEditableConstraints() method,
     * which will prevent modification, which could catch a bug or two.
     *
     * @return a List of PathConstraint
     */
    public synchronized List<PathConstraint> getModifiableEditableConstraints() {
        return new ArrayList<PathConstraint>(editableConstraints);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void replaceConstraint(PathConstraint old, PathConstraint replacement) {
        super.replaceConstraint(old, replacement);
        if (editableConstraints.contains(old)) {
            if ((replacement instanceof PathConstraintSubclass)
                 || (replacement instanceof PathConstraintLoop)) {
                editableConstraints.remove(editableConstraints.indexOf(old));
            } else {
                editableConstraints.set(editableConstraints.indexOf(old), replacement);
            }
        }
        String description = constraintDescriptions.remove(old);
        if (description != null) {
            constraintDescriptions.put(replacement, description);
        }
        SwitchOffAbility sbitchOffAbility = constraintSwitchOffAbility.remove(old);
        if (sbitchOffAbility != null) {
            constraintSwitchOffAbility.put(replacement, sbitchOffAbility);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void removeConstraint(PathConstraint constraint) {
        super.removeConstraint(constraint);
        editableConstraints.remove(constraint);
        constraintDescriptions.remove(constraint);
        constraintSwitchOffAbility.remove(constraint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void clearConstraints() {
        editableConstraints.clear();
        constraintDescriptions.clear();
        constraintSwitchOffAbility.clear();
    }

    /**
     * Return a clone of this template query with all editable constraints
     * removed - i.e. a query that will return all possible results of executing
     * the template.  The original template is left unaltered.
     *
     * @return a clone of the original template without editable constraints.
     */
    public TemplateQuery cloneWithoutEditableConstraints() {
        TemplateQuery clone = clone();

        List<PathConstraint> editable = new ArrayList<PathConstraint>(clone.editableConstraints);
        for (PathConstraint constraint : editable) {
            clone.removeConstraint(constraint);
        }
        return clone;
    }

    /**
     * Get the private comment for this template.
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @return the template's title, or its name if it has no title.
     */
    @Override
    public String getTitle() {
        String title = super.getTitle();
        if (title == null || "".equals(title)) {
            return getName();
        }
        return title;
    }

    /**
     * Get the paths of all editable constraints in this template.
     *
     * @return the nodes
     */
    public synchronized List<String> getEditablePaths() {
        List<String> editablePaths = new ArrayList<String>();
        for (PathConstraint constraint : editableConstraints) {
            if (!editablePaths.contains(constraint.getPath())) {
                editablePaths.add(constraint.getPath());
            }
        }
        return editablePaths;
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
     * Sets the query short name.
     *
     * @param name the template name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set the private comment for this template.
     * @param comment the comment
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Convert a template query to XML.
     *
     * @param version the version number of the XML format
     * @return this template query as XML.
     */
    @Override
    public synchronized String toXml(int version) {
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

    @Override
    public synchronized String toString() {
        String res = super.toString();
        res = getClass().getName() + "{ name: "  + getName() + ", title: " + getTitle()
                + ", comment: " + getComment() + ", description: " + getDescription()
                +  ", " + res + "}";
        return res;
    }

    @Override
    public synchronized Map<PathConstraint, String> getRelevantConstraints() {
        // Only switched-on constraints are relevant.
        Map<PathConstraint, String> retVal
            = new LinkedHashMap<PathConstraint, String>(getConstraints());
        for (PathConstraint con: getConstraints().keySet()) {
            if (getSwitchOffAbility(con) == SwitchOffAbility.OFF) {
                retVal.remove(con);
            }
        }
        return retVal;
    }

    @Override
    protected Map<String, Object> getHeadAttributes() {
        Map<String, Object> retVal = super.getHeadAttributes();
        retVal.put("name", getName());
        retVal.put("comment", getComment());

        return retVal;
    }

    @Override
    protected String getCommonJsonConstraintPrefix(String code, PathConstraint con) {
        StringBuilder sb = new StringBuilder(super.getCommonJsonConstraintPrefix(code, con));
        SwitchOffAbility soa = getSwitchOffAbility(con);
        sb.append(String.format(",\"editable\":%b",     isEditable(con)));
        sb.append(String.format(",\"switchable\":%b",   soa != SwitchOffAbility.LOCKED));
        sb.append(String.format(",\"switched\":\"%s\"", soa));
        return sb.toString();
    }

    /**
     * Returns a JSON string representation of the template query.
     * TODO: !! fix confusion between toJson and toJSON !!
     * @return A string representation of the template query.
     */
    public synchronized String toJSON() {
        return this.toJson();
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

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof TemplateQuery) {
            return ((TemplateQuery) other).toXml().equals(this.toXml());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.toXml().hashCode();
    }

    /**
     * Verify templates don't contain non-editable lookup constraints
     * @return true id the template is valid
     */
    public boolean validateLookupConstraints() {
        Map<PathConstraint, String> pathConstraints = getConstraints();
        for (PathConstraint constraint : pathConstraints.keySet()) {
            if (constraint instanceof PathConstraintLookup
                && !editableConstraints.contains(constraint)) {
                return false;
            }
        }
        return true;
    }

    /**
    * Return the constraint logic modified to contain only the blocks with editable constraints
    * @return the string representing the logic expression modified
    */
    public synchronized String getConstraintLogicForEditableConstraints() {
        List<String> editableConstraintCodes = new ArrayList<String>();
        Map<PathConstraint, String> allConstraints = getConstraints();
        for (PathConstraint pc : editableConstraints) {
            editableConstraintCodes.add(allConstraints.get(pc));
        }
        LogicExpression logicExpression = getLogicExpression();
        if (logicExpression == null) {
            return "";
        }
        return getLogicExpression().getPartialString(editableConstraintCodes);
    }
}
