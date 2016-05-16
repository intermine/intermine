package org.intermine.bio.chado;

import junit.framework.TestCase;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Tests for ChadoCVTerm.
 *
 * @author Kim Rutherford
 */

public class ChadoCVTermTest extends TestCase
{
    public void testCVTerm() {
        String testName = "a_name";
        ChadoCVTerm term = new ChadoCVTerm(testName);
        assertEquals(testName, term.getName());
        int hash = term.hashCode();
        ChadoCVTerm parent1 = new ChadoCVTerm("parent1");
        term.getDirectParents().add(parent1);
        ChadoCVTerm child1 = new ChadoCVTerm("child1");
        term.getDirectChildren().add(child1);
        // make sure that addin children doesn't change the hashCode
        assertEquals(hash, term.hashCode());
    }
}
