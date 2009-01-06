package org.intermine.web.logic.query;

/*
 * Copyright (C) 2002-2009 FlyMine
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.TestUtil;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryEvaluable;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.LogicExpression;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.BagQueryHelper;
import org.intermine.web.logic.bag.BagQueryRunner;

/**
 * Tests for the MainHelper class
 *
 * @author Kim Rutherford
 */

public class MainHelperTest extends TestCase {
    private BagQueryConfig bagQueryConfig;
    private ObjectStore os;
    private Map<String, List<FieldDescriptor>> classKeys;
    private BagQueryRunner bagQueryRunner;
    
    public MainHelperTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        os = ObjectStoreFactory.getObjectStore("os.unittest");
        classKeys = TestUtil.getClassKeys(TestUtil.getModel());
        InputStream config = MainHelperTest.class.getClassLoader()
            .getResourceAsStream("bag-queries.xml");
        bagQueryConfig = BagQueryHelper.readBagQueryConfig(os.getModel(), config);
        bagQueryRunner = new BagQueryRunner(os, classKeys, bagQueryConfig, Collections.EMPTY_LIST);
    }

    // Method converts path to default join styles: outer joins for collections, normal join otherwise
    public void testDefaultJoinStyles() {
        Model model = TestUtil.getModel();
        assertEquals("Company:departments", MainHelper.toPathDefaultJoinStyle(model, "Company.departments"));
        assertEquals("Company:departments.manager", MainHelper.toPathDefaultJoinStyle(model, "Company.departments.manager"));
        assertEquals("Company:departments.manager", MainHelper.toPathDefaultJoinStyle(model, "Company.departments:manager"));
        
        assertEquals("CEO.company:departments:employees.name", MainHelper.toPathDefaultJoinStyle(model, "CEO.company:departments:employees.name"));
        assertEquals("Company.name", MainHelper.toPathDefaultJoinStyle(model, "Company.name"));
    }
    
    // Method gets the last index of a '.' or a ':'
    public void testGetLastJoinIndex() {
        assertEquals(3, MainHelper.getLastJoinIndex("CEO.company"));
        assertEquals(3, MainHelper.getLastJoinIndex("CEO:company"));
        assertEquals(11, MainHelper.getLastJoinIndex("CEO.company:department"));
        assertEquals(11, MainHelper.getLastJoinIndex("CEO:company.department"));
        assertEquals(-1, MainHelper.getLastJoinIndex("CEO"));
    }
    
    // Method gets the first index of a '.' or a ':'
    public void testGetFirstJoinIndex() {
        assertEquals(3, MainHelper.getFirstJoinIndex("CEO.company"));
        assertEquals(3, MainHelper.getFirstJoinIndex("CEO:company"));
        assertEquals(3, MainHelper.getFirstJoinIndex("CEO.company:department"));
        assertEquals(3, MainHelper.getFirstJoinIndex("CEO:company.department"));
        assertEquals(-1, MainHelper.getFirstJoinIndex("CEO"));
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
        List<Path> paths = new LinkedList<Path> ();

        paths.add(PathQuery.makePath(model, query, "Employee"));
        paths.add(PathQuery.makePath(model, query, "Employee.end"));
        paths.add(PathQuery.makePath(model, query, "Employee.age"));
        paths.add(PathQuery.makePath(model, query, "Employee.department.manager"));
        paths.add(PathQuery.makePath(model, query, "Employee.department.manager.seniority"));
        paths.add(PathQuery.makePath(model, query, "Employee.department.manager.secretarys.name"));
        paths.add(PathQuery.makePath(model, query, "Employee.address.address"));

        query.addViewPaths(paths);

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
        
        assertEquals(q.toString(), MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap(), false).toString());
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
        SimpleConstraint sc1 = new SimpleConstraint(qFunc, ConstraintOp.MATCHES, new QueryValue("departmenta1"));
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

        assertEquals(q.toString(), MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap(), false).toString());
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

        assertEquals(q.toString(), MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap(), false).toString());
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
        SimpleConstraint sc1 = new SimpleConstraint(qFunc, ConstraintOp.MATCHES, new QueryValue("departmenta1"));
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

        assertEquals(q.toString(), MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap(), false).toString());
    }

    public void testMakeQueryDateConstraint() throws Exception {
        // 11:02:39am Sun Nov 16, 2008
        QueryNode qn = new QueryValue(new Date(1226833359000L));

        // startOfDate < queryDate = 12:20:34am Mon Nov 17, 2008 < endOfDay
        Date queryDate = new Date(1226881234565L);
        Date startOfDay = new Date(1226880000000L);
        Date endOfDay = new Date(1226966399999L);
        SimpleConstraint expLTConstraint =
                new SimpleConstraint((QueryEvaluable) qn, ConstraintOp.LESS_THAN,
                                     new QueryValue(startOfDay));
        Constraint ltConstraint = new Constraint(ConstraintOp.LESS_THAN, queryDate);
        org.intermine.objectstore.query.Constraint resLTConstraint =
            MainHelper.makeQueryDateConstraint(qn, ltConstraint);

        assertEquals(expLTConstraint, resLTConstraint);

        SimpleConstraint expLTEConstraint =
            new SimpleConstraint((QueryEvaluable) qn, ConstraintOp.LESS_THAN_EQUALS,
                                 new QueryValue(endOfDay));
        Constraint lteConstraint = new Constraint(ConstraintOp.LESS_THAN_EQUALS, queryDate);
        org.intermine.objectstore.query.Constraint resLTEConstraint =
            MainHelper.makeQueryDateConstraint(qn, lteConstraint);

        assertEquals(expLTEConstraint, resLTEConstraint);

        SimpleConstraint expGTConstraint =
            new SimpleConstraint((QueryEvaluable) qn, ConstraintOp.LESS_THAN,
                                 new QueryValue(startOfDay));
        Constraint gtConstraint = new Constraint(ConstraintOp.LESS_THAN, queryDate);
        org.intermine.objectstore.query.Constraint resGTConstraint =
            MainHelper.makeQueryDateConstraint(qn, gtConstraint);

        assertEquals(expGTConstraint, resGTConstraint);

        SimpleConstraint expGTEConstraint =
            new SimpleConstraint((QueryEvaluable) qn, ConstraintOp.LESS_THAN_EQUALS,
                                 new QueryValue(endOfDay));
        Constraint gteConstraint = new Constraint(ConstraintOp.LESS_THAN_EQUALS, queryDate);
        org.intermine.objectstore.query.Constraint resGTEConstraint =
            MainHelper.makeQueryDateConstraint(qn, gteConstraint);

        assertEquals(expGTEConstraint, resGTEConstraint);

        ConstraintSet expEQConstraint =
            new ConstraintSet(ConstraintOp.AND);
        SimpleConstraint expEQStartConstraint =
            new SimpleConstraint((QueryEvaluable) qn, ConstraintOp.GREATER_THAN_EQUALS,
                                 new QueryValue(startOfDay));
        SimpleConstraint expEQEndConstraint =
            new SimpleConstraint((QueryEvaluable) qn, ConstraintOp.LESS_THAN_EQUALS,
                                 new QueryValue(endOfDay));
        expEQConstraint.addConstraint(expEQStartConstraint);
        expEQConstraint.addConstraint(expEQEndConstraint);
        Constraint eqConstraint = new Constraint(ConstraintOp.EQUALS, queryDate);
        org.intermine.objectstore.query.Constraint resEQConstraint =
            MainHelper.makeQueryDateConstraint(qn, eqConstraint);

        assertEquals(expEQConstraint, resEQConstraint);

        ConstraintSet expNEQConstraint =
            new ConstraintSet(ConstraintOp.OR);
        SimpleConstraint expNEQStartConstraint =
            new SimpleConstraint((QueryEvaluable) qn, ConstraintOp.LESS_THAN,
                                 new QueryValue(startOfDay));
        SimpleConstraint expNEQEndConstraint =
            new SimpleConstraint((QueryEvaluable) qn, ConstraintOp.GREATER_THAN,
                                 new QueryValue(endOfDay));
        expNEQConstraint.addConstraint(expNEQStartConstraint);
        expNEQConstraint.addConstraint(expNEQEndConstraint);
        Constraint neqConstraint = new Constraint(ConstraintOp.NOT_EQUALS, queryDate);
        org.intermine.objectstore.query.Constraint resNEQConstraint =
            MainHelper.makeQueryDateConstraint(qn, neqConstraint);

        assertEquals(expNEQConstraint, resNEQConstraint);
    }

 
    // test that loop constraint queries are generated correctly
    public void testLoopConstraint() throws Exception {
        Map queries = readQueries();
        PathQuery pq = (PathQuery) queries.get("loopConstraint");
        Query q = MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap(), false);
        String got = q.toString();
        String iql = "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a2_ WHERE (a1_.departments CONTAINS a2_ AND a2_.company CONTAINS a1_) ORDER BY a1_.name";
        assertEquals("Expected: " + iql + ", got: " + got, iql, got);
    }
 
    
    private Map readQueries() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("MainHelperTest.xml");
        Map ret = PathQueryBinding.unmarshal(new InputStreamReader(is));
        return ret;
    }
    
    public void test1() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee\"></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_ ORDER BY a1_",
                "org.intermine.objectstore.query.QueryClass cannot be cast to org.intermine.objectstore.query.QueryField");
    }

    public void test2() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.age\" type=\"int\"><constraint op=\"&gt;=\" value=\"10\" description=\"\" identifier=\"\" code=\"A\"></constraint></node></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE a1_.age >= 10 ORDER BY a1_",
                "org.intermine.objectstore.query.QueryClass cannot be cast to org.intermine.objectstore.query.QueryField");
    }

    public void test3() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee\" constraintLogic=\"A and B\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.age\" type=\"int\"><constraint op=\"&gt;=\" value=\"10\" description=\"\" identifier=\"\" code=\"A\"></constraint></node><node path=\"Employee.fullTime\" type=\"boolean\"><constraint op=\"=\" value=\"true\" description=\"\" identifier=\"\" code=\"B\"></constraint></node></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE (a1_.age >= 10 AND a1_.fullTime = true) ORDER BY a1_",
                "org.intermine.objectstore.query.QueryClass cannot be cast to org.intermine.objectstore.query.QueryField");
    }

    public void test4() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee\" constraintLogic=\"A or B\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.age\" type=\"int\"><constraint op=\"&gt;=\" value=\"10\" description=\"\" identifier=\"\" code=\"A\"></constraint></node><node path=\"Employee.fullTime\" type=\"boolean\"><constraint op=\"=\" value=\"true\" description=\"\" identifier=\"\" code=\"B\"></constraint></node></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE (a1_.age >= 10 OR a1_.fullTime = true) ORDER BY a1_",
                "org.intermine.objectstore.query.QueryClass cannot be cast to org.intermine.objectstore.query.QueryField");
    }

    public void test5() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee\" constraintLogic=\"(A or B) and C\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.age\" type=\"int\"><constraint op=\"&gt;=\" value=\"10\" description=\"\" identifier=\"\" code=\"A\"></constraint></node><node path=\"Employee.fullTime\" type=\"boolean\"><constraint op=\"=\" value=\"true\" description=\"\" identifier=\"\" code=\"B\"></constraint></node><node path=\"Employee.name\" type=\"String\"><constraint op=\"=\" value=\"EmployeeA2\" description=\"\" identifier=\"\" code=\"C\"></constraint></node></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE ((a1_.age >= 10 OR a1_.fullTime = true) AND LOWER(a1_.name) LIKE 'employeea2') ORDER BY a1_",
                "org.intermine.objectstore.query.QueryClass cannot be cast to org.intermine.objectstore.query.QueryField");
    }

    public void test7() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee Employee.department\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.department\" type=\"Department\"></node><node path=\"Employee.department.employees\" type=\"Employee\"><constraint op=\"=\" value=\"Employee\"></constraint></node></query>",
                "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_ WHERE (a1_.department CONTAINS a2_ AND a2_.employees CONTAINS a1_) ORDER BY a1_, a2_",
                "org.intermine.objectstore.query.QueryClass cannot be cast to org.intermine.objectstore.query.QueryField",
                "org.intermine.objectstore.query.QueryClass cannot be cast to org.intermine.objectstore.query.QueryField");
    }

    public void test8() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Company.name Company.contractors.name\"><node path=\"Company\" type=\"Company\"></node><node path=\"Company.contractors\" type=\"Contractor\"></node><node path=\"Company.oldContracts\" type=\"Contractor\"><constraint op=\"=\" value=\"Company.contractors\" description=\"\" identifier=\"\" code=\"A\"></constraint></node></query>",
                "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Contractor AS a2_ WHERE (a1_.contractors CONTAINS a2_ AND a1_.oldContracts CONTAINS a2_) ORDER BY a1_.name, a2_.name",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a1_.name AS a3_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Contractor AS a2_ WHERE (a1_.contractors CONTAINS a2_ AND a1_.oldContracts CONTAINS a2_)) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a2_.name AS a3_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Contractor AS a2_ WHERE (a1_.contractors CONTAINS a2_ AND a1_.oldContracts CONTAINS a2_)) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC");
    }

    public void test9() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.department.company.name Employee.age Employee.fullTime Employee.address.address\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.department\" type=\"Department\"></node><node path=\"Employee.department.company\" type=\"Company\"></node><node path=\"Employee.department.company.address\" type=\"Address\"><constraint op=\"=\" value=\"Employee.address\" description=\"\" identifier=\"\" code=\"A\"></constraint></node><node path=\"Employee.address\" type=\"Address\"></node></query>",
                "SELECT DISTINCT a1_, a2_, a3_, a4_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_) ORDER BY a1_.name, a2_.name, a3_.name, a1_.age, a1_.fullTime, a4_.address",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.name AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a2_.name AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a3_.name AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT MIN(a1_.a5_) AS a2_, MAX(a1_.a5_) AS a3_, AVG(a1_.a5_) AS a4_, STDDEV(a1_.a5_) AS a5_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.age AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.fullTime AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a4_.address AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC");
    }

    public void test10() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.department.company.name Employee.age Employee.fullTime Employee.address.address\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.department\" type=\"Department\"></node><node path=\"Employee.department.company\" type=\"Company\"></node><node path=\"Employee.department.company.address\" type=\"Address\"><constraint op=\"!=\" value=\"Employee.address\" description=\"\" identifier=\"\" code=\"A\"></constraint></node><node path=\"Employee.address\" type=\"Address\"></node></query>",
                "SELECT DISTINCT a1_, a2_, a3_, a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_ AND a4_ != a5_) ORDER BY a1_.name, a2_.name, a3_.name, a1_.age, a1_.fullTime, a5_.address",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a5_, a1_.name AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_ AND a4_ != a5_)) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a5_, a2_.name AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_ AND a4_ != a5_)) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a5_, a3_.name AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_ AND a4_ != a5_)) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT MIN(a1_.a6_) AS a2_, MAX(a1_.a6_) AS a3_, AVG(a1_.a6_) AS a4_, STDDEV(a1_.a6_) AS a5_ FROM (SELECT DISTINCT a1_, a2_, a3_, a5_, a1_.age AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_ AND a4_ != a5_)) AS a1_",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a5_, a1_.fullTime AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_ AND a4_ != a5_)) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a5_, a5_.address AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_ AND a4_ != a5_)) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC");
    }

    public void test11() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.department.company.name Employee.age Employee.fullTime Employee.department.company.address.address\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.department\" type=\"Department\"></node><node path=\"Employee.department.company\" type=\"Company\"></node><node path=\"Employee.department.company.address\" type=\"Address\"><constraint op=\"=\" value=\"Employee.address\" description=\"\" identifier=\"\" code=\"A\"></constraint></node><node path=\"Employee.address\" type=\"Address\"></node></query>",
                "SELECT DISTINCT a1_, a2_, a3_, a4_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_) ORDER BY a1_.name, a2_.name, a3_.name, a1_.age, a1_.fullTime, a4_.address",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.name AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a2_.name AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a3_.name AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT MIN(a1_.a5_) AS a2_, MAX(a1_.a5_) AS a3_, AVG(a1_.a5_) AS a4_, STDDEV(a1_.a5_) AS a5_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.age AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.fullTime AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a4_.address AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC");
    }

    // This test exercises the MainHelper where there is a constraint that would be a loop if it wasn't ORed with another constraint.
    public void test12() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.department.company.name Employee.age Employee.fullTime Employee.department.company.address.address\" constraintLogic=\"A or B\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.name\"><constraint op=\"=\" value=\"EmployeeA1\" code=\"A\"></constraint></node><node path=\"Employee.department\" type=\"Department\"></node><node path=\"Employee.department.company\" type=\"Company\"></node><node path=\"Employee.department.company.address\" type=\"Address\"><constraint op=\"=\" value=\"Employee.address\" description=\"\" identifier=\"\" code=\"B\"></constraint></node><node path=\"Employee.address\" type=\"Address\"></node></query>",
                "SELECT DISTINCT a1_, a2_, a3_, a4_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE ((LOWER(a1_.name) LIKE 'employeea1' OR a4_ = a5_) AND a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_) ORDER BY a1_.name, a2_.name, a3_.name, a1_.age, a1_.fullTime, a4_.address",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.name AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE ((LOWER(a1_.name) LIKE 'employeea1' OR a4_ = a5_) AND a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_)) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a2_.name AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE ((LOWER(a1_.name) LIKE 'employeea1' OR a4_ = a5_) AND a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_)) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a3_.name AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE ((LOWER(a1_.name) LIKE 'employeea1' OR a4_ = a5_) AND a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_)) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT MIN(a1_.a6_) AS a2_, MAX(a1_.a6_) AS a3_, AVG(a1_.a6_) AS a4_, STDDEV(a1_.a6_) AS a5_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.age AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE ((LOWER(a1_.name) LIKE 'employeea1' OR a4_ = a5_) AND a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_)) AS a1_",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.fullTime AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE ((LOWER(a1_.name) LIKE 'employeea1' OR a4_ = a5_) AND a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_)) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a4_.address AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE ((LOWER(a1_.name) LIKE 'employeea1' OR a4_ = a5_) AND a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_)) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC");
    }

    public void test13() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.department.company.name Employee.age Employee.fullTime Employee.department.company.address.address\" constraintLogic=\"A and B and C\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.name\"><constraint op=\"=\" value=\"EmployeeA1\" code=\"A\"></constraint></node><node path=\"Employee.department\" type=\"Department\"></node><node path=\"Employee.department.company\" type=\"Company\"></node><node path=\"Employee.department.company.address\" type=\"Address\"><constraint op=\"=\" value=\"Employee.address\" description=\"\" identifier=\"\" code=\"B\"></constraint></node><node path=\"Employee.address\" type=\"Address\"></node><node path=\"Employee.address.address\"><constraint op=\"!=\" value=\"fred\" code=\"C\"></constraint></node></query>",
                "SELECT DISTINCT a1_, a2_, a3_, a4_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (LOWER(a1_.name) LIKE 'employeea1' AND a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND LOWER(a4_.address) NOT LIKE 'fred' AND a3_.address CONTAINS a4_) ORDER BY a1_.name, a2_.name, a3_.name, a1_.age, a1_.fullTime, a4_.address",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.name AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (LOWER(a1_.name) LIKE 'employeea1' AND a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND LOWER(a4_.address) NOT LIKE 'fred' AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a2_.name AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (LOWER(a1_.name) LIKE 'employeea1' AND a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND LOWER(a4_.address) NOT LIKE 'fred' AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a3_.name AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (LOWER(a1_.name) LIKE 'employeea1' AND a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND LOWER(a4_.address) NOT LIKE 'fred' AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT MIN(a1_.a5_) AS a2_, MAX(a1_.a5_) AS a3_, AVG(a1_.a5_) AS a4_, STDDEV(a1_.a5_) AS a5_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.age AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (LOWER(a1_.name) LIKE 'employeea1' AND a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND LOWER(a4_.address) NOT LIKE 'fred' AND a3_.address CONTAINS a4_)) AS a1_",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.fullTime AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (LOWER(a1_.name) LIKE 'employeea1' AND a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND LOWER(a4_.address) NOT LIKE 'fred' AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a4_.address AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (LOWER(a1_.name) LIKE 'employeea1' AND a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND LOWER(a4_.address) NOT LIKE 'fred' AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC");
    }

    public void test14() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.employees.name\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.department\" type=\"Department\"></node><node path=\"Employee.department.employees\" type=\"Employee\"><constraint op=\"!=\" value=\"Employee\" description=\"\" identifier=\"\" code=\"A\"></constraint></node></query>",
                "SELECT DISTINCT a1_, a3_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Employee AS a3_ WHERE (a1_.department CONTAINS a2_ AND a2_.employees CONTAINS a3_ AND a3_ != a1_) ORDER BY a1_.name, a3_.name",
                "SELECT DISTINCT a1_.a4_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a3_, a1_.name AS a4_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Employee AS a3_ WHERE (a1_.department CONTAINS a2_ AND a2_.employees CONTAINS a3_ AND a3_ != a1_)) AS a1_ GROUP BY a1_.a4_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a4_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a3_, a3_.name AS a4_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Employee AS a3_ WHERE (a1_.department CONTAINS a2_ AND a2_.employees CONTAINS a3_ AND a3_ != a1_)) AS a1_ GROUP BY a1_.a4_ ORDER BY COUNT(*) DESC");
    }


    public void test15() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee:department.name\"/>",
                "SELECT DISTINCT a1_, a1_.department AS a2_ FROM org.intermine.model.testmodel.Employee AS a1_ ORDER BY a1_.name",
                "SELECT DISTINCT a1_.a2_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a2_ FROM org.intermine.model.testmodel.Employee AS a1_) AS a1_ GROUP BY a1_.a2_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a2_.name AS a3_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_ WHERE a1_.department CONTAINS a2_) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC");
    }

    public void test16() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee:department.name Employee:department:company.name\"/>",
                "SELECT DISTINCT a1_, a2_.0 AS a3_, a2_.1 AS a4_ FROM org.intermine.model.testmodel.Employee AS a1_ ORDER BY a1_.name PATH a1_.department(SELECT default, default.company) AS a2_",
                "SELECT DISTINCT a1_.a4_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a4_ FROM org.intermine.model.testmodel.Employee AS a1_) AS a1_ GROUP BY a1_.a4_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a4_, a4_.name AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a4_ WHERE a1_.department CONTAINS a4_) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a3_.name AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_ WHERE a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC");
    }

    public void test17() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Company.name Company:departments.name Company:departments:employees.name\"/>",
                "SELECT DISTINCT a1_, a1_.departments(SELECT default, default.employees) AS a2_ FROM org.intermine.model.testmodel.Company AS a1_ ORDER BY a1_.name",
                "SELECT DISTINCT a1_.a2_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a2_ FROM org.intermine.model.testmodel.Company AS a1_) AS a1_ GROUP BY a1_.a2_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a4_, a4_.name AS a5_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a4_ WHERE a1_.departments CONTAINS a4_) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a4_, a5_, a5_.name AS a6_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a4_, org.intermine.model.testmodel.Employee AS a5_ WHERE a1_.departments CONTAINS a4_ AND a4_.employees CONTAINS a5_) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC");
    }

    public void test18() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Company.name\"><node path=\"Company\" type=\"Company\"><constraint op=\"LOOKUP\" value=\"CompanyAkjhadf\"/></node></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Company AS a1_ WHERE a1_.id IN ? ORDER BY a1_.name 1: []",
                "SELECT DISTINCT a1_.a2_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a2_ FROM org.intermine.model.testmodel.Company AS a1_ WHERE a1_.id IN ? 1: []) AS a1_ GROUP BY a1_.a2_ ORDER BY COUNT(*) DESC");
    }

    public void test19() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Company.name Company:departments.name\"><node path=\"Company:departments.name\"><constraint op=\"=\" value=\"%1\"/></node></query>",
                "SELECT DISTINCT a1_, a1_.departments(WHERE LOWER(default.name) LIKE '%1') AS a2_ FROM org.intermine.model.testmodel.Company AS a1_ ORDER BY a1_.name",
                "SELECT DISTINCT a1_.a2_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a2_ FROM org.intermine.model.testmodel.Company AS a1_) AS a1_ GROUP BY a1_.a2_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_.name AS a3_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a2_ WHERE (a1_.departments CONTAINS a2_ AND LOWER(a2_.name) LIKE '%1')) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC");
    }

    public void test20() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Company.name Company:departments.name Company:departments.employees.name\"/>",
                "SELECT DISTINCT a1_, a1_.departments(SELECT default, a1_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE default.employees CONTAINS a1_) AS a2_ FROM org.intermine.model.testmodel.Company AS a1_ ORDER BY a1_.name",
                "SELECT DISTINCT a1_.a2_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a2_ FROM org.intermine.model.testmodel.Company AS a1_) AS a1_ GROUP BY a1_.a2_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a4_, a5_, a4_.name AS a6_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a4_, org.intermine.model.testmodel.Employee AS a5_ WHERE a1_.departments CONTAINS a4_ AND a4_.employees CONTAINS a5_) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a4_, a5_, a5_.name AS a6_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a4_, org.intermine.model.testmodel.Employee AS a5_ WHERE a1_.departments CONTAINS a4_ AND a4_.employees CONTAINS a5_) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC");
    }

    public void test21() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Department.name Department:company.name Department:company.departments.name\"/>",
                "SELECT DISTINCT a1_, a1_.company(SELECT default, a1_ FROM org.intermine.model.testmodel.Department AS a1_ WHERE default.departments CONTAINS a1_) AS a2_ FROM org.intermine.model.testmodel.Department AS a1_ ORDER BY a1_.name",
                "SELECT DISTINCT a1_.a2_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a2_ FROM org.intermine.model.testmodel.Department AS a1_) AS a1_ GROUP BY a1_.a2_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a4_, a5_, a4_.name AS a6_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Company AS a4_, org.intermine.model.testmodel.Department AS a5_ WHERE a1_.company CONTAINS a4_ AND a4_.departments CONTAINS a5_) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a4_, a5_, a5_.name AS a6_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Company AS a4_, org.intermine.model.testmodel.Department AS a5_ WHERE a1_.company CONTAINS a4_ AND a4_.departments CONTAINS a5_) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC");
    }

    public void test22() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee:department:company.name\"/>",
                "SELECT DISTINCT a1_, a2_.0 AS a3_, a2_.1 AS a4_ FROM org.intermine.model.testmodel.Employee AS a1_ ORDER BY a1_.name PATH a1_.department(SELECT default, default.company) AS a2_",
                "SELECT DISTINCT a1_.a4_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a4_ FROM org.intermine.model.testmodel.Employee AS a1_) AS a1_ GROUP BY a1_.a4_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a5_, a5_.name AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a4_, org.intermine.model.testmodel.Company AS a5_ WHERE a1_.department CONTAINS a4_ AND a4_.company CONTAINS a5_) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC");
    }

    public void test23() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Department.name Department:company.name Department:company.departments.name\"><node path=\"Department\"><constraint op=\"=\" value=\"Department:company.departments\"/></node></query>",
                "Error - loop constraint spans path expression from Department to Department:company.departments",
                "Error - loop constraint spans path expression from Department to Department:company.departments",
                "Error - loop constraint spans path expression from Department to Department:company.departments",
                "Error - loop constraint spans path expression from Department to Department:company.departments");
    }

    public void test24() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Department.name Department.company.name Department.company.departments.name\"><node path=\"Department\"><constraint op=\"=\" value=\"Department.company.departments\"/></node></query>",
                "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Company AS a2_ WHERE (a1_.company CONTAINS a2_ AND a2_.departments CONTAINS a1_) ORDER BY a1_.name, a2_.name",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a1_.name AS a3_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Company AS a2_ WHERE (a1_.company CONTAINS a2_ AND a2_.departments CONTAINS a1_)) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a2_.name AS a3_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Company AS a2_ WHERE (a1_.company CONTAINS a2_ AND a2_.departments CONTAINS a1_)) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a1_.name AS a3_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Company AS a2_ WHERE (a1_.company CONTAINS a2_ AND a2_.departments CONTAINS a1_)) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC");
    }

    

    
    public void doQuery(String web, String iql, String ... summaries) throws Exception {
        try {
            Map parsed = PathQueryBinding.unmarshal(new StringReader(web));
            PathQuery pq = (PathQuery) parsed.get("test");
            Query q = MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap(), false);
            String got = q.toString();
            assertEquals("Expected: " + iql + ", got: " + got, iql, got);
        } catch (Exception e) {
            if (!e.getMessage().equals(iql)) {
                throw e;
            }
        }
        int columnNo = 0;
        String summaryPath = null;
        try {
            Map parsed = PathQueryBinding.unmarshal(new StringReader(web));
            PathQuery pq = (PathQuery) parsed.get("test");
            for (String summary : summaries) {
                try {
                    summaryPath = pq.getViewStrings().get(columnNo);
                    Query q = MainHelper.makeSummaryQuery(pq, summaryPath, new HashMap(), new HashMap(), bagQueryRunner);
                    String got = q.toString();
                    assertEquals("Failed for summaryPath " + summaryPath + ". Expected: " + summary + ", got; " + got, summary, got);
                    summaryPath = null;
                } catch (Exception e) {
                    if (!summary.equals(e.getMessage())) {
                        throw e;
                    }
                } finally {
                    columnNo++;
                }
            }
            assertEquals("Columns do not have summary tests", pq.getViewStrings().size(), columnNo);
        } catch (Exception e) {
            throw new Exception("Exception while testing summaryPath " + summaryPath, e);
        }
    }
}
