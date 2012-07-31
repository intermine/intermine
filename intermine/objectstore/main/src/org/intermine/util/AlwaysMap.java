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
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * This is a dumb implementation of a Map. It acts as if every object in the VM is mapped onto a
 * certain object.
 *
 * @author Matthew Wakeling
 * @param <K> The key type
 * @param <V> The value type
 */
public class AlwaysMap<K, V> implements Map<K, V>
{
    V toReturn;

    /**
     * Construct an AlwaysMap.
     *
     * @param toReturn the object to return every time
     */
    public AlwaysMap(V toReturn) {
        this.toReturn = toReturn;
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(@SuppressWarnings("unused") Object key) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(Object value) {
        return toReturn.equals(value);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Map.Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof AlwaysMap<?, ?>) {
            return ((AlwaysMap<?, ?>) o).toReturn.equals(toReturn);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public V get(@SuppressWarnings("unused") Object key) {
        return toReturn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return toReturn.hashCode();
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
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public V put(@SuppressWarnings("unused") K key, @SuppressWarnings("unused") V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(@SuppressWarnings("unused") Map<? extends K, ? extends V> map) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public V remove(@SuppressWarnings("unused") Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    public Collection<V> values() {
        return Collections.singleton(toReturn);
    }
}
