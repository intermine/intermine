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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.Reference;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * This is a Map implementation designed specifically for people intending to create a cache.
 * The class should be subclassed to provide soft or weak reference behaviour. The keys are held
 * strongly, but the values are held weakly or softly, so the values can be garbage-collected.
 * When an entry is garbage-collected, its key is removed from the Map on the next Map activity.
 * <p>
 * The entrySet() and values() methods of this class do not work.
 *
 * @see java.lang.ref.SoftReference
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @author Matthew Wakeling
 */
public abstract class ReferenceMap<K, V> implements Map<K, V>
{
    private static final Logger LOG = Logger.getLogger(ReferenceMap.class);
    protected static final NullValue NULL_VALUE = new NullValue();

    protected Map<K, Reference<Object>> subMap;
    protected ReferenceQueue<Object> queue = new ReferenceQueue<Object>();
    protected String name;

    /**
     * Internal method to clean out stale entries.
     * Note that the garbage collector has very nicely and thoughtfully placed all such value
     * References into the queue for us to pick up.
     */
    @SuppressWarnings("unchecked")
    private void expungeStaleEntries() {
        int oldSize = subMap.size();
        ReferenceWithKey<K> r;
        while ((r = (ReferenceWithKey<K>) queue.poll()) != null) {
            K key = r.getKey();
            Reference<Object> ref = subMap.get(key);
            if (r == ref) {
                subMap.remove(key);
            }
        }
        int newSize = subMap.size();
        if (newSize != oldSize) {
            LOG.debug(name + ": Expunged stale entries - size " + oldSize + " -> " + newSize);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        expungeStaleEntries();
        return subMap.size();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * {@inheritDoc}
     */
    public Set<K> keySet() {
        expungeStaleEntries();
        return subMap.keySet();
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        expungeStaleEntries();
        subMap.clear();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        expungeStaleEntries();
        Reference<Object> ref = subMap.get(key);
        if (ref != null) {
            Object value = ref.get();
            if (value instanceof NullValue) {
                return null;
            }
            return (V) value;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        expungeStaleEntries();
        // Note - expungeEntries is NOT guaranteed to remove everything from the subMap that has
        // been cleared. The garbage collector may insert a gap between clearing a Reference and
        // enqueueing it.
        if (subMap.containsKey(key)) {
            Reference<Object> ref = subMap.get(key);
            if (ref != null) {
                Object value = ref.get();
                if (value == null) {
                    return false;
                }
                return true;
            }
            return false;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        expungeStaleEntries();
        Object v = value;
        if (v == null) {
            v = NULL_VALUE;
        }
        Reference<Object> ref = subMap.put(key, newRef(v, queue, key));
        if (ref != null) {
            v = ref.get();
            if (v instanceof NullValue) {
                return null;
            }
            return (V) v;
        }
        return null;
    }

    /**
     * Private method to create a new ReferenceWithKey object. This should be overridden by
     * subclasses wishing to create soft/weak behaviour.
     *
     * @param value the value put into the Reference
     * @param queue the ReferenceQueue to register the Reference in
     * @param key the key
     * @return a ReferenceWithKey, that is also a Reference (long story, no multiple inheritance in
     * Java, and Reference is daftly an abstract class rather than an interface)
     */
    protected abstract Reference<Object> newRef(Object value, ReferenceQueue<Object> queue,
            K key);

    /**
     * {@inheritDoc}
     */
    public void putAll(Map<? extends K, ? extends V> t) {
        for (Map.Entry<? extends K, ? extends V> entry : t.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        expungeStaleEntries();
        Reference<Object> ref = subMap.remove(key);
        if (ref != null) {
            Object value = ref.get();
            if (value instanceof NullValue) {
                return null;
            }
            return (V) value;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(@SuppressWarnings("unused") Object value) {
        throw new UnsupportedOperationException();
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
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(@SuppressWarnings("unused") Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return subMap.toString();
    }

    /**
     * Interface for entries in the map.
     */
    protected static interface ReferenceWithKey<K>
    {
        /**
         * Returns the key in the entry.
         *
         * @return an Object
         */
        K getKey();
    }

    private static class NullValue
    {
        NullValue() {
        }
    }
}
