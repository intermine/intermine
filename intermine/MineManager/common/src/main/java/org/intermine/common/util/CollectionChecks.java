package org.intermine.common.util;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Class to convert the raw collection types to their typed equivalent.
 * Checks that the collections only contain objects of the right type,
 * throwing <code>ClassCastException</code>s if an inappropriate object
 * is found.
 *
 * @see Class#isAssignableFrom(Class)
 */
public class CollectionChecks
{
    private CollectionChecks() {
    }

    /**
     * Check that the given Collection only contains objects of the given
     * type (or nulls).
     *
     * @param c The Collection to check.
     * @param type The type of objects expected in <code>c</code>.
     * @param <E> The class of objects expected in <code>c</code>.
     *
     * @return <code>c</code>, unchanged.
     *
     * @throws ClassCastException if an inappropriate object is found in
     * <code>c</code>.
     *
     * @see java.util.Collections#checkedCollection(Collection, Class)
     */
    @SuppressWarnings("unchecked")
    public static <E> Collection<E> check(Collection c, Class<E> type) {
        assert verifyCollection(c, type, "Collection");
        return c;
    }

    /**
     * Check that the given List only contains objects of the given
     * type (or nulls).
     *
     * @param l The List to check.
     * @param type The type of objects expected in <code>l</code>.
     * @param <E> The class of objects expected in <code>l</code>.
     *
     * @return <code>l</code>, unchanged.
     *
     * @throws ClassCastException if an inappropriate object is found in
     * <code>l</code>.
     *
     * @see java.util.Collections#checkedList(List, Class)
     */
    @SuppressWarnings("unchecked")
    public static <E> List<E> check(List l, Class<E> type) {
        assert verifyCollection(l, type, "List");
        return l;
    }

    /**
     * Check that the given Set only contains objects of the given
     * type (or nulls).
     *
     * @param s The Set to check.
     * @param type The type of objects expected in <code>s</code>.
     * @param <E> The class of objects expected in <code>s</code>.
     *
     * @return <code>s</code>, unchanged.
     *
     * @throws ClassCastException if an inappropriate object is found in
     * <code>s</code>.
     *
     * @see java.util.Collections#checkedSet(Set, Class)
     */
    @SuppressWarnings("unchecked")
    public static <E> Set<E> check(Set s, Class<E> type) {
        assert verifyCollection(s, type, "Set");
        return s;
    }

    /**
     * Check that the given SortedSet only contains objects of the given
     * type (or nulls).
     *
     * @param s The SortedSet to check.
     * @param type The type of objects expected in <code>s</code>.
     * @param <E> The class of objects expected in <code>s</code>.
     *
     * @return <code>s</code>, unchanged.
     *
     * @throws ClassCastException if an inappropriate object is found in
     * <code>s</code>.
     *
     * @see java.util.Collections#checkedSortedSet(SortedSet, Class)
     */
    @SuppressWarnings("unchecked")
    public static <E> SortedSet<E> check(SortedSet s, Class<E> type) {
        assert verifyCollection(s, type, "SortedSet");
        return s;
    }

    /**
     * Check that the given Queue only contains objects of the given
     * type (or nulls).
     *
     * @param q The Queue to check.
     * @param type The type of objects expected in <code>q</code>.
     * @param <E> The class of objects expected in <code>q</code>.
     *
     * @return <code>q</code>, unchanged.
     *
     * @throws ClassCastException if an inappropriate object is found in
     * <code>q</code>.
     */
    @SuppressWarnings("unchecked")
    public static <E> Queue<E> check(Queue q, Class<E> type) {
        assert verifyCollection(q, type, "Queue");
        return q;
    }

    /**
     * Check that the given Map only contains objects of the given
     * types (or nulls).
     *
     * @param m The Map to check.
     * @param keyType The type of objects expected in as keys <code>m</code>.
     * @param valueType The type of objects expected in as values <code>m</code>.
     * @param <K> The class of key objects expected in <code>m</code>.
     * @param <V> The class of value objects expected in <code>m</code>.
     *
     * @return <code>m</code>, unchanged.
     *
     * @throws ClassCastException if an inappropriate object is found in
     * <code>m</code>.
     *
     * @see java.util.Collections#checkedMap(Map, Class, Class)
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> check(Map m, Class<K> keyType, Class<V> valueType) {
        assert verifyMap(m, keyType, valueType, "Map");
        return m;
    }

    /**
     * Check that the given SortedMap only contains objects of the given
     * types (or nulls).
     *
     * @param m The SortedMap to check.
     * @param keyType The type of objects expected in as keys <code>m</code>.
     * @param valueType The type of objects expected in as values <code>m</code>.
     * @param <K> The class of key objects expected in <code>m</code>.
     * @param <V> The class of value objects expected in <code>m</code>.
     *
     * @return <code>m</code>, unchanged.
     *
     * @throws ClassCastException if an inappropriate object is found in
     * <code>m</code>.
     *
     * @see java.util.Collections#checkedSortedMap(SortedMap, Class, Class)
     */
    @SuppressWarnings("unchecked")
    public static <K, V> SortedMap<K, V> check(SortedMap m, Class<K> keyType, Class<V> valueType) {
        assert verifyMap(m, keyType, valueType, "SortedMap");
        return m;
    }


    /**
     * Check that the given Collection only contains objects of the given
     * type (or nulls).
     *
     * @param c The Collection to check.
     * @param type The type of objects expected in <code>c</code>.
     * @param ifname The name of the more precise interface of the collection being checked.
     * @param <E> The class of objects expected in <code>c</code>.
     *
     * @return <code>true</code>, always. This allows it to be used as part
     * of an <code>assert</code>.
     *
     * @throws ClassCastException if an inappropriate object is found in
     * <code>c</code>.
     */
    private static <E> boolean verifyCollection(Collection<?> c, Class<E> type, String ifname) {
        if (c != null) {
            for (Object o : c) {
                if (o != null && !type.isAssignableFrom(o.getClass())) {
                    throw new ClassCastException(
                            ifname + " of expected type <" + type
                            + "> contains an invalid object of type " + o.getClass());
                }
            }
        }
        return true;
    }

    /**
     * Check that the given Map only contains objects of the given
     * types (or nulls).
     *
     * @param m The Map to check.
     * @param keyType The type of objects expected in as keys <code>m</code>.
     * @param valueType The type of objects expected in as values <code>m</code>.
     * @param ifname The name of the more precise interface of the map being checked.
     * @param <K> The class of key objects expected in <code>m</code>.
     * @param <V> The class of value objects expected in <code>m</code>.
     *
     * @return <code>true</code>, always. This allows it to be used as part
     * of an <code>assert</code>.
     *
     * @throws ClassCastException if an inappropriate object is found in
     * <code>m</code>.
     */
    private static <K, V> boolean verifyMap(
            Map<?, ?> m, Class<K> keyType, Class<V> valueType, String ifname) {
        if (m != null) {
            for (Object o : m.entrySet()) {
                Map.Entry<?, ?> pair = (Map.Entry<?, ?>) o;
                if (pair.getKey() != null
                        && !keyType.isAssignableFrom(pair.getKey().getClass())) {
                    throw new ClassCastException(
                            ifname + " of expected types <" + keyType + "," + valueType
                            + "> contains an invalid key of type " + pair.getKey().getClass());
                }
                if (pair.getValue() != null
                        && !valueType.isAssignableFrom(pair.getValue().getClass())) {
                    throw new ClassCastException(
                            ifname + " of expected types <" + keyType + "," + valueType
                            + "> contains an invalid value of type " + pair.getValue().getClass());
                }
            }
        }
        return true;
    }

}
