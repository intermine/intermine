package org.intermine.bio.chado;

/*
 * Copyright (C) 2002-2017 FlyMine
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

        ChadoCVTerm expChild1 = new ChadoCVTerm("child1");
        ChadoCVTerm expChild2 = new ChadoCVTerm("child2");
        ChadoCVTerm expChild3 = new ChadoCVTerm("child3");
        ChadoCVTerm expChild4 = new ChadoCVTerm("child4");

        Set<ChadoCVTerm> expectedRoot2DirectChildren = new HashSet<ChadoCVTerm>();

        expectedRoot2DirectChildren.add(expChild1);
        expectedRoot2DirectChildren.add(expChild4);

        Set<ChadoCVTerm> expectedRoot2AllChildren = new HashSet<ChadoCVTerm>();
        expectedRoot2AllChildren.add(expChild1);
        expectedRoot2AllChildren.add(expChild2);
        expectedRoot2AllChildren.add(expChild3);
        expectedRoot2AllChildren.add(expChild4);

        ChadoCVTerm root1 = null;
        ChadoCVTerm root2 = null;
        ChadoCVTerm root3 = null;
        for (ChadoCVTerm rootTerm: rootCVTerms) {
            if ("root1".equals(rootTerm.getName())) {
                root1 = rootTerm;
            } else {
                if ("root2".equals(rootTerm.getName())) {
                    root2 = rootTerm;
                } else {
                    if ("root3".equals(rootTerm.getName())) {
                        root3 = rootTerm;
                    } else {
                       fail("unknown root: " + rootTerm.getName());
                    }
                }
            }
        }

        assertEquals(expectedRoot2DirectChildren, root2.getDirectChildren());
        Set<ChadoCVTerm> root2AllChildren = root2.getAllChildren();
        assertEquals(expectedRoot2AllChildren, root2AllChildren);

        for (ChadoCVTerm root2Child: root2AllChildren) {
            if ("child4".equals(root2Child.getName())) {
                assertEquals(5, root2Child.getAllParents().size());
            }
        }
    }
}
