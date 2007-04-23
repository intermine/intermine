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
import java.util.Iterator;
import java.util.Set;

/**
 * A Set that always returns true for the contains method.
 *
 * @author Matthew Wakeling
 */
public class AlwaysSet implements Set
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
    public boolean add(Object o) {
        throw new UnsupportedOperationException("Immutable virtual AlwaysSet");
    }

    /**
     * {@inheritDoc}
     */
    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException("Immutable virtual AlwaysSet");
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        throw new UnsupportedOperationException("Immutable virtual AlwaysSet");
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
    public boolean equals(Object o) {
        return o == this;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return 8732342;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator iterator() {
        throw new UnsupportedOperationException("Immutable virtual AlwaysSet");
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Immutable virtual AlwaysSet");
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException("Immutable virtual AlwaysSet");
    }

    /**
     * {@inheritDoc}
     */
    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException("Immutable virtual AlwaysSet");
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        throw new UnsupportedOperationException("Immutable virtual AlwaysSet");
    }

    /**
     * {@inheritDoc}
     */
    public Object[] toArray() {
        throw new UnsupportedOperationException("Immutable virtual AlwaysSet");
    }

    /**
     * {@inheritDoc}
     */
    public Object[] toArray(Object o[]) {
        throw new UnsupportedOperationException("Immutable virtual AlwaysSet");
    }
}
