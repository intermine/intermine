package org.intermine.bio.ontology;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Extension of DagTerm that adds a namespace attribute that is specific to the OBO
 * format.
 *
 * @author Thomas Riley
 */
public class OboTerm
{
    private final String id;
    private String name;
    private Set<OboTerm> children = new HashSet<OboTerm>();
    private Set<OboTermSynonym> synonyms = new LinkedHashSet<OboTermSynonym>();
    private Set<OboTerm> components = new HashSet<OboTerm>();
    private String namespace = "";
    private String description = "";
    private boolean obsolete = false;
    private Set allParentIds = null;
    private Map tagValues;

    /**
     * Construct with an id and name.
     * @param id the id of this DAG term, may not be changed after construction
     * @param name a name for this DAG term
     */
    public OboTerm(String id, String name) {
        if (id == null || name == null) {
            throw new IllegalArgumentException("id and name arguments may not be null");
        }
        this.id = id;
        this.name = name;
    }

    /**
     * Get the id of this term.
     * @return the id
     */
    public String getId() {
        return this.id;
    }

    /**
     * Get the name of this term.
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Add a child DagTerm to this term (isa relationship).
     * @param child the child term
     */
    public void addChild(OboTerm child) {
        this.children.add(child);
    }

    /**
     * Get a set of direct child DagTerms of this term.
     * @return set of direct child DagTerms
     */
    public Set<OboTerm> getChildren() {
        return this.children;
    }

    /**
     * Add a component DagTerm to this term (partof relationship).
     * @param component the component term
     */
    public void addComponent(OboTerm component) {
        this.components.add(component);
    }

    /**
     * Get a set of direct component DagTerms of this term.
     * @return set of direct component DagTerms
     */
    public Set<OboTerm> getComponents() {
        return this.components;
    }

    /**
     * Add a synonym for this term.
     * @param synonym the synonym for this term
     */
    public void addSynonym(OboTermSynonym synonym) {
        this.synonyms.add(synonym);
    }

    /**
     * Get a set of synonyms (Strings) for this term.
     * @return a set of synonyms
     */
    public Set<OboTermSynonym> getSynonyms() {
        return this.synonyms;
    }

    /**
     * Create a string representation of the term.
     * @return a string representation of the term
     */
    public String toString() {
        return id + ", " + name;
    }

    /**
     * Get the namespace attribute.
     * @return term namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Set the namespace attribute.
     * @param namespace the term namespace
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Get the term description.
     * @return the description for this term
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the term description.
     * @param description the term description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Set the obsolete flag for this OboTerm as read from an OBO file.
     * @param obsolete the flag
     */
    public void setObsolete(boolean obsolete) {
        this.obsolete = obsolete;
    }

    /**
     * Return the obsolete flag for this term.
     * @return the obsolete flag
     */
    public boolean isObsolete() {
        return obsolete;
    }

    /**
     * Get the terms parents as a set.
     * @return a set of all the parents of this term.
     * */
    public Set getAllParentIds() {
        return allParentIds;
    }

    /**
     * Adds more parent ids to the set of parent ids for this item.
     * @param parentIds A collection of some parent go term ids to add to the set of parent ids
     * */
    protected void addToAllParentIds(Collection parentIds) {
        if (allParentIds == null) {
            allParentIds = new HashSet();
        }
        allParentIds.addAll(parentIds);
    }

    /**
     * Sets the variable holding the raw tagValues that created this Term.
     *
     * @param tagValues the values
     */
    protected void setTagValues(Map tagValues) {
        this.tagValues = tagValues;
    }

    /**
     * Gets the tagValues.
     *
     * @return the tagValues
     */
    protected Map getTagValues() {
        return tagValues;
    }
}
