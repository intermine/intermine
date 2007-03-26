package org.intermine.sql.query;

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

public class AbstractConstraintTest extends TestCase
{
    public AbstractConstraintTest(String arg1) {
        super(arg1);
    }

    public void testAlterComparisonNotThis() throws Exception {
        assertEquals(AbstractConstraint.OPPOSITE,
                AbstractConstraint.alterComparisonNotThis(AbstractConstraint.EQUAL));
        assertEquals(AbstractConstraint.EQUAL,
                AbstractConstraint.alterComparisonNotThis(AbstractConstraint.OPPOSITE));
        assertEquals(AbstractConstraint.EXCLUDES,
                AbstractConstraint.alterComparisonNotThis(AbstractConstraint.IMPLIED_BY));
        assertEquals(AbstractConstraint.OR,
                AbstractConstraint.alterComparisonNotThis(AbstractConstraint.IMPLIES));
        assertEquals(AbstractConstraint.IMPLIED_BY,
                AbstractConstraint.alterComparisonNotThis(AbstractConstraint.EXCLUDES));
        assertEquals(AbstractConstraint.IMPLIES,
                AbstractConstraint.alterComparisonNotThis(AbstractConstraint.OR));
        assertEquals(AbstractConstraint.INDEPENDENT,
                AbstractConstraint.alterComparisonNotThis(AbstractConstraint.INDEPENDENT));
    }

    public void testAlterComparisonNotObj() throws Exception {
        assertEquals(AbstractConstraint.OPPOSITE,
                AbstractConstraint.alterComparisonNotObj(AbstractConstraint.EQUAL));
        assertEquals(AbstractConstraint.EQUAL,
                AbstractConstraint.alterComparisonNotObj(AbstractConstraint.OPPOSITE));
        assertEquals(AbstractConstraint.OR,
                AbstractConstraint.alterComparisonNotObj(AbstractConstraint.IMPLIED_BY));
        assertEquals(AbstractConstraint.EXCLUDES,
                AbstractConstraint.alterComparisonNotObj(AbstractConstraint.IMPLIES));
        assertEquals(AbstractConstraint.IMPLIES,
                AbstractConstraint.alterComparisonNotObj(AbstractConstraint.EXCLUDES));
        assertEquals(AbstractConstraint.IMPLIED_BY,
                AbstractConstraint.alterComparisonNotObj(AbstractConstraint.OR));
        assertEquals(AbstractConstraint.INDEPENDENT,
                AbstractConstraint.alterComparisonNotObj(AbstractConstraint.INDEPENDENT));
    }

    public void testAlterComparisonSwitch() throws Exception {
        assertEquals(AbstractConstraint.EQUAL,
                AbstractConstraint.alterComparisonSwitch(AbstractConstraint.EQUAL));
        assertEquals(AbstractConstraint.OPPOSITE,
                AbstractConstraint.alterComparisonSwitch(AbstractConstraint.OPPOSITE));
        assertEquals(AbstractConstraint.IMPLIES,
                AbstractConstraint.alterComparisonSwitch(AbstractConstraint.IMPLIED_BY));
        assertEquals(AbstractConstraint.IMPLIED_BY,
                AbstractConstraint.alterComparisonSwitch(AbstractConstraint.IMPLIES));
        assertEquals(AbstractConstraint.EXCLUDES,
                AbstractConstraint.alterComparisonSwitch(AbstractConstraint.EXCLUDES));
        assertEquals(AbstractConstraint.OR,
                AbstractConstraint.alterComparisonSwitch(AbstractConstraint.OR));
        assertEquals(AbstractConstraint.INDEPENDENT,
                AbstractConstraint.alterComparisonSwitch(AbstractConstraint.INDEPENDENT));
    }

    public void testAlterComparisonAORB() throws Exception {
        assertEquals(AbstractConstraint.IMPLIES,
                AbstractConstraint.alterComparisonAORB(AbstractConstraint.EQUAL,
                    AbstractConstraint.INDEPENDENT));
        assertEquals(AbstractConstraint.INDEPENDENT,
                AbstractConstraint.alterComparisonAORB(AbstractConstraint.EXCLUDES,
                    AbstractConstraint.IMPLIED_BY));
        assertEquals(AbstractConstraint.IMPLIES,
                AbstractConstraint.alterComparisonAORB(AbstractConstraint.EXCLUDES,
                    AbstractConstraint.IMPLIES));
    }

