package org.flymine.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
    private static final int PUT_IN_STACK = 0;
    private static final int TAKE_FROM_SET = 1;
    private static final int LOOK_IN_LIST = 2;
    private static final int PUT_IN_SET = 3;
    private static final int TAKE_FROM_STACK = 4;
    private static final int FINISHED = 5;

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
     * equivalence operation.
     * @return a Set of Maps from items in the firstSet onto items in the secondSet
     */
    public static Set findCombinations(Set firstSet, Set secondSet, Comparator comparator) {
        List array = new ArrayList(secondSet);
        int arraySize = array.size();
        boolean covered[] = new boolean[arraySize];
        for (int i = 0; i < arraySize; i++) {
            covered[i] = false;
        }
        Set set = new HashSet(firstSet);
        Stack stack = new Stack();
        int state = TAKE_FROM_SET;
        Set resultSet = new HashSet();
        int currentIndex = 0;
        Object obj = null;

        do {
            switch(state) {
                case PUT_IN_STACK:
                    stack.push(new Integer(currentIndex));
                    stack.push(obj);
                    covered[currentIndex] = true;
                    state = TAKE_FROM_SET;
                    break;
                case TAKE_FROM_SET:
                    // First step - take a table out of the Set.
                    if (set.isEmpty()) {
                        // A possible combination is the contents of the stack.
                        Iterator stackIter = stack.iterator();
                        Map result = new HashMap();
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
                        state = PUT_IN_SET;
                    } else {
                        state = PUT_IN_STACK;
                    }
                    break;
                case PUT_IN_SET:
                    set.add(obj);
                    state = TAKE_FROM_STACK;
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

    private static String stateToString(int state) {
        switch(state) {
            case PUT_IN_STACK:
                return "PUT_IN_STACK";
            case TAKE_FROM_SET:
                return "TAKE_FROM_SET";
            case LOOK_IN_LIST:
                return "LOOK_IN_LIST";
            case PUT_IN_SET:
                return "PUT_IN_SET";
            case TAKE_FROM_STACK:
                return "TAKE_FROM_STACK";
            case FINISHED:
                return "FINISHED";
        }
        return "";
    }
}


