package org.intermine.api.lucene;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;

import org.intermine.metadata.AttributeDescriptor;

/**
 * container class to cache class attributes
 * @author nils
 */
class ClassAttributes
{
    String className;
    Set<AttributeDescriptor> attributes;

    /**
     * constructor
     * @param className
     *            name of the class
     * @param attributes
     *            set of attributes for the class
     */
    public ClassAttributes(String className, Set<AttributeDescriptor> attributes) {
        super();
        this.className = className;
        this.attributes = attributes;
    }

    /**
     * name of the class
     * @return name of the class
     */
    public String getClassName() {
        return className;
    }

    /**
     * attributes associated with the class
     * @return attributes associated with the class
     */
    public Set<AttributeDescriptor> getAttributes() {
        return attributes;
    }
}
