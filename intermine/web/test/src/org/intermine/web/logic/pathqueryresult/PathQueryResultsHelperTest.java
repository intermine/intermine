package org.intermine.web.logic.pathqueryresult;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.api.profile.InterMineBag;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.Type;
import org.intermine.web.logic.config.WebConfig;

/**
 * 
 * @author "Xavier Watkins"
 */
public class PathQueryResultsHelperTest extends TestCase
{
    private WebConfig webConfig;
    ObjectStoreWriter uosw;
    ObjectStore os;

    public PathQueryResultsHelperTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        super.setUp();
        webConfig = new WebConfig();
        uosw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");
        os = ObjectStoreFactory.getObjectStore("os.unittest");
        
        Type type  = new Type();
        type.setClassName("org.intermine.model.testmodel.Employee");
        FieldConfig df1 = new FieldConfig();
        df1.setFieldExpr("name");
        type.addFieldConfig(df1);
        FieldConfig df2 = new FieldConfig();
        df2.setFieldExpr("department.name");
        type.addFieldConfig(df2);
        FieldConfig df3 = new FieldConfig();
        df3.setFieldExpr("department.company.name");
        type.addFieldConfig(df3);
        FieldConfig df4 = new FieldConfig();
        df4.setFieldExpr("age");
        type.addFieldConfig(df4);
        FieldConfig df5 = new FieldConfig();
        df5.setFieldExpr("fullTime");
        type.addFieldConfig(df5);
        
        webConfig.addType(type);
    }

    public void testGetDefaultView() throws Exception {
        List<String> view = PathQueryResultHelper.getDefaultViewForClass("Employee", os.getModel(), webConfig, null);
        assertTrue(view.size() == 3);
    }

    public void testGetDefaultViewNoConfig() throws Exception {
        List<String> view = PathQueryResultHelper.getDefaultViewForClass("Address", os.getModel(), webConfig, null);
        assertTrue(view.size() == 1);
    }
    
    public void testMakePathQueryForBag() throws Exception {
        InterMineBag imBag = new InterMineBag("Fred", "Employee", "Test bag", new Date(), os, null, uosw);
        PathQuery pathQuery = PathQueryResultHelper.makePathQueryForBag(imBag, webConfig, os.getModel());
        String expectedXml = "<query name=\"query\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.department.company.name Employee.age Employee.fullTime\">"
        + "<constraint path=\"Employee\" op=\"IN\" value=\"Fred\"/>"
        + "</query>";
        assertEquals(expectedXml, pathQuery.toXml(PathQuery.USERPROFILE_VERSION));
    }

    public void testMakePathQueryForCollection() throws Exception {
        Department d1 = new Department();
        d1.setId(1);
        Set<Employee> employees = new HashSet<Employee>();
        Employee e1 = new Employee();
        e1.setId(2);
        employees.add(e1);
        d1.setEmployees(employees);
        List<Class<?>> sr = new ArrayList<Class<?>>();
        sr.add(Employee.class);
        PathQuery pathQuery = PathQueryResultHelper.makePathQueryForCollection(webConfig, os, (InterMineObject) d1, "Employee", "employees");
        String expectedXml = "<query name=\"query\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.department.company.name Employee.age Employee.fullTime\">"
            + "<join path=\"Employee.department\" style=\"OUTER\"/>"
            + "<join path=\"Employee.department.company\" style=\"OUTER\"/>"
            + "<constraint path=\"Department.id\" op=\"=\" value=\"1\"/>"
            + "</query>";
        assertEquals(expectedXml, pathQuery.toXml(PathQuery.USERPROFILE_VERSION));
    }
}
