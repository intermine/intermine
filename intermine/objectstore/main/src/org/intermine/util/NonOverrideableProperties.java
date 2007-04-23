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

import java.util.Properties;

/**
 * Subclass of java.util.Properties that rejects duplicate definitions of a given property.
 *
 * @author Matthew Wakeling
 */
public class NonOverrideableProperties extends Properties
{
    /**
     * Empty constructor.
     */
    public NonOverrideableProperties() {
        super();
    }

    /**
     * Constructor with defaults.
     *
     * @param p default properties
     */
    public NonOverrideableProperties(Properties p) {
        super(p);
    }

    /**
     * Override put, but do not allow existing values to be changed.
     *
     * {@inheritDoc}
     */
    public Object put(Object key, Object value) {
        Object old = get(key);
        if ((old != null) && (!old.equals(value))) {
            throw new IllegalArgumentException("Cannot override non-overrideable property " + key
                    + " = " + old + " with new value " + value);
        }
        return super.put(key, value);
    }
}
