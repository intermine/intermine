package org.flymine.objectstore.query;

import junit.framework.TestCase;

import org.flymine.model.testmodel.*;

public class QueryTest extends TestCase
{
    private Query query;

    public QueryTest(String arg) {
        super(arg);
    }

    public final void setUp() {
        query = new Query();
        assertNotNull("Problem creating Query instance", query);
    }

    public  void testAddClass() {
        query.addClass(new QueryClass(Department.class));
    }

    public  void testAddNull() {
        try {
            query.addClass(null);
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
        query.addClass(qn1);
        query.addClass(qn2);
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
        query.addClass(qn2);
        assertEquals("a1_", (String) query.getAliases().get(qn1));
        assertEquals("a2_", (String) query.getAliases().get(qn2));
    }

    public void testAddTwoSimilarClassesToBoth1() throws Exception {
        QueryClass qn1 = new QueryClass(Department.class);
        QueryClass qn2 = new QueryClass(Department.class);
        query.addClass(qn1);
        query.addClass(qn2);
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
        query.addClass(qn1);
        query.addClass(qn2);
        assertEquals("a1_", (String) query.getAliases().get(qn1));
        assertEquals("a2_", (String) query.getAliases().get(qn2));
    }

    public void testAddTwoSimilarFields() throws Exception {
        QueryClass qn1 = new QueryClass(Department.class);
        QueryField qf1 = new QueryField(qn1, "name");
        QueryField qf2 = new QueryField(qn1, "name");
        query.addClass(qn1);
        query.addToSelect(qf1);
        query.addToSelect(qf2);
        assertEquals("a1_", (String) query.getAliases().get(qn1));
        assertEquals("a2_", (String) query.getAliases().get(qf1));
        assertEquals("a3_", (String) query.getAliases().get(qf2));
    }
}
