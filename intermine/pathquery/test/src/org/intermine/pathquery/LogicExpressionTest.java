package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;

/**
 * Tests for the LogicExpression class
 *
 * @author Matthew Wakeling
 */
public class LogicExpressionTest extends TestCase
{
    public List<String> empty = Collections.emptyList();

    public LogicExpressionTest(String arg) {
        super(arg);
    }

    public void test1() {
        LogicExpression l = new LogicExpression("A");
        assertEquals(Collections.singleton("A"), l.getVariableNames());
        assertEquals("A", l.toString());
        assertEquals(Collections.singletonList(new LogicExpression("A")), l.split(Collections.singletonList(Collections.singletonList("A"))));
        assertEquals(new LogicExpression("A"), l.getSection(Arrays.asList("A")));
        assertEquals(null, l.getSection(empty));
        try {
            l.getSection(Arrays.asList("B"));
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
        }
    }

    public void test2() {
        LogicExpression l = new LogicExpression("A and B");
        assertEquals(new HashSet(Arrays.asList("A", "B")), l.getVariableNames());
        assertEquals("A and B", l.toString());
        assertEquals(Arrays.asList(new LogicExpression("A"), new LogicExpression("B")), l.split(Arrays.asList(Arrays.asList("A"), Arrays.asList("B"))));
        try {
            l.split(Arrays.asList(Arrays.asList("A")));
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
        }
        try {
            l.split(Arrays.asList(Arrays.asList("A", "B", "C")));
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
        }
        try {
            l.split(Arrays.asList(Arrays.asList("A", "B"), Arrays.asList("A")));
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
        }
        assertEquals(new LogicExpression("A"), l.getSection(Arrays.asList("A")));
        assertEquals(new LogicExpression("A and B"), l.getSection(Arrays.asList("A", "B")));
    }

    public void test3() {
        LogicExpression l = new LogicExpression("A or B");
        assertEquals(new HashSet(Arrays.asList("A", "B")), l.getVariableNames());
        assertEquals("A or B", l.toString());
        try {
            l.split(Arrays.asList(Arrays.asList("A"), Arrays.asList("B")));
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
        }
        assertEquals(Arrays.asList(new LogicExpression("A or B")), l.split(Arrays.asList(Arrays.asList("A", "B"))));
        assertEquals(new LogicExpression("A or B"), l.getSection(Arrays.asList("A", "B")));
        try {
            l.getSection(Arrays.asList("A"));
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
        }
    }

    public void test4() {
        LogicExpression l = new LogicExpression("A and (B and C)");
        assertEquals(new HashSet(Arrays.asList("A", "B", "C")), l.getVariableNames());
        assertEquals("A and B and C", l.toString());
        assertEquals(Arrays.asList(new LogicExpression("A and C"), new LogicExpression("B")), l.split(Arrays.asList(Arrays.asList("A", "C"), Arrays.asList("B"))));
    }

    public void test5() {
        LogicExpression l = new LogicExpression("A and (B or C)");
        assertEquals(new HashSet(Arrays.asList("A", "B", "C")), l.getVariableNames());
        assertEquals("A and (B or C)", l.toString());
        assertEquals(Arrays.asList(new LogicExpression("A"), new LogicExpression("B or C"), null), l.split(Arrays.asList(Arrays.asList("A"), Arrays.asList("B", "C"), empty)));
        assertEquals(new LogicExpression("A and (B or C)"), l.getSection(Arrays.asList("A", "B", "C")));
        assertEquals(Arrays.asList(new LogicExpression("A and (B or C)")), l.split(Arrays.asList(Arrays.asList("A", "B", "C"))));
    }

    public void test6() {
        LogicExpression l = new LogicExpression("A and (B or C and D)");
        assertEquals(new HashSet(Arrays.asList("A", "B", "C", "D")), l.getVariableNames());
        assertEquals("A and (B or C and D)", l.toString());
        assertEquals(Arrays.asList(new LogicExpression("A"), new LogicExpression("B or (C and D)")), l.split(Arrays.asList(Arrays.asList("A"), Arrays.asList("B", "C", "D"))));
        try {
            l.split(Arrays.asList(Arrays.asList("A", "B", "C"), Arrays.asList("D")));
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
        }
    }
}
