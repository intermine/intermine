package org.intermine.bio.ontology;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Extension of DagTerm that adds a namespace attribute that is specific to the OBO
 * format.
 *
 * @author Thomas Riley
 * @author Xavier Watkins
 */
public class OboTerm
{
    private final String id;
    private String name;
    private Set<OboTermSynonym> synonyms = new LinkedHashSet<OboTermSynonym>();
    private Set<OboTerm> xrefs = new LinkedHashSet<OboTerm>();
    private String namespace = "";
    private String description = "";
    private boolean obsolete = false;
    private Map<?, ?> tagValues;

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
     * Construct with an id, only used for uberon xrefs
     * @param id the id of this DAG term, may not be changed after construction
     */
    public OboTerm(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id argument may not be null");
        }
        this.id = id;
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
     * Add a xref for this term.
     * @param xref the xref for this term
     */
    public void addXref(OboTerm xref) {
        this.xrefs.add(xref);
    }

    /**
     * Get a set of xrefs (OboTerms) for this term.
     * @return a set of xrefs
     */
    public Set<OboTerm> getXrefs() {
        return this.xrefs;
    }

    /**
     * Create a string representation of the term.
     * @return a string representation of the term
     */
    @Override
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
     * Sets the variable holding the raw tagValues that created this Term.
     *
     * @param tagValues the values
     */
    protected void setTagValues(Map<?, ?> tagValues) {
        this.tagValues = tagValues;
    }

    /**
     * Gets the tagValues.
     *
     * @return the tagValues
     */
    protected Map<?, ?> getTagValues() {
        return tagValues;
    }
}
