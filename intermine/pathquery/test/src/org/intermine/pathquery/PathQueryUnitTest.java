package org.intermine.pathquery;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ConstraintOp;

public class PathQueryUnitTest extends TestCase
{
    public void testCheckPathFormat() throws Exception {
        PathQuery.checkPathFormat("Employee");
        PathQuery.checkPathFormat("Employee.name");
        PathQuery.checkPathFormat("Employee.department.address.address");
        try {
            PathQuery.checkPathFormat(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            PathQuery.checkPathFormat("Department.employees[CEO].name");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            PathQuery.checkPathFormat("Department:employees.name");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            PathQuery.checkPathFormat("Department$.employees.name");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            PathQuery.checkPathFormat("Department .employees.name");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testView() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery q = new PathQuery(model);
        assertEquals(model, q.getModel());
        assertEquals(Collections.EMPTY_LIST, q.getView());
        q.addView("Employee.name");
        assertEquals(Collections.singletonList("Employee.name"), q.getView());
        q.addViews("Employee.department.name", "Employee.department.address.address");
        assertEquals(Arrays.asList("Employee.name", "Employee.department.name", "Employee.department.address.address"), q.getView());
        q.removeView("Employee.name");
        assertEquals(Arrays.asList("Employee.department.name", "Employee.department.address.address"), q.getView());
        try {
            q.removeView("lkjadsfldjsaf");
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
        assertEquals(Arrays.asList("Employee.department.name", "Employee.department.address.address"), q.getView());
        q.addViewSpaceSeparated("Employee.name Employee.department.company.name");
        assertEquals(Arrays.asList("Employee.department.name", "Employee.department.address.address", "Employee.name", "Employee.department.company.name"), q.getView());
        q.clearView();
        assertEquals(Collections.EMPTY_LIST, q.getView());
        try {
            q.addView("Emploelkjadf k adf asd");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            q.addViews("kjsdaf a;lkjadsf", "Employee.name");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            q.addViews("Employee.name", null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            String array[] = null;
            q.addViews(array);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            q.addViews(Arrays.asList("kjsdaf a;lkjadsf", "Employee.name"));
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            q.addViews(Arrays.asList("Employee.name", null));
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            Collection<String> col = null;
            q.addViews(col);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            q.addViewSpaceSeparated("kjsdafa;lkjadsf Employee.name");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            q.addViewSpaceSeparated(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        assertEquals(Collections.EMPTY_LIST, q.getView());
        q.addViews(Arrays.asList("Employee.name", "Employee.age"));
        assertEquals(Arrays.asList("Employee.name", "Employee.age"), q.getView());
        PathQuery q2 = q.clone();
        q.addView("Employee.department.name");
        assertEquals(Arrays.asList("Employee.name", "Employee.age", "Employee.department.name"), q.getView());
        assertEquals(Arrays.asList("Employee.name", "Employee.age"), q2.getView());
        assertEquals(Collections.EMPTY_LIST, q2.verifyQuery());
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.age\"></query>", PathQueryBinding.marshal(q2, "test", "testmodel", 1));
        q2 = new PathQuery(q2);
        assertEquals(Arrays.asList("Employee.name", "Employee.age", "Employee.department.name"), q.getView());
        assertEquals(Arrays.asList("Employee.name", "Employee.age"), q2.getView());
        assertEquals(Collections.EMPTY_LIST, q2.verifyQuery());
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.age\"></query>", PathQueryBinding.marshal(q2, "test", "testmodel", 1));
        PathQuery q3 = new PathQuery(model);
        assertEquals("Queries with empty views are not valid",
                Arrays.asList("No columns selected for output"), q3.verifyQuery());
    }

    public void testOrderBy() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery q = new PathQuery(model);
        assertEquals(Collections.EMPTY_LIST, q.getOrderBy());
        q.addView("Employee.age");
        q.addOrderBy("Employee.name", OrderDirection.ASC);
        assertEquals("Employee.name", q.getOrderBy().iterator().next().getOrderPath());
        assertEquals(OrderDirection.ASC, q.getOrderBy().iterator().next().getDirection());
        assertEquals(Collections.singletonList(new OrderElement("Employee.name", OrderDirection.ASC)), q.getOrderBy());
        q.addOrderBys(new OrderElement("Employee.department.name", OrderDirection.ASC), new OrderElement("Employee.department.address.address", OrderDirection.DESC));
        assertEquals(Arrays.asList(new OrderElement("Employee.name", OrderDirection.ASC), new OrderElement("Employee.department.name", OrderDirection.ASC), new OrderElement("Employee.department.address.address", OrderDirection.DESC)), q.getOrderBy());
        Iterator<OrderElement> oeIter = q.getOrderBy().iterator();
        OrderElement oe1 = oeIter.next();
        OrderElement oe2 = oeIter.next();
        assertFalse(oe1.equals(oe2));
        assertTrue(oe1.equals(oe1));
        assertFalse(oe1.hashCode() == oe2.hashCode());
        assertFalse("Fred".equals(oe1));
        assertTrue(oe1.equals(new OrderElement("Employee.name", OrderDirection.ASC)));
        assertFalse(oe1.equals(new OrderElement("Employee.name", OrderDirection.DESC)));
        q.removeOrderBy("Employee.name");
        assertEquals(Arrays.asList(new OrderElement("Employee.department.name", OrderDirection.ASC), new OrderElement("Employee.department.address.address", OrderDirection.DESC)), q.getOrderBy());
        try {
            q.removeOrderBy("lkjadsfldjsaf");
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
        assertEquals(Arrays.asList(new OrderElement("Employee.department.name", OrderDirection.ASC), new OrderElement("Employee.department.address.address", OrderDirection.DESC)), q.getOrderBy());
        q.addOrderBySpaceSeparated("Employee.name asc Employee.department.company.name desc");
        assertEquals(Arrays.asList(new OrderElement("Employee.department.name", OrderDirection.ASC), new OrderElement("Employee.department.address.address", OrderDirection.DESC), new OrderElement("Employee.name", OrderDirection.ASC), new OrderElement("Employee.department.company.name", OrderDirection.DESC)), q.getOrderBy());
        q.clearOrderBy();
        assertEquals(Collections.EMPTY_LIST, q.getOrderBy());
        try {
            new OrderElement("Emploelkjadf k adf asd", OrderDirection.ASC);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            q.addOrderBy(null, OrderDirection.ASC);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            q.addOrderBy("Employee.name", null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            q.addOrderBy(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            OrderElement array[] = null;
            q.addOrderBys(array);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            Collection<OrderElement> col = null;
            q.addOrderBys(col);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            q.addOrderBys(new OrderElement("Employee.name", OrderDirection.ASC), null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            Collection<OrderElement> col = new ArrayList<OrderElement>();
            col.add(new OrderElement("Employee.name", OrderDirection.ASC));
            col.add(null);
            q.addOrderBys(col);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            q.addOrderBySpaceSeparated("Employee.name desc kjsdafa;lkjadsf asc");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            q.addOrderBySpaceSeparated("Employee.name");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            q.addOrderBySpaceSeparated("Employee.name flibble");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            q.addOrderBySpaceSeparated(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        assertEquals(Collections.EMPTY_LIST, q.getOrderBy());
        assertEquals(2, OrderDirection.values().length);
        assertEquals(OrderDirection.ASC, OrderDirection.valueOf("ASC"));
        PathQuery q2 = q.clone();
        q.addOrderBy("Employee.name", OrderDirection.ASC);
        assertEquals("Employee.name", q.getOrderBy().iterator().next().getOrderPath());
        assertEquals(Collections.EMPTY_LIST, q2.getOrderBy());
        assertEquals(Collections.EMPTY_LIST, q.verifyQuery());
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Employee.age\" sortOrder=\"Employee.name asc\"></query>", PathQueryBinding.marshal(q, "test", "testmodel", 1));
        q2 = new PathQuery(q2);
        assertEquals(Collections.EMPTY_LIST, q2.getOrderBy());
        assertEquals(Collections.EMPTY_LIST, q.verifyQuery());
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Employee.age\" sortOrder=\"Employee.name asc\"></query>", PathQueryBinding.marshal(q, "test", "testmodel", 1));
    }

    public void testAttributeConstraints() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery q = new PathQuery(model);
        q.addViews("Employee.age");
        assertEquals(Collections.EMPTY_MAP, q.getConstraints());
        PathConstraintAttribute c = new PathConstraintAttribute("Employee.name", ConstraintOp.EQUALS, "Fred");
        String code = q.addConstraint(c);
        assertEquals(Collections.singletonMap(c, code), q.getConstraints());
        assertEquals("Employee.name", c.getPath());
        assertEquals(ConstraintOp.EQUALS, c.getOp());
        assertEquals("Fred", c.getValue());
        try {
            new PathConstraintAttribute(null, ConstraintOp.EQUALS, "Fred");
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            new PathConstraintAttribute("Employee.name", null, "Fred");
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            new PathConstraintAttribute("Employee.name", ConstraintOp.EQUALS, null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            new PathConstraintAttribute("Employee.name", ConstraintOp.AND, "Fred");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            q.addConstraint(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        assertEquals(Collections.singletonMap(c, code), q.getConstraints());
        assertEquals(code, q.addConstraint(c));
        assertEquals(Collections.singletonMap(c, code), q.getConstraints());
        assertEquals(c, q.getConstraintForCode(code));
        try {
            q.getConstraintForCode(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            q.getConstraintForCode("Z");
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
        PathConstraintAttribute c2 = new PathConstraintAttribute("Employee.age", ConstraintOp.LESS_THAN, "50");
        String code2 = q.addConstraint(c2);
        assertFalse("Codes are the same: " + code + " and " + code2 + ", constraints: " + q.getConstraints(), code.equals(code2));
        Map<PathConstraint, String> expected = new HashMap<PathConstraint, String>();
        expected.put(c, code);
        expected.put(c2, code2);
        assertEquals(expected, q.getConstraints());
        assertEquals(Collections.singletonList(c), q.getConstraintsForPath("Employee.name"));
        assertEquals(Collections.singletonList(c2), q.getConstraintsForPath("Employee.age"));
        assertEquals(Collections.emptyList(), q.getConstraintsForPath("Employee"));
        try {
            q.addConstraint(c, "G");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
        }
        PathConstraintAttribute c3 = new PathConstraintAttribute("Employee.address.address", ConstraintOp.EQUALS, "Bob");
        try {
            q.addConstraint(c3, null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            q.addConstraint(null, "G");
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            q.addConstraint(c3, "g");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            q.addConstraint(c3, code);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
        }
        assertEquals(expected, q.getConstraints());
        q.addConstraint(c3, "G");
        expected.put(c3, "G");
        assertEquals(expected, q.getConstraints());
        q.addConstraint(c3, "G");
        assertEquals(expected, q.getConstraints());
        assertEquals("A and B and G", q.getConstraintLogic());
        q.removeConstraint(c);
        expected.remove(c);
        assertEquals(expected, q.getConstraints());
        assertEquals("B and G", q.getConstraintLogic());
        try {
            q.removeConstraint(c);
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
        try {
            q.removeConstraint(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        assertEquals(expected, q.getConstraints());
        assertEquals("B and G", q.getConstraintLogic());
        q.removeConstraint(c2);
        assertEquals(Collections.singletonMap(c3, "G"), q.getConstraints());
        assertEquals("G", q.getConstraintLogic());
        q.removeConstraint(c3);
        assertEquals(Collections.EMPTY_MAP, q.getConstraints());
        assertEquals("", q.getConstraintLogic());
        q.clearConstraints();
        assertEquals(Collections.EMPTY_MAP, q.getConstraints());
        assertEquals("", q.getConstraintLogic());
        q.addConstraint(c, "H");
        assertEquals(Collections.singletonMap(c, "H"), q.getConstraints());
        assertEquals("H", q.getConstraintLogic());
        q.clearConstraints();
        assertEquals(Collections.EMPTY_MAP, q.getConstraints());
        assertEquals("", q.getConstraintLogic());
        try {
            PathConstraint array[] = null;
            q.addConstraints(array);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            Collection<PathConstraint> col = null;
            q.addConstraints(col);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            q.addConstraints(new PathConstraintAttribute("Employee.name", ConstraintOp.EQUALS, "Fred"), null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            Collection<PathConstraint> col = new ArrayList<PathConstraint>();
            col.add(new PathConstraintAttribute("Employee.name", ConstraintOp.EQUALS, "Fred"));
            col.add(null);
            q.addConstraints(col);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        assertEquals("q should not have any constraints at this point", Collections.EMPTY_MAP, q.getConstraints());
        assertEquals("q's logic should equally be empty", "", q.getConstraintLogic());
        q.addConstraints(c, c2);
        assertEquals("The constraints we add turn up as keys of the getConstraints map",
                new HashSet<PathConstraint>(Arrays.asList(c, c2)), q.getConstraints().keySet());
        q.clearConstraints();
        assertEquals("Clearing the constraints brings us back to the pristine state", Collections.EMPTY_MAP, q.getConstraints());
        assertEquals("Logic is cleared too", "", q.getConstraintLogic());
        Collection<PathConstraint> col = new ArrayList<PathConstraint>();
        col.add(c);
        col.add(c2);
        q.addConstraints(col);
        assertEquals(new HashSet<PathConstraint>(Arrays.asList(c, c2)), q.getConstraints().keySet());
        PathQuery q2 = q.clone();
        PathQuery q3 = new PathQuery(q2);
        q2.clearConstraints();
        assertEquals(new HashSet<PathConstraint>(Arrays.asList(c, c2)), q.getConstraints().keySet());
        assertEquals(Collections.EMPTY_MAP, q2.getConstraints());
        assertEquals(Collections.EMPTY_LIST, q.verifyQuery());
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Employee.age\" constraintLogic=\"A and B\"><constraint path=\"Employee.name\" code=\"A\" op=\"=\" value=\"Fred\"/><constraint path=\"Employee.age\" code=\"B\" op=\"&lt;\" value=\"50\"/></query>", PathQueryBinding.marshal(q, "test", "testmodel", 1));
        assertEquals(new HashSet<PathConstraint>(Arrays.asList(c, c2)), q3.getConstraints().keySet());
        assertEquals(Collections.EMPTY_LIST, q3.verifyQuery());
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Employee.age\" constraintLogic=\"A and B\"><constraint path=\"Employee.name\" code=\"A\" op=\"=\" value=\"Fred\"/><constraint path=\"Employee.age\" code=\"B\" op=\"&lt;\" value=\"50\"/></query>", PathQueryBinding.marshal(q3, "test", "testmodel", 1));
        q.replaceConstraint(c2, new PathConstraintAttribute("Employee.flibble", ConstraintOp.EQUALS, "Flobble"));
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Employee.age\" constraintLogic=\"A and B\"><constraint path=\"Employee.name\" code=\"A\" op=\"=\" value=\"Fred\"/><constraint path=\"Employee.flibble\" code=\"B\" op=\"=\" value=\"Flobble\"/></query>", PathQueryBinding.marshal(q, "test", "testmodel", 1));
        try {
            q3.replaceConstraint(c2, null);
            fail("Expected exception");
        } catch (NullPointerException e) {
        }
        try {
            q3.replaceConstraint(null, c2);
            fail("Expected exception");
        } catch (NullPointerException e) {
        }
        try {
            q3.replaceConstraint(new PathConstraintAttribute("Employee.flibble", ConstraintOp.EQUALS, "Flobble"), new PathConstraintAttribute("Employee.flobble", ConstraintOp.EQUALS, "Flibble"));
            fail("Expected exception");
        } catch (NoSuchElementException e) {
        }
        try {
            q3.replaceConstraint(c2, c);
            fail("Expected exception");
        } catch (IllegalStateException e) {
        }
        try {
            q3.replaceConstraint(c2, new PathConstraintSubclass("Employee.department", "Department"));
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot replace a PathConstraintAttribute with a PathConstraintSubclass", e.getMessage());
        }
        try {
            q3.replaceConstraint(c2, new PathConstraintSubclass("Employee.department", "Department"));
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot replace a PathConstraintAttribute with a PathConstraintSubclass", e.getMessage());
        }
    }

    public void testOuterJoinStyle() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery q = new PathQuery(model);
        q.addView("Employee.department.name");
        q.setOuterJoinStatus("Employee.department", OuterJoinStatus.INNER);
        assertEquals(OuterJoinStatus.INNER, q.getOuterJoinStatus("Employee.department"));
        assertNull(q.getOuterJoinStatus("lkjhasdflkjhasdfaskj"));
        try {
            q.setOuterJoinStatus(null, OuterJoinStatus.INNER);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            q.setOuterJoinStatus("Empsda adsfasdf", OuterJoinStatus.INNER);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            q.getOuterJoinStatus(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            q.getOuterJoinStatus("Empsda adsfasdf");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        assertEquals(Collections.singletonMap("Employee.department", OuterJoinStatus.INNER), q.getOuterJoinStatus());
        assertEquals(2, OuterJoinStatus.values().length);
        assertEquals(OuterJoinStatus.INNER, OuterJoinStatus.valueOf("INNER"));
        assertEquals(Collections.EMPTY_LIST, q.verifyQuery());
        assertTrue(q.isValid());
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Employee.department.name\"><join path=\"Employee.department\" style=\"INNER\"/></query>", PathQueryBinding.marshal(q, "test", "testmodel", 1));
        q.setOuterJoinStatus("Employee.department", null);
        assertEquals(Collections.emptyMap(), q.getOuterJoinStatus());
        assertEquals("Employee", q.getOuterJoinGroup("Employee"));
        assertEquals("Employee", q.getOuterJoinGroup("Employee.department"));
        assertEquals("Employee", q.getOuterJoinGroup("Employee.department.name"));
        assertEquals(Collections.emptyMap(), q.getOuterMap());
        q.setOuterJoinStatus("Employee.department", OuterJoinStatus.OUTER);
        assertEquals("Employee", q.getOuterJoinGroup("Employee"));
        assertEquals("Employee.department", q.getOuterJoinGroup("Employee.department"));
        assertEquals("Employee.department", q.getOuterJoinGroup("Employee.department.name"));
        assertEquals(Collections.singletonMap("Employee.department", Boolean.TRUE), q.getOuterMap());
        try {
            q.getOuterJoinGroup(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            q.getOuterJoinGroup("fliakjdsf.kasdfs");
            fail("Expected PathException");
        } catch (PathException e) {
        }
        try {
            q.getOuterJoinGroup("Employee.address");
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
        assertTrue(q.isPathCompletelyInner("Employee.name"));
        assertFalse(q.isPathCompletelyInner("Employee.department"));
    }

    public void testGetCandidateLoops() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery q = new PathQuery(model);
        q.addView("Employee.department.employees.name");
        assertEquals(Collections.singleton("Employee"), q.getCandidateLoops("Employee.department.employees"));
        assertEquals(Collections.singleton("Employee.department.employees"), q.getCandidateLoops("Employee"));
        q.addView("Employee.department.company.departments.employees.name");
        assertEquals(new HashSet<String>(Arrays.asList("Employee.department.employees", "Employee.department.company.departments.employees")), q.getCandidateLoops("Employee"));
        assertEquals(new HashSet<String>(Arrays.asList("Employee", "Employee.department.company.departments.employees")), q.getCandidateLoops("Employee.department.employees"));
        assertEquals(new HashSet<String>(Arrays.asList("Employee.department.employees", "Employee")), q.getCandidateLoops("Employee.department.company.departments.employees"));
        q.setOuterJoinStatus("Employee.department", OuterJoinStatus.OUTER);
        assertEquals(Collections.emptySet(), q.getCandidateLoops("Employee"));
        assertEquals(Collections.singleton("Employee.department.company.departments.employees"), q.getCandidateLoops("Employee.department.employees"));
        assertEquals(Collections.singleton("Employee.department.employees"), q.getCandidateLoops("Employee.department.company.departments.employees"));
        q.setOuterJoinStatus("Employee.department.company", OuterJoinStatus.OUTER);
        assertEquals(Collections.emptySet(), q.getCandidateLoops("Employee"));
        assertEquals(Collections.emptySet(), q.getCandidateLoops("Employee.department.employees"));
        assertEquals(Collections.emptySet(), q.getCandidateLoops("Employee.department.company.departments.employees"));
        q.setOuterJoinStatus("Employee.department", OuterJoinStatus.INNER);
        assertEquals(Collections.singleton("Employee.department.employees"), q.getCandidateLoops("Employee"));
        assertEquals(Collections.singleton("Employee"), q.getCandidateLoops("Employee.department.employees"));
        assertEquals(Collections.emptySet(), q.getCandidateLoops("Employee.department.company.departments.employees"));
        try {
            q.getCandidateLoops(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            q.getCandidateLoops("Employee.name");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            q.getCandidateLoops("Employee.lkjhasdfdsa");
            fail("Expected PathException");
        } catch (PathException e) {
        }
        try {
            q.getCandidateLoops("Department.employees");
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
        assertEquals(Collections.emptySet(), q.getCandidateLoops("Employee.address"));
        assertEquals(new HashSet<String>(Arrays.asList("Employee", "Employee.department.employees")), q.getCandidateLoops("Employee.department.employees.department.employees"));
        q.setOuterJoinStatus("Employee.department.company", OuterJoinStatus.INNER);
        assertEquals(new HashSet<String>(Arrays.asList("Employee", "Employee.department.employees", "Employee.department.company.departments.employees")), q.getCandidateLoops("Employee.department.employees.department.employees"));
        q.clearView();
        q.clearOuterJoinStatus();
        assertEquals("Clearing the view means that there are no candidate loops",
                Collections.emptySet(), q.getCandidateLoops("Employee.department.employees.department.employees"));
    }

    public void testCandidateLoopsNoViewNoOuterJoins() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery q = new PathQuery(model);
        assertEquals("There are no candidate loops on a freshly minted query",
                Collections.emptySet(), q.getCandidateLoops("Employee.department.employees.department.employees"));
    }

    public void testCandidateLoopsOneViewNoOuterJoins() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery q = new PathQuery(model);
        q.addView("Employee.age");
        assertEquals("A rooted query will report candidate loops",
                new HashSet<String>(Arrays.asList("Employee", "Employee.department.employees")),
                q.getCandidateLoops("Employee.department.employees.department.employees"));
    }

    public void testDescription() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery q = new PathQuery(model);
        q.addView("Employee.name");
        q.setDescription("Employee.name", "Flibble");
        assertEquals("Flibble", q.getDescription("Employee.name"));
        assertEquals(Collections.EMPTY_LIST, q.verifyQuery());
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Employee.name\"><pathDescription pathString=\"Employee.name\" description=\"Flibble\"/></query>", PathQueryBinding.marshal(q, "test", "testmodel", 1));
        assertNull(q.getDescription("alkjhadlfkjh"));
        try {
            q.setDescription(null, "Flibble");
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            q.setDescription("Empsda adsfasdf", "Flibble");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            q.getDescription(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            q.getDescription("Empsda adsfasdf");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        assertEquals(Collections.singletonMap("Employee.name", "Flibble"), q.getDescriptions());
        q.setDescription("Employee.name", null);
        assertEquals(Collections.EMPTY_MAP, q.getDescriptions());
        assertEquals(Collections.EMPTY_LIST, q.verifyQuery());
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Employee.name\"></query>", PathQueryBinding.marshal(q, "test", "testmodel", 1));
    }

    public void testConstraintLogic() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery q = new PathQuery(model);
        q.addView("Employee.name");
        q.setConstraintLogic("F");
        assertEquals("", q.getConstraintLogic());
        q.addConstraint(new PathConstraintAttribute("Employee.name", ConstraintOp.EQUALS, "Fred"));
        q.addConstraint(new PathConstraintAttribute("Employee.age", ConstraintOp.LESS_THAN, "50"));
        q.addConstraint(new PathConstraintAttribute("Employee.age", ConstraintOp.GREATER_THAN, "2"));
        assertEquals("A and B and C", q.getConstraintLogic());
        q.setConstraintLogic("A or B or C");
        assertEquals("A or B or C", q.getConstraintLogic());
        q.setConstraintLogic("(A or B) and C");
        assertEquals("(A or B) and C", q.getConstraintLogic());
        q.setConstraintLogic("A or (B and C)");
        assertEquals("A or (B and C)", q.getConstraintLogic());
        q.setConstraintLogic("A or B");
        assertEquals("(A or B) and C", q.getConstraintLogic());
        q.setConstraintLogic("A");
        assertEquals("A and B and C", q.getConstraintLogic());
        q.setConstraintLogic("A or B or D");
        assertEquals("(A or B) and C", q.getConstraintLogic());
        q.setConstraintLogic("F");
        assertEquals("A and B and C", q.getConstraintLogic());
        assertEquals(Collections.EMPTY_LIST, q.verifyQuery());
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Employee.name\" constraintLogic=\"A and B and C\"><constraint path=\"Employee.name\" code=\"A\" op=\"=\" value=\"Fred\"/><constraint path=\"Employee.age\" code=\"B\" op=\"&lt;\" value=\"50\"/><constraint path=\"Employee.age\" code=\"C\" op=\"&gt;\" value=\"2\"/></query>", PathQueryBinding.marshal(q, "test", "testmodel", 1));
    }

    public void testSubclassConstraint() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery q = new PathQuery(model);
        q.addView("Department.name");
        q.addView("Department.employees.name");
        q.addView("Department.employees.title");
        assertNull(q.addConstraint(new PathConstraintSubclass("Department.employees", "Manager")));
        assertEquals(Collections.EMPTY_LIST, q.verifyQuery());
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Department.name Department.employees.name Department.employees.title\"><constraint path=\"Department.employees\" type=\"Manager\"/></query>", PathQueryBinding.marshal(q, "test", "testmodel", 1));
        q.clearConstraints();
        try {
            q.addConstraint(new PathConstraintSubclass("Department.employees", "Manager"), "A");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot associate a code with a subclass constraint. Use the addConstraint(PathConstraint) method instead", e.getMessage());
        }
    }
    
    public void testMultiTypeConstraint() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery q = new PathQuery(model);
        q.addView("Employable.name");
        assertNotNull(q.addConstraint(new PathConstraintMultitype(
                "Employable", ConstraintOp.ISA, Arrays.asList("Contractor", "Manager"))));
        assertEquals(Collections.EMPTY_LIST, q.verifyQuery());
        
        q.clearConstraints();
        assertTrue(q.isValid());
        q.addConstraint(new PathConstraintMultitype( // Bank cannot be an Employable
                "Employable", ConstraintOp.ISA, Arrays.asList("Contractor", "Bank")));
        assertTrue(!q.isValid());
        
        q.clearConstraints();
        assertTrue(q.isValid());
        q.addConstraint(new PathConstraintMultitype( // Contractors are not Employees
                "Employee", ConstraintOp.ISA, Arrays.asList("Contractor", "Manager")));
        assertTrue(!q.isValid());
        
        q.clearConstraints();
        assertTrue(q.isValid());
        q.addConstraint(new PathConstraintMultitype( // No such classes Foo and Bar
                "Employee", ConstraintOp.ISA, Arrays.asList("Foo", "Bar")));
        assertTrue(!q.isValid());
    }

    public void testVerifyQuery() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery q = new PathQuery(model);
        q.addView("Department.name");
        q.addConstraint(new PathConstraintSubclass("Department", "Broke"));
        q.addConstraint(new PathConstraintSubclass("Department.name", "Employee"));
        q.addConstraint(new PathConstraintSubclass("Department.employees", "String"));
        assertEquals(Arrays.asList("Root node Department may not have a subclass constraint", "Path Department.name (from subclass constraint) must not be an attribute", "Subclass String (for path Department.employees) is not in the model"), q.verifyQuery());
        try {
            q.getRootClass();
            fail("Expected exception");
        } catch (PathException e) {
        }
        try {
            q.getSubclasses();
            fail("Expected exception");
        } catch (PathException e) {
        }
        try {
            q.getOuterJoinGroups();
            fail("Expected exception");
        } catch (PathException e) {
        }
        try {
            q.getConstraintGroups();
            fail("Expected exception");
        } catch (PathException e) {
        }
        q.clearConstraints();
        q.addView("Department.employees.name");
        q.addConstraint(new PathConstraintSubclass("Department.employees", "Manager"));
        q.addConstraint(new PathConstraintSubclass("Department.employees", "CEO"));
        assertEquals(Collections.singletonList("Cannot have multiple subclass constraints on path Department.employees"), q.verifyQuery());
        q.clearConstraints();
        q.addConstraint(new PathConstraintSubclass("Department.kjasdf", "Manager"));
        assertEquals(Collections.singletonList("Path Department.kjasdf (from subclass constraint) is not in the model"), q.verifyQuery());
        q.clearConstraints();
        q.addConstraint(new PathConstraintSubclass("Department.employees", "Broke"));
        assertEquals(Collections.singletonList("Subclass constraint on path Department.employees (type Employee) restricting to type Broke is not possible, as it is not a subclass"), q.verifyQuery());
        q.clearConstraints();
        q.addView("Employee.name");
        q.addView("Department");
        assertEquals(Arrays.asList("Multiple root classes in query: Department and Employee", "Path Department in view list must be an attribute"), q.verifyQuery());
        q.clearView();
        q.addView("Department.flibble");
        assertEquals(Collections.singletonList("Path Department.flibble in view list is not in the model"), q.verifyQuery());
        q.clearView();
        q.addView("Department.employees.title");
        assertEquals(Collections.singletonList("Path Department.employees.title in view list is not in the model"), q.verifyQuery());
        q.addConstraint(new PathConstraintSubclass("Department.employees", "Manager"));
        assertEquals(Collections.EMPTY_LIST, q.verifyQuery());
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Department.employees.title\"><constraint path=\"Department.employees\" type=\"Manager\"/></query>", PathQueryBinding.marshal(q, "test", "testmodel", 1));
        q.addConstraint(new PathConstraintAttribute("Employee.name", ConstraintOp.EQUALS, "Fred"));
        assertEquals(Collections.singletonList("Multiple root classes in query: Department and Employee"), q.verifyQuery());
        q.clearView();
        q.clearConstraints();
        q.addView("Department.name");
        q.addOrderBy("Department.flibble", OrderDirection.ASC);
        q.addOrderBy("Employee.name", OrderDirection.DESC);
        q.addOrderBy("Department", OrderDirection.ASC);
        assertEquals(Arrays.asList("Path Department.flibble in order by list is not in the model", "Order by element for path Employee.name is not relevant to the query", "Path Department in order by list must be an attribute"), q.verifyQuery());
        q.clearOrderBy();
        q.addConstraint(new PathConstraintAttribute("Department.flibble", ConstraintOp.EQUALS, "Fred"));
        assertEquals(Collections.singletonList("Path Department.flibble in constraint is not in the model"), q.verifyQuery());
        q.clearConstraints();
        q.setOuterJoinStatus("Department", OuterJoinStatus.INNER);
        q.setOuterJoinStatus("Department.name", OuterJoinStatus.OUTER);
        q.setOuterJoinStatus("Department.employees", OuterJoinStatus.INNER);
        q.setOuterJoinStatus("Department.flibble", OuterJoinStatus.OUTER);
        assertEquals(Arrays.asList("Outer join status cannot be set on root path Department", "Outer join status on path Department.name must not be on an attribute", "Outer join status path Department.employees is not relevant to the query", "Path Department.flibble for outer join status is not in the model"), q.verifyQuery());
        q.clearOuterJoinStatus();
        q.addConstraint(new PathConstraintAttribute("Department", ConstraintOp.EQUALS, "Fred"));
        q.addConstraint(new PathConstraintAttribute("Department.company.vatNumber", ConstraintOp.EQUALS, "Albert"));
        assertEquals(Arrays.asList("Constraint Department = Fred must be on an attribute", "Value in constraint Department.company.vatNumber = Albert is not in correct format for type of Integer"), q.verifyQuery());
        q.clearConstraints();
        q.addConstraint(new PathConstraintNull("Department", ConstraintOp.IS_NOT_NULL));
        q.addConstraint(new PathConstraintNull("Department.employees", ConstraintOp.IS_NULL));
        q.addConstraint(new PathConstraintNull("Department.name", ConstraintOp.IS_NULL));
        q.addConstraint(new PathConstraintNull("Department.employees", ConstraintOp.IS_NOT_NULL));
        q.addConstraint(new PathConstraintBag("Department.name", ConstraintOp.IN, "bagname"));
        q.addConstraint(new PathConstraintBag("Department", ConstraintOp.NOT_IN, "bagname"));
        q.addConstraint(new PathConstraintIds("Department.name", ConstraintOp.IN, Arrays.asList(1, 2, 3)));
        q.addConstraint(new PathConstraintIds("Department", ConstraintOp.NOT_IN, Arrays.asList(1, 2, 3, 4)));
        q.addConstraint(new PathConstraintMultiValue("Department.name", ConstraintOp.NONE_OF, Arrays.asList("Fred", "Bernie")));
        q.addConstraint(new PathConstraintMultiValue("Department", ConstraintOp.ONE_OF, Arrays.asList("Albert", "Charlie")));
        q.addConstraint(new PathConstraintMultiValue("Department.employees.age", ConstraintOp.ONE_OF, Arrays.asList("5", "Fred")));
        q.addConstraint(new PathConstraintInvalid("Department"));
        assertEquals(Arrays.asList("Constraint Department IS NOT NULL cannot be applied to the root path", "Constraint Department.employees IS NULL is invalid - can only set IS NULL on an attribute", "Constraint Department.name IN bagname must not be on an attribute", "Constraint Department.name IN [1, 2, 3] must not be on an attribute", "Constraint Department ONE OF [Albert, Charlie] must be on an attribute", "Value (Fred) in list in constraint Department.employees.age ONE OF [5, Fred] is not in correct format for type of Integer", "Unrecognised constraint type org.intermine.pathquery.PathQueryUnitTest$PathConstraintInvalid"), q.verifyQuery());
        assertEquals(Collections.singleton("bagname"), q.getBagNames());
        q.clearConstraints();
        q.setDescription("Department", "Fred");
        q.setDescription("Department.name", "Fred");
        assertEquals("Fred", q.getGeneratedPathDescription("Department"));
        assertEquals("Fred", q.getGeneratedPathDescription("Department.name"));
        assertEquals("Fred > age", q.getGeneratedPathDescription("Department.age"));
        assertEquals("Company > name", q.getGeneratedPathDescription("Company.name"));
        q.setDescription("Department.employees.name", "Fred");
        q.setDescription("Department.flibble", "Fred");
        assertEquals(Arrays.asList("Description on path Department.employees.name is not relevant to the query", "Path Department.flibble for description is not in the model"), q.verifyQuery());
        q.clearDescriptions();
        q.setOuterJoinStatus("Department.employees", OuterJoinStatus.OUTER);
        q.addView("Department.employees.name");
        assertEquals(Collections.EMPTY_LIST, q.verifyQuery());
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Department.name Department.employees.name\"><join path=\"Department.employees\" style=\"OUTER\"/></query>", PathQueryBinding.marshal(q, "test", "testmodel", 1));
        q.addConstraint(new PathConstraintAttribute("Department.name", ConstraintOp.EQUALS, "Fred"));
        q.addConstraint(new PathConstraintAttribute("Department.employees.name", ConstraintOp.EQUALS, "Albert"));
        assertEquals(Collections.EMPTY_LIST, q.verifyQuery());
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Department.name Department.employees.name\" constraintLogic=\"A and B\"><join path=\"Department.employees\" style=\"OUTER\"/><constraint path=\"Department.name\" code=\"A\" op=\"=\" value=\"Fred\"/><constraint path=\"Department.employees.name\" code=\"B\" op=\"=\" value=\"Albert\"/></query>", PathQueryBinding.marshal(q, "test", "testmodel", 1));
        q.setConstraintLogic("A or B");
        assertEquals(Collections.singletonList("Logic expression is not compatible with outer join status: Cannot split OR constraint A or B"), q.verifyQuery());
        q.clearConstraints();
        q.clearOuterJoinStatus();
        assertEquals(Collections.EMPTY_LIST, q.verifyQuery());
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Department.name Department.employees.name\"></query>", PathQueryBinding.marshal(q, "test", "testmodel", 1));
        assertEquals("A", q.addConstraint(new PathConstraintLoop("Department.employees.department", ConstraintOp.EQUALS, "Department")));
        assertEquals(Collections.EMPTY_LIST, q.verifyQuery());
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Department.name Department.employees.name\"><constraint path=\"Department.employees.department\" op=\"=\" loopPath=\"Department\"/></query>", PathQueryBinding.marshal(q, "test", "testmodel", 1));
        q.addConstraint(new PathConstraintLoop("Department", ConstraintOp.EQUALS, "Department.employees.department"));
        assertEquals(Collections.singletonList("Cannot have two loop constraints between paths Department and Department.employees.department"), q.verifyQuery());
        q.clearConstraints();
        q.addConstraint(new PathConstraintLoop("Department.name", ConstraintOp.EQUALS, "Department.employees.department"));
        q.addConstraint(new PathConstraintLoop("Department", ConstraintOp.EQUALS, "Department"));
        q.addConstraint(new PathConstraintLoop("Department", ConstraintOp.EQUALS, "Department.name"));
        q.addConstraint(new PathConstraintLoop("Department", ConstraintOp.EQUALS, "Department.employees"));
        q.addConstraint(new PathConstraintLoop("Department", ConstraintOp.EQUALS, "Employee.department"));
        q.addConstraint(new PathConstraintLoop("Department", ConstraintOp.EQUALS, "Department.lkasdfs"));
        assertEquals(Arrays.asList("Constraint Department.name = Department.employees.department must not be on an attribute", "Path Department may not be looped back on itself", "Loop path in constraint Department = Department.name must not be an attribute", "Loop constraint Department = Department.employees must loop between similar types", "Multiple root classes in query: Department and Employee", "Path Department.lkasdfs in loop constraint from Department is not in the model"), q.verifyQuery());
        q.clearConstraints();
        assertEquals("A", q.addConstraint(new PathConstraintLoop("Department", ConstraintOp.EQUALS, "Department.employees.department")));
        q.addOrderBy("Department.employees.department.name", OrderDirection.ASC);
        assertEquals(Collections.EMPTY_LIST, q.verifyQuery());
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Department.name Department.employees.name\" sortOrder=\"Department.employees.department.name asc\"><constraint path=\"Department\" op=\"=\" loopPath=\"Department.employees.department\"/></query>", PathQueryBinding.marshal(q, "test", "testmodel", 1));
        q.clearOrderBy();
        assertEquals(Collections.EMPTY_LIST, q.verifyQuery());
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Department.name Department.employees.name\"><constraint path=\"Department\" op=\"=\" loopPath=\"Department.employees.department\"/></query>", PathQueryBinding.marshal(q, "test", "testmodel", 1));
        q.setOuterJoinStatus("Department.employees", OuterJoinStatus.OUTER);
        assertEquals(Collections.singletonList("Loop constraint Department = Department.employees.department crosses an outer join"), q.verifyQuery());
        q.clearOuterJoinStatus();
        q.addConstraint(new PathConstraintLookup("Department", "DepartmentA1, DepartmentB1", "CompanyA"));
        q.addConstraint(new PathConstraintLookup("Department.name", "Flibble", "CompanyA"));
        assertEquals(Collections.singletonList("Constraint Department.name LOOKUP Flibble IN CompanyA must not be on an attribute"), q.verifyQuery());
        q.clearConstraints();
        q.addView("Department.company.name");
        q.addOrderBy("Department.company.name", OrderDirection.ASC);
        assertEquals(Collections.EMPTY_LIST, q.verifyQuery());
        q.setOuterJoinStatus("Department.company", OuterJoinStatus.OUTER);
        assertEquals(Collections.singletonList("Order by element Department.company.name ASC is not in the root outer join group"), q.verifyQuery());
        q.fixUpForJoinStyle();
        assertEquals(Collections.EMPTY_LIST, q.verifyQuery());
    }

    public void testTicket2636() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery q = new PathQuery(model);

        // Add views
        q.addView("Company.CEO.address.address");
        // Add constraints and you can edit the constraint values below
        q.addConstraint(Constraints.lookup("Company", "Capitol Versicherung AG", ""), "A");
        q.addConstraint(Constraints.lessThan("Company.vatNumber", "392018"), "B");
        q.addConstraint(Constraints.eq("Company.departments.name", "Accounting"), "C");
        // Add join status
        q.setOuterJoinStatus("Company.CEO", OuterJoinStatus.OUTER);
        // Add constraintLogic
        q.setConstraintLogic("(A and B) or C");
        assertTrue("The query is now invalid", !q.isValid());

        List<String> messages = q.fixUpForJoinStyle();
        assertTrue("We should get a message that the logic was changed",
                messages.size() == 1 && messages.get(0).contains("Changed constraint logic"));
        assertNotNull(q.getConstraintLogic());
        assertEquals("A and B and C", q.getConstraintLogic());
        assertTrue("The query is now valid", q.isValid());
    }

