package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
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
import java.util.Properties;

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
    Map savedQueries, expected, classKeys;
    
    public void setUp() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("PathQueryBindingTest.xml");
        Model model = Model.getInstanceByName("testmodel");
        Properties classKeyProps = new Properties();
            classKeyProps.load(getClass().getClassLoader()
                               .getResourceAsStream("class_keys.properties"));
        classKeys = ClassKeyHelper.readKeys(model, classKeyProps);
        savedQueries = PathQueryBinding.unmarshal(new InputStreamReader(is), new HashMap(), classKeys);
        expected = getExpectedQueries();
    }
    
    public PathQueryBindingTest(String arg) {
        super(arg);
    }


    public Map getExpectedQueries() {
        Map expected = new LinkedHashMap();
        
        //allCompanies
        PathQuery allCompanies = new PathQuery(Model.getInstanceByName("testmodel"));
        List view = new ArrayList();
        view.add("Company");
        allCompanies.setView(view);
        expected.put("allCompanies", allCompanies);
        
        //managers
        PathQuery managers = new PathQuery(Model.getInstanceByName("testmodel"));
        PathNode employee = managers.addNode("Employee");
        employee.setType("Manager");
        expected.put("managers", managers);
        
        view = new ArrayList();
        //employeesWithOldManagers
        PathQuery employeesWithOldManagers = new PathQuery(Model.getInstanceByName("testmodel"));
        view = new ArrayList();
        view.add("Employee.name");
        view.add("Employee.age");
        view.add("Employee.department.name");
        view.add("Employee.department.manager.age");
        employeesWithOldManagers.setView(view);
        PathNode age = employeesWithOldManagers.addNode("Employee.department.manager.age");
        age.getConstraints().add(new Constraint(ConstraintOp.GREATER_THAN, new Integer(10),
                                                true, "age is greater than 10", null, "age_gt_10"));
        employeesWithOldManagers.addAlternativeView("altView1", Arrays.asList(new Object[]{"Employee.name", "Employee.age"}));
        employeesWithOldManagers.addAlternativeView("altView2", Arrays.asList(new Object[]{"Employee.name"}));
        expected.put("employeesWithOldManagers", employeesWithOldManagers);
        
        //vatNumberInBag
        PathQuery vatNumberInBag = new PathQuery(Model.getInstanceByName("testmodel"));
        view = new ArrayList();
        view.add("Company");
        vatNumberInBag.setView(view);
        PathNode company = vatNumberInBag.addNode("Company");
        company.getConstraints().add(new Constraint(ConstraintOp.IN, "bag1"));
        PathNode vatNumber = vatNumberInBag.addNode("Company.vatNumber");
        expected.put("vatNumberInBag", vatNumberInBag);
        
        // employeesInBag
        PathQuery employeesInBag = new PathQuery(Model.getInstanceByName("testmodel"));
        view = new ArrayList();
        view.add("Employee.name");
        employeesInBag.setView(view);
        PathNode employeeEnd = employeesInBag.addNode("Employee.end");
        employeeEnd.getConstraints().add(new Constraint(ConstraintOp.IN, "bag1"));
        Exception e = new Exception("Invalid bag constraint - only objects can be"
                                    + "constrained to be in bags.");
        employeesInBag.problems.add(e);
        expected.put("employeeEndInBag", employeesInBag);
        
        return expected;
    }
    
    public void testAllCompanies() throws Exception {
        assertEquals(expected.get("allCompanies"), savedQueries.get("allCompanies"));
    }
    public void testManagers() throws Exception {
        assertEquals(expected.get("managers"), savedQueries.get("managers"));
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
        assertEquals(((PathQuery) expected.get("employeeEndInBag")).problems,
                ((PathQuery) savedQueries.get("employeeEndInBag")));
    }
    
    public void testMarshallings() throws Exception {
        // Test marshallings
        String xml = PathQueryBinding.marshal((PathQuery) expected.get("employeesWithOldManagers"), "employeesWithOldManagers", "testmodel");
        Map readFromXml = new LinkedHashMap();
        readFromXml = PathQueryBinding.unmarshal(new InputStreamReader(new ByteArrayInputStream(xml.getBytes())),
                                                 new HashMap(), classKeys);
        Map expectedQuery = new LinkedHashMap();
        expectedQuery.put("employeesWithOldManagers", expected.get("employeesWithOldManagers"));

        
        assertEquals(expectedQuery, readFromXml);
    }
}
