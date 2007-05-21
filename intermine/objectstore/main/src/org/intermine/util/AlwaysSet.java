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

import java.util.Collection;

/**
 * A Set that always returns true for the contains method.
 *
 * @author Matthew Wakeling
 */
public class AlwaysSet extends PseudoSet
{
    /**
     * public instance
     */
    public static final AlwaysSet INSTANCE = new AlwaysSet();

    private AlwaysSet() {
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Object o) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsAll(Collection c) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "AlwaysSet";
    }
}
