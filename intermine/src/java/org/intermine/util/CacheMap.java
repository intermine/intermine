package org.intermine.util;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * This is a Map implementation designed specifically for people intending to create a cache.
 * The keys are held strongly, but the values are held softly, so the values can be
 * garbage-collected. When an entry is garbage-collected, its key is removed from the Map on
 * the next Map activity.
 * <p>
 * The entrySet() and values() methods of this class do not work.
 * 
 * @see java.lang.ref.SoftReference
 * @author Matthew Wakeling
 */
public class CacheMap implements Map
{
    private static final Logger LOG = Logger.getLogger(CacheMap.class);

    private Map subMap;
    private ReferenceQueue queue = new ReferenceQueue();
    private String name;

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
        subMap = new HashMap(initialCapacity, loadFactor);
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
        subMap = new HashMap(initialCapacity);
        this.name = "unknown";
    }

    /**
     * Constructs a new, empty <tt>CacheMap</tt> with the default initial
     * capacity (16) and the default load factor (0.75).
     */
    public CacheMap() {
        subMap = new HashMap();
        this.name = "unknown";
    }

    /**
     * Constructs a new, empty <tt>CacheMap</tt> with the default initial
     * capacity (16) and the default load factor (0.75), and a name.
     *
     * @param name the name of the CacheMap - printed out in log messages
     */
    public CacheMap(String name) {
        subMap = new HashMap();
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
    public CacheMap(Map t) {
        subMap = new HashMap();
        putAll(t);
    }

    /**
     * Internal method to clean out stale entries.
     * Note that the garbage collector has very nicely and thoughtfully placed all such value
     * SoftReferences into the queue for us to pick up.
     */
    private void expungeStaleEntries() {
        int oldSize = subMap.size();
        SoftReferenceWithKey r;
        while ((r = (SoftReferenceWithKey) queue.poll()) != null) {
            Object key = r.getKey();
            Object ref = subMap.get(key);
            if (r == ref) {
                subMap.remove(key);
                //System .out.println("Removing stale entry for key = " + key.toString());
            }
        }
        int newSize = subMap.size();
        if (newSize != oldSize) {
            LOG.debug(name + ": Expunged stale entries - size " + oldSize + " -> " + newSize);
        }
    }
    
    /**
     * @see java.util.Map#size
     */
    public int size() {
        expungeStaleEntries();
        return subMap.size();
    }

    /**
     * @see java.util.Map#isEmpty
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * @see java.util.Map#keySet
     */
    public Set keySet() {
        expungeStaleEntries();
        return subMap.keySet();
    }

    /**
     * @see java.util.Map#clear
     */
    public void clear() {
        expungeStaleEntries();
        subMap.clear();
    }

    /**
     * @see java.util.Map#get
     */
    public Object get(Object key) {
        expungeStaleEntries();
        SoftReference ref = (SoftReference) subMap.get(key);
        if (ref != null) {
            Object value = ref.get();
            if (value instanceof NullValue) {
                return null;
            }
            return value;
        }
        return null;
    }

    /**
     * @see java.util.Map#containsKey
     */
    public boolean containsKey(Object key) {
        expungeStaleEntries();
        return subMap.containsKey(key);
    }

    /**
     * @see java.util.Map#put
     */
    public Object put(Object key, Object value) {
        expungeStaleEntries();
        if (value == null) {
            value = new NullValue();
        }
        SoftReference ref = (SoftReference) subMap.put(key, new SoftReferenceWithKey(value, queue,
                    key));
        if (ref != null) {
            value = ref.get();
            if (value instanceof NullValue) {
                return null;
            }
            return value;
        }
        return null;
    }

    /**
     * @see java.util.Map.putAll
     */
    public void putAll(Map t) {
        Iterator i = t.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * @see java.util.Map#remove
     */
    public Object remove(Object key) {
        expungeStaleEntries();
        SoftReference ref = (SoftReference) subMap.remove(key);
        if (ref != null) {
            Object value = ref.get();
            if (value instanceof NullValue) {
                return null;
            }
            return value;
        }
        return null;
    }

    /**
     * @see java.util.Map#containsValue
     */
    public boolean containsValue(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.Map#entrySet
     */
    public Set entrySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.Map#values
     */
    public Collection values() {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.Map#equals
     */
    public boolean equals(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.Map#hashCode
     */
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    private static class SoftReferenceWithKey extends SoftReference
    {
        private Object key;

        SoftReferenceWithKey(Object value, ReferenceQueue queue, Object key) {
            super(value, queue);
            this.key = key;
        }

        public Object getKey() {
            return key;
        }
    }

    private static class NullValue
    {
        NullValue() {
        }
    }
}
