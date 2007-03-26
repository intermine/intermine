package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.intermine.model.testmodel.*;

public class QueryTest extends TestCase
{
    private Query query, clearQuery;

    public QueryTest(String arg) {
        super(arg);
    }

    public final void setUp() throws Exception {
        query = new Query();
        assertNotNull("Problem creating Query instance", query);

        // set up three queries for testing .equals() and .hashCode()
        QueryClass qc1 = new QueryClass(Company.class);
        QueryField qf1 = new QueryField(qc1, "name");
        QueryField qf2 = new QueryField(qc1, "vatNumber");
        QueryFunction f1 = new QueryFunction();  // count(*)
        QueryValue qv1 = new QueryValue("CompanyA");
        SimpleConstraint sc1 = new SimpleConstraint(qf1, ConstraintOp.NOT_EQUALS, qv1);

        clearQuery = new Query();
        clearQuery.addToSelect(qc1);
        clearQuery.addToSelect(f1);
        clearQuery.setConstraint(sc1);
        clearQuery.addToGroupBy(qc1);
        clearQuery.addToOrderBy(qf1);
    }

    public  void testAddClass() {
        query.addFrom(new QueryClass(Department.class));
    }

    public  void testAddNull() {
        try {
            query.addFrom(null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testAddToSelect() throws Exception {
        QueryClass qn1 = new QueryClass(Department.class);
        QueryClass qn2 = new QueryClass(Department.class);
        QueryField qn3 = new QueryField(qn1, "name");
        query.addToSelect(qn1);
        assertEquals("a1_", (String) query.getAliases().get(qn1));
        query.addToSelect(qn2);
        assertEquals("a2_", (String) query.getAliases().get(qn2));
        query.addToSelect(qn3);
        assertEquals("a3_", (String) query.getAliases().get(qn3));
    }

    // Test putting two similar QueryClass nodes into the select list, and make sure they can
    // be retrieved properly, with different aliases.
    public void testAddTwoSimilarClasses() throws Exception {
        QueryClass qn1 = new QueryClass(Department.class);
        QueryClass qn2 = new QueryClass(Department.class);
        query.addFrom(qn1);
        query.addFrom(qn2);
        assertEquals("a1_", (String) query.getAliases().get(qn1));
        assertEquals("a2_", (String) query.getAliases().get(qn2));
    }

    public void testAddTwoSimilarClassesToSelect() throws Exception {
        QueryClass qn1 = new QueryClass(Department.class);
        QueryClass qn2 = new QueryClass(Department.class);
        query.addToSelect(qn1);
        query.addToSelect(qn2);
        assertEquals("a1_", (String) query.getAliases().get(qn1));
        assertEquals("a2_", (String) query.getAliases().get(qn2));
    }

    public void testAddTwoSimilarClassesOneToSelect() throws Exception {
        QueryClass qn1 = new QueryClass(Department.class);
        QueryClass qn2 = new QueryClass(Department.class);
        query.addToSelect(qn1);
        query.addFrom(qn2);
        assertEquals("a1_", (String) query.getAliases().get(qn1));
        assertEquals("a2_", (String) query.getAliases().get(qn2));
    }

    public void testAddTwoSimilarClassesToBoth1() throws Exception {
        QueryClass qn1 = new QueryClass(Department.class);
        QueryClass qn2 = new QueryClass(Department.class);
        query.addFrom(qn1);
        query.addFrom(qn2);
        query.addToSelect(qn1);
        query.addToSelect(qn2);
        assertEquals("a1_", (String) query.getAliases().get(qn1));
        assertEquals("a2_", (String) query.getAliases().get(qn2));
    }

    public void testAddTwoSimilarClassesToBoth2() throws Exception {
        QueryClass qn1 = new QueryClass(Department.class);
        QueryClass qn2 = new QueryClass(Department.class);
        query.addToSelect(qn1);
        query.addToSelect(qn2);
        query.addFrom(qn1);
        query.addFrom(qn2);
        assertEquals("a1_", (String) query.getAliases().get(qn1));
        assertEquals("a2_", (String) query.getAliases().get(qn2));
    }

    public void testAddTwoSimilarFields() throws Exception {
        QueryClass qn1 = new QueryClass(Department.class);
        QueryField qf1 = new QueryField(qn1, "name");
        QueryField qf2 = new QueryField(qn1, "name");
        query.addFrom(qn1);
        query.addToSelect(qf1);
        query.addToSelect(qf2);
        assertEquals("a1_", (String) query.getAliases().get(qn1));
        assertEquals("a2_", (String) query.getAliases().get(qf1));
        assertEquals("a3_", (String) query.getAliases().get(qf2));
    }

    public void testAlias1() throws Exception {
        Query q = new Query();
        String s1 = "one";

        q.alias(s1, "alias1");

        try {
            q.alias(s1, "alias2");
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

    }

    public void testAlias2() throws Exception {
        Query q = new Query();
        String s1 = "one";
        String s2 = "two";

        q.alias(s1, "alias1");

        try {
            q.alias(s2, "alias1");
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

    }

    public void testAlias3() throws Exception {
        Query q = new Query();
        String s1 = "one";

        q.alias(s1, "alias1");

        q.alias(s1, null);

        assertEquals(Collections.singletonMap(s1, "alias1"), q.getAliases());

    }

    public void testAlias4() throws Exception {
        Query q = new Query();
        String s1 = "one";

        q.alias(s1, "alias1");

        q.alias(s1, "alias1");

        assertEquals(Collections.singletonMap(s1, "alias1"), q.getAliases());

    }


    public void testClearSelect() {
        clearQuery.clearSelect();
        assertEquals(0, clearQuery.getSelect().size());
    }

    public void testClearOrderBy() {
        clearQuery.clearOrderBy();
        assertEquals(0, clearQuery.getOrderBy().size());
    }

    public void testGetOrderBy() {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Employee.class);
        QueryClass qc2 = new QueryClass(Department.class);
        QueryField qf1 = new QueryField(qc1, "name");
        QueryField qf2 = new QueryField(qc1, "age");
        QueryField qf3 = new QueryField(qc2, "id");
        QueryField qf4 = new QueryField(qc2, "name");
        q.addFrom(qc1);
        q.addFrom(qc2);
        q.addToOrderBy(qf1);
        q.addToSelect(qc1);
        q.addToSelect(qf2);
        q.addToSelect(qf3);
        q.addToSelect(qc2);
        q.addToSelect(qf4);

        List expected = new ArrayList();
        expected.add(qf1);
        expected.add(qc1);
        expected.add(qf3);

        assertEquals(expected, q.getEffectiveOrderBy());
    }
}
