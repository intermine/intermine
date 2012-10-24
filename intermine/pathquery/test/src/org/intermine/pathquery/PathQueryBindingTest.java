package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ConstraintOp;

/**
 * Tests for the PathQueryBinding class
 *
 * @author Kim Rutherford
 */
public class PathQueryBindingTest extends TestCase
{
    Map<String, PathQuery> savedQueries, expected;

    public void setUp() throws Exception {
        super.setUp();
        InputStream is = getClass().getClassLoader().getResourceAsStream("PathQueryBindingTest.xml");
        savedQueries = PathQueryBinding.unmarshalPathQueries(new InputStreamReader(is), 1);
        // checking can be removed maybe
        expected = getExpectedQueries();
    }

    public PathQueryBindingTest(String arg) {
        super(arg);
    }


    public Map<String, PathQuery> getExpectedQueries() throws Exception {
        Map<String, PathQuery> expected = new LinkedHashMap<String, PathQuery>();

        Model model = Model.getInstanceByName("testmodel");
        // allCompanies
        PathQuery allCompanies = new PathQuery(model);
        allCompanies.addView("Company.name");
        expected.put("allCompanies", allCompanies);

        // employeesWithOldManagers
        PathQuery employeesWithOldManagers = new PathQuery(model);
        employeesWithOldManagers.addViews("Employee.name", "Employee.age", "Employee.department.name", "Employee.department.manager.age");
        employeesWithOldManagers.addConstraint(new PathConstraintAttribute("Employee.department.manager.age", ConstraintOp.GREATER_THAN, "10"));
        employeesWithOldManagers.setDescription("Employee.department",
                "Department of the Employee");
        expected.put("employeesWithOldManagers", employeesWithOldManagers);

        // companyInBag
        PathQuery companyInBag = new PathQuery(model);
        companyInBag.addView("Company");
        companyInBag.addConstraint(new PathConstraintBag("Company", ConstraintOp.IN, "bag1"));
        expected.put("companyInBag", companyInBag);

        // queryWithConstraint
        PathQuery queryWithConstraint = new PathQuery(model);
        queryWithConstraint.addViews("Company.name", "Company.departments.name", "Company.departments.employees.name", "Company.departments.employees.title");
        queryWithConstraint.addConstraint(new PathConstraintSubclass("Company.departments.employees", "CEO"));
        expected.put("queryWithConstraint", queryWithConstraint);

        // employeesInBag
        PathQuery employeesInBag = new PathQuery(model);
        employeesInBag.addView("Employee.name");
        employeesInBag.addConstraint(new PathConstraintBag("Employee.end", ConstraintOp.IN, "bag1"));
        //Exception e = new Exception("Invalid bag constraint - only objects can be"
        //                            + "constrained to be in bags.");
        //employeesInBag.problems.add(e);
        expected.put("employeeEndInBag", employeesInBag);

        return expected;
    }

    public void testAllCompanies() throws Exception {
        assertEquals(expected.get("allCompanies").toString(), savedQueries.get("allCompanies").toString());
    }

    public void testEmployeesWithOldManagers() throws Exception {
        assertEquals(expected.get("employeesWithOldManagers").toString(), savedQueries.get("employeesWithOldManagers").toString());
    }


    public void testCompanyNumberInBag() throws Exception {
        assertEquals(expected.get("companyInBag").toString(), savedQueries.get("companyInBag").toString());
    }

    public void testQueryWithConstraint() throws Exception {
        assertEquals(expected.get("queryWithConstraint").toString(), savedQueries.get("queryWithConstraint").toString());
    }
    
    public void testRangeQuery() throws Exception {
        PathQuery pq = new PathQuery(Model.getInstanceByName("testmodel"));
        pq.addViews("Employee.name");
        pq.addConstraint(new PathConstraintRange("Employee.age", ConstraintOp.WITHIN, Arrays.asList("40 .. 50", "55 .. 60")));
        pq.addConstraint(new PathConstraintRange("Employee.employmentPeriod", ConstraintOp.OVERLAPS, Arrays.asList("01-01-2012")));
        
        assertEquals(pq.toString(), savedQueries.get("rangeQueries").toString());
    }
    
    public void testMultiTypeQuery() throws Exception {
        PathQuery pq = new PathQuery(Model.getInstanceByName("testmodel"));
        pq.addViews("Employable.name");
        pq.addConstraint(new PathConstraintMultitype("Employable", ConstraintOp.ISA, Arrays.asList("Contractor", "Manager")));
        
        assertEquals(pq.toString(), savedQueries.get("multitype").toString());
    }

    public void testMarshallings() throws Exception {
        // Test marshallings
        String xml = PathQueryBinding.marshal(expected.get("employeesWithOldManagers"),
                "employeesWithOldManagers", "testmodel", 1);
        Map<String, PathQuery> readFromXml = new LinkedHashMap<String, PathQuery>();
        readFromXml = PathQueryBinding.unmarshalPathQueries(new InputStreamReader(new ByteArrayInputStream(xml.getBytes())), 1);
        // checking can be removed maybe
        Map<String, PathQuery> expectedQuery = new LinkedHashMap<String, PathQuery>();
        expectedQuery.put("employeesWithOldManagers", expected.get("employeesWithOldManagers"));

        assertEquals(xml, expectedQuery.toString(), readFromXml.toString());

        xml = PathQueryBinding.marshal(expected.get("queryWithConstraint"),
                "queryWithConstraint", "testmodel", 1);
        readFromXml = new LinkedHashMap<String, PathQuery>();
        readFromXml = PathQueryBinding.unmarshalPathQueries(new InputStreamReader(new ByteArrayInputStream(xml.getBytes())), 1);
        expectedQuery = new LinkedHashMap<String, PathQuery>();
        expectedQuery.put("queryWithConstraint", expected.get("queryWithConstraint"));

        assertEquals(xml, expectedQuery.toString(), readFromXml.toString());
    }

    public void testNewPathQuery() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery q = new PathQuery(model);
        q.addView("Employee.name");
        q.addConstraint(new PathConstraintAttribute("Employee.age", ConstraintOp.LESS_THAN, "50"));
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Employee.name\" longDescription=\"\"><constraint path=\"Employee.age\" op=\"&lt;\" value=\"50\"/></query>", PathQueryBinding.marshal(q, "test", "testmodel", 1));
    }

    public void testNewPathQuery2() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery q = new PathQuery(model);
        q.addView("Employee.name");
        q.addView("Employee.department.name");
        q.addOrderBy("Employee.age", OrderDirection.ASC);
        q.addConstraint(new PathConstraintAttribute("Employee.age", ConstraintOp.LESS_THAN, "50"));
        q.addConstraint(new PathConstraintAttribute("Employee.department.name", ConstraintOp.EQUALS, "Fred"));
        q.setConstraintLogic("A or B");
        q.setOuterJoinStatus("Employee.department", OuterJoinStatus.INNER);
        q.setDescription("Flibble");
        q.setDescription("Employee.name", "Albert");
        assertEquals("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.name\" longDescription=\"Flibble\" sortOrder=\"Employee.age asc\" constraintLogic=\"A or B\"><join path=\"Employee.department\" style=\"INNER\"/><pathDescription pathString=\"Employee.name\" description=\"Albert\"/><constraint path=\"Employee.age\" code=\"A\" op=\"&lt;\" value=\"50\"/><constraint path=\"Employee.department.name\" code=\"B\" op=\"=\" value=\"Fred\"/></query>", PathQueryBinding.marshal(q, "test", "testmodel", 1));
    }
}
