package org.intermine.util;

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
 * A Comparator that uses the natural ordering of elements implementing Comparable, plus null is
 * allowed and always before all other elements. It also copes with Boolean values, placing them
 * after null but before other values, TRUE before FALSE.
 *
 * @author Matthew Wakeling
 */
public class NullFirstComparator implements Comparator
{
    /** Publically-accessible instance */
    public static final NullFirstComparator SINGLETON = new NullFirstComparator();

    private NullFirstComparator() {
    }

    /**
     * {@inheritDoc}
     */
    public int compare(Object o1, Object o2) {
        if (o1 == null) {
            if (o2 == null) {
                return 0;
            }
            return -1;
        }
        if (o2 == null) {
            return 1;
        }
        if (o1 instanceof Boolean) {
            if (o2 instanceof Boolean) {
                if (Boolean.TRUE.equals(o1)) {
                    if (Boolean.TRUE.equals(o2)) {
                        return 0;
                    }
                    return -1;
                }
                if (Boolean.TRUE.equals(o2)) {
                    return 1;
                }
                return 0;
            }
            return -1;
        }
        if (o2 instanceof Boolean) {
            return 1;
        }
        return ((Comparable) o1).compareTo(o2);
    }
}
