package org.intermine.util;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;

/**
 * Utilities for Collections.
 *
 * @author Kim Rutherford
 */

public class CollectionUtil
{
    /**
     * Return a copy of the given map with the object inserted at the given index.
     * @param map the LinkedHashMap to copy
     * @param prevKey the newKey,newValue pair are added after this key.  If null the
     * newKey,newValue pair is added first
     * @param newKey the new key
     * @param newValue the new value
     * @return the copied LinkedHashMap with newKey and newValue added
     */
    public static LinkedHashMap linkedHashMapAdd(LinkedHashMap map, Object prevKey,
                                                 Object newKey, Object newValue) {

        if (prevKey == null) {
            LinkedHashMap newMap = new LinkedHashMap();

            newMap.put(newKey, newValue);

            newMap.putAll(map);

            return newMap;
        }

        if (!map.containsKey(prevKey)) {
            throw new IllegalArgumentException("LinkedHashMap does not contain: " + prevKey);
        }

        LinkedHashMap newMap = new LinkedHashMap();

        Iterator iter = map.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry mapEntry = (Map.Entry) iter.next();

            Object key = mapEntry.getKey();
            Object value = mapEntry.getValue();

            newMap.put(key, value);

            if (key.equals(prevKey)) {
                newMap.put(newKey, newValue);
            }
        }

        return newMap;
    }
}
