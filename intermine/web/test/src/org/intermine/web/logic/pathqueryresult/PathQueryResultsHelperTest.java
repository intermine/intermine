package org.intermine.web.logic.pathqueryresult;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.BagState;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Manager;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
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
    private WebConfig badWebConfig;
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
        // This should be ignored
        df2.setFieldExpr("department.name");
        type.addFieldConfig(df2);
        FieldConfig df3 = new FieldConfig();
        // This should be ignored
        df3.setFieldExpr("department.company.name");
        type.addFieldConfig(df3);
        FieldConfig df4 = new FieldConfig();
        df4.setFieldExpr("age");
        type.addFieldConfig(df4);
        FieldConfig df5 = new FieldConfig();
        df5.setFieldExpr("fullTime");
        type.addFieldConfig(df5);

        webConfig.addType(type);

        badWebConfig = new WebConfig();
        Type badType = new Type();
        badType.setClassName("org.intermine.model.testmodel.Employee");
        FieldConfig badFC = new FieldConfig();
        badFC.setFieldExpr("wibble");
        badType.addFieldConfig(badFC);
        badWebConfig.addType(badType);

    }

    protected void tearDown() throws Exception{
        uosw.close();
    }

    public void testGetDefaultView() throws Exception {
        List<String> view = PathQueryResultHelper.getDefaultViewForClass("Employee", os.getModel(), webConfig, null);
        assertTrue(view.size() == 3);
    }

    public void testGetDefaultViewNoConfig() throws Exception {
        List<String> view = PathQueryResultHelper.getDefaultViewForClass("Address", os.getModel(), webConfig, null);
        assertTrue(view.size() == 1);
    }

    public void testGetDefaultViewSubClass() {
        try {
            List<String> view = PathQueryResultHelper.getDefaultViewForClass(
                    "Manager", os.getModel(), webConfig, "Department.employees");
            fail("No exception thrown when getting default view for subclass, got: " + view);
        } catch (AssertionFailedError e) {
            throw e;
        } catch (IllegalArgumentException e) {
            assertEquals("Mismatch between end type of prefix: Employee and type parameter: Manager",
                    e.getMessage());
        } catch (Throwable t) {
            fail("Unexpected error when getting default view for subclass" + t.getMessage());
        }
    }

    public void testGetDefaultViewBadConfig() {

        List<String> view = PathQueryResultHelper.getDefaultViewForClass(
                "Employee", os.getModel(), badWebConfig, "Department.employees");
        List<String> expectedView = new ArrayList<String>(Arrays.asList(
                "Department.employees.fullTime",
                "Department.employees.age",
                "Department.employees.end",
                "Department.employees.name"
        ));
        assertEquals(expectedView, view);
    }


    // This test expects the references from the the configuration to be excluded.
    public void testMakePathQueryForBag() throws Exception {
        InterMineBag imBag = new InterMineBag("Fred", "Employee", "Test bag", new Date(), BagState.CURRENT, os, null, uosw);
        PathQuery pathQuery = PathQueryResultHelper.makePathQueryForBag(imBag, webConfig, os.getModel());
        String expectedXml = "<query name=\"query\" model=\"testmodel\" view=\"Employee.name Employee.age Employee.fullTime\">"
            + "<constraint path=\"Employee\" op=\"IN\" value=\"Fred\"/>"
            + "</query>";
        assertEquals(expectedXml, pathQuery.toXml(PathQuery.USERPROFILE_VERSION));
    }

    // This test expects the references from the configuration to be included.
    public void testMakePathQueryForCollection() throws Exception {
        Department d1 = new Department();
        d1.setId(1);
        Set<Employee> employees = new HashSet<Employee>();
        Employee e1 = new Employee();
        e1.setId(2);
        employees.add(e1);
        Manager m1 = new Manager();
        m1.setId(3);
        employees.add(m1);
        d1.setEmployees(employees);
        List<Class<?>> sr = new ArrayList<Class<?>>();
        sr.add(Employee.class);
        sr.add(Manager.class);
        PathQuery pathQuery = PathQueryResultHelper.makePathQueryForCollection(webConfig, os, (InterMineObject) d1, "Employee", "employees");
        String expectedXml = "<query name=\"query\" model=\"testmodel\" view=\"Department.employees.name Department.employees.department.name Department.employees.department.company.name Department.employees.age Department.employees.fullTime\">"
            + "<join path=\"Department.employees.department\" style=\"OUTER\"/>"
            + "<join path=\"Department.employees.department.company\" style=\"OUTER\"/>"
            + "<constraint path=\"Department.id\" op=\"=\" value=\"1\"/>"
            + "</query>";
        assertEquals(expectedXml, pathQuery.toXml(PathQuery.USERPROFILE_VERSION));
        PathQuery pathQuery2 = PathQueryResultHelper.makePathQueryForCollection(webConfig, os, (InterMineObject) e1, "Address", "address");
        String expectedXml2 =  "<query name=\"query\" model=\"testmodel\" "
            + "view=\"Employee.address.address\"><constraint path=\"Employee.id\" "
            + "op=\"=\" value=\"2\"/></query>";
        assertEquals(expectedXml2, pathQuery2.toXml(PathQuery.USERPROFILE_VERSION));
        PathQuery pathQuery3 = PathQueryResultHelper.makePathQueryForCollection(webConfig, os, (InterMineObject) d1, "Manager", "employees");
        String expectedXml3 = "<query name=\"query\" model=\"testmodel\" view=\"Department.employees.title "
            + "Department.employees.fullTime Department.employees.age Department.employees.end "
            + "Department.employees.name Department.employees.seniority\">"
            + "<constraint path=\"Department.employees\" type=\"Manager\"/>"
            + "<constraint path=\"Department.id\" op=\"=\" value=\"1\"/>"
            + "</query>";
        assertEquals(expectedXml3, pathQuery3.toXml(PathQuery.USERPROFILE_VERSION));
    }


    public void testMakePathQueryForCollectionFailure() {
        Department d1 = new Department();
        d1.setId(1);
        try {
            PathQuery pathQuery = PathQueryResultHelper.makePathQueryForCollection(
                    webConfig, os, (InterMineObject) d1, "Employee", "pencilPushers");
            fail("No exception thrown when passing bad arguments to makePathQueryForCollection, got: " + pathQuery);
        } catch (AssertionFailedError e) {
            throw e;
        } catch (IllegalArgumentException e) {
            assertEquals("Could not build path for \"Department.pencilPushers\".",
                    e.getMessage());
        } catch (Throwable t) {
            fail("Unexpected error when getting default view for subclass" + t.getMessage());
        }
    }

    public void testGetQueryWithDefaultView() {
        String objType = "Manager";
        String fieldType = "Department.employees";
        Model model = Model.getInstanceByName("testmodel");
        PathQuery pq = PathQueryResultHelper.getQueryWithDefaultView(objType, model, webConfig, fieldType);
        assertEquals(
                "<query name=\"query\" model=\"testmodel\" view=\"Department.employees.title " +
                "Department.employees.fullTime Department.employees.age Department.employees.end " +
                "Department.employees.name Department.employees.seniority\"><constraint " +
                "path=\"Department.employees\" type=\"Manager\"/></query>",
                pq.toXml(PathQuery.USERPROFILE_VERSION));
    }

    public void testQueryForTypesInCollection() throws ObjectStoreException {
        String field = "employees";
        Department d1 = (Department) DynamicUtil.createObject(Department.class);
        d1.setId(1);
        Employee e1 = (Employee) DynamicUtil.createObject(Employee.class);
        e1.setId(2);
        e1.setDepartment(d1);
        Manager m1 = DynamicUtil.createObject(Manager.class);
        m1.setId(3);
        m1.setDepartment(d1);
        ObjectStoreWriter osw = os.getNewWriter();
        osw.store(e1);
        osw.store(m1);
        osw.store(d1);
        List<Class<?>> classes = PathQueryResultHelper.queryForTypesInCollection(d1, field, os);
        List<Class<?>> expectedClasses = new ArrayList<Class<?>>(
                Arrays.asList(Employee.class, Manager.class));
        assertEquals(expectedClasses, classes);
        osw.delete(d1);
        osw.delete(e1);
        osw.delete(m1);
    }
}
