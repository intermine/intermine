package org.intermine.metadata;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.util.Util;

/**
 * Describes a field that references a collection of other objects.
 * getReverseReferenceDescriptor()
 * allows one ot work out the multiplicity of the association's other end.
 *
 * @author Richard Smith
 */

public class CollectionDescriptor extends ReferenceDescriptor
{
    /**
     * Construct a CollectionDescriptor.  name and referencedType may not be null.
     * @param name name of this field in parent class
     * @param referencedType the fully qualified name of the business object type in this collection
     * @param reverseRefName name of field in the referenced class that points back to this class
     *                       (may be null)
     * @throws IllegalArgumentException if arguments are null
     */
    public CollectionDescriptor(String name, String referencedType,
                                   String reverseRefName) {
        // should define type of collection properly somehow
        super(name, referencedType, reverseRefName);
    }

    /**
     * {@inheritDoc}
     */
    public int relationType() {
        ReferenceDescriptor rd = getReverseReferenceDescriptor();
        if (rd == null || rd instanceof CollectionDescriptor) {
            return M_N_RELATION;
        } else {
            return ONE_N_RELATION;
        } 
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (obj instanceof CollectionDescriptor) {
            CollectionDescriptor ref = (CollectionDescriptor) obj;
            return (cld == null || cld.getName().equals(ref.cld.getName()))
                && name.equals(ref.name)
                && referencedType.equals(ref.referencedType)
                && Util.equals(reverseRefName, ref.reverseRefName);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return 2 * (cld == null ? 0 : cld.getName().hashCode())
            + 3 * name.hashCode()
            + 7 * referencedType.hashCode()
            + 11 * Util.hashCode(reverseRefName);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("<collection name=\"" + name + "\" referenced-type=\"" + referencedType + "\"")
            .append(reverseRefName != null ? " reverse-reference=\"" + reverseRefName + "\"" : "")
            .append("/>");
        return sb.toString();
    }
}
