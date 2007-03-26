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

import junit.framework.TestCase;

public class ConstraintOpTest extends TestCase
{
    public ConstraintOpTest(String arg1) {
        super(arg1);
    }

    public void testFieldValid() {
        assertTrue(ConstraintOp.EQUALS instanceof ConstraintOp);
    }

    public void testIndexValid() {
        assertEquals(ConstraintOp.EQUALS, ConstraintOp.getOpForIndex(ConstraintOp.EQUALS.getIndex()));
    }

    public void testToString() {
        assertEquals("=", ConstraintOp.EQUALS.toString());
    }
}
