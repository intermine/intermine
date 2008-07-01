package org.intermine.web.logic.query;

/*
 * Copyright (C) 2002-2008 FlyMine
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.TestUtil;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.path.Path;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;

/**
 * Tests for the PathQueryBinding class
 *
 * @author Kim Rutherford
 */
public class PathQueryBindingTest extends TestCase
{
    Map savedQueries, expected, classKeys;

    public void setUp() throws Exception {
        super.setUp();
        InputStream is = getClass().getClassLoader().getResourceAsStream("PathQueryBindingTest.xml");
        classKeys = TestUtil.getClassKeys(TestUtil.getModel());
        savedQueries = PathQueryBinding.unmarshal(new InputStreamReader(is), classKeys);
        // checking can be removed maybe
        MainHelper.checkPathQueries(savedQueries, new HashMap());
        expected = getExpectedQueries();
    }

    public PathQueryBindingTest(String arg) {
        super(arg);
    }


    public Map getExpectedQueries() {
        Map expected = new LinkedHashMap();

        Model model = Model.getInstanceByName("testmodel");
        // allCompanies
        PathQuery allCompanies = new PathQuery(model);
        List<Path> view = new ArrayList();
        view.add(PathQuery.makePath(model, allCompanies, "Company"));
        allCompanies.setViewPaths(view);
        expected.put("allCompanies", allCompanies);

        view = new ArrayList();
        // employeesWithOldManagers
        PathQuery employeesWithOldManagers = new PathQuery(model);
        view = new ArrayList();
        view.add(PathQuery.makePath(model, employeesWithOldManagers, "Employee.name"));
        view.add(PathQuery.makePath(model, employeesWithOldManagers, "Employee.age"));
        view.add(PathQuery.makePath(model, employeesWithOldManagers, "Employee.department.name"));
        view.add(PathQuery.makePath(model, employeesWithOldManagers,
                                     "Employee.department.manager.age"));
        employeesWithOldManagers.setViewPaths(view);
        PathNode age = employeesWithOldManagers.addNode("Employee.department.manager.age");
        age.getConstraints().add(new Constraint(ConstraintOp.GREATER_THAN, new Integer(10),
                                                true, "age is greater than 10", null, "age_gt_10", null));
        employeesWithOldManagers.addPathStringDescription("Employee.department",
                                                          "Department of the Employee");
        expected.put("employeesWithOldManagers", employeesWithOldManagers);

        // vatNumberInBag
        PathQuery vatNumberInBag = new PathQuery(model);
        view = new ArrayList();
        view.add(PathQuery.makePath(model, vatNumberInBag, "Company"));
        vatNumberInBag.setViewPaths(view);
        PathNode company = vatNumberInBag.addNode("Company");
        company.getConstraints().add(new Constraint(ConstraintOp.IN, "bag1"));
        PathNode vatNumber = vatNumberInBag.addNode("Company.vatNumber");
        expected.put("vatNumberInBag", vatNumberInBag);

        // queryWithConstraint
        PathQuery queryWithConstraint = new PathQuery(model);
        view = new ArrayList();
        queryWithConstraint.addNode("Company");
        queryWithConstraint.addNode("Company.departments");
        PathNode pathNode = queryWithConstraint.addNode("Company.departments.employees");
        pathNode.setType("CEO");
        view.add(PathQuery.makePath(model, queryWithConstraint, "Company.name"));
        view.add(PathQuery.makePath(model, queryWithConstraint, "Company.departments.name"));
        view.add(PathQuery.makePath(model, queryWithConstraint, "Company.departments.employees.name"));
        view.add(PathQuery.makePath(model, queryWithConstraint, "Company.departments.employees.title"));

        queryWithConstraint.setViewPaths(view);
        expected.put("queryWithConstraint", queryWithConstraint);

        // employeesInBag
        PathQuery employeesInBag = new PathQuery(model);
        view = new ArrayList();
        view.add(PathQuery.makePath(model, employeesInBag, "Employee.name"));
        employeesInBag.setViewPaths(view);
        PathNode employeeEnd = employeesInBag.addNode("Employee.end");
        employeeEnd.getConstraints().add(new Constraint(ConstraintOp.IN, "bag1"));
        Exception e = new Exception("Invalid bag constraint - only objects can be"
                                    + "constrained to be in bags.");
        //employeesInBag.problems.add(e);
        List<Throwable> problems = new ArrayList<Throwable>(Arrays.asList(employeesInBag.getProblems()));
        problems.add(e);
        employeesInBag.setProblems(problems);
        expected.put("employeeEndInBag", employeesInBag);

        return expected;
    }

    public void testAllCompanies() throws Exception {
        assertEquals(expected.get("allCompanies"), savedQueries.get("allCompanies"));
    }
    public void testEmployeesWithOldManagers() throws Exception {
        assertEquals(expected.get("employeesWithOldManagers"), savedQueries.get("employeesWithOldManagers"));
    }

    // will move vatNumber bag constraint to parent node
    public void testVatNumberInBag() throws Exception {
        assertEquals(expected.get("vatNumberInBag"), savedQueries.get("vatNumberInBag"));
    }

    // this won't move bag constraint to parent, will not produce a valid query
    public void employeeEndInBag() throws Exception {
        assertEquals(expected.get("employeeEndInBag"), savedQueries.get("employeeEndInBag"));
        System.out.println(((PathQuery) savedQueries.get("employeeEndInBag")));
        List<Throwable> problems = Arrays.asList(((PathQuery) expected.get("employeeEndInBag")).getProblems());
        assertEquals(problems,
                ((PathQuery) savedQueries.get("employeeEndInBag")));
    }

    public void testMarshallings() throws Exception {
        // Test marshallings
        String xml = PathQueryBinding.marshal((PathQuery) expected.get("employeesWithOldManagers"),
                                              "employeesWithOldManagers", "testmodel");
        Map readFromXml = new LinkedHashMap();
        readFromXml = PathQueryBinding.unmarshal(new InputStreamReader(new ByteArrayInputStream(xml.getBytes())), classKeys);
        // checking can be removed maybe
        MainHelper.checkPathQueries(readFromXml, new HashMap());
        Map expectedQuery = new LinkedHashMap();
        expectedQuery.put("employeesWithOldManagers", expected.get("employeesWithOldManagers"));

        assertEquals(xml, expectedQuery, readFromXml);

        xml = PathQueryBinding.marshal((PathQuery) expected.get("queryWithConstraint"),
                                       "queryWithConstraint", "testmodel");
        readFromXml = new LinkedHashMap();
        readFromXml = PathQueryBinding.unmarshal(new InputStreamReader(new ByteArrayInputStream(xml.getBytes())), classKeys);
        MainHelper.checkPathQueries(readFromXml, new HashMap());
        expectedQuery = new LinkedHashMap();
        expectedQuery.put("queryWithConstraint", expected.get("queryWithConstraint"));

        assertEquals(xml, expectedQuery, readFromXml);
    }
}
