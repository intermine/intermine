package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.LinkedHashSet;

import junit.framework.TestCase;


public class ConstraintSetTest extends TestCase {

    private SimpleConstraint sc1;
    private SimpleConstraint sc2;


    public ConstraintSetTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        sc1 = new SimpleConstraint(new QueryValue("test"), ConstraintOp.EQUALS, new QueryValue("test"));
        Integer testInt1 = new Integer(5);
        Integer testInt2 = new Integer(7);
        sc2 = new SimpleConstraint(new QueryValue(testInt1), ConstraintOp.LESS_THAN, new QueryValue(testInt2));
    }

    public void testAddConstrint() {
        ConstraintSet set = new ConstraintSet(ConstraintOp.AND);
        set.addConstraint(sc1);
        set.addConstraint(sc2);

        LinkedHashSet test = new LinkedHashSet();
        test.add(sc1);
        test.add(sc2);

        assertEquals(test, set.getConstraints());
    }

    public void testRemoveConstrint() {
        ConstraintSet set = new ConstraintSet(ConstraintOp.AND);
        set.addConstraint(sc1);
        set.addConstraint(sc2);
        set.removeConstraint(sc1);

        LinkedHashSet test = new LinkedHashSet();
        test.add(sc2);

        assertEquals(test, set.getConstraints());
    }


    public void testRemoveNotExists() throws Exception {
        try {
            ConstraintSet set = new ConstraintSet(ConstraintOp.OR);
            set.addConstraint(sc1);
            set.removeConstraint(sc2);
            fail("Expected IllegalArgumentExcepion");
        } catch (IllegalArgumentException e) {
        }
    }


    public void testEqual() throws Exception {
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        ConstraintSet cs2 = new ConstraintSet(ConstraintOp.AND);
        ConstraintSet cs3 = new ConstraintSet(ConstraintOp.AND);
        ConstraintSet cs4 = new ConstraintSet(ConstraintOp.AND);
        ConstraintSet cs5 = new ConstraintSet(ConstraintOp.OR);
        ConstraintSet cs6 = new ConstraintSet(ConstraintOp.NAND);

        cs1.addConstraint(sc1).addConstraint(sc2);
        cs2.addConstraint(sc1).addConstraint(sc2);
        assertEquals(cs1, cs1);
        assertEquals(cs1, cs2);

        // cs3 has same two constraints but in different oreder, should still be equal
        cs3.addConstraint(sc2).addConstraint(sc1);
        assertEquals(cs1, cs3);

        cs4.addConstraint(sc1);
        assertEquals(cs4, cs4);
        assertTrue("Expected cs1 and cs4 to not be equal", !cs1.equals(cs4));

        // cs5 is AND, cs4 is OR
        cs5.addConstraint(sc1);
        assertTrue("Expected cs4 and cs5 to not be equal", !cs4.equals(cs5));

        // cs6 is negated, cs4 is not
        cs6.addConstraint(sc1);
        assertTrue("Expected cs4 and cs6 to not be equal", !cs4.equals(cs6));

    }

    public void testHashCode() throws Exception {
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        ConstraintSet cs2 = new ConstraintSet(ConstraintOp.AND);
        ConstraintSet cs3 = new ConstraintSet(ConstraintOp.AND);
        ConstraintSet cs4 = new ConstraintSet(ConstraintOp.AND);
        ConstraintSet cs5 = new ConstraintSet(ConstraintOp.OR);
        ConstraintSet cs6 = new ConstraintSet(ConstraintOp.NAND);

        cs1.addConstraint(sc1).addConstraint(sc2);
        cs2.addConstraint(sc1).addConstraint(sc2);
        assertEquals(cs1.hashCode(), cs1.hashCode());
        assertEquals(cs1.hashCode(), cs2.hashCode());

        // cs3 has same two constraints but in different oreder, should still be equal
        cs3.addConstraint(sc2).addConstraint(sc1);
        assertEquals(cs1.hashCode(), cs3.hashCode());

        cs4.addConstraint(sc1);
        assertEquals(cs4.hashCode(), cs4.hashCode());
        assertTrue("Expected cs1.hashCode() and cs4.hashCode() to not be equal", cs1.hashCode() != cs4.hashCode());

        // cs5 is AND, cs4 is OR
        cs5.addConstraint(sc1);
        assertTrue("Expected cs4.hashCode() and cs5.hashCode() to not be equal", cs4.hashCode() != cs5.hashCode());

        // cs6 is negated, cs4 is not
        cs6.addConstraint(sc1);
        assertTrue("Expected cs4.hashCode() and cs6.hashCode() to not be equal", cs4.hashCode() != cs6.hashCode());

    }


}
