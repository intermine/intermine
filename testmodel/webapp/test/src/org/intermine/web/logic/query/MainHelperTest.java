package org.intermine.web.logic.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.web.logic.ClassKeyHelper;
import org.intermine.web.struts.WebTestBase;

import servletunit.struts.MockStrutsTestCase;

/**
 * Tests for the MainHelper class
 *
 * @author Kim Rutherford
 */

public class MainHelperTest extends MockStrutsTestCase {
    private Map classKeys;
    
    public MainHelperTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        Properties classKeyProps = new Properties();
        classKeyProps.load(getClass().getClassLoader()
                           .getResourceAsStream("class_keys.properties"));
        Model model = Model.getInstanceByName("testmodel");
        classKeys = ClassKeyHelper.readKeys(model, classKeyProps);
    }
    public void testGetQualifiedTypeName() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        assertEquals("org.intermine.model.testmodel.Employee",
                     MainHelper.getQualifiedTypeName("Employee", model));
        assertEquals("java.lang.String",
                     MainHelper.getQualifiedTypeName("String", model));
        assertEquals("int",
                     MainHelper.getQualifiedTypeName("int", model));
        assertEquals("java.util.Date",
                     MainHelper.getQualifiedTypeName("Date", model));
        assertEquals("java.math.BigDecimal",
                     MainHelper.getQualifiedTypeName("BigDecimal", model));

        try {
            MainHelper.getQualifiedTypeName("SomeUnkownClass", model);
            fail("Expected ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            // expected
        }

        try {
            MainHelper.getQualifiedTypeName("java.lang.String", model);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testGetTypeForPath() throws Exception {
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
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager.seniority"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.department.manager.secretarys.name"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.address.address"));

        assertEquals("org.intermine.model.testmodel.Employee",
                     MainHelper.getTypeForPath("Employee", query));

        assertEquals("java.lang.String", MainHelper.getTypeForPath("Employee.end", query));

        assertEquals("int", MainHelper.getTypeForPath("Employee.age", query));

        assertEquals("org.intermine.model.testmodel.CEO",
                     MainHelper.getTypeForPath("Employee.department.manager", query));

        assertEquals("org.intermine.model.testmodel.Department",
                     MainHelper.getTypeForPath("Employee.department", query));

        assertEquals("java.lang.Integer",
                     MainHelper.getTypeForPath("Employee.department.manager.seniority", query));

        assertEquals("java.lang.String",
                     MainHelper.getTypeForPath("Employee.department.manager.secretarys.name",
                                               query));

        assertEquals("java.lang.String",
                     MainHelper.getTypeForPath("Employee.address.address", query));

        try {
            MainHelper.getTypeForPath("Employee.foobar", query);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            MainHelper.getTypeForPath("some.illegal.class", query);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            MainHelper.getTypeForPath("some_illegal_class", query);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            MainHelper.getTypeForPath(null, query);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            MainHelper.getTypeForPath("Employee.department", null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testMakeConstraintSets() {
        HashMap map = new HashMap();
        LogicExpression expr = new LogicExpression("a and b");
        ConstraintSet set = MainHelper.makeConstraintSets(expr, map, new ConstraintSet(ConstraintOp.AND));

        assertEquals(2, map.size());
        assertEquals(ConstraintOp.AND, set.getOp());

        HashMap expecting = new HashMap();
        expecting.put("a", set);
        expecting.put("b", set);
        assertEquals(expecting, map);

        expr = new LogicExpression("a and (b or c)");
        set = MainHelper.makeConstraintSets(expr, map, new ConstraintSet(ConstraintOp.AND));

        assertEquals(3, map.size());
        assertEquals(ConstraintOp.AND, set.getOp());
        assertEquals(1, set.getConstraints().size());
        assertEquals(ConstraintOp.OR, ((ConstraintSet) set.getConstraints().iterator().next()).getOp());

        expecting = new HashMap();
        expecting.put("a", set);
        expecting.put("b", (ConstraintSet) set.getConstraints().iterator().next());
        expecting.put("c", (ConstraintSet) set.getConstraints().iterator().next());
        assertEquals(expecting, map);

    }

    // Select Employee.name
    public void testMakeQueryOneField() throws Exception {
        Map queries = readQueries();
        PathQuery pq = (PathQuery) queries.get("employeeName");

        Query q = new Query();
        QueryClass qc1 = new QueryClass(Employee.class);
        q.addToSelect(qc1);
        q.addFrom(qc1);
        q.addToOrderBy(new QueryField(qc1, "name"));

        assertEquals(q.toString(), MainHelper.makeQuery(pq, new HashMap(), new HashMap(), null, null, false).toString());
    }

     // Select Employee.name, Employee.departments.name, Employee.departments.company.name
    // Constrain Employee.department.name = 'DepartmentA1'
    public void testMakeQueryThreeClasses() throws Exception {
        Map queries = readQueries();
        PathQuery pq = (PathQuery) queries.get("employeeDepartmentCompany");

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Employee.class);
        q.addToSelect(qc1);
        q.addFrom(qc1);
        QueryClass qc2 = new QueryClass(Department.class);
        q.addToSelect(qc2);
        q.addFrom(qc2);
        QueryField qf1 = new QueryField(qc2, "name");
        QueryExpression qFunc = new QueryExpression(QueryExpression.LOWER, (QueryField) qf1);
        SimpleConstraint sc1 = new SimpleConstraint(qFunc, ConstraintOp.EQUALS, new QueryValue("departmenta1"));
        QueryObjectReference qor1 = new QueryObjectReference(qc1, "department");
        ContainsConstraint cc1 = new ContainsConstraint(qor1, ConstraintOp.CONTAINS, qc2);
        cs.addConstraint(cc1);
        cs.addConstraint(sc1);
        QueryClass qc3 = new QueryClass(Company.class);
        q.addToSelect(qc3);
        q.addFrom(qc3);
        QueryObjectReference qor2 = new QueryObjectReference(qc2, "company");
        ContainsConstraint cc2 = new ContainsConstraint(qor2, ConstraintOp.CONTAINS, qc3);
        cs.addConstraint(cc2);
        q.setConstraint(cs);
        q.addToOrderBy(new QueryField(qc1, "name"));
        q.addToOrderBy(qf1);
        q.addToOrderBy(new QueryField(qc3, "name"));

        assertEquals(q.toString(), MainHelper.makeQuery(pq, new HashMap(), new HashMap(), null, null, false).toString());
    }

    // As above but add a wildcard in the constraint which makes a MATCHES constraint
    // Constrain Employee.department.name = 'DepartmentA*'
    public void testMakeQueryWildcard() throws Exception {
        Map queries = readQueries();
        PathQuery pq = (PathQuery) queries.get("employeeDepartmentCompanyWildcard");

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Employee.class);
        q.addToSelect(qc1);
        q.addFrom(qc1);
        QueryClass qc2 = new QueryClass(Department.class);
        q.addToSelect(qc2);
        q.addFrom(qc2);
        QueryField qf1 = new QueryField(qc2, "name");
        QueryExpression qFunc = new QueryExpression(QueryExpression.LOWER, (QueryField) qf1);
        SimpleConstraint sc1 = new SimpleConstraint(qFunc, ConstraintOp.MATCHES, 
                                                    new QueryValue("departmenta%"));
        QueryObjectReference qor1 = new QueryObjectReference(qc1, "department");
        ContainsConstraint cc1 = new ContainsConstraint(qor1, ConstraintOp.CONTAINS, qc2);
        cs.addConstraint(cc1);
        cs.addConstraint(sc1);
        QueryClass qc3 = new QueryClass(Company.class);
        q.addToSelect(qc3);
        q.addFrom(qc3);
        QueryObjectReference qor2 = new QueryObjectReference(qc2, "company");
        ContainsConstraint cc2 = new ContainsConstraint(qor2, ConstraintOp.CONTAINS, qc3);
        cs.addConstraint(cc2);
        q.setConstraint(cs);
        q.addToOrderBy(new QueryField(qc1, "name"));
        q.addToOrderBy(qf1);
        q.addToOrderBy(new QueryField(qc3, "name"));

        assertEquals(q.toString(), MainHelper.makeQuery(pq, new HashMap(), new HashMap(), null, null, false).toString());
    }



    // Select Employee.name, Employee.departments.company.name  (should not select Department)
    // Constrain Employee.department.name = 'DepartmentA1'
    public void testConstrainedButNotInView() throws Exception {
        Map queries = readQueries();
        PathQuery pq = (PathQuery) queries.get("employeeCompany");

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Employee.class);
        q.addToSelect(qc1);
        q.addFrom(qc1);
        QueryClass qc2 = new QueryClass(Department.class);
        q.addFrom(qc2);
        QueryField qf1 = new QueryField(qc2, "name");
        QueryExpression qFunc = new QueryExpression(QueryExpression.LOWER, (QueryField) qf1);
        SimpleConstraint sc1 = new SimpleConstraint(qFunc, ConstraintOp.EQUALS, new QueryValue("departmenta1"));
        QueryObjectReference qor1 = new QueryObjectReference(qc1, "department");
        ContainsConstraint cc1 = new ContainsConstraint(qor1, ConstraintOp.CONTAINS, qc2);
        cs.addConstraint(cc1);
        cs.addConstraint(sc1);
        QueryClass qc3 = new QueryClass(Company.class);
        q.addToSelect(qc3);
        q.addFrom(qc3);
        QueryObjectReference qor2 = new QueryObjectReference(qc2, "company");
        ContainsConstraint cc2 = new ContainsConstraint(qor2, ConstraintOp.CONTAINS, qc3);
        cs.addConstraint(cc2);
        q.setConstraint(cs);
        q.addToOrderBy(new QueryField(qc1, "name"));
        q.addToOrderBy(new QueryField(qc3, "name"));

        assertEquals(q.toString(), MainHelper.makeQuery(pq, new HashMap(), new HashMap(), null, null, false).toString());
    }

    private Map readQueries() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("MainHelperTest.xml");
        return PathQueryBinding.unmarshal(new InputStreamReader(is), new HashMap(), 
                                          getActionServlet().getServletContext());
    }
    public void test1() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee\"></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_ ORDER BY a1_");
    }

    public void test2() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.age\" type=\"int\"><constraint op=\"&gt;=\" value=\"10\" description=\"\" identifier=\"\" code=\"A\"></constraint></node></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE a1_.age >= 10 ORDER BY a1_");
    }

    public void test3() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee\" constraintLogic=\"A and B\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.age\" type=\"int\"><constraint op=\"&gt;=\" value=\"10\" description=\"\" identifier=\"\" code=\"A\"></constraint></node><node path=\"Employee.fullTime\" type=\"boolean\"><constraint op=\"=\" value=\"true\" description=\"\" identifier=\"\" code=\"B\"></constraint></node></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE (a1_.age >= 10 AND a1_.fullTime = true) ORDER BY a1_");
    }
    
    public void test4() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee\" constraintLogic=\"A or B\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.age\" type=\"int\"><constraint op=\"&gt;=\" value=\"10\" description=\"\" identifier=\"\" code=\"A\"></constraint></node><node path=\"Employee.fullTime\" type=\"boolean\"><constraint op=\"=\" value=\"true\" description=\"\" identifier=\"\" code=\"B\"></constraint></node></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE (a1_.age >= 10 OR a1_.fullTime = true) ORDER BY a1_");
    }

    public void test5() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee\" constraintLogic=\"(A or B) and C\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.age\" type=\"int\"><constraint op=\"&gt;=\" value=\"10\" description=\"\" identifier=\"\" code=\"A\"></constraint></node><node path=\"Employee.fullTime\" type=\"boolean\"><constraint op=\"=\" value=\"true\" description=\"\" identifier=\"\" code=\"B\"></constraint></node><node path=\"Employee.name\" type=\"String\"><constraint op=\"=\" value=\"EmployeeA2\" description=\"\" identifier=\"\" code=\"C\"></constraint></node></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE ((a1_.age >= 10 OR a1_.fullTime = true) AND LOWER(a1_.name) = 'employeea2') ORDER BY a1_");
    }

    public void test7() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee Employee.department\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.department\" type=\"Department\"></node><node path=\"Employee.department.employees\" type=\"Employee\"><constraint op=\"=\" value=\"Employee\"></constraint></node></query>",
                "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_ WHERE a1_.department CONTAINS a2_ ORDER BY a1_, a2_");
    }

    public void test8() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Company.name Company.contractors.name\"><node path=\"Company\" type=\"Company\"></node><node path=\"Company.contractors\" type=\"Contractor\"></node><node path=\"Company.oldContracts\" type=\"Contractor\"><constraint op=\"=\" value=\"Company.contractors\" description=\"\" identifier=\"\" code=\"A\"></constraint></node></query>",
                "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Contractor AS a2_ WHERE (a1_.contractors CONTAINS a2_ AND a1_.oldContracts CONTAINS a2_) ORDER BY a1_.name, a2_.name");
    }

    public void test9() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.department.company.name Employee.age Employee.fullTime Employee.address.address\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.department\" type=\"Department\"></node><node path=\"Employee.department.company\" type=\"Company\"></node><node path=\"Employee.department.company.address\" type=\"Address\"><constraint op=\"=\" value=\"Employee.address\" description=\"\" identifier=\"\" code=\"A\"></constraint></node><node path=\"Employee.address\" type=\"Address\"></node></query>",
                "SELECT DISTINCT a1_, a2_, a3_, a4_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_) ORDER BY a1_.name, a2_.name, a3_.name, a1_.age, a1_.fullTime, a4_.address");
    }

    public void test10() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.department.company.name Employee.age Employee.fullTime Employee.address.address\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.department\" type=\"Department\"></node><node path=\"Employee.department.company\" type=\"Company\"></node><node path=\"Employee.department.company.address\" type=\"Address\"><constraint op=\"!=\" value=\"Employee.address\" description=\"\" identifier=\"\" code=\"A\"></constraint></node><node path=\"Employee.address\" type=\"Address\"></node></query>",
                "SELECT DISTINCT a1_, a2_, a3_, a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_ AND a4_ != a5_) ORDER BY a1_.name, a2_.name, a3_.name, a1_.age, a1_.fullTime, a5_.address");
    }

    public void test11() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.department.company.name Employee.age Employee.fullTime Employee.department.company.address.address\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.department\" type=\"Department\"></node><node path=\"Employee.department.company\" type=\"Company\"></node><node path=\"Employee.department.company.address\" type=\"Address\"><constraint op=\"=\" value=\"Employee.address\" description=\"\" identifier=\"\" code=\"A\"></constraint></node><node path=\"Employee.address\" type=\"Address\"></node></query>",
                "SELECT DISTINCT a1_, a2_, a3_, a4_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_) ORDER BY a1_.name, a2_.name, a3_.name, a1_.age, a1_.fullTime, a4_.address");
    }

    public void test12() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.department.company.name Employee.age Employee.fullTime Employee.department.company.address.address\" constraintLogic=\"A or B\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.name\"><constraint op=\"=\" value=\"EmployeeA1\" code=\"A\"></constraint></node><node path=\"Employee.department\" type=\"Department\"></node><node path=\"Employee.department.company\" type=\"Company\"></node><node path=\"Employee.department.company.address\" type=\"Address\"><constraint op=\"=\" value=\"Employee.address\" description=\"\" identifier=\"\" code=\"B\"></constraint></node><node path=\"Employee.address\" type=\"Address\"></node></query>",
                "SELECT DISTINCT a1_, a2_, a3_, a4_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE ((LOWER(a1_.name) = 'employeea1' OR a4_ = a5_) AND a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_) ORDER BY a1_.name, a2_.name, a3_.name, a1_.age, a1_.fullTime, a4_.address");
    }

    public void test13() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.department.company.name Employee.age Employee.fullTime Employee.department.company.address.address\" constraintLogic=\"A and B and C\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.name\"><constraint op=\"=\" value=\"EmployeeA1\" code=\"A\"></constraint></node><node path=\"Employee.department\" type=\"Department\"></node><node path=\"Employee.department.company\" type=\"Company\"></node><node path=\"Employee.department.company.address\" type=\"Address\"><constraint op=\"=\" value=\"Employee.address\" description=\"\" identifier=\"\" code=\"B\"></constraint></node><node path=\"Employee.address\" type=\"Address\"></node><node path=\"Employee.address.address\"><constraint op=\"!=\" value=\"fred\" code=\"C\"></constraint></node></query>",
                "SELECT DISTINCT a1_, a2_, a3_, a4_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (LOWER(a1_.name) = 'employeea1' AND a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND LOWER(a4_.address) != 'fred' AND a3_.address CONTAINS a4_) ORDER BY a1_.name, a2_.name, a3_.name, a1_.age, a1_.fullTime, a4_.address");
    }

    public void test14() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.employees.name\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.department\" type=\"Department\"></node><node path=\"Employee.department.employees\" type=\"Employee\"><constraint op=\"!=\" value=\"Employee\" description=\"\" identifier=\"\" code=\"A\"></constraint></node></query>",
                "SELECT DISTINCT a1_, a3_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Employee AS a3_ WHERE (a1_.department CONTAINS a2_ AND a2_.employees CONTAINS a3_ AND a3_ != a1_) ORDER BY a1_.name, a3_.name");
    }

    public void doQuery(String web, String iql) throws Exception {
        Map parsed = PathQueryBinding.unmarshal(new StringReader(web), new HashMap(),
                                                getActionServlet().getServletContext());
        PathQuery pq = (PathQuery) parsed.get("test");
        Query q = MainHelper.makeQuery(pq, Collections.EMPTY_MAP, null, null);
        String got = q.toString();
        assertEquals(iql, got);
    }
}
