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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Useful utilities to do with mappings.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class MappingUtil
{
    private static final int TAKE_FROM_SET = 1;
    private static final int LOOK_IN_LIST = 2;
    private static final int TAKE_FROM_STACK = 4;
    private static final int FINISHED = 5;

    private static final MappingUtilChecker DEFAULT_CHECKER = new MappingUtilChecker() {
        public boolean check(Map map) {
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
     * @return a Set of Maps from items in the firstSet onto items in the secondSet
     */
    public static Set findCombinations(Set firstSet, Set secondSet, Comparator comparator) {
        return findCombinations(firstSet, secondSet, comparator, DEFAULT_CHECKER);
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
     * @return a Set of Maps from items in the firstSet onto items in the secondSet
     */
    public static Set findCombinations(Set firstSet, Set secondSet, Comparator comparator,
            MappingUtilChecker checker) {
        List array = new ArrayList(secondSet);
        int arraySize = array.size();
        boolean covered[] = new boolean[arraySize];
        for (int i = 0; i < arraySize; i++) {
            covered[i] = false;
        }
        Set set = new LinkedHashSet(firstSet);
        Stack stack = new Stack();
        int state = TAKE_FROM_SET;
        Set resultSet = new LinkedHashSet();
        int currentIndex = 0;
        Object obj = null;

        do {
            switch(state) {
                case TAKE_FROM_SET:
                    // First step - take a table out of the Set.
                    if (set.isEmpty()) {
                        // A possible combination is the contents of the stack.
                        Iterator stackIter = stack.iterator();
                        Map result = new LinkedHashMap();
                        while (stackIter.hasNext()) {
                            int indexOfArray = ((Integer) stackIter.next()).intValue();
                            Object objFromSet = stackIter.next();
                            result.put(objFromSet, array.get(indexOfArray));
                        }
                        resultSet.add(result);
                        state = TAKE_FROM_STACK;
                    } else {
                        obj = set.iterator().next();
                        set.remove(obj);
                        currentIndex = 0;
                        state = LOOK_IN_LIST;
                    }
                    break;
                case LOOK_IN_LIST:
                    // Second step - look for it in the List.
                    while ((currentIndex < arraySize)
                            && ((comparator.compare(array.get(currentIndex), obj) != 0)
                                || covered[currentIndex])) {
                        currentIndex++;
                    }
                    if (currentIndex == arraySize) {
                        set.add(obj);
                        state = TAKE_FROM_STACK;
                    } else {
                        // At this point we want to check whether this is a valid partial
                        // combination. So first, create the map:
                        Iterator stackIter = stack.iterator();
                        Map result = new LinkedHashMap();
                        while (stackIter.hasNext()) {
                            int indexOfArray = ((Integer) stackIter.next()).intValue();
                            Object objFromSet = stackIter.next();
                            result.put(objFromSet, array.get(indexOfArray));
                        }
                        result.put(obj, array.get(currentIndex));
                        // Now, if it is valid, then record it. Otherwise, go back to the beginning
                        // of LOOK_IN_LIST.
                        if (checker.check(result)) {
                            stack.push(new Integer(currentIndex));
                            stack.push(obj);
                            covered[currentIndex] = true;
                            state = TAKE_FROM_SET;
                        } else {
                            currentIndex++;
                        }
                    }
                    break;
                case TAKE_FROM_STACK:
                    if (stack.isEmpty()) {
                        state = FINISHED;
                    } else {
                        obj = stack.pop();
                        currentIndex = ((Integer) stack.pop()).intValue();
                        covered[currentIndex] = false;
                        currentIndex++;
                        state = LOOK_IN_LIST;
                    }
                    break;
            }
        } while (state != FINISHED);

        return resultSet;
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
     * @return a Set of Sets of Maps, where each Set of Maps is a set of mappings that have all
     * items being mapped onto disjoint
     */
    public static Set findMultipleCombinations(Set combinations) {
        Set retval = new LinkedHashSet(); // the result we will return.
        Set newCombinations = new LinkedHashSet(combinations); // clone, so we don't alter.
                                                         // Actually, we don't need to do this as
                                                         // long as there isn't multi-threaded
                                                         // access, since we restore the Set in the
                                                         // end.
        Set combinationsSoFar = new LinkedHashSet(); // An empty set.
        recurseFindMultipleCombinations(retval, newCombinations, combinationsSoFar);
        return retval;
    }

    private static void recurseFindMultipleCombinations(Set retval, Set combinations,
            Set combinationsSoFar) {
        if (!combinations.isEmpty()) {
            Map currentCombination = (Map) combinations.iterator().next();
            combinations.remove(currentCombination);

            // Now, we must check to see if any of the values in currentCombination clashes with
            // any value in combinationsSoFar.
            // First, put all values from currentCombination into a Set:
            Iterator valueIter = currentCombination.entrySet().iterator();
            Set currentValues = new LinkedHashSet();
            while (valueIter.hasNext()) {
                Map.Entry valueEntry = (Map.Entry) valueIter.next();
                Object value = valueEntry.getValue();
                currentValues.add(value);
            }

            boolean clashes = false;
            Iterator mapIter = combinationsSoFar.iterator();
            while ((!clashes) && mapIter.hasNext()) {
                Map map = (Map) mapIter.next();
                Iterator mapValueIter = map.entrySet().iterator();
                while ((!clashes) && mapValueIter.hasNext()) {
                    Map.Entry mapValueEntry = (Map.Entry) mapValueIter.next();
                    Object mapValue = mapValueEntry.getValue();
                    clashes = currentValues.contains(mapValue);
                }
            }
            if (!clashes) {
                // In that case, we can add the currentCombination into combinationsSoFar, record
                // the combination as possible, and recurse.
                combinationsSoFar.add(currentCombination);
                retval.add(new LinkedHashSet(combinationsSoFar));
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


