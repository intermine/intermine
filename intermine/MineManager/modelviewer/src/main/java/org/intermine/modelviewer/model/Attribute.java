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
 * Class modelling an attribute of a genomic model class.
 */
public class Attribute implements Serializable
{
    private static final long serialVersionUID = 7402749130736027383L;

    /**
     * The name of the attribute.
     * @serial
     */
    protected String name;

    /**
     * The Java class of the attribute. 
     * @serial
     */
    protected String type;
    
    /**
     * The marker tag for this attribute's origin. 
     * @serial
     */
    protected String tag;
    
    
    /**
     * Initialise with a name and type but no tag.
     * 
     * @param name The attribute name.
     * @param type The attribute's Java class.
     */
    public Attribute(String name, String type) {
        this(name, type, null);
    }

    /**
     * Initialise with a name, type and marker tag.
     * 
     * @param name The attribute name.
     * @param type The attribute's Java class.
     * @param tag The marker tag.
     */
    public Attribute(String name, String type, String tag) {
        this.name = name;
        this.type = type;
        this.tag = tag;
    }

    /**
     * Get the name of this attribute.
     * @return The attribute name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the type of this attribute.
     * @return The Java class name.
     */
    public String getType() {
        return type;
    }

    /**
     * Get the marker tag for the source of this attribute.
     * @return The tag.
     */
    public String getTag() {
        return tag;
    }

    /**
     * Create a human-readable representation of this attribute.
     * @return A printable String.
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Attribute[name=").append(name).append(",type=").append(type);
        if (tag != null) {
            b.append(",tag=").append(tag);
        }
        b.append(']');
        return b.toString();
    }
}
