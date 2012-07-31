package org.intermine.util;

/*
 * Copyright (C) 2002-2012 FlyMine
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
 * @param <E> The element type
 */
public final class AlwaysSet<E> extends PseudoSet<E>
{
    private static final AlwaysSet<Object> INSTANCE = new AlwaysSet<Object>();

    /**
     * Returns a new AlwaysSet with the correct type parameters.
     *
     * @return a Set that contains everything
     * @param <T> The type of the Set
     */
    @SuppressWarnings("unchecked")
    public static <T> AlwaysSet<T> getInstance() {
        return (AlwaysSet<T>) INSTANCE;
    }

    private AlwaysSet() {
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(@SuppressWarnings("unused") Object o) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsAll(@SuppressWarnings("unused") Collection<?> c) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "AlwaysSet";
    }
}
