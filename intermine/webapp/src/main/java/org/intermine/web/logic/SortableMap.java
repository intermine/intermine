package org.intermine.web.logic;

/*
 * Copyright (C) 2002-2016 FlyMine
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
 * @author Alex Kalderimis (added generics).
 * @param <K> The type of keys.
 * @param <V> The type of values.
 */
public class SortableMap<K extends Comparable<K>, V extends Comparable<V>>
    extends LinkedHashMap<K, V>
{
    // Constructors -----------------------------------------------------------

    private final class ValueComparator implements Comparator<K>
    {
        private final boolean sortAscending;
        private final boolean checkNumbers;

        private ValueComparator(boolean sortAscending, boolean checkNumbers) {
            this.sortAscending = sortAscending;
            this.checkNumbers = checkNumbers;
        }

        @Override
        public int compare(K key1, K key2) {
            V value1 = get(key1);
            V value2 = get(key2);

            if (value1 == null || value2 == null || value1.equals(value2)) {
                // Values are null or equal. Sort on its keys then.
                return compareThings(checkNumbers, sortAscending, key1, key2);
            } else {
                // Values are not null or equal. Proceed to sort on values.
                return compareThings(checkNumbers, sortAscending, value1, value2);
            }
        }
    }

    private final class KeyComparator implements Comparator<K>
    {
        private final boolean checkNumbers;
        private final boolean sortAscending;

        private KeyComparator(boolean checkNumbers, boolean sortAscending) {
            this.checkNumbers = checkNumbers;
            this.sortAscending = sortAscending;
        }

        @Override
        public int compare(K key1, K key2) {
            return compareThings(checkNumbers, sortAscending, key1, key2);
        }
    }

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
    public SortableMap(Map<K, V> map) {
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
        Comparator<K> comparator = new KeyComparator(checkNumbers, sortAscending);
        Map<K, V> treeMap = new TreeMap<K, V>(comparator);
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
        Comparator<K> comparator = new ValueComparator(sortAscending, checkNumbers);
        Map<K, V> treeMap = new TreeMap<K, V>(comparator);
        treeMap.putAll(this);
        this.clear();
        this.putAll(treeMap);
    }

    private static <T extends Comparable<T>>
    int compareThings(final boolean checkNumbers, final boolean sortAscending, T a, T b) {
        if (checkNumbers) {
            // Check numeric values in keys.
            try {
                // Are the keys parseable as numbers?
                BigDecimal asNum1 = new BigDecimal(a.toString());
                BigDecimal asNum2 = new BigDecimal(b.toString());
                if (sortAscending) {
                    // Sort keys ascending.
                    return asNum1.compareTo(asNum2);
                } else {
                    // Sort keys descending.
                    return asNum2.compareTo(asNum1);
                }
            } catch (NumberFormatException e) {
                // Ignore.
            }
        }
        if (sortAscending) {
            // Sort keys ascending.
            return a.compareTo(b);
        } else {
            // Sort keys descending.
            return b.compareTo(a);
        }
    }
}

