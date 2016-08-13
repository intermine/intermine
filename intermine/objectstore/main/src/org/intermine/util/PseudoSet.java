package org.intermine.util;

/*
 * Copyright (C) 2002-2016 FlyMine
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
 * A Set that does not actually contain objects, but provides useful results for the contains
 * method.
 *
 * @author Matthew Wakeling
 * @param <E> The element type
 */
public abstract class PseudoSet<E> implements Set<E>
{
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(@SuppressWarnings("unused") E o) {
        throw new UnsupportedOperationException("Immutable virtual PseudoSet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(@SuppressWarnings("unused") Collection<? extends E> c) {
        throw new UnsupportedOperationException("Immutable virtual PseudoSet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException("Immutable virtual PseudoSet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException("Immutable virtual PseudoSet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(@SuppressWarnings("unused") Object o) {
        throw new UnsupportedOperationException("Immutable virtual PseudoSet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeAll(@SuppressWarnings("unused") Collection<?> c) {
        throw new UnsupportedOperationException("Immutable virtual PseudoSet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean retainAll(@SuppressWarnings("unused") Collection<?> c) {
        throw new UnsupportedOperationException("Immutable virtual PseudoSet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        throw new UnsupportedOperationException("Immutable virtual PseudoSet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("Immutable virtual PseudoSet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T[] toArray(@SuppressWarnings("unused") T[] o) {
        throw new UnsupportedOperationException("Immutable virtual PseudoSet");
    }
}
