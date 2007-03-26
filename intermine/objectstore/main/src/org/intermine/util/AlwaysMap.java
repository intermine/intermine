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
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * This is a dumb implementation of a Map. It acts as if every object in the VM is mapped onto a
 * certain object.
 *
 * @author Matthew Wakeling
 */
public class AlwaysMap implements Map
{
    Object toReturn;

    /**
     * Construct an AlwaysMap.
     *
     * @param toReturn the object to return every time
     */
    public AlwaysMap(Object toReturn) {
        this.toReturn = toReturn;
    }

    /**
     * @see Map#clear
     */
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * @see Map#containsKey
     */
    public boolean containsKey(Object key) {
        return true;
    }

    /**
     * @see Map#containsValue
     */
    public boolean containsValue(Object value) {
        return toReturn.equals(value);
    }

    /**
     * @see Map#entrySet
     */
    public Set entrySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * @see Map#equals
     */
    public boolean equals(Object o) {
        if (o instanceof AlwaysMap) {
            return ((AlwaysMap) o).toReturn.equals(toReturn);
        }
        return false;
    }

    /**
     * @see Map#get
     */
    public Object get(Object key) {
        return toReturn;
    }

    /**
     * @see Map#hashCode
     */
    public int hashCode() {
        return toReturn.hashCode();
    }

    /**
     * @see Map#isEmpty
     */
    public boolean isEmpty() {
        return false;
    }

    /**
     * @see Map#keySet
     */
    public Set keySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * @see Map#put
     */
    public Object put(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see Map#putAll
     */
    public void putAll(Map map) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see Map#remove
     */
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see Map#size
     */
    public int size() {
        return 1;
    }

    /**
     * @see Map#values
     */
    public Collection values() {
        return Collections.singleton(toReturn);
    }
}
