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
import java.util.Map;
import java.util.Set;

/**
 * This is a dumb implementation of a Map. It acts as if every object in the VM is mapped onto
 * itself.
 *
 * @author Matthew Wakeling
 */
public class IdentityMap implements Map
{
    /**
     * A singleton instance of this class
     */
    public static final IdentityMap INSTANCE = new IdentityMap();

    /**
     * Construct an IdentityMap.
     */
    private IdentityMap() {
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
    public boolean containsKey(Object key) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(Object value) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public Set entrySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (o instanceof IdentityMap) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Object get(Object key) {
        return key;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return 87623;
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
    public Set keySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Object put(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(Map map) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Object remove(Object key) {
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
    public Collection values() {
        throw new UnsupportedOperationException();
    }
}

