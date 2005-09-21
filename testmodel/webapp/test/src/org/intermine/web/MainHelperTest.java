package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;

import junit.framework.TestCase;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;

/**
 * Tests for the MainHelper class
 *
 * @author Kim Rutherford
 */

public class MainHelperTest extends TestCase {
    public MainHelperTest(String arg) {
        super(arg);
    }

    public void testGetQualifiedTypeName() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        assertEquals("org.intermine.model.testmodel.Employee",
                     MainHelper.getQualifiedTypeName("Employee", model));
        assertEquals("java.lang.String",
                     MainHelper.getQualifiedTypeName("String", model));
        assertEquals("int",
                     MainHelper.getQualifiedTypeName("int", model));
        assertEquals("java.util.Date",
                     MainHelper.getQualifiedTypeName("Date", model));
        assertEquals("java.math.BigDecimal",
                     MainHelper.getQualifiedTypeName("BigDecimal", model));

        try {
            MainHelper.getQualifiedTypeName("SomeUnkownClass", model);
            fail("Expected ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            // expected
        }

        try {
            MainHelper.getQualifiedTypeName("java.lang.String", model);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testGetTypeForPath() throws Exception {
        PathQuery query = new PathQuery(Model.getInstanceByName("testmodel"));
        PathNode employeeNode = query.addNode("Employee");
        employeeNode.setType("Employee");
        query.addNode("Employee.department");
        query.addNode("Employee.age");
        PathNode managerNode = query.addNode("Employee.department.manager");
        managerNode.setType("CEO");
        query.getView().add("Employee");
        query.getView().add("Employee.end");
        query.getView().add("Employee.age");
        query.getView().add("Employee.department.manager");
        query.getView().add("Employee.department.manager.seniority");
        query.getView().add("Employee.department.manager.secretarys.name");
        query.getView().add("Employee.address.address");

        assertEquals("org.intermine.model.testmodel.Employee",
                     MainHelper.getTypeForPath("Employee", query));

        assertEquals("int", MainHelper.getTypeForPath("Employee.end", query));

        assertEquals("int", MainHelper.getTypeForPath("Employee.age", query));

        assertEquals("org.intermine.model.testmodel.CEO",
                     MainHelper.getTypeForPath("Employee.department.manager", query));

        assertEquals("org.intermine.model.testmodel.Department",
                     MainHelper.getTypeForPath("Employee.department", query));

        assertEquals("java.lang.Integer",
                     MainHelper.getTypeForPath("Employee.department.manager.seniority", query));

        assertEquals("java.lang.String",
                     MainHelper.getTypeForPath("Employee.department.manager.secretarys.name",
                                               query));

        assertEquals("java.lang.String",
                     MainHelper.getTypeForPath("Employee.address.address", query));

        try {
            MainHelper.getTypeForPath("Employee.foobar", query);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            MainHelper.getTypeForPath("some.illegal.class", query);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            MainHelper.getTypeForPath("some_illegal_class", query);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            MainHelper.getTypeForPath(null, query);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            MainHelper.getTypeForPath("Employee.department", null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
    
    public void testMakeConstraintSets() {
        HashMap map = new HashMap();
        LogicExpression expr = new LogicExpression("a and b");
        ConstraintSet set = MainHelper.makeConstraintSets(expr, map);
        
        assertEquals(2, map.size());
        assertEquals(ConstraintOp.AND, set.getOp());
        
        HashMap expecting = new HashMap();
        expecting.put("a", set);
        expecting.put("b", set);
        assertEquals(expecting, map);
        
        expr = new LogicExpression("a and (b or c)");
        set = MainHelper.makeConstraintSets(expr, map);
        
        assertEquals(3, map.size());
        assertEquals(ConstraintOp.AND, set.getOp());
        assertEquals(1, set.getConstraints().size());
        assertEquals(ConstraintOp.OR, ((ConstraintSet) set.getConstraints().iterator().next()).getOp());
        
        expecting = new HashMap();
        expecting.put("a", set);
        expecting.put("b", (ConstraintSet) set.getConstraints().iterator().next());
        expecting.put("c", (ConstraintSet) set.getConstraints().iterator().next());
        assertEquals(expecting, map);
        
    }
}
