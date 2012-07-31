package org.intermine.modelviewer.project;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.text.Collator;
import java.util.Comparator;

import org.apache.commons.collections15.comparators.NullComparator;

/**
 * Comparator for sorting lists of Source objects by name.
 */
public class SourceNameComparator implements Comparator<Source>
{
    /**
     * Shared-use instance of PropertyNameComparator.
     */
    public static final Comparator<Source> INSTANCE = new SourceNameComparator();
    
    /**
     * Null-safe comparator of Strings, using the default Collator.
     * @see Collator
     */
    private NullComparator<Object> nameComparator =
        new NullComparator<Object>(Collator.getInstance(), false);

    /**
     * Compare the two Source objects for ordering by name.
     * 
     * @param o1 The first Source.
     * @param o2 The second Source.
     * 
     * @return A negative integer, zero, or a positive integer as the
     *         first argument is less than, equal to, or greater than the
     *         second.
     */
    @Override
    public int compare(Source o1, Source o2) {
        return nameComparator.compare(o1.getName(), o2.getName());
    }

}
