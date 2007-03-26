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
     * @see Set#add
     */
    public boolean add(Object o) {
        throw new UnsupportedOperationException("Immutable virtual AlwaysSet");
    }

    /**
     * @see Set#addAll
     */
    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException("Immutable virtual AlwaysSet");
    }

    /**
     * @see Set#clear
     */
    public void clear() {
        throw new UnsupportedOperationException("Immutable virtual AlwaysSet");
    }

    /**
     * @see Set#contains
     */
    public boolean contains(Object o) {
        return true;
    }

    /**
     * @see Set#containsAll
     */
    public boolean containsAll(Collection c) {
        return true;
    }

    /**
     * @see Set#equals
     */
    public boolean equals(Object o) {
        return o == this;
    }

    /**
     * @see Set#hashCode
     */
    public int hashCode() {
        return 8732342;
    }

    /**
     * @see Set#isEmpty
     */
    public boolean isEmpty() {
        return false;
    }

    /**
     * @see Set#iterator
     */
    public Iterator iterator() {
        throw new UnsupportedOperationException("Immutable virtual AlwaysSet");
    }

    /**
     * @see Set#remove
     */
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Immutable virtual AlwaysSet");
    }

    /**
     * @see Set#removeAll
     */
    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException("Immutable virtual AlwaysSet");
    }

    /**
     * @see Set#retainAll
     */
    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException("Immutable virtual AlwaysSet");
    }

    /**
     * @see Set#size
     */
    public int size() {
        throw new UnsupportedOperationException("Immutable virtual AlwaysSet");
    }

    /**
     * @see Set#toArray
     */
    public Object[] toArray() {
        throw new UnsupportedOperationException("Immutable virtual AlwaysSet");
    }

    /**
     * @see Set#add
     */
    public Object[] toArray(Object o[]) {
        throw new UnsupportedOperationException("Immutable virtual AlwaysSet");
    }
}
