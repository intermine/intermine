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
}
