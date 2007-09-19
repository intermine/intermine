package org.intermine.bio.ontology;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Extension of DagTerm that adds a namespace attribute that is specific to the OBO
 * format.
 *
 * @author Thomas Riley
 */
public class OboTerm extends DagTerm
{
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
        super(id, name);
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
