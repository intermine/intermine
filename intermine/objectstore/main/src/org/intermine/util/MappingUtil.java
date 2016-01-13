package org.intermine.util;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Useful utilities to do with mappings.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public final class MappingUtil
{
    private MappingUtil() {
    }

    private static final MappingUtilChecker<Object> DEFAULT_CHECKER
        = new MappingUtilChecker<Object>() {
            @Override
            public boolean check(@SuppressWarnings("unused") Map<Object, Object> map) {
                return true;
            }
        };

    /**
     * Produces a Set of possible mappings from items in the firstSet onto items in the secondSet,
     * where the mapping maps from one item to another item that is equal according to the
     * comparator.
     *
     * For example, if firstSet is {a1, b1, c2}, and secondSet is {d1, e1, f2}, and the comparator
     * returns zero when comparing items with the same number, then the result will be a Set that
     * contains two maps, and these are {a1-&gt;d1, b1-&gt;e1, c2-&gt;f2}, and {a1-&gt;e1,
     * b1-&gt;d1, c2-&gt;f2}. For more examples, see the MappingUtilTest.java file.
     *
     * If firstSet is smaller than secondSet, then there are possible mappings, but if firstSet is
     * bigger than secondSet, then there are no possible mappings.
     *
     * @param firstSet the set of items to map from
     * @param secondSet the set of items to map to
     * @param comparator a comparator for the items in the two sets that returns 0 for the required
     * equivalence operation
     * @param <T> The element type of the sets
     * @return a Set of Maps from items in the firstSet onto items in the secondSet
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<Map<T, T>> findCombinations(Set<T> firstSet, Set<T> secondSet,
            Comparator<? super T> comparator) {
        return findCombinations(firstSet, secondSet, comparator,
                (MappingUtilChecker<T>) DEFAULT_CHECKER);
    }

     /**
     * Produces a Set of possible mappings from items in the firstSet onto items in the secondSet,
     * where the mapping maps from one item to another item that is equal according to the
     * comparator.
     *
     * For example, if firstSet is {a1, b1, c2}, and secondSet is {d1, e1, f2}, and the comparator
     * returns zero when comparing items with the same number, then the result will be a Set that
     * contains two maps, and these are {a1-&gt;d1, b1-&gt;e1, c2-&gt;f2}, and {a1-&gt;e1,
     * b1-&gt;d1, c2-&gt;f2}. For more examples, see the MappingUtilTest.java file.
     *
     * If firstSet is smaller than secondSet, then there are possible mappings, but if firstSet is
     * bigger than secondSet, then there are no possible mappings.
     *
     * @param firstSet the set of items to map from
     * @param secondSet the set of items to map to
     * @param comparator a comparator for the items in the two sets that returns 0 for the required
     * equivalence operation
     * @param checker an object that can check a partial mapping for validity
     * @param <T> The element type of the sets
     * @return a Set of Maps from items in the firstSet onto items in the secondSet
     */
    public static <T> Set<Map<T, T>> findCombinations(Set<T> firstSet, Set<T> secondSet,
            Comparator<? super T> comparator, MappingUtilChecker<T> checker) {
        List<T> firstList = new ArrayList<T>(firstSet);
        List<T> secondList = new ArrayList<T>(secondSet);
        Set<Integer> takenSeconds = Collections.emptySet();
        Map<T, T> soFar = Collections.emptyMap();
        Set<Map<T, T>> retval = new LinkedHashSet<Map<T, T>>();
        findCombinations(retval, firstList, secondList, comparator, checker, soFar, 0,
                takenSeconds);
        return retval;
    }

    private static <T> void findCombinations(Set<Map<T, T>> retval, List<T> firstList,
            List<T> secondList, Comparator<? super T> comparator, MappingUtilChecker<T> checker,
            Map<T, T> soFar, int firstIndex, Set<Integer> takenSeconds) {
        if (firstIndex >= firstList.size()) {
            retval.add(soFar);
        } else {
            T firstElement = firstList.get(firstIndex);
            for (int i = 0; i < secondList.size(); i++) {
                if (!takenSeconds.contains(new Integer(i))) {
                    T secondElement = secondList.get(i);
                    if (comparator.compare(firstElement, secondElement) == 0) {
                        Set<Integer> newTakenSeconds = new HashSet<Integer>(takenSeconds);
                        Map<T, T> newSoFar = new LinkedHashMap<T, T>(soFar);
                        newTakenSeconds.add(new Integer(i));
                        newSoFar.put(firstElement, secondElement);
                        if (checker.check(newSoFar)) {
                            findCombinations(retval, firstList, secondList, comparator, checker,
                                    newSoFar, firstIndex + 1, newTakenSeconds);
                        }
                    }
                }
            }
        }
    }

    /**
     * Produces a Set of possible combinations of multiple mappings (as produced by
     * findCombinations), where all mappings in a multiple mapping combination map onto distinct
     * objects.
     *
     * For example, if combinations contains {{a1-&gt;b1}, {a1-&gt;c1}}, then the result will be a
     * Set that contains three Sets:
     * <ol><li>{{a1-&gt;b1}}</li>
     *     <li>{{a1-&gt;c1}}</li>
     *     <li>{{a1-&gt;b1}, {a1-&gt;c1}}</li></ol>
     *
     * @param combinations a Set of Maps, each of which is a mapping from one set of items onto
     * another set of items
     * @param <T> The element type
     * @return a Set of Sets of Maps, where each Set of Maps is a set of mappings that have all
     * items being mapped onto disjoint
     */
    public static <T> Set<Set<Map<T, T>>> findMultipleCombinations(Set<Map<T, T>> combinations) {
        Set<Set<Map<T, T>>> retval = new LinkedHashSet<Set<Map<T, T>>>(); // the result we will
                                                                          // return.
        Set<Map<T, T>> newCombinations = new LinkedHashSet<Map<T, T>>(combinations);
                        // clone, so we don't alter.
                        // Actually, we don't need to do this as long as there isn't multi-threaded
                        // access, since we restore the Set in the end.
        Set<Map<T, T>> combinationsSoFar = new LinkedHashSet<Map<T, T>>(); // An empty set.
        recurseFindMultipleCombinations(retval, newCombinations, combinationsSoFar);
        return retval;
    }

    private static <T> void recurseFindMultipleCombinations(Set<Set<Map<T, T>>> retval,
            Set<Map<T, T>> combinations, Set<Map<T, T>> combinationsSoFar) {
        if (!combinations.isEmpty()) {
            Map<T, T> currentCombination = combinations.iterator().next();
            combinations.remove(currentCombination);

            // Now, we must check to see if any of the values in currentCombination clashes with
            // any value in combinationsSoFar.
            // First, put all values from currentCombination into a Set:
            Set<T> currentValues = new LinkedHashSet<T>();
            for (Map.Entry<T, T> valueEntry : currentCombination.entrySet()) {
                T value = valueEntry.getValue();
                currentValues.add(value);
            }

            boolean clashes = false;
            for (Map<T, T> map : combinationsSoFar) {
                Iterator<Map.Entry<T, T>> mapValueIter = map.entrySet().iterator();
                while ((!clashes) && mapValueIter.hasNext()) {
                    Map.Entry<T, T> mapValueEntry = mapValueIter.next();
                    Object mapValue = mapValueEntry.getValue();
                    clashes = currentValues.contains(mapValue);
                }
            }
            if (!clashes) {
                // In that case, we can add the currentCombination into combinationsSoFar, record
                // the combination as possible, and recurse.
                combinationsSoFar.add(currentCombination);
                retval.add(new LinkedHashSet<Map<T, T>>(combinationsSoFar));
                recurseFindMultipleCombinations(retval, combinations, combinationsSoFar);
                // And then set combinationsSoFar back to how it was before.
                combinationsSoFar.remove(currentCombination);
            }
            // Recurse to try the next in combinations
            recurseFindMultipleCombinations(retval, combinations, combinationsSoFar);
            // And then set combinations back to how it was before
            combinations.add(currentCombination);
        }
    }
}
