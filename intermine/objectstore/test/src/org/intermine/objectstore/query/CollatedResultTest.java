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

import java.util.Collections;
import junit.framework.Test;

import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.testing.OneTimeTestCase;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

import org.intermine.model.testmodel.*;

public class CollatedResultTest extends QueryTestCase
{
    public CollatedResultTest(String arg1) {
        super(arg1);
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(CollatedResultTest.class);
    }
    
    public void testConstructNullRow() throws Exception {
        try {
            CollatedResult res = new CollatedResult(null, new QueryClass(Department.class), new Query(), new ObjectStoreDummyImpl());
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testConstructNullQueryNode() throws Exception {
        try {
            CollatedResult res = new CollatedResult(new ResultsRow(), null, new Query(), new ObjectStoreDummyImpl());
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testConstructNullQuery() throws Exception {
        try {
            CollatedResult res = new CollatedResult(new ResultsRow(), new QueryClass(Department.class), null, new ObjectStoreDummyImpl());
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testConstructNullObjectStore() throws Exception {
        try {
            CollatedResult res = new CollatedResult(new ResultsRow(), new QueryClass(Department.class), new Query(), null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testConstructInvalidQueryNode() throws Exception {
        Department dept = new Department();
        dept.setName("Purchasing");

        ResultsRow row1 = new ResultsRow();
        row1.add(dept);

        QueryNode qc1 = new QueryClass(Employee.class);
        QueryNode qc2 = new QueryClass(Department.class);
        Query q = new Query();
        q.addToSelect(qc2);

        try {
            CollatedResult res = new CollatedResult(row1, qc1, q, new ObjectStoreDummyImpl());
            fail("Expected: IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
        }
    }


    public void testConstructInvalidRowLength() throws Exception {
        Department dept = new Department();
        dept.setName("Purchasing");

        ResultsRow row1 = new ResultsRow();
        row1.add(dept);
        row1.add(new Integer(4));

        QueryNode qc1 = new QueryClass(Department.class);
        QueryNode qc2 = new QueryClass(Department.class);
        Query q = new Query();
        q.addToSelect(qc2);

        try {
            CollatedResult res = new CollatedResult(row1, qc1, q, new ObjectStoreDummyImpl());
            fail("Expected: IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
        }
    }


    public void testMatchesSingleGroupByClassCountFunction() throws Exception {
        /* select department, count()
         * from department, employee
         * where employee.department = department
         * group by department
         */
        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Employee.class);
        QueryFunction qf1 = new QueryFunction();
        q1.addToSelect(qc1);
        q1.addToSelect(qf1);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        Constraint con1 = new ContainsConstraint(new QueryCollectionReference(qc1, "employees"),
                                                 ConstraintOp.CONTAINS, qc2);
        q1.setConstraint(con1);
        q1.addToGroupBy(qc1);

        Department dept = new Department();
        TypeUtil.setFieldValue(dept, "id", new Integer(42));
        dept.setName("Purchasing");

        ResultsRow row1 = new ResultsRow();
        row1.add(dept);
        row1.add(new Integer(4));

        CollatedResult cr = new CollatedResult();
        Query q2 = cr.getMatchesQuery(qf1, q1, row1);

        /* select department, employee
         * from department, employee
         * where employee.department = department
         * and department = <department>
         */

        Query q3 = new Query();
        q3.addToSelect(qc1);
        q3.addToSelect(qc2);
        q3.addFrom(qc1);
        q3.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        cs1.addConstraint(con1);
        cs1.addConstraint(new ClassConstraint(qc1, ConstraintOp.EQUALS, dept));
        q3.setConstraint(cs1);

        assertEquals(q3, q2);
    }

    public void testMatchesSingleGroupByFieldCountFunction() throws Exception {
        /* select department.name, count()
         * from department, employee
         * where employee.department = department
         * group by department
         */
        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        QueryField qfield1 = new QueryField(qc1, "name");
        QueryClass qc2 = new QueryClass(Employee.class);
        QueryFunction qf1 = new QueryFunction();
        q1.addToSelect(qfield1);
        q1.addToSelect(qf1);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        Constraint con1 = new ContainsConstraint(new QueryCollectionReference(qc1, "employees"),
                                                 ConstraintOp.CONTAINS, qc2);
        q1.setConstraint(con1);
        q1.addToGroupBy(qfield1);

        Department dept = new Department();
        TypeUtil.setFieldValue(dept, "id", new Integer(42));
        dept.setName("Purchasing");

        ResultsRow row1 = new ResultsRow();
        row1.add(dept.getName());
        row1.add(new Integer(4));

        CollatedResult cr = new CollatedResult();
        Query q2 = cr.getMatchesQuery(qf1, q1, row1);

        /* select department, employee
         * from department, employee
         * where employee.department = department
         * and department.name = <department name>
         */

        Query q3 = new Query();
        q3.addToSelect(qc1);
        q3.addToSelect(qc2);
        q3.addFrom(qc1);
        q3.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        cs1.addConstraint(con1);
        cs1.addConstraint(new SimpleConstraint(qfield1, ConstraintOp.EQUALS, new QueryValue(dept.getName())));
        q3.setConstraint(cs1);

        assertEquals(q3, q2);
    }

    public void testMatchesSingleGroupByClassMinFunction() throws Exception {
        /* select department, min(employee.name)
         * from department, employee
         * where employee.department = department
         * group by department
         */
        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Employee.class);
        QueryField qfield1 = new QueryField(qc2, "age");
        QueryFunction qf1 = new QueryFunction(qfield1, QueryFunction.MIN);
        q1.addToSelect(qc1);
        q1.addToSelect(qf1);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        Constraint con1 = new ContainsConstraint(new QueryCollectionReference(qc1, "employees"),
                                                 ConstraintOp.CONTAINS,
                                                 qc2);
        q1.setConstraint(con1);
        q1.addToGroupBy(qc1);

        Department dept = new Department();
        TypeUtil.setFieldValue(dept, "id", new Integer(42));
        dept.setName("Purchasing");

        ResultsRow row1 = new ResultsRow();
        row1.add(dept);
        row1.add(new Integer(23));

        CollatedResult cr = new CollatedResult();
        Query q2 = cr.getMatchesQuery(qf1, q1, row1);

        /* select department, employee
         * from department, employee
         * where employee.department = department
         * and department = <department>
         * and employee.age = <age>
         */

        Query q3 = new Query();
        q3.addToSelect(qc1);
        q3.addToSelect(qc2);
        q3.addFrom(qc1);
        q3.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        cs1.addConstraint(con1);
        cs1.addConstraint(new ClassConstraint(qc1, ConstraintOp.EQUALS, dept));
        cs1.addConstraint(new SimpleConstraint(qfield1, ConstraintOp.EQUALS, new QueryValue(new Integer(23))));
        q3.setConstraint(cs1);

        assertEquals(q3, q2);
    }

    public void testMatchesSingleGroupByClassCountMinFunction() throws Exception {
        /* select department, count()
         * from department, employee
         * where employee.department = department
         * group by department
         */
        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Employee.class);
        QueryFunction qf1 = new QueryFunction();
        QueryField qfield1 = new QueryField(qc2, "age");
        QueryFunction qf2 = new QueryFunction(qfield1, QueryFunction.MIN);
        q1.addToSelect(qc1);
        q1.addToSelect(qf1);
        q1.addToSelect(qf2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        Constraint con1 = new ContainsConstraint(new QueryCollectionReference(qc1, "employees"),
                                                 ConstraintOp.CONTAINS, qc2);
        q1.setConstraint(con1);
        q1.addToGroupBy(qc1);

        Department dept = new Department();
        TypeUtil.setFieldValue(dept, "id", new Integer(42));
        dept.setName("Purchasing");

        ResultsRow row1 = new ResultsRow();
        row1.add(dept);
        row1.add(new Integer(4));
        row1.add(new Integer(23));

        CollatedResult cr = new CollatedResult();
        Query q2 = cr.getMatchesQuery(qf1, q1, row1);
        Query q3 = cr.getMatchesQuery(qf2, q1, row1);

        /* select department, employee
         * from department, employee
         * where employee.department = department
         * and department = <department>
         */

        Query q4 = new Query();
        q4.addToSelect(qc1);
        q4.addToSelect(qc2);
        q4.addFrom(qc1);
        q4.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        cs1.addConstraint(con1);
        cs1.addConstraint(new ClassConstraint(qc1, ConstraintOp.EQUALS, dept));
        q4.setConstraint(cs1);

        /* select department, employee
         * from department, employee
         * where employee.department = department
         * and department = <department>
         * and employee.age = <age>
         */

        Query q5 = new Query();
        q5.addToSelect(qc1);
        q5.addToSelect(qc2);
        q5.addFrom(qc1);
        q5.addFrom(qc2);
        ConstraintSet cs2 = new ConstraintSet(ConstraintOp.AND);
        cs2.addConstraint(con1);
        cs2.addConstraint(new ClassConstraint(qc1, ConstraintOp.EQUALS, dept));
        q5.setConstraint(cs2);

        assertEquals(q4, q2);
        assertEquals(q5, q2);
    }

    public void testMatchesDoubleGroupByClassCountFunction() throws Exception {
        /* select company, department, count()
         * from company, department, employee
         * where company.department = department,
         * and employee.department = department
         * group by company, department
         */
        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        QueryClass qc3 = new QueryClass(Employee.class);
        QueryFunction qf1 = new QueryFunction();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addToSelect(qf1);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addFrom(qc3);
        Constraint con1 = new ContainsConstraint(new QueryCollectionReference(qc1, "departments"),
                                                 ConstraintOp.CONTAINS, qc2);
        Constraint con2 = new ContainsConstraint(new QueryCollectionReference(qc2, "employees"),
                                                 ConstraintOp.CONTAINS, qc3);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        cs1.addConstraint(con1);
        cs1.addConstraint(con2);
        q1.setConstraint(cs1);
        q1.addToGroupBy(qc1);
        q1.addToGroupBy(qc2);

        Company company = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        TypeUtil.setFieldValue(company, "id", new Integer(42));
        company.setName("Acme");

        Department dept = new Department();
        TypeUtil.setFieldValue(dept, "id", new Integer(43));
        dept.setName("Purchasing");

        ResultsRow row1 = new ResultsRow();
        row1.add(company);
        row1.add(dept);
        row1.add(new Integer(4));

        CollatedResult cr = new CollatedResult();
        Query q2 = cr.getMatchesQuery(qf1, q1, row1);

        /* select company, department, employee
         * from company, department, employee
         * where company.department = department,
         * and employee.department = department
         * and company = <company>
         * and department = <department>
         */

        Query q3 = new Query();
        q3.addToSelect(qc1);
        q3.addToSelect(qc2);
        q3.addToSelect(qc3);
        q3.addFrom(qc1);
        q3.addFrom(qc2);
        q3.addFrom(qc3);
        ConstraintSet cs2 = new ConstraintSet(ConstraintOp.AND);
        cs2.addConstraint(cs1);
        cs2.addConstraint(new ClassConstraint(qc1, ConstraintOp.EQUALS, company));
        cs2.addConstraint(new ClassConstraint(qc2, ConstraintOp.EQUALS, dept));
        q3.setConstraint(cs2);

        assertEquals(q3, q2);
    }

    public void testMatchesSingleDistinctField() throws Exception {
        /* select distinct department.name
         * from department, employee
         * where employee.name matches "A"
         */
        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Employee.class);
        QueryField qf1 = new QueryField(qc1, "name");
        QueryField qf2 = new QueryField(qc2, "name");
        q1.addToSelect(qf1);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        Constraint con1 = new SimpleConstraint(qf2,
                                               ConstraintOp.MATCHES,
                                               new QueryValue("A"));
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        cs1.addConstraint(con1);
        q1.setConstraint(cs1);
        q1.setDistinct(true);

        Department dept = new Department();
        TypeUtil.setFieldValue(dept, "id", new Integer(42));
        dept.setName("Purchasing");

        ResultsRow row1 = new ResultsRow();
        row1.add(dept.getName());

        CollatedResult cr = new CollatedResult();
        Query q2 = cr.getMatchesQuery(qf1, q1, row1);

        /* select department, employee
         * from department, employee
         * where employee.name matches "A"
         * where department.name = <department name>
         */

        Query q3 = new Query();
        q3.addToSelect(qc1);
        q3.addToSelect(qc2);
        q3.addFrom(qc1);
        q3.addFrom(qc2);
        ConstraintSet cs2 = new ConstraintSet(ConstraintOp.AND);
        cs2.addConstraint(cs1);
        cs2.addConstraint(new SimpleConstraint(qf1, ConstraintOp.EQUALS, new QueryValue(dept.getName())));
        q3.setConstraint(cs2);

        assertEquals(q3, q2);
    }

    public void testMatchesSingleDistinctClass() throws Exception {
        /* select distinct department
         * from department, employee
         * where employee.name matches "A"
         */
        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Employee.class);
        QueryField qf1 = new QueryField(qc1, "name");
        QueryField qf2 = new QueryField(qc2, "name");
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        Constraint con1 = new SimpleConstraint(qf2,
                                               ConstraintOp.MATCHES,
                                               new QueryValue("A"));
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        cs1.addConstraint(con1);
        q1.setConstraint(cs1);
        q1.setDistinct(true);

        Department dept = new Department();
        TypeUtil.setFieldValue(dept, "id", new Integer(42));
        dept.setName("Purchasing");

        ResultsRow row1 = new ResultsRow();
        row1.add(dept);

        CollatedResult cr = new CollatedResult();
        Query q2 = cr.getMatchesQuery(qf1, q1, row1);

        /* select department, employee
         * from department, employee
         * where employee.name matches "A"
         * and department = <department>
         */

        Query q3 = new Query();
        q3.addToSelect(qc1);
        q3.addToSelect(qc2);
        q3.addFrom(qc1);
        q3.addFrom(qc2);
        ConstraintSet cs2 = new ConstraintSet(ConstraintOp.AND);
        cs2.addConstraint(cs1);
        cs2.addConstraint(new ClassConstraint(qc1, ConstraintOp.EQUALS, dept));
        q3.setConstraint(cs2);

        assertEquals(q3, q2);
    }

    public void testMatchesDoubleDistinctClass() throws Exception {
        /* select distinct department, employee
         * from department, employee
         * where employee.name matches "A"
         */
        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Employee.class);
        QueryField qf1 = new QueryField(qc1, "name");
        QueryField qf2 = new QueryField(qc2, "name");
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        Constraint con1 = new SimpleConstraint(qf2,
                                               ConstraintOp.MATCHES,
                                               new QueryValue("A"));
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        cs1.addConstraint(con1);
        q1.setConstraint(cs1);
        q1.setDistinct(true);

        Department dept = new Department();
        TypeUtil.setFieldValue(dept, "id", new Integer(42));
        dept.setName("Purchasing");
        Employee employee = new Employee();
        TypeUtil.setFieldValue(employee, "id", new Integer(43));
        employee.setName("Employee");

        ResultsRow row1 = new ResultsRow();
        row1.add(dept);
        row1.add(employee);

        CollatedResult cr = new CollatedResult();
        Query q2 = cr.getMatchesQuery(qc1, q1, row1);
        Query q3 = cr.getMatchesQuery(qc2, q1, row1);

        /* select department, employee
         * from department, employee
         * where employee.name matches "A"
         * where department.name = <department name>
         * where employee.name = <employee name>
         */

        Query q4 = new Query();
        q4.addToSelect(qc1);
        q4.addToSelect(qc2);
        q4.addFrom(qc1);
        q4.addFrom(qc2);
        ConstraintSet cs2 = new ConstraintSet(ConstraintOp.AND);
        cs2.addConstraint(cs1);
        cs2.addConstraint(new ClassConstraint(qc1, ConstraintOp.EQUALS, dept));
        cs2.addConstraint(new ClassConstraint(qc2, ConstraintOp.EQUALS, employee));
        q4.setConstraint(cs2);

        assertEquals(q4, q2);
        assertEquals(q4, q3);
    }

    public void testMatchesDoubleDistinctField() throws Exception {
        /* select distinct department.name, employee.name
         * from department, employee
         * where employee.name matches "A"
         */
        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Employee.class);
        QueryField qf1 = new QueryField(qc1, "name");
        QueryField qf2 = new QueryField(qc2, "name");
        q1.addToSelect(qf1);
        q1.addToSelect(qf2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        Constraint con1 = new SimpleConstraint(qf2,
                                               ConstraintOp.MATCHES,
                                               new QueryValue("A"));
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        cs1.addConstraint(con1);
        q1.setConstraint(cs1);
        q1.setDistinct(true);

        Department dept = new Department();
        dept.setName("Purchasing");
        Employee employee = new Employee();
        employee.setName("Employee");

        ResultsRow row1 = new ResultsRow();
        row1.add(dept.getName());
        row1.add(employee.getName());

        CollatedResult cr = new CollatedResult();
        Query q2 = cr.getMatchesQuery(qf1, q1, row1);
        Query q3 = cr.getMatchesQuery(qf2, q1, row1);

        /* select department, employee
         * from department, employee
         * where employee.name matches "A"
         * where department.name = <department name>
         * where employee.name = <employee name>
         */

        Query q4 = new Query();
        q4.addToSelect(qc1);
        q4.addToSelect(qc2);
        q4.addFrom(qc1);
        q4.addFrom(qc2);
        ConstraintSet cs2 = new ConstraintSet(ConstraintOp.AND);
        cs2.addConstraint(cs1);
        cs2.addConstraint(new SimpleConstraint(qf1, ConstraintOp.EQUALS, new QueryValue(dept.getName())));
        cs2.addConstraint(new SimpleConstraint(qf2, ConstraintOp.EQUALS, new QueryValue(employee.getName())));
        q4.setConstraint(cs2);

        assertEquals(q4, q2);
        assertEquals(q4, q3);
    }

    public void testReturnNullForSubquery() throws Exception {
        /* select department, employee
         * from department, (subquery)
         * where employee.department = department
         */
        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Employee.class);
        QueryFunction qf1 = new QueryFunction();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);

        Query q2 = new Query();

        q1.addFrom(q2);
        Constraint con1 = new ContainsConstraint(new QueryCollectionReference(qc1, "employees"),
                                                 ConstraintOp.CONTAINS, qc2);
        q1.setConstraint(con1);

        Department dept = new Department();
        dept.setName("Purchasing");

        ResultsRow row1 = new ResultsRow();
        row1.add(dept);

        CollatedResult cr = new CollatedResult();
        assertNull(cr.getMatchesQuery(qc1, q1, row1));

    }

    public void testReturnNullForNoGroupByOrDistinct() throws Exception {
        /* select department, employee
         * from department, employee
         * where employee.department = department
         */
        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Employee.class);
        QueryFunction qf1 = new QueryFunction();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        Constraint con1 = new ContainsConstraint(new QueryCollectionReference(qc1, "employees"),
                                                 ConstraintOp.CONTAINS, qc2);
        q1.setConstraint(con1);
        q1.setDistinct(false);

        Department dept = new Department();
        dept.setName("Purchasing");

        Employee employee = new Employee();
        employee.setName("Employee");

        ResultsRow row1 = new ResultsRow();
        row1.add(dept);
        row1.add(employee);

        CollatedResult cr = new CollatedResult();
        assertNull(cr.getMatchesQuery(qc1, q1, row1));
    }
}