    public void testFixingConstraintLogicWhilePreservingWhatWeCan() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery q = new PathQuery(model);

        q.addViews("Employee.name",
                "Employee.department.manager.name",
                "Employee.department.manager.address.address");
        q.addConstraint(Constraints.eq("Employee.age", "28"));
        q.addConstraint(Constraints.eq("Employee.fullTime", "true"));
        q.addConstraint(Constraints.eq("Employee.department.manager.name", "David"));
        q.addConstraint(Constraints.eq("Employee.department.manager.address.address", "123 Some St."));

        q.setOuterJoinStatus("Employee.department.manager", OuterJoinStatus.OUTER);
        q.setConstraintLogic("A or B or C or D");

        List<String> messages = q.fixUpForJoinStyle();
        assertTrue("We should get a message that the logic was changed",
                messages.size() == 1 && messages.get(0).contains("Changed constraint logic"));
        assertNotNull(q.getConstraintLogic());
        assertEquals("(A or B) and (C or D)", q.getConstraintLogic());
        assertTrue("The query is now valid", q.isValid());

    }

    public void testValidQueries() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery q = new PathQuery(model);
        q.addViews("Employee.name", "Employee.department.name", "Employee.age");
        q.addOrderBy("Employee.name", OrderDirection.ASC);
        q.addOrderBy("Employee.age", OrderDirection.DESC);
        q.addConstraint(new PathConstraintAttribute("Employee.name", ConstraintOp.EQUALS, "Fred"));
        q.addConstraint(new PathConstraintNull("Employee.department.name", ConstraintOp.IS_NOT_NULL));
        q.addConstraint(new PathConstraintBag("Employee", ConstraintOp.IN, "bagName"));
        q.addConstraint(new PathConstraintLookup("Employee.department", "DepartmentA1", "CompanyA"));
        q.setOuterJoinStatus("Employee.department", OuterJoinStatus.INNER);
        q.addConstraint(new PathConstraintSubclass("Employee.department.employees", "Manager"));
        q.addConstraint(new PathConstraintLoop("Employee", ConstraintOp.EQUALS, "Employee.department.employees"));
        q.setDescription("Employee", "Flibble");
        q.setDescription("Hello");
        assertEquals(Collections.EMPTY_LIST, q.verifyQuery());
        String marshalled = PathQueryBinding.marshal(q, "test", "testmodel", 1);
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.age\" longDescription=\"Hello\" sortOrder=\"Employee.name asc Employee.age desc\" constraintLogic=\"A and B and C and D and E\"><join path=\"Employee.department\" style=\"INNER\"/><pathDescription pathString=\"Employee\" description=\"Flibble\"/><constraint path=\"Employee.name\" code=\"A\" op=\"=\" value=\"Fred\"/><constraint path=\"Employee.department.name\" code=\"B\" op=\"IS NOT NULL\"/><constraint path=\"Employee\" code=\"C\" op=\"IN\" value=\"bagName\"/><constraint path=\"Employee.department\" code=\"D\" op=\"LOOKUP\" value=\"DepartmentA1\" extraValue=\"CompanyA\"/><constraint path=\"Employee.department.employees\" type=\"Manager\"/><constraint path=\"Employee\" code=\"E\" op=\"=\" loopPath=\"Employee.department.employees\"/></query>", marshalled);
        PathQuery rt = PathQueryBinding.unmarshalPathQuery(new StringReader(marshalled), 1);
        assertEquals(marshalled, PathQueryBinding.marshal(rt, "test", "testmodel", 1));
        assertEquals(q.toString(), rt.toString());
        PathQuery unmarshalled = PathQueryBinding.unmarshalPathQuery(new StringReader("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.age\" longDescription=\"Hello\" sortOrder=\"Employee.name asc Employee.age desc\"><join path=\"Employee.department\" style=\"INNER\"/><pathDescription pathString=\"Employee\" description=\"Flibble\"/><constraint path=\"Employee.name\" op=\"=\" value=\"Fred\"/><constraint path=\"Employee.department.name\" op=\"IS NOT NULL\"/><constraint path=\"Employee\" op=\"IN\" value=\"bagName\"/><constraint path=\"Employee.department\" op=\"LOOKUP\" value=\"DepartmentA1\" extraValue=\"CompanyA\"/><constraint path=\"Employee.department.employees\" type=\"Manager\"/><constraint path=\"Employee\" op=\"=\" loopPath=\"Employee.department.employees\"/></query>"), 1);
        assertEquals(q.toString(), unmarshalled.toString());
        assertEquals(marshalled, PathQueryBinding.marshal(unmarshalled, "test", "testmodel", 1));
    }

    public void testVerifiedMethods() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery q = new PathQuery(model);
        q.addViews("Employee.name", "Employee.department.name", "Employee.department.company.name");
        q.addConstraint(new PathConstraintAttribute("Employee.name", ConstraintOp.EQUALS, "Fred"));
        q.addConstraint(new PathConstraintAttribute("Employee.age", ConstraintOp.EQUALS, "12"));
        q.addConstraint(new PathConstraintAttribute("Employee.department.name", ConstraintOp.EQUALS, "Albert"));
        q.addConstraint(new PathConstraintAttribute("Employee.department.company.name", ConstraintOp.EQUALS, "Ermintrude"));
        q.setConstraintLogic("(A and B) and (C or D)");
        q.setOuterJoinStatus("Employee.department", OuterJoinStatus.OUTER);
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.department.company.name\" constraintLogic=\"A and B and (C or D)\"><join path=\"Employee.department\" style=\"OUTER\"/><constraint path=\"Employee.name\" code=\"A\" op=\"=\" value=\"Fred\"/><constraint path=\"Employee.age\" code=\"B\" op=\"=\" value=\"12\"/><constraint path=\"Employee.department.name\" code=\"C\" op=\"=\" value=\"Albert\"/><constraint path=\"Employee.department.company.name\" code=\"D\" op=\"=\" value=\"Ermintrude\"/></query>", PathQueryBinding.marshal(q, "test", "testmodel", 1));
        assertEquals("Employee", q.getRootClass());
        assertEquals(Collections.EMPTY_MAP, q.getSubclasses());
        Map<String, String> outerJoinGroups = new HashMap<String, String>();
        outerJoinGroups.put("Employee", "Employee");
        outerJoinGroups.put("Employee.department", "Employee.department");
        outerJoinGroups.put("Employee.department.company", "Employee.department");
        assertEquals(outerJoinGroups, q.getOuterJoinGroups());
        Map<String, Set<String>> constraintGroups = new HashMap<String, Set<String>>();
        constraintGroups.put("Employee", new HashSet<String>(Arrays.asList("A", "B")));
        constraintGroups.put("Employee.department", new HashSet<String>(Arrays.asList("C", "D")));
        assertEquals(constraintGroups, q.getConstraintGroups());
        assertEquals("A and B", q.getConstraintLogicForGroup("Employee").toString());
        assertEquals("C or D", q.getConstraintLogicForGroup("Employee.department").toString());
        try {
            q.getConstraintLogicForGroup("Flibble");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
        }
        q.clearConstraints();
        assertNull(q.getConstraintLogicForGroup("Employee"));
        q.addConstraint(new PathConstraintAttribute("Employee.department.name", ConstraintOp.EQUALS, "Albert"));
        assertNull(q.getConstraintLogicForGroup("Employee"));

        q = new PathQuery(model);
        q.addView("Employee.name");
        assertNull(q.getConstraintLogicForGroup("Employee"));

        q.addView("Employee.flibble");
        try {
            q.getConstraintLogicForGroup("Employee");
            fail("Expected exception");
        } catch (PathException e) {
        }
    }

    public void testGetConstraintCodes() {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery q = new PathQuery(model);
        q.addViews("Department.name");
        q.addConstraint(new PathConstraintSubclass("Department.employees", "Manager"));

        Set<String> expected = new HashSet<String>();
        assertEquals(expected, q.getConstraintCodes());

        q.addConstraint(new PathConstraintAttribute("Department.employees.age", ConstraintOp.EQUALS, "12"));
        expected.add("A");
        assertEquals(expected, q.getConstraintCodes());

        q.addConstraint(new PathConstraintAttribute("Department.name", ConstraintOp.EQUALS, "DepartmentA"));
        expected.add("B");
        assertEquals(expected, q.getConstraintCodes());
    }

    public void testRemoveSubclass() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery q = PathQueryBinding.unmarshalPathQuery(new StringReader("<query name=\"\" model=\"testmodel\" view=\"Department.name Department.employees.name Department.employees.company.name Department.employees.title Department.employees.company.departments.name\" sortOrder=\"Department.employees.company.name asc Department.name asc\" constraintLogic=\"A and B and C and D\"><join path=\"Department.employees.address\" style=\"INNER\"/><join path=\"Department.employees\" style=\"INNER\"/><join path=\"Department.employees.company\" style=\"INNER\"/><pathDescription pathString=\"Department\" description=\"wurble\"/><pathDescription pathString=\"Department.employees.company\" description=\"flibble\"/><constraint path=\"Department.employees.address.address\" code=\"A\" op=\"=\" value=\"sdfsg\"/><constraint path=\"Department.employees\" type=\"CEO\"/><constraint path=\"Department.employees.company.vatNumber\" code=\"B\" op=\"=\" value=\"435\"/><constraint path=\"Department\" code=\"C\" op=\"=\" loopPath=\"Department.employees.company.departments\"/><constraint path=\"Department\" code=\"D\" op=\"=\" loopPath=\"Department.company.departments\"/></query>"), 1);
        List<String> messages = Arrays.asList("Removed path Department.employees.company.name from view, because you removed the subclass constraint that it depended on.", "Removed path Department.employees.title from view, because you removed the subclass constraint that it depended on.", "Removed path Department.employees.company.departments.name from view, because you removed the subclass constraint that it depended on.", "Removed constraint Department.employees.company.vatNumber = 435 because you removed the subclass constraint it depended on.", "Removed constraint Department = Department.employees.company.departments because you removed the subclass constraint it depended on.", "Removed path Department.employees.company.name from ORDER BY, because you removed the subclass constraint it depended on.", "Removed description on path Department.employees.company, because you removed the subclass constraint it depended on.");
        assertEquals(messages, q.removeSubclassAndFixUp("Department.employees"));
        assertEquals("<query name=\"\" model=\"testmodel\" view=\"Department.name Department.employees.name\" sortOrder=\"Department.name asc\" constraintLogic=\"A and D\"><join path=\"Department.employees.address\" style=\"INNER\"/><join path=\"Department.employees\" style=\"INNER\"/><pathDescription pathString=\"Department\" description=\"wurble\"/><constraint path=\"Department.employees.address.address\" code=\"A\" op=\"=\" value=\"sdfsg\"/><constraint path=\"Department\" code=\"D\" op=\"=\" loopPath=\"Department.company.departments\"/></query>", PathQueryBinding.marshal(q, "", "testmodel", 1));
        assertEquals(Collections.emptyList(), q.removeSubclassAndFixUp("Department.employees"));
        assertEquals("<query name=\"\" model=\"testmodel\" view=\"Department.name Department.employees.name\" sortOrder=\"Department.name asc\" constraintLogic=\"A and D\"><join path=\"Department.employees.address\" style=\"INNER\"/><join path=\"Department.employees\" style=\"INNER\"/><pathDescription pathString=\"Department\" description=\"wurble\"/><constraint path=\"Department.employees.address.address\" code=\"A\" op=\"=\" value=\"sdfsg\"/><constraint path=\"Department\" code=\"D\" op=\"=\" loopPath=\"Department.company.departments\"/></query>", PathQueryBinding.marshal(q, "", "testmodel", 1));
        q = new PathQuery(model);
        q.addView("asdkfgh");
        try {
            q.removeSubclassAndFixUp("Department.employees");
            fail("Expected exception");
        } catch (PathException e) {
        }
    }


    public void testSortConstraints() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery q = new PathQuery(model);
        q.addViews("Department.name");
        PathConstraintAttribute a = Constraints.eq("Deparment.name", "a");
        q.addConstraints(a);
        PathConstraintAttribute b = Constraints.eq("Deparment.name", "b");
        q.addConstraints(b);
        PathConstraintAttribute c = Constraints.eq("Deparment.name", "c");
        q.addConstraints(c);

        // original order should be as added to query
        List<PathConstraint> expected = makeConstraintOrder(a, b, c);
        assertEquals(expected, readActualConstraintOrder(q));

        // any constraints not in list to sort by should move to start
        q.sortConstraints(makeConstraintOrder(c, a));
        expected = makeConstraintOrder(b, c, a);
        assertEquals(expected, readActualConstraintOrder(q));

        q.sortConstraints(makeConstraintOrder(c, b));
        expected = makeConstraintOrder(a, c, b);
        assertEquals(expected, readActualConstraintOrder(q));

        q.sortConstraints(makeConstraintOrder(c, a, b));
        expected = makeConstraintOrder(c, a, b);
        assertEquals(expected, readActualConstraintOrder(q));

        // neither a or c in list, order of a and c should be consistent but not predictable. b last
        q.sortConstraints(makeConstraintOrder(b));
        expected = makeConstraintOrder(a, c, b);
        assertEquals(expected, readActualConstraintOrder(q));
}

    private List<PathConstraint> makeConstraintOrder(PathConstraint... cons) {
        ArrayList<PathConstraint> expected = new ArrayList<PathConstraint>();
        for (PathConstraint con : cons) {
            expected.add(con);
        }
        return expected;
    }

    private List<PathConstraint> readActualConstraintOrder(PathQuery q) {
        ArrayList<PathConstraint> actual = new ArrayList<PathConstraint>();
        for (Map.Entry<PathConstraint, String> entry : q.getConstraints().entrySet()) {
            actual.add(entry.getKey());
        }
        return actual;
    }

    public static class PathConstraintInvalid extends PathConstraint
    {
        public PathConstraintInvalid(String path) {
            super(path, null);
        }
    }
}
