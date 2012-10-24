package org.intermine.modelviewer.model;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Serializable;

/**
 * Class to define a relationship between one model class and another.
 */
public class ForeignKey implements Serializable
{
    private static final long serialVersionUID = 6506173701704455734L;
    
    /**
     * The name of this relationship.
     * @serial
     */
    protected String name;
    
    /**
     * The model class on the other end of the relationship.
     * @serial
     */
    protected ModelClass referencedType;
    
    /**
     * The name of the reverse attribute on the target class.
     * @serial
     */
    protected String reverseReference;
    
    /**
     * The marker tag for this foreign key's origin. 
     * @serial
     */
    protected String tag;
    
    
    /**
     * Initialise with a name but no tag.
     * 
     * @param name The foreign key name.
     */
    public ForeignKey(String name) {
        this(name, null);
    }

    /**
     * Initialise with a name and marker tag.
     * 
     * @param name The foreign key name.
     * @param tag The marker tag.
     */
    public ForeignKey(String name, String tag) {
        this.name = name;
        this.tag = tag;
    }

    /**
     * Get the model class at the other end of the relationship.
     *
     * @return The target model class.
     */
    public ModelClass getReferencedType() {
        return referencedType;
    }

    /**
     * Set the model class at the other end of the relationship.
     * 
     * @param referencedType The target model class.
     */
    public void setReferencedType(ModelClass referencedType) {
        this.referencedType = referencedType;
    }

    /**
     * Get the name of the reverse relationship on the target class back
     * to this class.
     * 
     * @return The name of the reverse relationship, or <code>null</code>
     * if the relationship is unidirectional.
     */
    public String getReverseReference() {
        return reverseReference;
    }

    /**
     * Set the name of the reverse relationship on the target class.
     * 
     * @param reverseReference The name of the reverse relationship,
     * or <code>null</code> if the relationship is unidirectional.
     */
    public void setReverseReference(String reverseReference) {
        this.reverseReference = reverseReference;
    }

    /**
     * Get the name of this foreign key.
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the marker tag for the source of this foreign key.
     * @return The tag.
     */
    public String getTag() {
        return tag;
    }

    /**
     * Create a human-readable representation of this foreign key.
     * @return A printable String.
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("ForeignKey[name=").append(name).append(",referencedType=");
        b.append(referencedType == null ? "null" : referencedType.getName());
        if (reverseReference != null) {
            b.append(",reverseReference=").append(reverseReference);
        }
        if (tag != null) {
            b.append(",tag=").append(tag);
        }
        b.append(']');
        return b.toString();
    }
}
