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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.Reference;
import java.util.Collection;
import java.util.Iterator;
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
 * @author Matthew Wakeling
 */
public abstract class ReferenceMap implements Map
{
    private static final Logger LOG = Logger.getLogger(ReferenceMap.class);
    protected static final NullValue NULL_VALUE = new NullValue();

    protected Map subMap;
    protected ReferenceQueue queue = new ReferenceQueue();
    protected String name;

    /**
     * Internal method to clean out stale entries.
     * Note that the garbage collector has very nicely and thoughtfully placed all such value
     * References into the queue for us to pick up.
     */
    private void expungeStaleEntries() {
        int oldSize = subMap.size();
        ReferenceWithKey r;
        while ((r = (ReferenceWithKey) queue.poll()) != null) {
            Object key = r.getKey();
            Object ref = subMap.get(key);
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
        Reference ref = (Reference) subMap.get(key);
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
            value = NULL_VALUE;
        }
        Reference ref = (Reference) subMap.put(key, newRef(value, queue, key));
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
     * Private method to create a new ReferenceWithKey object. This should be overridden by
     * subclasses wishing to create soft/weak behaviour.
     *
     * @param value the value put into the Reference
     * @param queue the ReferenceQueue to register the Reference in
     * @param key the key
     * @return a ReferenceWithKey, that is also a Reference (long story, no multiple inheritance in
     * Java)
     */
    protected abstract ReferenceWithKey newRef(Object value, ReferenceQueue queue, Object key);

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
        Reference ref = (Reference) subMap.remove(key);
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
    
    /**
     * @see Object#toString
     */
    public String toString() {
        return subMap.toString();
    }

    /**
     * Interface for entries in the map.
     */
    protected static interface ReferenceWithKey
    {
        /**
         * Returns the key in the entry.
         *
         * @return an Object
         */
        public Object getKey();
    }

    private static class NullValue
    {
        NullValue() {
        }
    }
}
