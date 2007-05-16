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
 * A Set that does not actually contain objects, but provides useful results for the contains
 * method.
 *
 * @author Matthew Wakeling
 */
public abstract class PseudoSet implements Set
{
    /**
     * {@inheritDoc}
     */
    public boolean add(Object o) {
        throw new UnsupportedOperationException("Immutable virtual PseudoSet");
    }

    /**
     * {@inheritDoc}
     */
    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException("Immutable virtual PseudoSet");
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        throw new UnsupportedOperationException("Immutable virtual PseudoSet");
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsAll(Collection c) {
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
    public boolean isEmpty() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator iterator() {
        throw new UnsupportedOperationException("Immutable virtual PseudoSet");
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Immutable virtual PseudoSet");
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException("Immutable virtual PseudoSet");
    }

    /**
     * {@inheritDoc}
     */
    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException("Immutable virtual PseudoSet");
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        throw new UnsupportedOperationException("Immutable virtual PseudoSet");
    }

    /**
     * {@inheritDoc}
     */
    public Object[] toArray() {
        throw new UnsupportedOperationException("Immutable virtual PseudoSet");
    }

    /**
     * {@inheritDoc}
     */
    public Object[] toArray(Object o[]) {
        throw new UnsupportedOperationException("Immutable virtual PseudoSet");
    }
}
