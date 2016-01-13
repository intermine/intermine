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

/**
 * Object representing an OBO relation.
 *
 * @author Xavier Watkins
 */
public class OboRelation
{

    OboTypeDefinition relationship;
    String parentTermId;
    String childTermId;
    boolean direct;
    boolean redundant;

    /**
     * Constructor
     *
     * @param childTermId the id of the second term
     * @param parentTermId the id of the first term
     * @param relationship the relationship
     */
    public OboRelation(String childTermId, String parentTermId, OboTypeDefinition relationship) {
        this.parentTermId = parentTermId;
        this.childTermId = childTermId;
        this.relationship = relationship;
    }

    /**
     * @return the relationship
     */
    public OboTypeDefinition getRelationship() {
        return relationship;
    }

    /**
     * @param relationship the relationship to set
     */
    public void setRelationship(OboTypeDefinition relationship) {
        this.relationship = relationship;
    }

    /**
     * @return the parentTermId
     */
    public String getParentTermId() {
        return parentTermId;
    }

    /**
     * @param parentTermId the parentTermId to set
     */
    public void setParentTermId(String parentTermId) {
        this.parentTermId = parentTermId;
    }

    /**
     * @return the childTermId
     */
    public String getChildTermId() {
        return childTermId;
    }

    /**
     * @param childTermId the childTermId to set
     */
    public void setChildTermId(String childTermId) {
        this.childTermId = childTermId;
    }

    /**
     * @return the direct
     */
    public boolean isDirect() {
        return direct;
    }

    /**
     * @param direct the direct to set
     */
    public void setDirect(boolean direct) {
        this.direct = direct;
    }

    /**
     * @return the redundant
     */
    public boolean isRedundant() {
        return redundant;
    }

    /**
     * @param redundant the redundant to set
     */
    public void setRedundant(boolean redundant) {
        this.redundant = redundant;
    }
}
