package org.intermine.webservice.server.jbrowse.util;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Enumeration;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

/**
 * Class for simplifying access to sets of namespaces properties.
 *
 * This class translates all accesses to <code>key</code> to accesses
 * to <code>namespace + "." + key</code> in the parent set of properties.
 *
 * Note that this class is read-only. New properties should not be added to it.
 *
 * @author Alex Kalderimis.
 *
 */
public class NameSpacedProperties extends Properties
{

    private static final long serialVersionUID = -4232852922218752169L;
    private final String namespace;
    private final Properties parents;

    /**
     * Build a new name-spaced property set with a given parent set of properties, and
     * the designated name-space.
     * @param namespace The prefix to append to all property access.
     * @param parents The source of all property look-ups.
     */
    public NameSpacedProperties(String namespace, Properties parents) {
        this.namespace = namespace + ".";
        this.parents = parents;
    }

    @Override
    public String getProperty(String key) {
        return getProperty(key, null);
    }

    @Override
    public String getProperty(String key, String ifAbsent) {
        return parents.getProperty(namespace + key, ifAbsent);
    }

    @Override
    public Enumeration<?> propertyNames() {
        final Enumeration<?> parentNames = parents.propertyNames();
        return new Enumeration<Object>() {
            private Object currentElement = null;
            private boolean finished = false;

            @Override
            public boolean hasMoreElements() {
                if (!finished && currentElement == null) {
                    currentElement = nextElementInternal();
                }
                return currentElement != null;
            }

            @Override
            public Object nextElement() {
                if (finished && currentElement == null) {
                    throw new NoSuchElementException();
                } else if (currentElement != null) {
                    Object next = currentElement;
                    currentElement = null;
                    return next;
                }
                Object next = nextElementInternal();
                if (next == null) {
                    throw new NoSuchElementException();
                }
                return next;
            }

            private Object nextElementInternal() {
                Object next = null;
                while (!finished && next == null) {
                    if (parentNames.hasMoreElements()) {
                        Object nextParentName = parentNames.nextElement();
                        if (nextParentName != null
                                && String.valueOf(nextParentName).startsWith(namespace)) {
                            next = String.valueOf(nextParentName).replace(namespace, "");
                        }
                    } else {
                        finished = true;
                    }
                }
                return next;
            }
        };
    }

    /* This object is read only. Fail fast at write methods. */

    @Override
    public synchronized Object setProperty(String key, String value) {
        throw new RuntimeException("This object is read only.");
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        throw new RuntimeException("This object is read only.");
    }

    @Override
    public synchronized void putAll(Map<? extends Object, ? extends Object> mapping) {
        throw new RuntimeException("This object is read only.");
    }
}
