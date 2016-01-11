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
import java.util.Map;
import java.util.Set;

/**
 * This is a dumb implementation of a Map. It acts as if every object in the VM is mapped onto
 * itself.
 *
 * @author Matthew Wakeling
 * @param <E> The element type
 */
public final class IdentityMap<E> implements Map<E, E>
{
    /**
     * Obtain an instance of this class
     *
     * @param <T> The type of the map
     * @return a singleton
     */
    @SuppressWarnings("unchecked")
    public static <T> IdentityMap<T> getInstance() {
        return (IdentityMap<T>) INSTANCE;
    }

    private static final IdentityMap<Object> INSTANCE = new IdentityMap<Object>();

    /**
     * Construct an IdentityMap.
     */
    private IdentityMap() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(@SuppressWarnings("unused") Object key) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsValue(@SuppressWarnings("unused") Object value) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Map.Entry<E, E>> entrySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof IdentityMap<?>) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public E get(Object key) {
        return (E) key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return 87623;
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
    public Set<E> keySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public E put(@SuppressWarnings("unused") E key, @SuppressWarnings("unused") E value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(@SuppressWarnings("unused") Map<? extends E, ? extends E> map) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public E remove(@SuppressWarnings("unused") Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<E> values() {
        throw new UnsupportedOperationException();
    }
}

