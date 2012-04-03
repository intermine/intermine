package org.intermine.template.xml;

/*
 * Copyright (C) 2002-2011 FlyMine
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
import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathConstraintSubclass;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;

/**
 * Tests for the TemplateQueryBinding class
 *
 * @author Julie Sullivan
 */
public class TemplateQueryBindingTest extends TestCase
{
    Map<String, TemplateQuery> savedTemplates, expected;

    public void setUp() throws Exception {
        super.setUp();
        InputStream is = getClass().getClassLoader().getResourceAsStream("TemplateQueryBindingTest.xml");
        savedTemplates = TemplateQueryBinding.unmarshalTemplates(new InputStreamReader(is), 1);
        // checking can be removed maybe
        expected = getExpectedQueries();
    }

    public TemplateQueryBindingTest(String arg) {
        super(arg);
    }

    public Map<String, TemplateQuery> getExpectedQueries() throws Exception {
        Map<String, TemplateQuery> expected = new LinkedHashMap<String, TemplateQuery>();

        Model model = Model.getInstanceByName("testmodel");
        // allCompanies
        PathQuery allCompanies = new PathQuery(model);
        allCompanies.addView("Company");

        TemplateQuery t = new TemplateQuery("allCompanies", "All Companies --> Data", "comment", allCompanies);
        expected.put("allCompanies", t);

        // employeesWithOldManagers
        PathQuery employeesWithOldManagers = new PathQuery(model);
        employeesWithOldManagers.addViews("Employee.name", "Employee.age", "Employee.department.name", "Employee.department.manager.age");
        employeesWithOldManagers.addConstraint(new PathConstraintAttribute("Employee.department.manager.age", ConstraintOp.GREATER_THAN, "10"));
        employeesWithOldManagers.setDescription("Employee.department",
                "Department of the Employee");

        t = new TemplateQuery("employeesWithOldManagers", "Old Managers --> Employees", "This template is exciting", employeesWithOldManagers);
        expected.put("employeesWithOldManagers", t);

        // companyInBag
        PathQuery companyInBag = new PathQuery(model);
        companyInBag.addView("Company");
        companyInBag.addConstraint(new PathConstraintBag("Company", ConstraintOp.IN, "bag1"));

        t = new TemplateQuery("companyInBag", "List --> Company", "Côte d'Ivoire", companyInBag);
        expected.put("companyInBag", t);

        // queryWithConstraint
        PathQuery queryWithConstraint = new PathQuery(model);
        queryWithConstraint.addViews("Company.name", "Company.departments.name", "Company.departments.employees.name", "Company.departments.employees.title");
        queryWithConstraint.addConstraint(new PathConstraintSubclass("Company.departments.employees", "CEO"));
        t = new TemplateQuery("queryWithConstraint", "Company --> CEO", "", queryWithConstraint);
        t.setDescription("this is the queryWithConstraint description");
        expected.put("queryWithConstraint", t);

        // employeesInBag
        PathQuery employeesInBag = new PathQuery(model);
        employeesInBag.addView("Employee.name");
        employeesInBag.addConstraint(new PathConstraintBag("Employee.end", ConstraintOp.IN, "bag1"));
        //Exception e = new Exception("Invalid bag constraint - only objects can be"
        //                            + "constrained to be in bags.");
        //employeesInBag.problems.add(e);
        t = new TemplateQuery("employeeEndInBag", "List --> Employee", null, employeesInBag);
        expected.put("employeeEndInBag", t);

        return expected;
    }

    public void testAllCompanies() throws Exception {
        assertEquals(expected.get("allCompanies").toXml(), savedTemplates.get("allCompanies").toXml());
    }

    public void testEmployeesWithOldManagers() throws Exception {
        assertEquals(expected.get("employeesWithOldManagers").toString(), savedTemplates.get("employeesWithOldManagers").toString());
    }

    public void testCompanyNumberInBag() throws Exception {
        assertEquals(expected.get("companyInBag").toString(), savedTemplates.get("companyInBag").toString());
    }

    public void testQueryWithConstraint() throws Exception {
        assertEquals(expected.get("queryWithConstraint").toString(), savedTemplates.get("queryWithConstraint").toString());
    }

    public void employeeEndInBag() throws Exception {
        assertEquals(expected.get("employeeEndInBag").toString(), savedTemplates.get("employeeEndInBag").toString());
    }

    public void testMarshallings() throws Exception {
        // Test marshallings
        String xml = TemplateQueryBinding.marshal(expected.get("employeesWithOldManagers"), "employeesWithOldManagers", "testmodel", 1);
        System.out.println(xml);
        Map<String, TemplateQuery> readFromXml = new LinkedHashMap<String, TemplateQuery>();
        readFromXml = TemplateQueryBinding.unmarshalTemplates(new StringReader(xml), 1);
        System.out.println(readFromXml.size());
        // checking can be removed maybe
        Map<String, TemplateQuery> expectedQuery = new LinkedHashMap<String, TemplateQuery>();
        expectedQuery.put("employeesWithOldManagers", expected.get("employeesWithOldManagers"));

        assertEquals(xml, expectedQuery.toString(), readFromXml.toString());

        xml = TemplateQueryBinding.marshal(expected.get("queryWithConstraint"), "queryWithConstraint", "testmodel", 1);
        readFromXml = new LinkedHashMap<String, TemplateQuery>();
        readFromXml = TemplateQueryBinding.unmarshalTemplates(new StringReader(xml), 1);
        expectedQuery = new LinkedHashMap<String, TemplateQuery>();
        expectedQuery.put("queryWithConstraint", expected.get("queryWithConstraint"));

        assertEquals(xml, expectedQuery.toString(), readFromXml.toString());
    }
}
