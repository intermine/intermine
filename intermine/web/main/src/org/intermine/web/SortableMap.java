package org.intermine.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/*
 * Original released into the public domain by balusc@xs4all.nl
 */


import java.math.BigDecimal;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * SortableMap extends a LinkedHashMap with some useful sorting features.
 * By default it behaves identical to a LinkedHashMap. When you want to sort
 * keys or values, then invoke the sorting as follows:
 * 
 * sortableMap.sortKeys(); or sortableMap.sortValues();
 *
 * By default the objects are sorted ascending on their natural order. For
 * strings that is "111", "22", "3". If you want to check numeric strings and 
 * force them  to be sorted on numeric order "3", "22", "111", then invoke the
 * sorting as follows:
 * 
 * sortableMap.sortKeys(true, true); or sortableMap.sortValues(true, true);
 *
 * The boolean 2nd parameter indicates the sort order. If true, then the map
 * will be sorted ascending. If false, then the map will be sorted descending.
 * The following example will sort the objects descending on natural order.
 * 
 * sortableMap.sortKeys(false, false); or sortableMap.sortValues(false, false);
 *
 * @author balusc@xs4all.nl
 * @version Without Generics for < Java 5.0
 */
public class SortableMap
extends LinkedHashMap
{
    // Constructors -----------------------------------------------------------

    /**
     * Default constructor. 
     */
    public SortableMap() {
        // Keep it alive.
    }

    /**
     * Create new SortableMap based on the given Map.
     * @param map Any map.
     */
    public SortableMap(Map map) {
        super(map);
    }

    // Actions ----------------------------------------------------------------

    /**
     * Sort this map ascending on keys in natural order (default).
     */
    public void sortKeys() {
        sortKeys(false, true);
    }

    /**
     * Sort this map on keys.
     * @param checkNumbers Check numeric values in Strings?
     * @param sortAscending Sort ascending?
     */
    public void sortKeys(final boolean checkNumbers, final boolean sortAscending) {
        Map treeMap = new TreeMap(new Comparator() {
            public int compare(Object key1, Object key2) {
                Comparable key1a = (Comparable) key1;
                Comparable key2a = (Comparable) key2;

                if (checkNumbers) {
                    // Check numeric values in keys.
                    try {
                        // Are the keys parseable as numbers?
                        key1a = new BigDecimal(key1.toString());
                        key2a = new BigDecimal(key2.toString());
                    } catch (NumberFormatException e) {
                        // Revert both back otherwise.
                        key1a = (Comparable) key1;
                        key2a = (Comparable) key2;
                    }
                }

                if (sortAscending) {
                    // Sort keys ascending.
                    return key1a.compareTo(key2a);
                } else {
                    // Sort keys descending.
                    return key2a.compareTo(key1a);
                }
            }
        });
        treeMap.putAll(this);
        this.clear();
        this.putAll(treeMap);
    }

    /**
     * Sort this map ascending on values in natural order (default).
     */
    public void sortValues() {
        sortValues(false, true);
    }

    /**
     * Sort this map on values.
     * @param checkNumbers Check numeric values in Strings?
     * @param sortAscending Sort ascending?
     */
    public void sortValues(final boolean checkNumbers, final boolean sortAscending) {
        Map treeMap = new TreeMap(new Comparator() {
            public int compare(Object key1, Object key2) {
                Object value1 = get(key1);
                Object value2 = get(key2);

                if (value1 == null || value2 == null || value1.equals(value2)) {
                    // Values are null or equal. Sort on it's keys then.
                    Comparable key1a = (Comparable) key1;
                    Comparable key2a = (Comparable) key2;

                    if (checkNumbers) {
                        // Check numeric values in keys.
                        try {
                            // Are the keys parseable as numbers?
                            key1a = new BigDecimal(key1.toString());
                            key2a = new BigDecimal(key2.toString());
                        } catch (NumberFormatException e) {
                            // Revert both back otherwise.
                            key1a = (Comparable) key1;
                            key2a = (Comparable) key2;
                        }
                    }

                    if (sortAscending) {
                        // Sort keys ascending.
                        return key1a.compareTo(key2a);
                    } else {
                        // Sort keys descending.
                        return key2a.compareTo(key1a);
                    }
                } else {
                    // Values are not null or equal. Proceed to sort on values.
                    Comparable value1a = (Comparable) value1;
                    Comparable value2a = (Comparable) value2;

                    if (checkNumbers) {
                        // Check numeric values in values.
                        try {
                            // Are the values parseable as numbers?
                            value1a = new BigDecimal(value1.toString());
                            value2a = new BigDecimal(value2.toString());
                        } catch (NumberFormatException e) {
                            // Revert both back otherwise.
                            value1a = (Comparable) value1;
                            value2a = (Comparable) value2;
                        }
                    }

                    if (sortAscending) {
                        // Sort values ascending.
                        return value1a.compareTo(value2a);
                    } else {
                        // Sort values descending.
                        return value2a.compareTo(value1a);
                    }
                }
            }
        });
        treeMap.putAll(this);
        this.clear();
        this.putAll(treeMap);
    }
}



