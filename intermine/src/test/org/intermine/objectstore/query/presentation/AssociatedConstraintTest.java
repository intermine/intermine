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

import org.flymine.model.testmodel.*;
import org.flymine.objectstore.query.ClassConstraint;
import org.flymine.objectstore.query.Constraint;
import org.flymine.objectstore.query.ConstraintSet;
import org.flymine.objectstore.query.ContainsConstraint;
import org.flymine.objectstore.query.FromElement;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryCollectionReference;
import org.flymine.objectstore.query.QueryExpression;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.query.SubqueryConstraint;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.fql.FqlQuery;

public class AssociatedConstraintTest extends TestCase
{
    private Query q;
    private QueryClass qc1, qc2, qc3;
    private AssociatedConstraint ac1, ac2, ac3, ac4, ac5, ac6, ac7;

    public AssociatedConstraintTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        // Set up a query with every type of constraint in it
        q = new Query();
        qc1 = new QueryClass(Company.class);
        qc2 = new QueryClass(Department.class);
        qc3 = new QueryClass(Department.class);
        QueryField qf1 = new QueryField(qc1, "name");
        QueryField qf2 = new QueryField(qc2, "name");
        QueryField qf3 = new QueryField(qc3, "name");

        QueryCollectionReference qcr1 = new QueryCollectionReference(qc1, "departments");

        QueryValue value1 = new QueryValue("Company1");

        QueryExpression expr1 = new QueryExpression(qf1, new QueryValue(new Integer(1)), new QueryValue(new Integer(1)));
        QueryExpression expr2 = new QueryExpression(qf2, new QueryValue(new Integer(1)), new QueryValue(new Integer(1)));

        Query subquery = new Query();
        QueryClass subQc1 = new QueryClass(Department.class);

        subquery.addToSelect(subQc1);
        subquery.addFrom(subQc1);


        q.addFrom(qc1, "company1");
        q.addFrom(qc2, "department1");
        q.addFrom(qc3, "department2");
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        SimpleConstraint simpleConstraint1 = new SimpleConstraint(qf1, SimpleConstraint.EQUALS, value1);
        cs1.addConstraint(simpleConstraint1);
        SimpleConstraint simpleConstraint2 = new SimpleConstraint(qf2, SimpleConstraint.EQUALS, qf3);
        cs1.addConstraint(simpleConstraint2);
        ClassConstraint classConstraint1 = new ClassConstraint(qc2, ClassConstraint.NOT_EQUALS, qc3);
        cs1.addConstraint(classConstraint1);
        ClassConstraint classConstraint2 = new ClassConstraint(qc2, ClassConstraint.NOT_EQUALS, new Department());
        cs1.addConstraint(classConstraint2);
        ContainsConstraint containsConstraint1 = new ContainsConstraint(qcr1, ContainsConstraint.CONTAINS, qc2);
        cs1.addConstraint(containsConstraint1);
        SubqueryConstraint subqueryConstraint1 = new SubqueryConstraint(subquery, SubqueryConstraint.CONTAINS, qc2);
        cs1.addConstraint(subqueryConstraint1);
        SimpleConstraint simpleConstraint3 = new SimpleConstraint(expr1, SimpleConstraint.EQUALS, expr2);
        cs1.addConstraint(simpleConstraint1);
        q.setConstraint(cs1);

        ac1 = new AssociatedConstraint(q, simpleConstraint1);
        ac2 = new AssociatedConstraint(q, simpleConstraint2);
        ac3 = new AssociatedConstraint(q, classConstraint1);
        ac4 = new AssociatedConstraint(q, classConstraint2);
        ac5 = new AssociatedConstraint(q, containsConstraint1);
        ac6 = new AssociatedConstraint(q, subqueryConstraint1);
        ac7 = new AssociatedConstraint(q, simpleConstraint3);
    }

    public void testAssociatedWith() {
        assertTrue(ac1.isAssociatedWith(qc1));
        assertFalse(ac1.isAssociatedWith(qc2));
        assertFalse(ac1.isAssociatedWith(qc3));

        assertFalse(ac2.isAssociatedWith(qc1));
        assertTrue(ac2.isAssociatedWith(qc2));
        assertFalse(ac2.isAssociatedWith(qc3));

        assertFalse(ac3.isAssociatedWith(qc1));
        assertTrue(ac3.isAssociatedWith(qc2));
        assertFalse(ac3.isAssociatedWith(qc3));

        assertFalse(ac4.isAssociatedWith(qc1));
        assertTrue(ac4.isAssociatedWith(qc2));
        assertFalse(ac4.isAssociatedWith(qc3));

        assertTrue(ac5.isAssociatedWith(qc1));
        assertFalse(ac5.isAssociatedWith(qc2));
        assertFalse(ac5.isAssociatedWith(qc3));

        assertFalse(ac6.isAssociatedWith(qc1));
        assertTrue(ac6.isAssociatedWith(qc2));
        assertFalse(ac6.isAssociatedWith(qc3));

        assertFalse(ac7.isAssociatedWith(qc1));
        assertFalse(ac7.isAssociatedWith(qc2));
        assertFalse(ac7.isAssociatedWith(qc3));
    }

    public void testAssociatedWithNothing() {
        assertFalse(ac1.isAssociatedWithNothing());
        assertFalse(ac2.isAssociatedWithNothing());
        assertFalse(ac3.isAssociatedWithNothing());
        assertFalse(ac4.isAssociatedWithNothing());
        assertFalse(ac5.isAssociatedWithNothing());
        assertFalse(ac6.isAssociatedWithNothing());
        assertTrue(ac7.isAssociatedWithNothing());
    }

    public void testLeft() {
        assertEquals("name", ac1.getLeft());
        assertEquals("name", ac2.getLeft());
        assertEquals("department1", ac3.getLeft());
        assertEquals("department1", ac4.getLeft());
        assertEquals("departments", ac5.getLeft());
        assertEquals("department1", ac6.getLeft());
        assertEquals("SUBSTR(company1.name, 1, 1)", ac7.getLeft());
    }

    public void testOp() {
        assertEquals("=", ac1.getOp());
        assertEquals("=", ac2.getOp());
        assertEquals("!=", ac3.getOp());
        assertEquals("!=", ac4.getOp());
        assertEquals("CONTAINS", ac5.getOp());
        assertEquals("IN", ac6.getOp());
        assertEquals("=", ac7.getOp());
    }

    public void testRight() {
        assertEquals("\'Company1\'", ac1.getRight());
        assertEquals("department2.name", ac2.getRight());
        assertEquals("department2", ac3.getRight());
        assertEquals("\"Department [null] null, null\"", ac4.getRight());
        assertEquals("department1", ac5.getRight());
        assertEquals("SELECT a1_ FROM org.flymine.model.testmodel.Department AS a1_", ac6.getRight());
        assertEquals("SUBSTR(department1.name, 1, 1)", ac7.getRight());
    }


}
