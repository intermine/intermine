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

import junit.framework.*;
import java.util.Comparator;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

public class MappingUtilTest extends TestCase
{
    private MappingUtilTestObject a1, b1, c1, c2, d2, e1, f1, g2, h2, c3, d4, f2, g3, h4, e4, f3, h1, g1, b2;
    
    public MappingUtilTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        a1 = new MappingUtilTestObject(1, "a");
        b1 = new MappingUtilTestObject(1, "b");
        b2 = new MappingUtilTestObject(2, "b");
        c1 = new MappingUtilTestObject(1, "c");
        c2 = new MappingUtilTestObject(2, "c");
        c3 = new MappingUtilTestObject(3, "c");
        d2 = new MappingUtilTestObject(2, "d");
        d4 = new MappingUtilTestObject(4, "d");
        e1 = new MappingUtilTestObject(1, "e");
        e4 = new MappingUtilTestObject(4, "e");
        f1 = new MappingUtilTestObject(1, "f");
        f2 = new MappingUtilTestObject(2, "f");
        f3 = new MappingUtilTestObject(3, "f");
        g1 = new MappingUtilTestObject(1, "g");
        g2 = new MappingUtilTestObject(2, "g");
        g3 = new MappingUtilTestObject(3, "g");
        h1 = new MappingUtilTestObject(1, "h");
        h2 = new MappingUtilTestObject(2, "h");
        h4 = new MappingUtilTestObject(4, "h");
    }

    public void testASimpleMapping() throws Exception {
        Set firstSet = new HashSet();
        Set secondSet = new HashSet();
        firstSet.add(a1);
        firstSet.add(b1);
        firstSet.add(c2);
        firstSet.add(d2);
        secondSet.add(e1);
        secondSet.add(f1);
        secondSet.add(g2);
        secondSet.add(h2);
        Comparator comparator = new MappingUtilTestComparator();
        Set combinations = MappingUtil.findCombinations(firstSet, secondSet, comparator);

        Set resultSet = new HashSet();
        Map result1 = new HashMap();
        result1.put(a1, e1);
        result1.put(b1, f1);
        result1.put(c2, g2);
        result1.put(d2, h2);
        resultSet.add(result1);
        Map result2 = new HashMap();
        result2.put(a1, f1);
        result2.put(b1, e1);
        result2.put(c2, g2);
        result2.put(d2, h2);
        resultSet.add(result2);
        Map result3 = new HashMap();
        result3.put(a1, e1);
        result3.put(b1, f1);
        result3.put(c2, h2);
        result3.put(d2, g2);
        resultSet.add(result3);
        Map result4 = new HashMap();
        result4.put(a1, f1);
        result4.put(b1, e1);
        result4.put(c2, h2);
        result4.put(d2, g2);
        resultSet.add(result4);

        assertEquals(resultSet, combinations);

        Set multiCombinations = MappingUtil.findMultipleCombinations(combinations);

        Set combination1 = new HashSet();
        combination1.add(result1);
        Set combination2 = new HashSet();
        combination2.add(result2);
        Set combination3 = new HashSet();
        combination3.add(result3);
        Set combination4 = new HashSet();
        combination4.add(result4);
        Set multiResult = new HashSet();
        multiResult.add(combination1);
        multiResult.add(combination2);
        multiResult.add(combination3);
        multiResult.add(combination4);

        assertEquals(multiResult, multiCombinations);
    }

    public void testALooseMapping() throws Exception {
        Set firstSet = new HashSet();
        Set secondSet = new HashSet();
        firstSet.add(a1);
        firstSet.add(c2);
        secondSet.add(e1);
        secondSet.add(f1);
        secondSet.add(g2);
        secondSet.add(h2);
        Comparator comparator = new MappingUtilTestComparator();
        Set combinations = MappingUtil.findCombinations(firstSet, secondSet, comparator);

        Set resultSet = new HashSet();
        Map result1 = new HashMap();
        result1.put(a1, e1);
        result1.put(c2, g2);
        resultSet.add(result1);
        Map result2 = new HashMap();
        result2.put(a1, f1);
        result2.put(c2, g2);
        resultSet.add(result2);
        Map result3 = new HashMap();
        result3.put(a1, e1);
        result3.put(c2, h2);
        resultSet.add(result3);
        Map result4 = new HashMap();
        result4.put(a1, f1);
        result4.put(c2, h2);
        resultSet.add(result4);

        assertEquals(resultSet, combinations);

        Set multiCombinations = MappingUtil.findMultipleCombinations(combinations);

        Set combination1 = new HashSet();
        combination1.add(result1);
        Set combination2 = new HashSet();
        combination2.add(result2);
        Set combination3 = new HashSet();
        combination3.add(result3);
        Set combination4 = new HashSet();
        combination4.add(result4);
        Set combination5 = new HashSet();
        combination5.add(result1);
        combination5.add(result4);
        Set combination6 = new HashSet();
        combination6.add(result2);
        combination6.add(result3);
        Set multiResult = new HashSet();
        multiResult.add(combination1);
        multiResult.add(combination2);
        multiResult.add(combination3);
        multiResult.add(combination4);
        multiResult.add(combination5);
        multiResult.add(combination6);

        assertEquals(multiResult, multiCombinations);
    }

    public void testAnotherSimpleMapping() throws Exception {
        Set firstSet = new HashSet();
        Set secondSet = new HashSet();
        firstSet.add(a1);
        firstSet.add(b2);
        firstSet.add(c3);
        firstSet.add(d4);
        secondSet.add(e1);
        secondSet.add(f2);
        secondSet.add(g3);
        secondSet.add(h4);

        Set resultSet = new HashSet();
        Map result = new HashMap();
        result.put(a1, e1);
        result.put(b2, f2);
        result.put(c3, g3);
        result.put(d4, h4);
        resultSet.add(result);
        
        Comparator comparator = new MappingUtilTestComparator();
        assertEquals(resultSet, MappingUtil.findCombinations(firstSet, secondSet, comparator));
    }

    public void testYetAnotherSimpleMapping() throws Exception {
        Set firstSet = new HashSet();
        Set secondSet = new HashSet();
        firstSet.add(a1);
        firstSet.add(b2);
        firstSet.add(c3);
        firstSet.add(d4);
        secondSet.add(e4);
        secondSet.add(f3);
        secondSet.add(g2);
        secondSet.add(h1);

        Set resultSet = new HashSet();
        Map result = new HashMap();
        result.put(a1, h1);
        result.put(b2, g2);
        result.put(c3, f3);
        result.put(d4, e4);
        resultSet.add(result);
        
        Comparator comparator = new MappingUtilTestComparator();
        assertEquals(resultSet, MappingUtil.findCombinations(firstSet, secondSet, comparator));
    }

    public void testAWrongMapping() throws Exception {
        Set firstSet = new HashSet();
        Set secondSet = new HashSet();
        firstSet.add(a1);
        firstSet.add(b1);
        firstSet.add(c3);
        firstSet.add(d4);
        secondSet.add(e1);
        secondSet.add(f2);
        secondSet.add(g3);
        secondSet.add(h4);

        Set resultSet = new HashSet();

        Comparator comparator = new MappingUtilTestComparator();
        assertEquals(resultSet, MappingUtil.findCombinations(firstSet, secondSet, comparator));
    }

    public void testDifferentSizeMapping() throws Exception {
        Set firstSet = new HashSet();
        Set secondSet = new HashSet();
        firstSet.add(a1);
        firstSet.add(b2);
        secondSet.add(c2);
        secondSet.add(e1);
        secondSet.add(f2);
        secondSet.add(g1);
        secondSet.add(h4);

        Set resultSet = new HashSet();
        Map result = new HashMap();
        result.put(a1, e1);
        result.put(b2, c2);
        resultSet.add(result);
        result = new HashMap();
        result.put(a1, g1);
        result.put(b2, c2);
        resultSet.add(result);
        result = new HashMap();
        result.put(a1, e1);
        result.put(b2, f2);
        resultSet.add(result);
        result = new HashMap();
        result.put(a1, g1);
        result.put(b2, f2);
        resultSet.add(result);

        Set resultSet2 = new HashSet();
        
        Comparator comparator = new MappingUtilTestComparator();
        assertEquals(resultSet, MappingUtil.findCombinations(firstSet, secondSet, comparator));
        assertEquals(resultSet2, MappingUtil.findCombinations(secondSet, firstSet, comparator));
    }

    public void testMultiple1() throws Exception {
        Set firstSet = new HashSet();
        Set secondSet = new HashSet();
        firstSet.add(a1);
        secondSet.add(b1);
        secondSet.add(c1);

        Comparator comparator = new MappingUtilTestComparator();
        Set combinations = MappingUtil.findCombinations(firstSet, secondSet, comparator);
        Set multiCombinations = MappingUtil.findMultipleCombinations(combinations);

        Map firstMapping = new HashMap();
        firstMapping.put(a1, b1);
        Map secondMapping = new HashMap();
        secondMapping.put(a1, c1);
        Set firstCombination = new HashSet();
        firstCombination.add(firstMapping);
        Set secondCombination = new HashSet();
        secondCombination.add(secondMapping);
        Set thirdCombination = new HashSet();
        thirdCombination.add(firstMapping);
        thirdCombination.add(secondMapping);
        Set result = new HashSet();
        result.add(firstCombination);
        result.add(secondCombination);
        result.add(thirdCombination);
        
        assertEquals(result, multiCombinations);
    }

    public void testChecker() throws Exception {
        Set firstSet = new HashSet();
        Set secondSet = new HashSet();
        firstSet.add(a1);
        firstSet.add(b1);
        firstSet.add(c2);
        firstSet.add(d2);
        secondSet.add(e1);
        secondSet.add(f1);
        secondSet.add(g2);
        secondSet.add(h2);
        Comparator comparator = new MappingUtilTestComparator();
        MappingUtilChecker checker = new MappingUtilChecker() {
            public boolean check(Map map) {
                Object o = map.get(a1);
                return (o == null) || o.equals(e1);
            }
        };
        Set combinations = MappingUtil.findCombinations(firstSet, secondSet, comparator, checker);

        Set resultSet = new HashSet();
        Map result1 = new HashMap();
        result1.put(a1, e1);
        result1.put(b1, f1);
        result1.put(c2, g2);
        result1.put(d2, h2);
        resultSet.add(result1);
        Map result3 = new HashMap();
        result3.put(a1, e1);
        result3.put(b1, f1);
        result3.put(c2, h2);
        result3.put(d2, g2);
        resultSet.add(result3);

        assertEquals(resultSet, combinations);
    }

    class MappingUtilTestObject {
        public int number;
        public String description;

        public MappingUtilTestObject(int number, String description) {
            this.number = number;
            this.description = description;
        }

        public String toString() {
            return "(" + number + ", " + description + ")";
        }
    }

    class MappingUtilTestComparator implements Comparator {
        public MappingUtilTestComparator() {
        }

        public int compare(Object a, Object b) {
            return ((MappingUtilTestObject) a).number  - ((MappingUtilTestObject) b).number;
        }
    }
}
