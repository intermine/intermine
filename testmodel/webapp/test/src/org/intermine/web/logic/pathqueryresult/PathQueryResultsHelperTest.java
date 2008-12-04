package org.intermine.web.logic.pathqueryresult;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.ClassKeyHelper;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.Type;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.results.WebResults;
import org.intermine.web.logic.search.SearchRepository;

import servletunit.ServletContextSimulator;
import servletunit.struts.MockStrutsTestCase;

/**
 * 
 * @author "Xavier Watkins"
 */
public class PathQueryResultsHelperTest extends MockStrutsTestCase
{
    private Map<String,List<FieldDescriptor>> classKeys;
    private WebConfig webConfig;
    
    public PathQueryResultsHelperTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        super.setUp();
        Properties classKeyProps = new Properties();
        classKeyProps.load(getClass().getClassLoader()
                           .getResourceAsStream("class_keys.properties"));
        Model model = Model.getInstanceByName("testmodel");
        classKeys = ClassKeyHelper.readKeys(model, classKeyProps);
        webConfig = new WebConfig();

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

    public void testGetDefaultview() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        getSession().getServletContext().setAttribute(Constants.WEBCONFIG, webConfig);
        PathQuery pathQuery = new PathQuery(model);
        List<Path> view = PathQueryResultHelper.getDefaultView("Employee", model, webConfig, null, false);
        assertTrue(view.size() == 5);

        PathQuery pathQuery2 = new PathQuery(model);
        List<Path> view2 = PathQueryResultHelper.getDefaultView("Employee", model, webConfig, null, true);
        assertTrue(view2.size() == 3);
    }

    public void testRunPathQueryGetResults() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        BagQueryConfig bagQueryConfig = null;
        ServletContext servletContext = new ServletContextSimulator();
        ObjectStoreWriter userProfileOSW =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");
        ProfileManager profileManager = new ProfileManager(os, userProfileOSW);
        int userId = 0;
        Profile profile = new Profile(profileManager, "modifyBagActionTest", userId, "pass",
                                      new HashMap(), new HashMap(), new HashMap());
        servletContext.setAttribute(Constants.GLOBAL_SEARCH_REPOSITORY, new SearchRepository(null));
        
        PathQuery pathQuery = new PathQuery(model);
        pathQuery.getView().add(PathQuery.makePath(model, pathQuery, "Employee.age"));
        pathQuery.getView().add(PathQuery.makePath(model, pathQuery, "Employee.name"));
        WebResults webResults = PathQueryResultHelper.createPathQueryGetResults(pathQuery, profile, os, classKeys, bagQueryConfig, servletContext);
        assertEquals(webResults.size(), 1);
    }
    
    public void testMakePathQueryForBag() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        ServletContext context = getActionServlet().getServletContext();
        ObjectStore os = (ObjectStore) context.getAttribute(Constants.OBJECTSTORE);
        ObjectStoreWriter uosw = ((ProfileManager) context.getAttribute(Constants.PROFILE_MANAGER)).getUserProfileObjectStore();
        InterMineBag imBag = new InterMineBag("Fred", "Employee", "Test bag", new Date(), os, null, uosw);
        PathQuery pathQuery = PathQueryResultHelper.makePathQueryForBag(imBag, webConfig, model);
        String expectedXml = "<query name=\"\" model=\"testmodel\" view=\"Employee.name Employee.age Employee.fullTime\" sortOrder=\"Employee.name asc\">"
        + "<node path=\"Employee\" type=\"Employee\">"
        + "<constraint op=\"IN\" value=\"Fred\" description=\"\" identifier=\"\" code=\"A\">"
        + "</constraint>"
        + "</node>"
        + "</query>";
        assertEquals(pathQuery.toXml(), expectedXml);
    }

    public void testMakePathQueryForCollection() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        ServletContext context = getActionServlet().getServletContext();
        ObjectStore os = (ObjectStore) context.getAttribute(Constants.OBJECTSTORE);
        Department d1 = new Department();
        d1.setId(1);
        Set<Employee> employees = new HashSet<Employee>();
        Employee e1 = new Employee();
        e1.setId(2);
        employees.add(e1);
        d1.setEmployees(employees);
        List<Class> sr = new ArrayList<Class>();
        sr.add(Employee.class);
        PathQuery pathQuery = PathQueryResultHelper.makePathQueryForCollectionForClass(webConfig, os, (InterMineObject)d1, "employees",sr);
        String expectedXml = "<query name=\"\" model=\"testmodel\" view=\"Department.employees.name Department.employees.department.name Department.employees.department.company.name Department.employees.age Department.employees.fullTime\" sortOrder=\"Department.employees.name asc\">" +
        		"<node path=\"Department\" type=\"Department\"></node><node path=\"Department.id\" type=\"Integer\">" +
        		"<constraint op=\"=\" value=\"1\" description=\"\" identifier=\"\" code=\"A\">" +
        		"</constraint>" +
        		"</node>" +
        		"</query>";
        assertEquals(pathQuery.toXml(), expectedXml);
    }
    
}
