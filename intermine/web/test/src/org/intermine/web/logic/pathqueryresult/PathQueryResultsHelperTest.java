package org.intermine.web.logic.pathqueryresult;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.intermine.api.profile.BagState;
import org.intermine.api.profile.InterMineBag;
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
import org.intermine.pathquery.PathQueryBinding;
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
    private Department department;
    private List<Class<?>> types;
    private Set<Employee> employees;
    private Employee employee;
    private static Map<String, PathQuery> queries = null;

    public PathQueryResultsHelperTest(String testName) {
        super(testName);
    }

    private String toXML(PathQuery pq) {
        return pq.toXml(PathQuery.USERPROFILE_VERSION);
    }

    protected void setUp() throws Exception {
        super.setUp();
        
        uosw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");
        os = ObjectStoreFactory.getObjectStore("os.unittest");

        initQueries();

        initWebConfig();

        initData();

    }

    private void initData() {
        employees = new HashSet<Employee>();
        types = new ArrayList<Class<?>>();
        Department d1 = new Department();
        d1.setId(1);
        Employee e1 = new Employee();
        e1.setId(2);
        employees.add(e1);
        Manager m1 = new Manager();
        m1.setId(3);
        employees.add(m1);
        d1.setEmployees(employees);
        types.add(Employee.class);
        types.add(Manager.class);

        department = d1;
        employee = e1;
    }

    private void initWebConfig() {
        webConfig = new WebConfig();
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

    private void initQueries() throws IOException {
        if (queries == null) { // Cache rather than re-read.
            InputStream is = getClass().getResourceAsStream("queries.xml");
            if (is == null) {
                throw new IOException("Could not find queries.xml");
            }
            Reader r = new InputStreamReader(is);
            queries = PathQueryBinding.unmarshalPathQueries(r, PathQuery.USERPROFILE_VERSION);
        }
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
        List<String> expectedView = Arrays.asList(
                "Department.employees.fullTime",
                "Department.employees.age",
                "Department.employees.end",
                "Department.employees.name"
        );
        assertEquals(expectedView, view);
    }


    // This test expects the references from the the configuration to be excluded.
    public void testMakePathQueryForBag() throws Exception {
        InterMineBag imBag = new InterMineBag("Fred", "Employee", "Test bag", new Date(), BagState.CURRENT, os, null, uosw, Arrays.asList("name"));
        PathQuery pathQuery = PathQueryResultHelper.makePathQueryForBag(imBag, webConfig, os.getModel());
        String expectedXml = toXML(queries.get("for-bag"));

        assertEquals(expectedXml, toXML(pathQuery));
    }

    // This test expects the references from the configuration to be included.
    public void testMakePathQueryForCollection() {
        PathQuery pathQuery = PathQueryResultHelper.makePathQueryForCollection(webConfig, os, department, "Employee", "employees");
        String expectedXml = toXML(queries.get("for-collection-Department.employees"));

        assertEquals("Pathquery not as expected", expectedXml, toXML(pathQuery));
    }

    public void testMakePathQueryForReference() {

        PathQuery pathQuery2 = PathQueryResultHelper.makePathQueryForCollection(webConfig, os, employee, "Address", "address");
        String expectedXml2 =  toXML(queries.get("for-reference-Employee.address"));
        assertEquals("Not as expected", expectedXml2, toXML(pathQuery2));
    }

    public void testMakePathQueryForCollectionSubtypes() {

        PathQuery pathQuery3 = PathQueryResultHelper.makePathQueryForCollection(webConfig, os, department, "Manager", "employees");
        String expectedXml3 = toXML(queries.get("for-collection-Department.managers"));
        assertEquals("Pathquery is not as expected", expectedXml3, toXML(pathQuery3));
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
        assertEquals(toXML(queries.get("default-view")), toXML(pq));
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
        List<Class<? extends Employee>> expectedClasses = Arrays.asList(Employee.class, Manager.class);
        assertEquals(expectedClasses, classes);
        osw.delete(d1);
        osw.delete(e1);
        osw.delete(m1);
    }
}
