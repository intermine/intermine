package org.intermine.ontology;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

public class DagTermTest extends TestCase
{
    public DagTermTest(String arg) {
        super(arg);
    }

    public void testEquals() {
        DagTerm d1 = new DagTerm("name", "id");
        DagTerm d2 = new DagTerm("name", "id");

        assertTrue(d1.equals(d2));
        assertTrue(d2.equals(d1));

        d1.addSynonym("s1");
        d1.addSynonym("s2");
        assertFalse(d1.equals(d2));
        d2.addSynonym("s1");
        d2.addSynonym("s2");
        assertTrue(d1.equals(d2));

    }
}
