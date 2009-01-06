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

import junit.framework.TestCase;

/**
 * Tests for the PathNode class
 *
 * @author Kim Rutherford
 */
public class PathNodeTest extends TestCase
{

    public PathNodeTest(String arg) {
        super(arg);
    }

    public void testConstruct() {
        Node parent = new PathNode("Employee");
        Node pathNode = new PathNode(parent, "department", false);
    }
}
