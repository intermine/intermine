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
import java.lang.ref.WeakReference;

/**
 * This is a Map implementation designed specifically for people intending to create a cache.
 * The keys are held strongly, but the values are held weakly, so the values can be
 * garbage-collected. When an entry is garbage-collected, its key is removed from the Map on
 * the next Map activity. Subclasses of this map are required to provide the actual Map
 * functionality.
 * <p>
 * The entrySet() and values() methods of this class do not work.
 *
 * @see java.lang.ref.WeakReference
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @author Matthew Wakeling
 */
public abstract class WeakReferenceMap<K, V> extends ReferenceMap<K, V>
{
    /**
     * Returns a new WeakReferenceWithKey object for the given objects.
     *
     * @param value an Object
     * @param queue a ReferenceQueue
     * @param key an Object
     * @return a WeakReferenceWithKey object
     */
    @Override
    protected WeakReferenceWithKey<K> newRef(Object value, ReferenceQueue<Object> queue, K key) {
        return new WeakReferenceWithKey<K>(value, queue, key);
    }

    private static class WeakReferenceWithKey<K> extends WeakReference<Object>
        implements ReferenceWithKey<K>
    {
        private K key;

        WeakReferenceWithKey(Object value, ReferenceQueue<Object> queue, K key) {
            super(value, queue);
            this.key = key;
        }

        public K getKey() {
            return key;
        }
    }
}
