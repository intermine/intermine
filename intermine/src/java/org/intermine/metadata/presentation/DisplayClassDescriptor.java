package org.flymine.metadata.presentation;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.flymine.metadata.ClassDescriptor;
import org.flymine.util.TypeUtil;

import java.util.Set;

/**
 *
 * @author Richard Smith
 */
public class DisplayClassDescriptor
{
    protected ClassDescriptor cld;

    /**
     * Construct with a ClassDescriptor.
     *
     * @param cld a ClassDescriptor
     */
    public DisplayClassDescriptor(ClassDescriptor cld) {
        this.cld = cld;
    }

    /**
     * Return the ClassDescriptor
     *
     * @return the ClassDescriptor
     */
    public ClassDescriptor getClassDescriptor() {
        return this.cld;
    }

    /**
     * Get the unqualified name of the described class.
     *
     * @return the unqualified class name
     */
    public String getUnqualifiedName() {
        return TypeUtil.unqualifiedName(cld.getName());
    }

    /**
     * Get list of AttributeDescriptors for this class
     *
     * @return the AttributeDescriptors
     */
    public Set getAttributeDescriptors() {
        return cld.getAttributeDescriptors();
    }

    /**
     * Get list of ReferenceDescriptors for this class
     *
     * @return the ReferenceDescriptors
     */
    public Set getReferenceDescriptors() {
        return cld.getReferenceDescriptors();
    }

    /**
     * Get list of CollectionDescriptors for this class
     *
     * @return the CollectionDescriptors
     */
    public Set getCollectionDescriptors() {
        return cld.getCollectionDescriptors();
    }

}
