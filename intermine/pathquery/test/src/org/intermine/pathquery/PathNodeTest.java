package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.HashSet;

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

    public void testEquals() {
        Node parent = new PathNode("Employee");
        Node n1 = new PathNode(parent, "department", false);
        Node n2 = new PathNode(parent, "department", false);
        Node n3 = new PathNode(parent, "address", false);
        Node n4 = new PathNode(n1, "company", false);
        Node n5 = new PathNode(n4, "address", false);
        Node parent2 = new PathNode("Manager");
        Node n6 = new PathNode(parent2, "department", false);

        assertEquals(n1, n2);
        assertFalse(n1.equals(n3));
        assertFalse(n3.equals(n5));
        assertFalse(n1.equals(n6));
    }

    public void testOuterJoinGroup() {
        Node n = new PathNode("Employee");
        assertEquals("Employee", n.getOuterJoinGroup());
        n = new PathNode("Employee.name");
        assertEquals("Employee", n.getOuterJoinGroup());
        n = new PathNode("Employee.department.name");
        assertEquals("Employee", n.getOuterJoinGroup());
        n = new PathNode("Employee:department");
        assertEquals("Employee:department", n.getOuterJoinGroup());
        n = new PathNode("Employee:department.companys.name");
        assertEquals("Employee:department", n.getOuterJoinGroup());
        n = new PathNode("Employee:department:companys.name");
        assertEquals("Employee:department:companys", n.getOuterJoinGroup());
    }

    public void testFindForcedInnerJoins() {
        assertEquals(new HashSet(Arrays.asList("Company.department", "Company.department.companys")), PathNode.findForcedInnerJoins("Company", "Company.department.companys"));
        assertEquals(new HashSet(Arrays.asList("Company.department.companys", "Company.department.companys.department")), PathNode.findForcedInnerJoins("Company.department", "Company.department.companys.department"));
        assertEquals(new HashSet(Arrays.asList("Company.department", "Company.department.address", "Company.address")), PathNode.findForcedInnerJoins("Company.address", "Company.department.address"));
    }
}
