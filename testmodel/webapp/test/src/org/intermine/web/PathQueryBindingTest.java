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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.query.ConstraintOp;

import org.intermine.metadata.Model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.TestCase;

/**
 * Tests for the PathQueryBinding class
 *
 * @author Kim Rutherford
 */
public class PathQueryBindingTest extends TestCase
{

    public PathQueryBindingTest(String arg) {
        super(arg);
    }

    public void testProcess() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("PathQueryBindingTest.xml");
        Map savedQueries = PathQueryBinding.unmarshal(new InputStreamReader(is), new HashMap());

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
        PathNode vatNumber = vatNumberInBag.addNode("Company.vatNumber");
        vatNumber.getConstraints().add(new Constraint(ConstraintOp.IN, "bag1"));
        expected.put("vatNumberInBag", vatNumberInBag);

        assertEquals(expected, savedQueries);
        
        // Test marshallings
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        String xml = PathQueryBinding.marshal(employeesWithOldManagers, "employeesWithOldManagers", "testmodel");
        savedQueries = PathQueryBinding.unmarshal(new InputStreamReader(new ByteArrayInputStream(xml.getBytes())), new HashMap());
        expected = new LinkedHashMap();
        expected.put("employeesWithOldManagers", employeesWithOldManagers);
        
        assertEquals(expected, savedQueries);
    }
}
