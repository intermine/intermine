package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ConstraintOp;

/**
 * Tests for the SavedQueryParser class
 *
 * @author Kim Rutherford
 */
public class SavedQueryParserTest extends TestCase
{
    public SavedQueryParserTest(String arg) {
        super(arg);
    }

    public void testProcess() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("test/SavedQueryParserTest.xml");
        Map savedQueries = new SavedQueryParser().process(new InputStreamReader(is));
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
        age.getConstraints().add(new Constraint(ConstraintOp.GREATER_THAN, new Integer(10)));
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
    }
}
