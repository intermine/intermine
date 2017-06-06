package org.intermine.pathquery;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.metadata.Model;

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
 * Tests reaction of unmarshalling implementation at errors in query.
 * The tests are written in the way,
 * that they pass for current implementation of unmarshalling, but the behavior
 * of tests and implementation of unmarshalling should be changed.
 * @author Jakub Kulaviak
 **/
public class PathQueryUnmarshalTest extends  TestCase
{

    public void testUnknownModel() {
        /*
         * Just now throws exception. It will change later.
         */
        try {
            createQuery("UnknownModel.xml");
        } catch (Exception ex) {
            return;
        }
        fail("Expected exception");
    }

    public void testInvalidView() {
        PathQuery query = createQuery("InvalidView.xml");
        assertEquals(1, query.verifyQuery().size());
    }

    public void testEmptyView() {
        PathQuery query = createQuery("emptyView.xml");
        assertFalse("Viewless queries should be invalid", query.isValid());
        assertEquals(1, query.verifyQuery().size());
    }

    public void testInvalidSortOrder() {
        PathQuery query = createQuery("InvalidSortOrder.xml");
        assertTrue("Invalid sort orders are not tolerated", !query.isValid());
    }

    public void testOrderlessSortOrder() {
        PathQuery query = createQuery("OrderlessSortOrder.xml");
        assertTrue("Orderless sort orders are tolerated", query.isValid());
    }

    public void testIrrelevantSortOrder() {
        PathQuery query = createQuery("IrrelevantSortOrder.xml");
        assertEquals(Arrays.asList("Order by element for path Employee.department.name is not relevant to the query"), query.verifyQuery());
    }

    public void testInvalidConstraintLogic() {
        PathQuery query = createQuery("InvalidConstraintLogic.xml");
        assertEquals(Collections.EMPTY_LIST, query.verifyQuery());
    }

    public void testIncompleteConstraintLogic() {
        PathQuery query = createQuery("IncompleteConstraintLogic.xml");
        assertEquals(Arrays.asList(
                "Value in constraint Employee.age > bad is not in correct format for " +
                "type of Integer"), query.verifyQuery());
    }

    /* ? */
    public void testInvalidConstraintIdentifier() {
    }

    /* ? */
    public void testInvalidConstraintEditable() {
    }

    public void testInvalidConstraintOperation() {
        try {
            createQuery("InvalidConstraintOperation.xml");
        } catch (Exception ex) {
            return;
        }
        fail("Exception expected, but wasn't thrown.");
    }

    public void testInvalidConstraintValue() {
        PathQuery query = createQuery("InvalidConstraintValue.xml");
        assertEquals(Collections.EMPTY_LIST, query.verifyQuery());
    }

    public void testInvalidValueTags() {
        try {
            createQuery("BadValueTags.xml");
        } catch (RuntimeException e) {
            assertEquals(e.getMessage(),
                    "Cannot have any tags inside a value tag");
            return;
        }
        fail("Expected exception");
    }

    public void testBadConstraintStructure() {
        try {
            createQuery("BadConstraintStructure.xml");
        } catch (RuntimeException e) {
            assertEquals("Cannot have anything other than value tag inside a constraint",
                    e.getCause().getMessage());
            return;
        }
        fail("Expected exception");
    }

    public void testBadDescriptionPath() {
        try {
            createQuery("BadDescriptionPath.xml");
        } catch (RuntimeException e) {
            assertEquals("Invalid path 'Employee.department.' for description: a nice place to work",
                e.getCause().getMessage());
            return;
        }
        fail("Expected exception");
    }

    public void testBadJoinStyle() {
        try {
            createQuery("BadJoinStyle.xml");
        } catch (RuntimeException e) {
            assertEquals("Unknown join style funky for path Employee.department",
                e.getCause().getMessage());
            return;
        }
        fail("Expected exception");
    }

    public void testEmptyValueTag() {
        PathQuery pq;
        try {
            pq = createQuery("EmptyValueTag.xml");
        } catch (Exception e) {
            assertEquals("No value provided in value tag. Failed for template query: unnamed_query on constraint: Employee.name",
                e.getCause().getMessage());
            return;
        }
        fail("Expected exception, but got: " + pq.toString() );
    }

    public void testRangeConstraint() {
        PathQuery pq = createQuery("employeesEmployedInMarchOrMay.xml");

        assertEquals(1, pq.getConstraints().size());
        PathConstraint pc = pq.getConstraints().keySet().iterator().next();
        assertTrue(pc instanceof PathConstraintRange);
        assertEquals(2, ((PathConstraintRange) pc).getValues().size());
    }

    public void testMultipleQueries() {
        String path = "PathQueryBindingUnmarshal/MultipleQueries.xml";
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        Model.getInstanceByName("testmodel");
        Collection<PathQuery> pqlist = PathQueryBinding.unmarshalPathQueries(new InputStreamReader(is), 1).values();

        assertEquals(pqlist.size(), 2);
    }

    public void testInterMineObjectQuery() {
        PathQuery query = createQuery("IMObj.xml");
        assertEquals(Collections.EMPTY_LIST, query.verifyQuery());
        assertEquals("InterMineObject.id", query.getView().get(0));
    }

    public void testMultipleQueriesWithSameName() {
        String path = "PathQueryBindingUnmarshal/MultipleQueriesSameName.xml";
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        Model.getInstanceByName("testmodel");
        Set<String> pqnames = PathQueryBinding.unmarshalPathQueries(new InputStreamReader(is), 1).keySet();

        assertTrue(pqnames.contains("a_query"));
        assertTrue(pqnames.contains("a_query_1"));
        assertTrue(pqnames.contains("a_query_2"));

    }

    /* Test not currently working: TODO
    public void testPointlessSubclass() throws PathException {
        PathQuery query = createQuery("PointlessSubclass.xml");
        assertEquals(Collections.EMPTY_LIST, query.verifyQuery());
        assertEquals("Department", query.getSubclasses().get("Employee.department"));
        assertEquals("Manager", query.getSubclasses().get("Employee.department.manager"));
    } */

    public void testHeaderAttributes() {
        PathQuery query = createQuery("HeaderAttributes.xml");
        assertEquals(Collections.EMPTY_LIST, query.verifyQuery());
        assertEquals(query.getTitle(), "Query Title");
        assertEquals(query.getDescription(), "Query Description");
    }

    public void testOldStyleJoins() throws PathException {
        PathQuery query = createQuery("OldStyleJoins.xml");
        assertEquals(Collections.EMPTY_LIST, query.verifyQuery());
        assertEquals(query.getOuterJoinStatus("Employee.department.employees"),
                OuterJoinStatus.OUTER);
    }

    public void testExplicitJoins() throws PathException {
        PathQuery query = createQuery("ExplicitJoins.xml");
        assertEquals(Collections.EMPTY_LIST, query.verifyQuery());
        assertEquals(query.getOuterJoinStatus("Employee.department.company"), OuterJoinStatus.OUTER);
        assertEquals(query.getOuterJoinStatus("Employee.department"), OuterJoinStatus.INNER);
    }

    private PathQuery createQuery(String fileName)  {
        String path = "PathQueryBindingUnmarshal/" + fileName;
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        if (is == null) {
            throw new RuntimeException("Could not find the required XML file: " + path);
        }
        Model.getInstanceByName("testmodel");
        PathQuery ret = PathQueryBinding.unmarshalPathQueries(new InputStreamReader(is), 1).values().iterator().next();
        return ret;
    }
}
