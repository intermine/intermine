package org.intermine.xml.full;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Comparator;

/**
 * An implementation of Comparator to order pairs of Class objects.
 *
 * @author Kim Rutherford
 */

public class RendererComparator implements Comparator
{
    /**
     * Compare two Class objects by name.
     * @param a an object to compare
     * @param b an object to compare
     * @return integer result of comparason
     */
    public int compare(Object a, Object b) {
        if (a instanceof Class && b instanceof Class) {
            return compare((Class) a, (Class) b);
        } else if (a instanceof Item && b instanceof Item) {
            return compare((Item) a, (Item) b);
        } else if (a instanceof Attribute && b instanceof Attribute) {
            return compare((Attribute) a, (Attribute) b);
        } else if (a instanceof Reference && b instanceof Reference) {
            return compare((Reference) a, (Reference) b);
        } else if (a instanceof ReferenceList && b instanceof ReferenceList) {
            return compare((ReferenceList) a, (ReferenceList) b);
        } else {
            throw new IllegalArgumentException("Cannot compare: " + a.getClass().getName()
                                               + " and " + b.getClass().getName());
        }
    }

    private int compare(Class a, Class b) {
        return a.getName().compareTo(b.getName());
    }

    private int compare(Item a, Item b) {
        return a.getIdentifier().compareTo(b.getIdentifier());
    }

    private int compare(Attribute a, Attribute b) {
        return a.getName().compareTo(b.getName());
    }

    private int compare(Reference a, Reference b) {
        return a.getName().compareTo(b.getName());
    }

    private int compare(ReferenceList a, ReferenceList b) {
        return a.getName().compareTo(b.getName());
    }
}
