package org.intermine.testing;

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
import java.util.Map;
import java.util.Set;

/**
 * A restrictive implementation of the Map interface.
 *
 * Every time put() is called, the value must be different to the value currently in the map.
 * This is intended for enforcing the simplification of tests.
 *
 * @author Matthew Wakeling
 */
public class MustBeDifferentMap implements Map
{
    private Map underlying;

    /**
     * Constructor - takes an underlying Map.
     *
     * @param underlying a Map.
     */
    public MustBeDifferentMap(Map underlying) {
        this.underlying = underlying;
    }

    public void clear() {
        if (underlying.isEmpty()) {
            System.err.println("Clear called on empty map");
        }
        underlying.clear();
    }

    public boolean containsKey(Object key) {
        return underlying.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return underlying.containsValue(value);
    }

    public Set entrySet() {
        return underlying.entrySet();
    }

    public boolean equals(Object o) {
        return underlying.equals(o);
    }

    public Object get(Object key) {
        return underlying.get(key);
    }

    public int hashCode() {
        return underlying.hashCode();
    }

    public boolean isEmpty() {
        return underlying.isEmpty();
    }

    public Set keySet() {
        return underlying.keySet();
    }

    public Object put(Object key, Object value) {
        Object retval = underlying.put(key, value);
        try {
            if ((retval != null) && retval.equals(value)) {
                System.err.println("Same value put into map: (" + key + ", " + value + ")");
            }
        } catch (ClassCastException e) {
        }
        return retval;
    }

    public void putAll(Map t) {
        Iterator iter = t.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Object key = entry.getKey();
            Object value = entry.getValue();
            put(key, value);
        }
    }

    public Object remove(Object key) {
        Object retval = underlying.remove(key);
        if (retval == null) {
            System.err.println("Remove called when no value present for key: " + key);
        }
        return retval;
    }

    public int size() {
        return underlying.size();
    }

    public Collection values() {
        return underlying.values();
    }
}
