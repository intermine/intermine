package org.intermine.web.results;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

public class ColumnTest extends TestCase
{
    public ColumnTest(String arg) {
        super(arg);
    }

    private Column col1, col2, col3, col4;

    public void setUp() {
        col1 = new Column();
        col1.setAlias("c1");
        col1.setIndex(1);
        col1.setVisible(true);
        col2 = new Column();
        col2.setAlias("c1");
        col2.setIndex(1);
        col2.setVisible(false);
        col3 = new Column();
        col3.setAlias("c1");
        col3.setIndex(2);
        col3.setVisible(false);
        col4 = new Column();
        col4.setAlias("c2");
        col4.setIndex(2);
        col4.setVisible(false);
    }

    public void testUpdate() {
        col1.setVisible(true);
        col1.update(col2);
        assertFalse(col1.isVisible());

        col1.setVisible(true);
        col1.update(col3);
        assertFalse(col1.isVisible());

        col1.setVisible(true);
        try {
            col1.update(col4);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testEquals() {
        assertEquals(col1, col1);
        assertEquals(col1, col2);
        assertEquals(col1, col3);
        assertFalse(col1.equals(col4));
        assertFalse(col3.equals(col4));
    }

    public void testHashCode() {
        assertEquals(col1.hashCode(), col2.hashCode());
        assertEquals(col1.hashCode(), col3.hashCode());
        assertTrue(col1.hashCode() != col4.hashCode());
        assertTrue(col3.hashCode() != col4.hashCode());
    }

}