    public void testAlterComparisonAnd() throws Exception {
        assertEquals(AbstractConstraint.EQUAL,
                AbstractConstraint.alterComparisonAnd(AbstractConstraint.IMPLIES,
                    AbstractConstraint.IMPLIED_BY));
    }

    public void testCheckComparisonImplies() throws Exception {
        assertTrue(AbstractConstraint.checkComparisonImplies(AbstractConstraint.IMPLIES));
        assertTrue(AbstractConstraint.checkComparisonImplies(AbstractConstraint.EQUAL));
        assertTrue(AbstractConstraint.checkComparisonImplies(AbstractConstraint.BOTH_TRUE));
        assertTrue(AbstractConstraint.checkComparisonImplies(AbstractConstraint.BOTH_FALSE));
        assertTrue(AbstractConstraint.checkComparisonImplies(AbstractConstraint.LEFT_FALSE_RIGHT_TRUE));
        assertTrue(AbstractConstraint.checkComparisonImplies(AbstractConstraint.LEFT_FALSE));
        assertTrue(AbstractConstraint.checkComparisonImplies(AbstractConstraint.RIGHT_TRUE));
        assertTrue(!AbstractConstraint.checkComparisonImplies(AbstractConstraint.LEFT_TRUE));
        assertTrue(!AbstractConstraint.checkComparisonImplies(AbstractConstraint.RIGHT_FALSE));
        assertTrue(!AbstractConstraint.checkComparisonImplies(AbstractConstraint.OPPOSITE));
        assertTrue(!AbstractConstraint.checkComparisonImplies(AbstractConstraint.OR));
        assertTrue(!AbstractConstraint.checkComparisonImplies(AbstractConstraint.IMPLIED_BY));
        assertTrue(!AbstractConstraint.checkComparisonImplies(AbstractConstraint.INDEPENDENT));
        assertTrue(!AbstractConstraint.checkComparisonImplies(AbstractConstraint.LEFT_TRUE_RIGHT_FALSE));
        assertTrue(!AbstractConstraint.checkComparisonImplies(AbstractConstraint.EXCLUDES));
    }

    public void testCheckComparisonEquals() throws Exception {
        assertTrue(!AbstractConstraint.checkComparisonEquals(AbstractConstraint.IMPLIES));
        assertTrue(AbstractConstraint.checkComparisonEquals(AbstractConstraint.EQUAL));
        assertTrue(AbstractConstraint.checkComparisonEquals(AbstractConstraint.BOTH_TRUE));
        assertTrue(AbstractConstraint.checkComparisonEquals(AbstractConstraint.BOTH_FALSE));
        assertTrue(!AbstractConstraint.checkComparisonEquals(AbstractConstraint.LEFT_FALSE_RIGHT_TRUE));
        assertTrue(!AbstractConstraint.checkComparisonEquals(AbstractConstraint.LEFT_FALSE));
        assertTrue(!AbstractConstraint.checkComparisonEquals(AbstractConstraint.RIGHT_TRUE));
        assertTrue(!AbstractConstraint.checkComparisonEquals(AbstractConstraint.LEFT_TRUE));
        assertTrue(!AbstractConstraint.checkComparisonEquals(AbstractConstraint.RIGHT_FALSE));
        assertTrue(!AbstractConstraint.checkComparisonEquals(AbstractConstraint.OPPOSITE));
        assertTrue(!AbstractConstraint.checkComparisonEquals(AbstractConstraint.OR));
        assertTrue(!AbstractConstraint.checkComparisonEquals(AbstractConstraint.IMPLIED_BY));
        assertTrue(!AbstractConstraint.checkComparisonEquals(AbstractConstraint.INDEPENDENT));
        assertTrue(!AbstractConstraint.checkComparisonEquals(AbstractConstraint.LEFT_TRUE_RIGHT_FALSE));
        assertTrue(!AbstractConstraint.checkComparisonEquals(AbstractConstraint.EXCLUDES));
    }
        
}

