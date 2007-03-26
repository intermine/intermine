package org.intermine.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.intermine.metadata.Model;

import junit.framework.TestCase;

/**
 * Tests for the MainChange class
 *
 * @author Kim Rutherford
 */

public class MainChangeTest  extends TestCase
{
    public MainChangeTest(String arg) {
        super(arg);
    }

    public void testRemoveNode1() throws Exception {
        PathQuery query = new PathQuery(Model.getInstanceByName("testmodel"));
        PathNode employeeNode = query.addNode("Employee");
        employeeNode.setType("Employee");
        query.addNode("Employee.department");
        query.addNode("Employee.age");
        PathNode managerNode = query.addNode("Employee.department.manager");
        managerNode.setType("CEO");
        query.getView().add("Employee");
        query.getView().add("Employee.end");
        query.getView().add("Employee.age");
        query.getView().add("Employee.department");
        query.getView().add("Employee.department.manager");
        query.getView().add("Employee.department.manager.seniority");
        query.getView().add("Employee.department.manager.company.address.address");
        query.getView().add("Employee.department.manager.company.name");
        query.getView().add("Employee.department.manager.company.address");
        MainChange.removeNode(query, "Employee");

        assertEquals(0, query.getNodes().keySet().size());

        List expectedView = Arrays.asList(new Object[] {
                                              "Employee",
                                              "Employee.end",
                                              "Employee.age",
                                              "Employee.department",
                                              "Employee.department.manager",
                                              "Employee.department.manager.seniority"
                                          });

        assertEquals(expectedView, query.getView());
    }

    public void testRemoveNode2() throws Exception {
        PathQuery query = new PathQuery(Model.getInstanceByName("testmodel"));
        PathNode employeeNode = query.addNode("Employee");
        employeeNode.setType("Employee");
        query.addNode("Employee.department");
        query.addNode("Employee.age");
        PathNode managerNode = query.addNode("Employee.department.manager");
        managerNode.setType("CEO");
        query.getView().add("Employee");
        query.getView().add("Employee.end");
        query.getView().add("Employee.age");
        query.getView().add("Employee.department");
        query.getView().add("Employee.department.manager");
        query.getView().add("Employee.department.manager.seniority");
        query.getView().add("Employee.department.manager.company.address.address");
        query.getView().add("Employee.department.manager.company.name");
        query.getView().add("Employee.department.manager.company.address");
        MainChange.removeNode(query, "Employee.department");

        List expectedNodes = Arrays.asList(new Object[] {
                                               "Employee",
                                               "Employee.age"
                                           });
        assertEquals(expectedNodes, new ArrayList(query.getNodes().keySet()));

        List expectedView = Arrays.asList(new Object[] {
                                              "Employee",
                                              "Employee.end",
                                              "Employee.age",
                                              "Employee.department",
                                              "Employee.department.manager",
                                              "Employee.department.manager.seniority"
                                          });

        assertEquals(expectedView, query.getView());
    }

    public void testRemoveNode3() throws Exception {
        PathQuery query = new PathQuery(Model.getInstanceByName("testmodel"));
        PathNode employeeNode = query.addNode("Employee");
        employeeNode.setType("Employee");
        query.addNode("Employee.department");
        query.addNode("Employee.age");
        PathNode managerNode = query.addNode("Employee.department.manager");
        managerNode.setType("CEO");
        query.getView().add("Employee");
        query.getView().add("Employee.end");
        query.getView().add("Employee.age");
        query.getView().add("Employee.department");
        query.getView().add("Employee.department.manager");
        query.getView().add("Employee.department.manager.seniority");
        query.getView().add("Employee.department.manager.company.address.address");
        query.getView().add("Employee.department.manager.company.name");
        query.getView().add("Employee.department.manager.company.address");
        MainChange.removeNode(query, "Employee.department.manager");

        List expectedView = Arrays.asList(new Object[] {
                                              "Employee",
                                              "Employee.end",
                                              "Employee.age",
                                              "Employee.department",
                                              "Employee.department.manager",
                                              "Employee.department.manager.seniority"
                                          });

        assertEquals(expectedView, query.getView());
    }

    public void testRemoveNode4() throws Exception {
        PathQuery query = new PathQuery(Model.getInstanceByName("testmodel"));
        PathNode employeeNode = query.addNode("Employee");
        employeeNode.setType("Employee");
        query.addNode("Employee.department");
        query.addNode("Employee.age");
        PathNode managerNode = query.addNode("Employee.department.manager");
        managerNode.setType("CEO");
        query.getView().add("Employee");
        query.getView().add("Employee.end");
        query.getView().add("Employee.age");
        query.getView().add("Employee.department");
        query.getView().add("Employee.department.manager");
        query.getView().add("Employee.department.manager.seniority");
        query.getView().add("Employee.department.manager.company.address.address");
        query.getView().add("Employee.department.manager.company.name");
        query.getView().add("Employee.department.manager.company.address");
        MainChange.removeNode(query, "Employee.department.manager.company");

        List expectedView = Arrays.asList(new Object[] {
                                              "Employee",
                                              "Employee.end",
                                              "Employee.age",
                                              "Employee.department",
                                              "Employee.department.manager",
                                              "Employee.department.manager.seniority",
                                              "Employee.department.manager.company.address.address",
                                              "Employee.department.manager.company.name",
                                              "Employee.department.manager.company.address"
                                          });

        assertEquals(expectedView, query.getView());
    }
}
