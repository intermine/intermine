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
}
