package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2011 FlyMine
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

import junit.framework.TestCase;

import org.intermine.metadata.Model;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.OldPathQuery;

/**
 * Tests for the MainChange class
 *
 * @author Kim Rutherford
 */

public class QueryBuilderChangeTest  extends TestCase
{
    public QueryBuilderChangeTest(String arg) {
        super(arg);
    }

    public void testRemoveNode1() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        OldPathQuery query = new OldPathQuery(model);
        PathNode employeeNode = query.addNode("Employee");
        employeeNode.setType("Employee");
        query.addNode("Employee.department");
        query.addNode("Employee.age");
        PathNode managerNode = query.addNode("Employee.department.manager");
        managerNode.setType("CEO");
        query.getView().add(OldPathQuery.makePath(model, query, "Employee"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.end"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.age"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department.manager"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department.manager.seniority"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department.manager.company.address.address"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department.manager.company.name"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department.manager.company.address"));
        QueryBuilderChange.removeNode(query, "Employee");

        assertEquals(0, query.getNodes().keySet().size());

        List expectedView = Arrays.asList(new Path[] {
            OldPathQuery.makePath(model, query, "Employee"),
            OldPathQuery.makePath(model, query, "Employee.end"),
            OldPathQuery.makePath(model, query, "Employee.age"),
            OldPathQuery.makePath(model, query, "Employee.department"),
            OldPathQuery.makePath(model, query, "Employee.department.manager"),
            OldPathQuery.makePath(model, query, "Employee.department.manager.seniority")
        });

        assertEquals(expectedView, query.getView());
    }

    public void testRemoveNode2() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        OldPathQuery query = new OldPathQuery(model);
        PathNode employeeNode = query.addNode("Employee");
        employeeNode.setType("Employee");
        query.addNode("Employee.department");
        query.addNode("Employee.age");
        PathNode managerNode = query.addNode("Employee.department.manager");
        managerNode.setType("CEO");
        query.getView().add(OldPathQuery.makePath(model, query, "Employee"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.end"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.age"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department.manager"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department.manager.seniority"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department.manager.company.address.address"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department.manager.company.name"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department.manager.company.address"));
        QueryBuilderChange.removeNode(query, "Employee.department");

        List expectedNodes = Arrays.asList(new Object[] {
                                               "Employee",
                                               "Employee.age"
                                           });
        assertEquals(expectedNodes, new ArrayList(query.getNodes().keySet()));

        List expectedView = Arrays.asList(new Object[] {
            OldPathQuery.makePath(model, query, "Employee"),
            OldPathQuery.makePath(model, query, "Employee.end"),
            OldPathQuery.makePath(model, query, "Employee.age"),
            OldPathQuery.makePath(model, query, "Employee.department"),
            OldPathQuery.makePath(model, query, "Employee.department.manager"),
            OldPathQuery.makePath(model, query, "Employee.department.manager.seniority")
        });

        assertEquals(expectedView, query.getView());
    }

    public void testRemoveNode3() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        OldPathQuery query = new OldPathQuery(model);
        PathNode employeeNode = query.addNode("Employee");
        employeeNode.setType("Employee");
        query.addNode("Employee.department");
        query.addNode("Employee.age");
        PathNode managerNode = query.addNode("Employee.department.manager");
        managerNode.setType("CEO");
        query.getView().add(OldPathQuery.makePath(model, query, "Employee"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.end"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.age"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department.manager"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department.manager.seniority"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department.manager.company.address.address"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department.manager.company.name"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department.manager.company.address"));
        QueryBuilderChange.removeNode(query, "Employee.department.manager");

        List expectedView = Arrays.asList(new Object[] {
            OldPathQuery.makePath(model, query, "Employee"),
            OldPathQuery.makePath(model, query, "Employee.end"),
            OldPathQuery.makePath(model, query, "Employee.age"),
            OldPathQuery.makePath(model, query, "Employee.department"),
            OldPathQuery.makePath(model, query, "Employee.department.manager"),
            OldPathQuery.makePath(model, query, "Employee.department.manager.seniority")
        });

        assertEquals(expectedView, query.getView());
    }

    public void testRemoveNode4() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        OldPathQuery query = new OldPathQuery(model);
        PathNode employeeNode = query.addNode("Employee");
        employeeNode.setType("Employee");
        query.addNode("Employee.department");
        query.addNode("Employee.age");
        PathNode managerNode = query.addNode("Employee.department.manager");
        managerNode.setType("CEO");
        query.getView().add(OldPathQuery.makePath(model, query, "Employee"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.end"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.age"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department.manager"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department.manager.seniority"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department.manager.company.address.address"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department.manager.company.name"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.department.manager.company.address"));
        QueryBuilderChange.removeNode(query, "Employee.department.manager.company");

        List expectedView = Arrays.asList(new Object[] {
            OldPathQuery.makePath(model, query, "Employee"),
            OldPathQuery.makePath(model, query, "Employee.end"),
            OldPathQuery.makePath(model, query, "Employee.age"),
            OldPathQuery.makePath(model, query, "Employee.department"),
            OldPathQuery.makePath(model, query, "Employee.department.manager"),
            OldPathQuery.makePath(model, query, "Employee.department.manager.seniority"),
            OldPathQuery.makePath(model, query, "Employee.department.manager.company.address.address"),
            OldPathQuery.makePath(model, query, "Employee.department.manager.company.name"),
            OldPathQuery.makePath(model, query, "Employee.department.manager.company.address")
        });

        assertEquals(expectedView, query.getView());
    }
}
