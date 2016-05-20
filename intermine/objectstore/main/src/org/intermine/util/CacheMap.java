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

import java.lang.ref.Reference;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a Map implementation designed specifically for people intending to create a cache.
 * The keys are held strongly, but the values are held softly, so the values can be
 * garbage-collected. When an entry is garbage-collected, its key is removed from the Map on
 * the next Map activity.
 * <p>
 * The entrySet() and values() methods of this class do not work.
 *
 * @see java.lang.ref.SoftReference
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @author Matthew Wakeling
 */
public class CacheMap<K, V> extends SoftReferenceMap<K, V>
{
    /**
     * Constructs a new, empty <tt>CacheMap</tt> with the given initial
     * capacity and the given load factor.
     *
     * @param  initialCapacity The initial capacity of the <tt>CacheMap</tt>
     * @param  loadFactor      The load factor of the <tt>CacheMap</tt>
     * @throws IllegalArgumentException  If the initial capacity is negative,
     *         or if the load factor is nonpositive.
     */
    public CacheMap(int initialCapacity, float loadFactor) {
        subMap = new HashMap<K, Reference<Object>>(initialCapacity, loadFactor);
        this.name = "unknown";
    }

    /**
     * Constructs a new, empty <tt>CacheMap</tt> with the given initial
     * capacity and the default load factor, which is <tt>0.75</tt>.
     *
     * @param  initialCapacity The initial capacity of the <tt>CacheMap</tt>
     * @throws IllegalArgumentException  If the initial capacity is negative.
     */
    public CacheMap(int initialCapacity) {
        subMap = new HashMap<K, Reference<Object>>(initialCapacity);
        this.name = "unknown";
    }

    /**
     * Constructs a new, empty <tt>CacheMap</tt> with the default initial
     * capacity (16) and the default load factor (0.75).
     */
    public CacheMap() {
        subMap = new HashMap<K, Reference<Object>>();
        this.name = "unknown";
    }

    /**
     * Constructs a new, empty <tt>CacheMap</tt> with the default initial
     * capacity (16) and the default load factor (0.75), and a name.
     *
     * @param name the name of the CacheMap - printed out in log messages
     */
    public CacheMap(String name) {
        subMap = new HashMap<K, Reference<Object>>();
        this.name = name;
    }

    /**
     * Constructs a new <tt>CacheMap</tt> with the same mappings as the
     * specified <tt>Map</tt>.  The <tt>CacheMap</tt> is created with
     * default load factor, which is <tt>0.75</tt> and an initial capacity
     * sufficient to hold the mappings in the specified <tt>Map</tt>.
     *
     * @param   t the map whose mappings are to be placed in this map.
     * @throws  NullPointerException if the specified map is null.
     */
    public CacheMap(Map<K, V> t) {
        subMap = new HashMap<K, Reference<Object>>();
        this.name = "unknown";
        putAll(t);
    }
}
