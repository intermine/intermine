package org.flymine.objectstore.query;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import org.flymine.model.testmodel.*;

public class QueryTest extends TestCase
{
    private Query query, q1, q2, q3;

    public QueryTest(String arg) {
        super(arg);
    }

    public final void setUp() {
        query = new Query();
        assertNotNull("Problem creating Query instance", query);

        // set up three queries for testing .equals() and .hashCode()
        try {
            QueryClass qc1 = new QueryClass(Company.class);
            QueryField qf1 = new QueryField(qc1, "name");
            QueryField qf2 = new QueryField(qc1, "vatNumber");
            QueryFunction f1 = new QueryFunction();  // count(*)
            QueryValue qv1 = new QueryValue("CompanyA");
            SimpleConstraint sc1 = new SimpleConstraint(qf1, SimpleConstraint.NOT_EQUALS, qv1);

            q1 = new Query();
            q1.addToSelect(qc1);
            q1.addToSelect(f1);
            q1.setConstraint(sc1);
            q1.addToGroupBy(qc1);
            q1.addToOrderBy(qf1);

            q2 = new Query();
            q2.addToSelect(qc1);
            q2.addToSelect(f1);
            q2.setConstraint(sc1);
            q2.addToGroupBy(qc1);
            q2.addToOrderBy(qf1);

            q3 = new Query();
            q3.addToSelect(qc1);
            q3.addToSelect(qc1);
            q3.setConstraint(sc1);
            q3.addToGroupBy(qc1);
            q3.addToOrderBy(qf1);
        }
        catch (NoSuchFieldException e) {
            fail("Problem occurred in setup: " + e.getMessage());
        }
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

    public void testEquals() throws Exception {
        assertEquals(q1, q1);
        assertEquals(q1, q2);
        assertTrue("Expected q1 to not equal q3", !q1.equals(q3));
    }

    public void testHashCode() throws Exception {
        assertEquals(q1.hashCode(), q1.hashCode());
        assertEquals(q1.hashCode(), q2.hashCode());
        assertTrue("Expected q1 hashcode not to equal q3 hashcode", q1.hashCode() != q3.hashCode());
    }

}
