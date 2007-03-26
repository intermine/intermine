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
import java.lang.ref.SoftReference;

import org.apache.log4j.Logger;

/**
 * This is a Map implementation designed specifically for people intending to create a cache.
 * The keys are held strongly, but the values are held softly, so the values can be
 * garbage-collected. When an entry is garbage-collected, its key is removed from the Map on
 * the next Map activity. Subclasses of this map are required to provide the actual Map
 * functionality.
 * <p>
 * The entrySet() and values() methods of this class do not work.
 * 
 * @see java.lang.ref.SoftReference
 * @author Matthew Wakeling
 */
public abstract class SoftReferenceMap extends ReferenceMap
{
    private static final Logger LOG = Logger.getLogger(SoftReferenceMap.class);

    /**
     * Returns a new SoftReferenceWithKey object for the given objects.
     *
     * @param value an Object
     * @param queue a ReferenceQueue
     * @param key an Object
     * @return a SoftReferenceWithKey object
     */
    protected ReferenceWithKey newRef(Object value, ReferenceQueue queue, Object key) {
        return new SoftReferenceWithKey(value, queue, key);
    }

    private static class SoftReferenceWithKey extends SoftReference implements ReferenceWithKey
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
}
