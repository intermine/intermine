package org.intermine.web.struts;

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
import org.intermine.path.Path;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.query.PathNode;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.struts.QueryBuilderChange;

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
        Model model = Model.getInstanceByName("testmodel");
        PathQuery query = new PathQuery(model);
        PathNode employeeNode = query.addNode("Employee");
        employeeNode.setType("Employee");
        query.addNode("Employee.department");
        query.addNode("Employee.age");
        PathNode managerNode = query.addNode("Employee.department.manager");
        managerNode.setType("CEO");
        query.getView().add(MainHelper.makePath(model, query, "Employee"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.end"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.age"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager.seniority"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager.company.address.address"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager.company.name"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager.company.address"));
        QueryBuilderChange.removeNode(query, "Employee");

        assertEquals(0, query.getNodes().keySet().size());

        List expectedView = Arrays.asList(new Path[] {
            MainHelper.makePath(model, query, "Employee"),
            MainHelper.makePath(model, query, "Employee.end"),
            MainHelper.makePath(model, query, "Employee.age"),
            MainHelper.makePath(model, query, "Employee.department"),
            MainHelper.makePath(model, query, "Employee.department.manager"),
            MainHelper.makePath(model, query, "Employee.department.manager.seniority")
        });

        assertEquals(expectedView, query.getView());
    }

    public void testRemoveNode2() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery query = new PathQuery(model);
        PathNode employeeNode = query.addNode("Employee");
        employeeNode.setType("Employee");
        query.addNode("Employee.department");
        query.addNode("Employee.age");
        PathNode managerNode = query.addNode("Employee.department.manager");
        managerNode.setType("CEO");
        query.getView().add(MainHelper.makePath(model, query, "Employee"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.end"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.age"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager.seniority"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager.company.address.address"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager.company.name"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager.company.address"));
        QueryBuilderChange.removeNode(query, "Employee.department");

        List expectedNodes = Arrays.asList(new Object[] {
                                               "Employee",
                                               "Employee.age"
                                           });
        assertEquals(expectedNodes, new ArrayList(query.getNodes().keySet()));

        List expectedView = Arrays.asList(new Object[] {
            MainHelper.makePath(model, query, "Employee"),
            MainHelper.makePath(model, query, "Employee.end"),
            MainHelper.makePath(model, query, "Employee.age"),
            MainHelper.makePath(model, query, "Employee.department"),
            MainHelper.makePath(model, query, "Employee.department.manager"),
            MainHelper.makePath(model, query, "Employee.department.manager.seniority")
        });

        assertEquals(expectedView, query.getView());
    }

    public void testRemoveNode3() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery query = new PathQuery(model);
        PathNode employeeNode = query.addNode("Employee");
        employeeNode.setType("Employee");
        query.addNode("Employee.department");
        query.addNode("Employee.age");
        PathNode managerNode = query.addNode("Employee.department.manager");
        managerNode.setType("CEO");
        query.getView().add(MainHelper.makePath(model, query, "Employee"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.end"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.age"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager.seniority"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager.company.address.address"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager.company.name"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager.company.address"));
        QueryBuilderChange.removeNode(query, "Employee.department.manager");

        List expectedView = Arrays.asList(new Object[] {
            MainHelper.makePath(model, query, "Employee"),
            MainHelper.makePath(model, query, "Employee.end"),
            MainHelper.makePath(model, query, "Employee.age"),
            MainHelper.makePath(model, query, "Employee.department"),
            MainHelper.makePath(model, query, "Employee.department.manager"),
            MainHelper.makePath(model, query, "Employee.department.manager.seniority")
        });

        assertEquals(expectedView, query.getView());
    }

    public void testRemoveNode4() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery query = new PathQuery(model);
        PathNode employeeNode = query.addNode("Employee");
        employeeNode.setType("Employee");
        query.addNode("Employee.department");
        query.addNode("Employee.age");
        PathNode managerNode = query.addNode("Employee.department.manager");
        managerNode.setType("CEO");
        query.getView().add(MainHelper.makePath(model, query, "Employee"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.end"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.age"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager.seniority"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager.company.address.address"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager.company.name"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager.company.address"));
        QueryBuilderChange.removeNode(query, "Employee.department.manager.company");

        List expectedView = Arrays.asList(new Object[] {
            MainHelper.makePath(model, query, "Employee"),
            MainHelper.makePath(model, query, "Employee.end"),
            MainHelper.makePath(model, query, "Employee.age"),
            MainHelper.makePath(model, query, "Employee.department"),
            MainHelper.makePath(model, query, "Employee.department.manager"),
            MainHelper.makePath(model, query, "Employee.department.manager.seniority"),
            MainHelper.makePath(model, query, "Employee.department.manager.company.address.address"),
            MainHelper.makePath(model, query, "Employee.department.manager.company.name"),
            MainHelper.makePath(model, query, "Employee.department.manager.company.address")
        });

        assertEquals(expectedView, query.getView());
    }
}
