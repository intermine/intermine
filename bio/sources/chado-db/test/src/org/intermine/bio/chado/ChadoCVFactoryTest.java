package org.intermine.bio.chado;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashSet;
import java.util.Set;

import java.sql.SQLException;

import junit.framework.TestCase;

/**
 * Tests for ChadoCVFactory.
 *
 * @author Kim Rutherford
 */

public class ChadoCVFactoryTest extends TestCase
{
    public ChadoCVFactoryTest(String arg) {
        super(arg);
    }

    public void testGetChadoCV() throws SQLException {
        ChadoCVFactory factory = new TestChadoCVFactory();
        ChadoCV cv = factory.getChadoCV("test");
        assertEquals(7, cv.getAllCVTerms().size());
        Set<ChadoCVTerm> rootCVTerms = cv.getRootCVTerms();
        assertEquals(3, rootCVTerms.size());
        Set<ChadoCVTerm> expectedRoots = new HashSet<ChadoCVTerm>();
        ChadoCVTerm expRoot1 = new ChadoCVTerm("root1");
        expectedRoots.add(expRoot1);
        ChadoCVTerm expRoot2 = new ChadoCVTerm("root2");
        expectedRoots.add(expRoot2);
        ChadoCVTerm expRoot3 = new ChadoCVTerm("root3");
        expectedRoots.add(expRoot3);
        assertEquals(expectedRoots, rootCVTerms);

        Set<ChadoCVTerm> expectedRoot2Children = new HashSet<ChadoCVTerm>();
        ChadoCVTerm expChild1 = new ChadoCVTerm("child1");
        expectedRoot2Children.add(expChild1);
        ChadoCVTerm expChild4 = new ChadoCVTerm("child4");
        expectedRoot2Children.add(expChild4);

        ChadoCVTerm root2 = null;
        for (ChadoCVTerm rootTerm: rootCVTerms) {
            if (rootTerm.getName().equals("root2")) {
                root2 = rootTerm;
            }
        }

        if (root2 == null) {
            fail("can't find root2");
        }

        assertEquals(expectedRoot2Children, root2.getDirectChildren());
    }
}
