package org.intermine.common.util;

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
import java.text.Collator;
import java.util.Comparator;


/**
 * Comparator for sorting collections of enums by their name.
 *
 * @param <T> The enum type.
 */
public class EnumByNameComparator<T extends Enum<T>> implements Comparator<T>, Serializable
{
    private static final long serialVersionUID = -4892365822461020775L;

    /**
     * Flag indicating whether the comparison should be case sensitive.
     * @serial
     */
    private boolean caseSensitive;

    /**
     * Initialise as a case sensitive comparator.
     */
    public EnumByNameComparator() {
        this(true);
    }

    /**
     * Initialise with the given state for case comparison.
     * 
     * @param caseSensitive Whether to compare in a case sensitive manner or not.
     */
    public EnumByNameComparator(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    /**
     * Compares the two enumeration values by their names.
     * 
     * @param o1 The first value.
     * @param o2 The second value.
     * 
     * @return A value &lt;0, =0 or &gt;0 if <code>o1</code> should be before,
     * the same, or after <code>o2</code>.
     */
    @Override
    public int compare(T o1, T o2) {
        return caseSensitive
            ? Collator.getInstance().compare(o1.toString(), o2.toString())
            : o1.toString().compareTo(o2.toString());
    }

}
