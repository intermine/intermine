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
import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.objectstore.query.ClassConstraint;
import org.flymine.objectstore.query.ConstraintSet;
import org.flymine.objectstore.query.ContainsConstraint;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryCollectionReference;
import org.flymine.objectstore.query.QueryExpression;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.query.SubqueryConstraint;
import org.flymine.objectstore.query.Query;

public class PrintableConstraintTest extends TestCase
{
    private Query q;
    private QueryClass qc1, qc2, qc3;
    private PrintableConstraint pc1, pc2, pc3, pc4, pc5, pc6, pc7;

    public PrintableConstraintTest(String arg) {
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
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        SimpleConstraint simpleConstraint1 = new SimpleConstraint(qf1, ConstraintOp.EQUALS, value1);
        cs1.addConstraint(simpleConstraint1);
        SimpleConstraint simpleConstraint2 = new SimpleConstraint(qf2, ConstraintOp.EQUALS, qf3);
        cs1.addConstraint(simpleConstraint2);
        ClassConstraint classConstraint1 = new ClassConstraint(qc2, ConstraintOp.NOT_EQUALS, qc3);
        cs1.addConstraint(classConstraint1);
        ClassConstraint classConstraint2 = new ClassConstraint(qc2, ConstraintOp.NOT_EQUALS, new Department());
        cs1.addConstraint(classConstraint2);
        ContainsConstraint containsConstraint1 = new ContainsConstraint(qcr1, ConstraintOp.CONTAINS, qc2);
        cs1.addConstraint(containsConstraint1);
        SubqueryConstraint subqueryConstraint1 = new SubqueryConstraint(qc2, ConstraintOp.IN, subquery);
        cs1.addConstraint(subqueryConstraint1);
        SimpleConstraint simpleConstraint3 = new SimpleConstraint(expr1, ConstraintOp.EQUALS, expr2);
        cs1.addConstraint(simpleConstraint1);
        q.setConstraint(cs1);

        pc1 = new PrintableConstraint(q, simpleConstraint1);
        pc2 = new PrintableConstraint(q, simpleConstraint2);
        pc3 = new PrintableConstraint(q, classConstraint1);
        pc4 = new PrintableConstraint(q, classConstraint2);
        pc5 = new PrintableConstraint(q, containsConstraint1);
        pc6 = new PrintableConstraint(q, subqueryConstraint1);
        pc7 = new PrintableConstraint(q, simpleConstraint3);
    }

    public void testLeft() {
        assertEquals("name", pc1.getLeft());
        assertEquals("name", pc2.getLeft());
        assertEquals("department1", pc3.getLeft());
        assertEquals("department1", pc4.getLeft());
        assertEquals("departments", pc5.getLeft());
        assertEquals("department1", pc6.getLeft());
        assertEquals("SUBSTR(company1.name, 1, 1)", pc7.getLeft());
    }

    public void testOp() {
        assertEquals("=", pc1.getOp());
        assertEquals("=", pc2.getOp());
        assertEquals("!=", pc3.getOp());
        assertEquals("!=", pc4.getOp());
        assertEquals("CONTAINS", pc5.getOp());
        assertEquals("IN", pc6.getOp());
        assertEquals("=", pc7.getOp());
    }

    public void testRight() {
        assertEquals("\'Company1\'", pc1.getRight());
        assertEquals("department2.name", pc2.getRight());
        assertEquals("department2", pc3.getRight());
        assertEquals("\"Department [null] null\"", pc4.getRight());
        assertEquals("department1", pc5.getRight());
        assertEquals("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Department AS a1_", pc6.getRight());
        assertEquals("SUBSTR(department1.name, 1, 1)", pc7.getRight());
    }




    public void testEquals() {
        Query q1 = new Query();
        Query q2 = new Query();
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        ConstraintSet cs2 = new ConstraintSet(ConstraintOp.OR);

        pc1 = new PrintableConstraint(q1, cs1);
        pc2 = new PrintableConstraint(q1, cs1);
        pc3 = new PrintableConstraint(q2, cs1);
        pc4 = new PrintableConstraint(q2, cs2);

        assertEquals(pc1, pc1);
        assertEquals(pc1, pc2);
        assertFalse(pc1.equals(pc3));
        assertFalse(pc1.equals(pc4));
        assertFalse(pc3.equals(pc4));
    }

    public void testHashCode() {
        Query q1 = new Query();
        Query q2 = new Query();
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        ConstraintSet cs2 = new ConstraintSet(ConstraintOp.OR);

        pc1 = new PrintableConstraint(q1, cs1);
        pc2 = new PrintableConstraint(q1, cs1);
        pc3 = new PrintableConstraint(q2, cs1);
        pc4 = new PrintableConstraint(q2, cs2);

        assertEquals(pc1.hashCode(), pc1.hashCode());
        assertEquals(pc1.hashCode(), pc2.hashCode());
        // Not testing this
        //assertFalse(pc1.hashCode() == pc3.hashCode());
        assertFalse(pc1.hashCode() == pc4.hashCode());
        assertFalse(pc3.hashCode() == pc4.hashCode());
    }


}
