package org.flymine.objectstore.query.presentation;

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

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;


import org.flymine.objectstore.query.*;
import org.flymine.model.testmodel.Employee;

public class QueryCreatorTest extends TestCase
{

    public QueryCreatorTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
    }

    public void testAddToQuery() throws Exception {
        Map fields = new HashMap();
        fields.put("name", "Dennis");
        fields.put("fullTime", "true");

        Query q = new Query();
        QueryCreator.addToQuery(q, "org.flymine.model.testmodel.Employee", fields);

        ArrayList from = new ArrayList(q.getFrom());
        assertEquals(Employee.class, ((QueryClass) from.get(0)).getType());

        ArrayList list = new ArrayList(((ConstraintSet) q.getConstraint()).getConstraints());
        SimpleConstraint res1 = (SimpleConstraint) list.get(0);
        assertEquals("fullTime", ((QueryField) res1.getArg1()).getFieldName());
        assertEquals(Boolean.class, ((QueryField) res1.getArg1()).getType());
        assertEquals(new Boolean(true), ((QueryValue) res1.getArg2()).getValue());
        assertEquals(Boolean.class, ((QueryValue) res1.getArg2()).getType());

        SimpleConstraint res2 = (SimpleConstraint) list.get(1);
        assertEquals("name", ((QueryField) res2.getArg1()).getFieldName());
        assertEquals(String.class, ((QueryField) res2.getArg1()).getType());
        assertEquals("Dennis", ((QueryValue) res2.getArg2()).getValue());
        assertEquals(String.class, ((QueryValue) res2.getArg2()).getType());
    }

    public void testAddToQueryNullParameters() throws Exception {
        Query q = new Query();

        try {
            QueryCreator.addToQuery(null, "org.flymine.model.testmodel.Employee", new HashMap());
            fail("Expected NullPointerException, q parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryCreator.addToQuery(q, null, new HashMap());
            fail("Expected NullPointerException, clsName parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryCreator.addToQuery(q, "org.flymine.model.testmodel.Employee", null);
            fail("Expected NullPointerException, fields parameter null");
        } catch (NullPointerException e) {
        }
    }

    public void testGenerateConstraints() throws Exception {
        Map fields = new HashMap();
        fields.put("name", "Dennis");
        fields.put("fullTime", "true");

        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);

        ConstraintSet c = QueryCreator.generateConstraints(q, qc, fields);
        ArrayList list = new ArrayList(c.getConstraints());
        SimpleConstraint res1 = (SimpleConstraint) list.get(0);
        assertEquals("fullTime", ((QueryField) res1.getArg1()).getFieldName());
        assertEquals(Boolean.class, ((QueryField) res1.getArg1()).getType());
        assertEquals(new Boolean(true), ((QueryValue) res1.getArg2()).getValue());
        assertEquals(Boolean.class, ((QueryValue) res1.getArg2()).getType());

        SimpleConstraint res2 = (SimpleConstraint) list.get(1);
        assertEquals("name", ((QueryField) res2.getArg1()).getFieldName());
        assertEquals(String.class, ((QueryField) res2.getArg1()).getType());
        assertEquals("Dennis", ((QueryValue) res2.getArg2()).getValue());
        assertEquals(String.class, ((QueryValue) res2.getArg2()).getType());

    }



    public void testGetConstraintSet() throws Exception {
        Query q = new Query();;
        QueryClass qc = new QueryClass(Employee.class);
        QueryField qf = new QueryField(qc, "name");
        SimpleConstraint sc = new SimpleConstraint(qf, SimpleConstraint.EQUALS, new QueryValue("Neville"));

        // query has no constraint set
        ConstraintSet c = new ConstraintSet(ConstraintSet.AND);
        assertEquals(c, QueryCreator.getConstraintSet(q));

        // existing ConstraintSet
        c = new ConstraintSet(ConstraintSet.AND);
        c.addConstraint(sc);
        q.setConstraint(c);
        assertEquals(c, QueryCreator.getConstraintSet(q));

        // existing constraint
        q.setConstraint(sc);
        c = new ConstraintSet(ConstraintSet.AND);
        c.addConstraint(sc);
        assertEquals(c, QueryCreator.getConstraintSet(q));
    }


    public void testCreateQueryValue() throws Exception {
        String value = null;
        QueryValue qv = null;

        qv = QueryCreator.createQueryValue(Integer.class, "101");
        assertEquals(new Integer(101), (Integer) qv.getValue());
        qv = QueryCreator.createQueryValue(Float.class, "1.01");
        assertEquals(new Float(1.01), (Float) qv.getValue());
        qv = QueryCreator.createQueryValue(Double.class, "1.01");
        assertEquals(new Double(1.01), (Double) qv.getValue());
        qv = QueryCreator.createQueryValue(Boolean.class, "false");
        assertEquals(new Boolean(false), (Boolean) qv.getValue());
        qv = QueryCreator.createQueryValue(String.class, "test");
        assertEquals("test", qv.getValue());
        qv = QueryCreator.createQueryValue(Long.class, "101");
        assertEquals(new Long(101), (Long) qv.getValue());
        qv = QueryCreator.createQueryValue(Short.class, "101");
        assertEquals(new Short((short)101), (Short) qv.getValue());

        //qv = QueryCreator.createQueryValue(Date.class, "30/08/76");
        //assertEquals(new Date(, (Date) qv.getValue());
        // test dates

        try {
            qv = QueryCreator.createQueryValue(java.util.Iterator.class, "test");
            fail("Expected an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

}
