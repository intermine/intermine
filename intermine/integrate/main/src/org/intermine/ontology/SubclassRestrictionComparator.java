package org.intermine.ontology;

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
 * Compare two SubclassRestrictions by number of restrictions, more
 * restrictions comes first.  Used to ensure most detailed restrictions
 * are examied first, others may be a subset of this.
 *
 * @author Richard Smith
 */

public class SubclassRestrictionComparator implements Comparator
{
    /**
     * Compare two SubclassRestrictions by number of restrictions, more
     * restrictions comes first.
     * @param a an object to compare
     * @param b an object to compare
     * @return integer result of comparason
     */
    public int compare(Object a, Object b) {
        if (a instanceof SubclassRestriction && b instanceof SubclassRestriction) {
            return compare ((SubclassRestriction) a, (SubclassRestriction) b);
        } else {
            throw new IllegalArgumentException("Cannot compare: " + a.getClass().getName()
                                               + " and " + b.getClass().getName());
        }
    }

    // reverse natural order
    private int compare(SubclassRestriction a, SubclassRestriction b) {
        return -(new Integer(a.getRestrictions().size())
                 .compareTo(new Integer(b.getRestrictions().size())));
    }
}
