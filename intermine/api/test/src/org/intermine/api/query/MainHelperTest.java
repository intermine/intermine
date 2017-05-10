package org.intermine.api.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.bag.BagQueryHelper;
import org.intermine.api.bag.TestingBagQueryRunner;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.EmploymentPeriod;
import org.intermine.model.testmodel.Types;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.query.Constraint;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionPathExpression;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryEvaluable;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryObjectPathExpression;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Queryable;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.LogicExpression;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintNull;
import org.intermine.pathquery.PathConstraintRange;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.metadata.StringUtil;

/**
 * Tests for the MainHelper class
 *
 * @author Kim Rutherford
 */

public class MainHelperTest extends TestCase {
    private BagQueryConfig bagQueryConfig;
    private ObjectStore os;
    private Map<String, List<FieldDescriptor>> classKeys;
    private TestingBagQueryRunner bagQueryRunner;

    public MainHelperTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        os = ObjectStoreFactory.getObjectStore("os.unittest");
        Properties classKeyProps = new Properties();
        try {
            classKeyProps.load(MainHelperTest.class.getClassLoader()
                                   .getResourceAsStream("class_keys.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Some IO error happened getting class keys ", e);
        }
        classKeys = ClassKeyHelper.readKeys(os.getModel(), classKeyProps);
        InputStream config = MainHelperTest.class.getClassLoader()
            .getResourceAsStream("bag-queries.xml");
        bagQueryConfig = BagQueryHelper.readBagQueryConfig(os.getModel(), config);
        bagQueryRunner = new TestingBagQueryRunner(os, classKeys, bagQueryConfig, null);
        bagQueryRunner.setConversionTemplates(Collections.EMPTY_LIST);
    }

    public void testMakeConstraintSets() {
        HashMap map = new HashMap();
        LogicExpression expr = new LogicExpression("A and B");
        QueryClass qc = new QueryClass(Employee.class);
        QueryField fA = new QueryField(qc, "name");
        QueryField fB = new QueryField(qc, "age");
        QueryField fC = new QueryField(qc, "end");
        Constraint a = new SimpleConstraint(fA, ConstraintOp.IS_NULL);
        Constraint b = new SimpleConstraint(fB, ConstraintOp.IS_NULL);
        Constraint c = new SimpleConstraint(fC, ConstraintOp.IS_NULL);
        Map<String, Constraint> codeToConstraint = new HashMap<String, Constraint>();
        codeToConstraint.put("A", a);
        codeToConstraint.put("B", b);
        ConstraintSet andCs = new ConstraintSet(ConstraintOp.AND);
        MainHelper.createConstraintStructure(expr, andCs, codeToConstraint);

        ConstraintSet expected = new ConstraintSet(ConstraintOp.AND);
        expected.addConstraint(a);
        expected.addConstraint(b);
        assertEquals(expected, andCs);

        expr = new LogicExpression("A and (B or C)");
        codeToConstraint.put("C", c);
        andCs = new ConstraintSet(ConstraintOp.AND);
        expected = new ConstraintSet(ConstraintOp.AND);
        expected.addConstraint(a);
        ConstraintSet expectedOr = new ConstraintSet(ConstraintOp.OR);
        expectedOr.addConstraint(b);
        expectedOr.addConstraint(c);
        expected.addConstraint(expectedOr);
        MainHelper.createConstraintStructure(expr, andCs, codeToConstraint);

        assertEquals(expected, andCs);
    }

    // Select Employee.name
    public void testMakeQueryOneField() throws Exception {
        Map<String, PathQuery> queries = readQueries();
        PathQuery pq = queries.get("employeeName");

        Query q = new Query();
        QueryClass qc1 = new QueryClass(Employee.class);
        q.addToSelect(qc1);
        q.addFrom(qc1);
        q.addToOrderBy(new QueryField(qc1, "name"));

        assertEquals(q.toString(), MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap()).toString());
    }

     // Select Employee.name, Employee.departments.name, Employee.departments.company.name
    // Constrain Employee.department.name = 'DepartmentA1'
    public void testMakeQueryThreeClasses() throws Exception {
        Map<String, PathQuery> queries = readQueries();
        PathQuery pq = queries.get("employeeDepartmentCompany");

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
        QueryClass qc3 = new QueryClass(Company.class);
        q.addToSelect(qc3);
        q.addFrom(qc3);
        QueryObjectReference qor2 = new QueryObjectReference(qc2, "company");
        ContainsConstraint cc2 = new ContainsConstraint(qor2, ConstraintOp.CONTAINS, qc3);
        cs.addConstraint(cc2);
        cs.addConstraint(sc1);
        q.setConstraint(cs);
        q.addToOrderBy(new QueryField(qc1, "name"));
        q.addToOrderBy(qf1);
        q.addToOrderBy(new QueryField(qc3, "name"));

        assertEquals(q.toString(), MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap()).toString());
    }

    // As above but add a wildcard in the constraint which makes a MATCHES constraint
    // Constrain Employee.department.name = 'DepartmentA*'
    public void testMakeQueryWildcard() throws Exception {
        Map<String, PathQuery> queries = readQueries();
        PathQuery pq = queries.get("employeeDepartmentCompanyWildcard");

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
        QueryClass qc3 = new QueryClass(Company.class);
        q.addToSelect(qc3);
        q.addFrom(qc3);
        QueryObjectReference qor2 = new QueryObjectReference(qc2, "company");
        ContainsConstraint cc2 = new ContainsConstraint(qor2, ConstraintOp.CONTAINS, qc3);
        cs.addConstraint(cc2);
        cs.addConstraint(sc1);
        q.setConstraint(cs);
        q.addToOrderBy(new QueryField(qc1, "name"));
        q.addToOrderBy(qf1);
        q.addToOrderBy(new QueryField(qc3, "name"));

        assertEquals(q.toString(), MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap()).toString());
    }

    // Select Employee.name, Employee.departments.company.name  (should not select Department)
    // Constrain Employee.department.name = 'DepartmentA1'
    public void testConstrainedButNotInView() throws Exception {
        Map<String, PathQuery> queries = readQueries();
        PathQuery pq = queries.get("employeeCompany");

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
        QueryClass qc3 = new QueryClass(Company.class);
        q.addToSelect(qc3);
        q.addFrom(qc3);
        QueryObjectReference qor2 = new QueryObjectReference(qc2, "company");
        ContainsConstraint cc2 = new ContainsConstraint(qor2, ConstraintOp.CONTAINS, qc3);
        cs.addConstraint(cc2);
        cs.addConstraint(sc1);
        q.setConstraint(cs);
        q.addToOrderBy(new QueryField(qc1, "name"));
        q.addToOrderBy(new QueryField(qc3, "name"));

        assertEquals(q.toString(), MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap()).toString());
    }

    private static class DummyHelper implements RangeHelper {
        @Override
        public Constraint createConstraint(Queryable q, QueryNode node, PathConstraintRange con) {
            return new SimpleConstraint(new QueryValue("Foo"), ConstraintOp.EQUALS, new QueryValue("Bar"));
        }
    }

    public void testRangeConstraint() throws Exception {
        QueryClass qc = new QueryClass(EmploymentPeriod.class);

        Constraint exp = new SimpleConstraint(new QueryValue("Foo"), ConstraintOp.EQUALS, new QueryValue("Bar"));

        List<String> ranges = Arrays.asList("2008-11-17");
        PathConstraintRange con = new PathConstraintRange("EmploymentPeriod", ConstraintOp.WITHIN, ranges);

        MainHelper.RangeConfig.reset(); // Call to avoid setup conflict.

        try {
            MainHelper.makeRangeConstraint(null, qc, con);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("No range constraints are possible"));
        }

        MainHelper.RangeConfig.rangeHelpers.put(EmploymentPeriod.class, new DummyHelper());

        org.intermine.objectstore.query.Constraint got = MainHelper.makeRangeConstraint(null, qc, con);
        assertEquals(exp, got);
    }

    public void testMakeQueryDateConstraint() throws Exception {
        // 11:02:39am Sun Nov 16, 2008
        QueryClass qc = new QueryClass(Types.class);
        QueryField qn = new QueryField(qc, "dateObjType");
        // startOfDate < queryDate = 12:20:34am Mon Nov 17, 2008 < endOfDay
        Date queryDate = new Date(1226881234565L);
        Date startOfDay = new Date(1226880000000L);
        Date endOfDay = new Date(1226966400000L);
        SimpleConstraint expLTConstraint =
                new SimpleConstraint((QueryEvaluable) qn, ConstraintOp.LESS_THAN,
                                     new QueryValue(startOfDay));
        PathConstraintAttribute ltConstraint = new PathConstraintAttribute("Types.dateObjType", ConstraintOp.LESS_THAN, "2008-11-17 12:20:34");
        org.intermine.objectstore.query.Constraint resLTConstraint =
            MainHelper.makeQueryDateConstraint(qn, ltConstraint);

        assertEquals(expLTConstraint, resLTConstraint);

        SimpleConstraint expLTEConstraint =
            new SimpleConstraint((QueryEvaluable) qn, ConstraintOp.LESS_THAN,
                                 new QueryValue(endOfDay));
        PathConstraintAttribute lteConstraint = new PathConstraintAttribute("Types.dateObjType", ConstraintOp.LESS_THAN_EQUALS, "2008-11-17 12:20:34");
        org.intermine.objectstore.query.Constraint resLTEConstraint =
            MainHelper.makeQueryDateConstraint(qn, lteConstraint);

        assertEquals(expLTEConstraint, resLTEConstraint);

        SimpleConstraint expGTConstraint =
            new SimpleConstraint((QueryEvaluable) qn, ConstraintOp.LESS_THAN,
                                 new QueryValue(startOfDay));
        PathConstraintAttribute gtConstraint = new PathConstraintAttribute("Types.dateObjType", ConstraintOp.LESS_THAN, "2008-11-17 12:20:34");
        org.intermine.objectstore.query.Constraint resGTConstraint =
            MainHelper.makeQueryDateConstraint(qn, gtConstraint);

        assertEquals(expGTConstraint, resGTConstraint);

        SimpleConstraint expGTEConstraint =
            new SimpleConstraint((QueryEvaluable) qn, ConstraintOp.LESS_THAN,
                                 new QueryValue(endOfDay));
        PathConstraintAttribute gteConstraint = new PathConstraintAttribute("Types.dateObjType", ConstraintOp.LESS_THAN_EQUALS, "2008-11-17 12:20:34");
        org.intermine.objectstore.query.Constraint resGTEConstraint =
            MainHelper.makeQueryDateConstraint(qn, gteConstraint);

        assertEquals(expGTEConstraint, resGTEConstraint);

        ConstraintSet expEQConstraint =
            new ConstraintSet(ConstraintOp.AND);
        SimpleConstraint expEQStartConstraint =
            new SimpleConstraint((QueryEvaluable) qn, ConstraintOp.GREATER_THAN_EQUALS,
                                 new QueryValue(startOfDay));
        SimpleConstraint expEQEndConstraint =
            new SimpleConstraint((QueryEvaluable) qn, ConstraintOp.LESS_THAN,
                                 new QueryValue(endOfDay));
        expEQConstraint.addConstraint(expEQStartConstraint);
        expEQConstraint.addConstraint(expEQEndConstraint);
        PathConstraintAttribute eqConstraint = new PathConstraintAttribute("Types.dateObjType", ConstraintOp.EQUALS, "2008-11-17 12:20:34");
        org.intermine.objectstore.query.Constraint resEQConstraint =
            MainHelper.makeQueryDateConstraint(qn, eqConstraint);

        assertEquals(expEQConstraint, resEQConstraint);

        ConstraintSet expNEQConstraint =
            new ConstraintSet(ConstraintOp.OR);
        SimpleConstraint expNEQStartConstraint =
            new SimpleConstraint((QueryEvaluable) qn, ConstraintOp.LESS_THAN,
                                 new QueryValue(startOfDay));
        SimpleConstraint expNEQEndConstraint =
            new SimpleConstraint((QueryEvaluable) qn, ConstraintOp.GREATER_THAN_EQUALS,
                                 new QueryValue(endOfDay));
        expNEQConstraint.addConstraint(expNEQStartConstraint);
        expNEQConstraint.addConstraint(expNEQEndConstraint);
        PathConstraintAttribute neqConstraint = new PathConstraintAttribute("Types.dateObjType", ConstraintOp.NOT_EQUALS, "2008-11-17 12:20:34");
        org.intermine.objectstore.query.Constraint resNEQConstraint =
            MainHelper.makeQueryDateConstraint(qn, neqConstraint);

        assertEquals(expNEQConstraint, resNEQConstraint);
    }


    // test that loop constraint queries are generated correctly
    public void testLoopConstraint() throws Exception {
        Map<String, PathQuery> queries = readQueries();
        PathQuery pq = queries.get("loopConstraint");
        Query q = MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap());
        String got = q.toString();
        String iql = "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a2_ WHERE (a1_.departments CONTAINS a2_ AND a2_.company CONTAINS a1_) ORDER BY a1_.name";
        assertEquals("Expected: " + iql + ", got: " + got, iql, got);
    }


    // Test that IS_NULL and IS_NOT_NULL queries are generated correctly, the referenced clas
    // should not appear on from FROM list
    public void testNullReference() throws Exception {
        PathQuery pq = new PathQuery(os.getModel());
        pq.addView("Department.name");
        pq.addConstraint(new PathConstraintNull("Department.company", ConstraintOp.IS_NULL));

        Query expected = new Query();
        QueryClass qc = new QueryClass(Department.class);
        expected.addFrom(qc);
        expected.addToSelect(qc);
        QueryObjectReference ref = new QueryObjectReference(qc, "company");
        ContainsConstraint cc = new ContainsConstraint(ref, ConstraintOp.IS_NULL);
        expected.setConstraint(cc);
        expected.addToOrderBy(new QueryField(qc, "name"));

        Query actual = MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap());
        assertEquals(expected.toString(), actual.toString());

        // Same test for IS_NOT_NULL
        pq = new PathQuery(os.getModel());
        pq.addView("Department.name");
        pq.addConstraint(new PathConstraintNull("Department.company", ConstraintOp.IS_NOT_NULL));

        cc = new ContainsConstraint(ref, ConstraintOp.IS_NOT_NULL);
        expected.setConstraint(cc);
        actual = MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap());
        assertEquals(expected.toString(), actual.toString());
    }

    // Test that IS_NULL and IS_NOT_NULL queries are generated correctly for collections. It doesn't
    // matter if the collection is 1:N or N:M for the ObjectStore query
    public void testNullCollection() throws Exception {
        PathQuery pq = new PathQuery(os.getModel());
        pq.addView("Company.name");
        pq.addConstraint(new PathConstraintNull("Company.contractors", ConstraintOp.IS_NULL));

        Query expected = new Query();
        QueryClass qc = new QueryClass(Company.class);
        expected.addFrom(qc);
        expected.addToSelect(qc);
        QueryCollectionReference col = new QueryCollectionReference(qc, "contractors");
        ContainsConstraint cc = new ContainsConstraint(col, ConstraintOp.IS_NULL);
        expected.setConstraint(cc);
        expected.addToOrderBy(new QueryField(qc, "name"));

        Query actual = MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap());
        assertEquals(expected.toString(), actual.toString());

        // Same test for IS_NOT_NULL
        pq = new PathQuery(os.getModel());
        pq.addView("Company.name");
        pq.addConstraint(new PathConstraintNull("Company.contractors", ConstraintOp.IS_NOT_NULL));

        cc = new ContainsConstraint(col, ConstraintOp.IS_NOT_NULL);
        expected.setConstraint(cc);
        actual = MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap());
        assertEquals(expected.toString(), actual.toString());
    }


    // Test NULL/NOT NULL references where there are other constraints in the query, this means
    // the null referenced class SHOULD be in the FROM list and joined. The code allows creation
    // of daft queries that will never produce results.
    public void testNullReferenceOtherPaths() throws Exception {
        // the IS_NOT_NULL constraint doesn't actually do anything because we have a normal join
        PathQuery pq = new PathQuery(os.getModel());
        pq.addView("Department.company.name");
        pq.addConstraint(new PathConstraintNull("Department.company", ConstraintOp.IS_NOT_NULL));

        Query expected = new Query();
        QueryClass qcDep = new QueryClass(Department.class);
        QueryClass qcCom = new QueryClass(Company.class);
        expected.addFrom(qcDep);
        expected.addFrom(qcCom);
        expected.addToSelect(qcCom);
        QueryObjectReference ref = new QueryObjectReference(qcDep, "company");
        ContainsConstraint cc1 = new ContainsConstraint(ref, ConstraintOp.CONTAINS, qcCom);
        ContainsConstraint cc2 = new ContainsConstraint(ref, ConstraintOp.IS_NOT_NULL);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(cc1);
        cs.addConstraint(cc2);
        expected.setConstraint(cs);
        expected.addToOrderBy(new QueryField(qcCom, "name"));

        Query actual = MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap());
        String e = expected.toString();
        String a = actual.toString();
        assertEquals(expected.toString(), actual.toString());

        // This time other occurrence of path is in a constraint not the view and op is IS_NULL
        pq = new PathQuery(os.getModel());
        pq.addView("Department.company.name");
        pq.addConstraint(new PathConstraintNull("Department.company", ConstraintOp.IS_NULL));
        pq.addConstraint(new PathConstraintAttribute("Department.company.name", ConstraintOp.EQUALS, "Com1"));

        cc2 = new ContainsConstraint(ref, ConstraintOp.IS_NULL);
        SimpleConstraint sc = new SimpleConstraint(
                new QueryExpression(QueryExpression.LOWER, new QueryField(qcCom, "name")),
                ConstraintOp.MATCHES, new QueryValue("com1"));
        cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(cc1);
        cs.addConstraint(cc2);
        cs.addConstraint(sc);
        expected.setConstraint(cs);
        actual = MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap());
        assertEquals(expected.toString(), actual.toString());
    }

    // Test that a null reference constraint works when the same reference has been set as an outer
    // join. This isn't particularly useful but needs to generate a valid query.
    public void testNullReferenceOuterJoin() throws Exception {
        PathQuery pq = new PathQuery(os.getModel());
        pq.addViews("Department.company.name");
        pq.addConstraint(new PathConstraintNull("Department.company", ConstraintOp.IS_NOT_NULL));
        pq.setOuterJoinStatus("Department.company", OuterJoinStatus.OUTER);

        Query expected = new Query();
        QueryClass qc = new QueryClass(Department.class);
        expected.addFrom(qc);
        expected.addToSelect(qc);

        // outer join to select Department.company.name
        QueryObjectPathExpression comOuter = new QueryObjectPathExpression(qc, "company");
        expected.addToSelect(comOuter);
        // Company IS_NOT_NULL
        QueryObjectReference ref = new QueryObjectReference(qc, "company");
        ContainsConstraint cc = new ContainsConstraint(ref, ConstraintOp.IS_NOT_NULL);
        expected.setConstraint(cc);

        Query actual = MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap());
        assertEquals(expected.toString(), actual.toString());

        // Same test for IS_NOT_NULL
        pq = new PathQuery(os.getModel());
        pq.addView("Department.company.name");
        pq.addConstraint(new PathConstraintNull("Department.company", ConstraintOp.IS_NOT_NULL));
        pq.setOuterJoinStatus("Department.company", OuterJoinStatus.OUTER);

        cc = new ContainsConstraint(ref, ConstraintOp.IS_NOT_NULL);
        expected.setConstraint(cc);
        actual = MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap());
        assertEquals(expected.toString(), actual.toString());
    }


    // Test that NULL/NOT NULL constraints work either side of outer joins in a query but where
    // the NULL reference isn't itself an outer join.
    public void testNullReferenceOuterJoinElsewhere() throws Exception {
        // Test outer join beyond the NULL constraint
        PathQuery pq = new PathQuery(os.getModel());
        pq.addViews("Department.company.name");
        pq.addConstraint(new PathConstraintNull("Department.company.contractors", ConstraintOp.IS_NOT_NULL));
        pq.setOuterJoinStatus("Department.company", OuterJoinStatus.OUTER);

        Query expected = new Query();
        QueryClass qc = new QueryClass(Department.class);
        expected.addFrom(qc);
        expected.addToSelect(qc);

        QueryObjectPathExpression comOuter = new QueryObjectPathExpression(qc, "company");
        comOuter.addToSelect(comOuter.getDefaultClass());
        QueryCollectionReference col = new QueryCollectionReference(comOuter.getDefaultClass(), "contractors");
        ContainsConstraint cc = new ContainsConstraint(col, ConstraintOp.IS_NOT_NULL);
        comOuter.setConstraint(cc);
        expected.addToSelect(comOuter);

        Query actual = MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap());
        assertEquals(expected.toString(), actual.toString());

        // Test outer join before the NULL constraint
        pq = new PathQuery(os.getModel());
        pq.addViews("Department.company.name", "Department.company.contractors.name");
        pq.addConstraint(new PathConstraintNull("Department.company", ConstraintOp.IS_NOT_NULL));
        pq.setOuterJoinStatus("Department.company.contractors", OuterJoinStatus.OUTER);

        expected = new Query();
        QueryClass qcDep = new QueryClass(Department.class);
        expected.addFrom(qcDep);
        QueryClass qcCom = new QueryClass(Company.class);
        expected.addFrom(qcCom);
        expected.addToSelect(qcCom);
        QueryObjectReference ref = new QueryObjectReference(qcDep, "company");
        // Here we have two contains constraints, this doesn't make much sense as the NOT NULL
        // constraint is actually redundant.
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        ContainsConstraint ccNormal = new ContainsConstraint(ref, ConstraintOp.CONTAINS, qcCom);
        cs.addConstraint(ccNormal);
        ContainsConstraint ccNull = new ContainsConstraint(ref, ConstraintOp.IS_NOT_NULL);
        cs.addConstraint(ccNull);
        expected.setConstraint(cs);
        QueryCollectionPathExpression conOuter = new QueryCollectionPathExpression(qcCom, "contractors");
        conOuter.addToSelect(conOuter.getDefaultClass());
        expected.addToSelect(conOuter);
        expected.addToOrderBy(new QueryField(qcCom, "name"));

        actual = MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap());
        assertEquals(expected.toString(), actual.toString());
    }


    public void testAddToConstraintLogic() throws Exception {
        LogicExpression l = new LogicExpression("A");
        LogicExpression updated = MainHelper.addToConstraintLogic(l, "B");
        assertEquals("A and B", updated.toString());
        l = new LogicExpression("A and B");
        updated = MainHelper.addToConstraintLogic(l, "C");
        assertEquals("A and B and C", updated.toString());
        l = new LogicExpression("(A or B) and (C or D)");
        updated = MainHelper.addToConstraintLogic(l, "E");
        assertEquals("(A or B) and (C or D) and E", updated.toString());
        l = null;
        updated = MainHelper.addToConstraintLogic(l, "A");
        assertEquals("A", updated.toString());
    }


    public void testRemoveFromConstraintLogic() throws Exception {
        LogicExpression l = new LogicExpression("A and B");
        LogicExpression updated = MainHelper.removeFromConstraintLogic(l, "B");
        assertEquals("A", updated.toString());
        l = new LogicExpression("A and B");
        updated = MainHelper.removeFromConstraintLogic(l, "A");
        assertEquals("B", updated.toString());
        l = new LogicExpression("(A and B) or (C and D)");
        updated = MainHelper.removeFromConstraintLogic(l, "C");
        assertEquals("(A and B) or D", updated.toString());
        l = new LogicExpression("A");
        assertNull(MainHelper.removeFromConstraintLogic(l, "A"));
    }

    private Map<String, PathQuery> readQueries() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("MainHelperTest.xml");
        Map<String, PathQuery> ret = PathQueryBinding.unmarshalPathQueries(new InputStreamReader(is), PathQuery.USERPROFILE_VERSION);
        return ret;
    }

    public void test1() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name\"></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_ ORDER BY a1_.name",
                "SELECT DISTINCT a1_.a2_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a2_ FROM org.intermine.model.testmodel.Employee AS a1_) AS a1_ GROUP BY a1_.a2_ ORDER BY COUNT(*) DESC");
    }

    public void test2() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name\"><constraint path=\"Employee.age\" op=\"&gt;=\" value=\"10\" code=\"A\"/></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE a1_.age >= 10 ORDER BY a1_.name",
                "SELECT DISTINCT a1_.a2_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a2_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE a1_.age >= 10) AS a1_ GROUP BY a1_.a2_ ORDER BY COUNT(*) DESC");
    }

    public void test3() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name\" constraintLogic=\"A and B\"><constraint path=\"Employee.age\" op=\"&gt;=\" value=\"10\" code=\"A\"/><constraint path=\"Employee.fullTime\" op=\"=\" value=\"true\" code=\"B\"/></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE (a1_.age >= 10 AND a1_.fullTime = true) ORDER BY a1_.name",
                "SELECT DISTINCT a1_.a2_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a2_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE (a1_.age >= 10 AND a1_.fullTime = true)) AS a1_ GROUP BY a1_.a2_ ORDER BY COUNT(*) DESC");
    }

    public void test4() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name\" constraintLogic=\"A or B\"><constraint path=\"Employee.age\" op=\"&gt;=\" value=\"10\" code=\"A\"/><constraint path=\"Employee.fullTime\" op=\"=\" value=\"true\" code=\"B\"/></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE (a1_.age >= 10 OR a1_.fullTime = true) ORDER BY a1_.name",
                "SELECT DISTINCT a1_.a2_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a2_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE (a1_.age >= 10 OR a1_.fullTime = true)) AS a1_ GROUP BY a1_.a2_ ORDER BY COUNT(*) DESC");
    }

    public void test5() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name\" constraintLogic=\"(A or B) and C\"><constraint path=\"Employee.age\" op=\"&gt;=\" value=\"10\" code=\"A\"/><constraint path=\"Employee.fullTime\" op=\"=\" value=\"true\" code=\"B\"/><constraint path=\"Employee.name\" op=\"=\" value=\"EmployeeA2\" code=\"C\"/></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE ((a1_.age >= 10 OR a1_.fullTime = true) AND LOWER(a1_.name) LIKE 'employeea2') ORDER BY a1_.name",
                "SELECT DISTINCT a1_.a2_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a2_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE ((a1_.age >= 10 OR a1_.fullTime = true) AND LOWER(a1_.name) LIKE 'employeea2')) AS a1_ GROUP BY a1_.a2_ ORDER BY COUNT(*) DESC");
    }

    public void test7() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.name\"><constraint path=\"Employee.department.employees\" op=\"=\" value=\"Employee\"/></query>",
                "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_ WHERE (a1_.department CONTAINS a2_ AND a2_.employees CONTAINS a1_) ORDER BY a1_.name, a2_.name",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a1_.name AS a3_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_ WHERE (a1_.department CONTAINS a2_ AND a2_.employees CONTAINS a1_)) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a2_.name AS a3_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_ WHERE (a1_.department CONTAINS a2_ AND a2_.employees CONTAINS a1_)) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC");
    }

    public void test8() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Company.name Company.contractors.name\"><constraint path=\"Company.oldContracts\" op=\"=\" value=\"Company.contractors\"/></query>",
                "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Contractor AS a2_ WHERE (a1_.contractors CONTAINS a2_ AND a1_.oldContracts CONTAINS a2_) ORDER BY a1_.name, a2_.name",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a1_.name AS a3_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Contractor AS a2_ WHERE (a1_.contractors CONTAINS a2_ AND a1_.oldContracts CONTAINS a2_)) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a2_.name AS a3_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Contractor AS a2_ WHERE (a1_.contractors CONTAINS a2_ AND a1_.oldContracts CONTAINS a2_)) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC");
    }

    public void test9() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.department.company.name Employee.age Employee.fullTime Employee.address.address\"><constraint path=\"Employee.department.company.address\" op=\"=\" loopPath=\"Employee.address\" code=\"A\"/></query>",
                "SELECT DISTINCT a1_, a2_, a3_, a4_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_) ORDER BY a1_.name, a2_.name, a3_.name, a1_.age, a1_.fullTime, a4_.address",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.name AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a2_.name AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a3_.name AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, a1_.a5_ AS a3_, a1_.a7_ AS a4_, a1_.a8_ AS a5_, a1_.a10_ AS a6_, a1_.a9_ AS a7_, SUM(a1_.a3_) AS a8_ FROM (SELECT COUNT(*) AS a3_, a1_.a2_ AS a4_, a2_.a3_ AS a5_, a2_.a2_ AS a6_, a2_.a4_ AS a7_, a2_.a5_ AS a8_, WIDTH_BUCKET(a1_.a2_, (a2_.a3_)::BigDecimal * (1.01)::BigDecimal, a2_.a2_, a2_.a6_) AS a9_, a2_.a6_ AS a10_ FROM (SELECT a1_.a5_ AS a2_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.age AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_) AS a1_, (SELECT DISTINCT MIN(a1_.a5_) AS a2_, MAX(a1_.a5_) AS a3_, AVG(a1_.a5_) AS a4_, STDDEV(a1_.a5_) AS a5_, LEAST(20,MAX(a1_.a5_) - MIN(a1_.a5_)) AS a6_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.age AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_) AS a2_ GROUP BY a1_.a2_, a2_.a3_, a2_.a2_, a2_.a4_, a2_.a5_, a2_.a6_ ORDER BY a9_, a1_.a2_) AS a1_ GROUP BY a1_.a6_, a1_.a5_, a1_.a7_, a1_.a8_, a1_.a9_, a1_.a10_ ORDER BY a1_.a9_",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.fullTime AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a4_.address AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC");
    }

    public void test10() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.department.company.name Employee.age Employee.fullTime Employee.address.address\"><constraint path=\"Employee.department.company.address\" op=\"!=\" value=\"Employee.address\" code=\"A\"/></query>",
                "SELECT DISTINCT a1_, a2_, a3_, a4_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a5_ AND a4_ != a5_) ORDER BY a1_.name, a2_.name, a3_.name, a1_.age, a1_.fullTime, a4_.address",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.name AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a5_ AND a4_ != a5_)) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a2_.name AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a5_ AND a4_ != a5_)) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a3_.name AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a5_ AND a4_ != a5_)) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, a1_.a5_ AS a3_, a1_.a7_ AS a4_, a1_.a8_ AS a5_, a1_.a10_ AS a6_, a1_.a9_ AS a7_, SUM(a1_.a3_) AS a8_ FROM (SELECT COUNT(*) AS a3_, a1_.a2_ AS a4_, a2_.a3_ AS a5_, a2_.a2_ AS a6_, a2_.a4_ AS a7_, a2_.a5_ AS a8_, WIDTH_BUCKET(a1_.a2_, (a2_.a3_)::BigDecimal * (1.01)::BigDecimal, a2_.a2_, a2_.a6_) AS a9_, a2_.a6_ AS a10_ FROM (SELECT a1_.a6_ AS a2_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.age AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a5_ AND a4_ != a5_)) AS a1_) AS a1_, (SELECT DISTINCT MIN(a1_.a6_) AS a2_, MAX(a1_.a6_) AS a3_, AVG(a1_.a6_) AS a4_, STDDEV(a1_.a6_) AS a5_, LEAST(20,MAX(a1_.a6_) - MIN(a1_.a6_)) AS a6_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.age AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a5_ AND a4_ != a5_)) AS a1_) AS a2_ GROUP BY a1_.a2_, a2_.a3_, a2_.a2_, a2_.a4_, a2_.a5_, a2_.a6_ ORDER BY a9_, a1_.a2_) AS a1_ GROUP BY a1_.a6_, a1_.a5_, a1_.a7_, a1_.a8_, a1_.a9_, a1_.a10_ ORDER BY a1_.a9_",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.fullTime AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a5_ AND a4_ != a5_)) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a4_.address AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a5_ AND a4_ != a5_)) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC");
    }

    public void test11() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.department.company.name Employee.age Employee.fullTime Employee.department.company.address.address\"><constraint path=\"Employee.department.company.address\" op=\"=\" loopPath=\"Employee.address\" code=\"A\"/></query>",
                "SELECT DISTINCT a1_, a2_, a3_, a4_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_) ORDER BY a1_.name, a2_.name, a3_.name, a1_.age, a1_.fullTime, a4_.address",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.name AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a2_.name AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a3_.name AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, a1_.a5_ AS a3_, a1_.a7_ AS a4_, a1_.a8_ AS a5_, a1_.a10_ AS a6_, a1_.a9_ AS a7_, SUM(a1_.a3_) AS a8_ FROM (SELECT COUNT(*) AS a3_, a1_.a2_ AS a4_, a2_.a3_ AS a5_, a2_.a2_ AS a6_, a2_.a4_ AS a7_, a2_.a5_ AS a8_, WIDTH_BUCKET(a1_.a2_, (a2_.a3_)::BigDecimal * (1.01)::BigDecimal, a2_.a2_, a2_.a6_) AS a9_, a2_.a6_ AS a10_ FROM (SELECT a1_.a5_ AS a2_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.age AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_) AS a1_, (SELECT DISTINCT MIN(a1_.a5_) AS a2_, MAX(a1_.a5_) AS a3_, AVG(a1_.a5_) AS a4_, STDDEV(a1_.a5_) AS a5_, LEAST(20,MAX(a1_.a5_) - MIN(a1_.a5_)) AS a6_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.age AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_) AS a2_ GROUP BY a1_.a2_, a2_.a3_, a2_.a2_, a2_.a4_, a2_.a5_, a2_.a6_ ORDER BY a9_, a1_.a2_) AS a1_ GROUP BY a1_.a6_, a1_.a5_, a1_.a7_, a1_.a8_, a1_.a9_, a1_.a10_ ORDER BY a1_.a9_",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.fullTime AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a4_.address AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_)) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC");
    }

    // This test exercises the MainHelper where there is a constraint that would be a loop if it wasn't ORed with another constraint.
    public void test12() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.department.company.name Employee.age Employee.fullTime Employee.department.company.address.address\" constraintLogic=\"A or B\"><constraint path=\"Employee.name\" op=\"=\" value=\"EmployeeA1\" code=\"A\"/><constraint path=\"Employee.department.company.address\" op=\"=\" loopPath=\"Employee.address\" code=\"B\"/></query>",
                "SELECT DISTINCT a1_, a2_, a3_, a4_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_ AND (LOWER(a1_.name) LIKE 'employeea1' OR a5_ = a4_)) ORDER BY a1_.name, a2_.name, a3_.name, a1_.age, a1_.fullTime, a4_.address",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.name AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_ AND (LOWER(a1_.name) LIKE 'employeea1' OR a5_ = a4_))) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a2_.name AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_ AND (LOWER(a1_.name) LIKE 'employeea1' OR a5_ = a4_))) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a3_.name AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_ AND (LOWER(a1_.name) LIKE 'employeea1' OR a5_ = a4_))) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, a1_.a5_ AS a3_, a1_.a7_ AS a4_, a1_.a8_ AS a5_, a1_.a10_ AS a6_, a1_.a9_ AS a7_, SUM(a1_.a3_) AS a8_ FROM (SELECT COUNT(*) AS a3_, a1_.a2_ AS a4_, a2_.a3_ AS a5_, a2_.a2_ AS a6_, a2_.a4_ AS a7_, a2_.a5_ AS a8_, WIDTH_BUCKET(a1_.a2_, (a2_.a3_)::BigDecimal * (1.01)::BigDecimal, a2_.a2_, a2_.a6_) AS a9_, a2_.a6_ AS a10_ FROM (SELECT a1_.a6_ AS a2_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.age AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_ AND (LOWER(a1_.name) LIKE 'employeea1' OR a5_ = a4_))) AS a1_) AS a1_, (SELECT DISTINCT MIN(a1_.a6_) AS a2_, MAX(a1_.a6_) AS a3_, AVG(a1_.a6_) AS a4_, STDDEV(a1_.a6_) AS a5_, LEAST(20,MAX(a1_.a6_) - MIN(a1_.a6_)) AS a6_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.age AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_ AND (LOWER(a1_.name) LIKE 'employeea1' OR a5_ = a4_))) AS a1_) AS a2_ GROUP BY a1_.a2_, a2_.a3_, a2_.a2_, a2_.a4_, a2_.a5_, a2_.a6_ ORDER BY a9_, a1_.a2_) AS a1_ GROUP BY a1_.a6_, a1_.a5_, a1_.a7_, a1_.a8_, a1_.a9_, a1_.a10_ ORDER BY a1_.a9_",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.fullTime AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_ AND (LOWER(a1_.name) LIKE 'employeea1' OR a5_ = a4_))) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a4_.address AS a6_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_, org.intermine.model.testmodel.Address AS a5_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a1_.address CONTAINS a5_ AND (LOWER(a1_.name) LIKE 'employeea1' OR a5_ = a4_))) AS a1_ GROUP BY a1_.a6_ ORDER BY COUNT(*) DESC");
    }

    public void test13() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.department.company.name Employee.age Employee.fullTime Employee.department.company.address.address\" constraintLogic=\"A and B and C\"><constraint path=\"Employee.name\" op=\"=\" value=\"EmployeeA1\" code=\"A\"/><constraint path=\"Employee.department.company.address\" op=\"=\" loopPath=\"Employee.address\" code=\"B\"/><constraint path=\"Employee.address.address\" op=\"!=\" value=\"fred\" code=\"C\"/></query>",
                "SELECT DISTINCT a1_, a2_, a3_, a4_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_ AND LOWER(a1_.name) LIKE 'employeea1' AND LOWER(a4_.address) NOT LIKE 'fred') ORDER BY a1_.name, a2_.name, a3_.name, a1_.age, a1_.fullTime, a4_.address",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.name AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_ AND LOWER(a1_.name) LIKE 'employeea1' AND LOWER(a4_.address) NOT LIKE 'fred')) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a2_.name AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_ AND LOWER(a1_.name) LIKE 'employeea1' AND LOWER(a4_.address) NOT LIKE 'fred')) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a3_.name AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_ AND LOWER(a1_.name) LIKE 'employeea1' AND LOWER(a4_.address) NOT LIKE 'fred')) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, a1_.a5_ AS a3_, a1_.a7_ AS a4_, a1_.a8_ AS a5_, a1_.a10_ AS a6_, a1_.a9_ AS a7_, SUM(a1_.a3_) AS a8_ FROM (SELECT COUNT(*) AS a3_, a1_.a2_ AS a4_, a2_.a3_ AS a5_, a2_.a2_ AS a6_, a2_.a4_ AS a7_, a2_.a5_ AS a8_, WIDTH_BUCKET(a1_.a2_, (a2_.a3_)::BigDecimal * (1.01)::BigDecimal, a2_.a2_, a2_.a6_) AS a9_, a2_.a6_ AS a10_ FROM (SELECT a1_.a5_ AS a2_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.age AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_ AND LOWER(a1_.name) LIKE 'employeea1' AND LOWER(a4_.address) NOT LIKE 'fred')) AS a1_) AS a1_, (SELECT DISTINCT MIN(a1_.a5_) AS a2_, MAX(a1_.a5_) AS a3_, AVG(a1_.a5_) AS a4_, STDDEV(a1_.a5_) AS a5_, LEAST(20,MAX(a1_.a5_) - MIN(a1_.a5_)) AS a6_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.age AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_ AND LOWER(a1_.name) LIKE 'employeea1' AND LOWER(a4_.address) NOT LIKE 'fred')) AS a1_) AS a2_ GROUP BY a1_.a2_, a2_.a3_, a2_.a2_, a2_.a4_, a2_.a5_, a2_.a6_ ORDER BY a9_, a1_.a2_) AS a1_ GROUP BY a1_.a6_, a1_.a5_, a1_.a7_, a1_.a8_, a1_.a9_, a1_.a10_ ORDER BY a1_.a9_",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a1_.fullTime AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_ AND LOWER(a1_.name) LIKE 'employeea1' AND LOWER(a4_.address) NOT LIKE 'fred')) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a4_, a4_.address AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_, org.intermine.model.testmodel.Address AS a4_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.address CONTAINS a4_ AND a3_.address CONTAINS a4_ AND LOWER(a1_.name) LIKE 'employeea1' AND LOWER(a4_.address) NOT LIKE 'fred')) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC");
    }

    public void test14() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.employees.name\"><constraint path=\"Employee.department.employees\" op=\"!=\" value=\"Employee\" code=\"A\"/></query>",
                "SELECT DISTINCT a1_, a3_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Employee AS a3_ WHERE (a1_.department CONTAINS a2_ AND a2_.employees CONTAINS a3_ AND a1_ != a3_) ORDER BY a1_.name, a3_.name",
                "SELECT DISTINCT a1_.a4_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a3_, a1_.name AS a4_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Employee AS a3_ WHERE (a1_.department CONTAINS a2_ AND a2_.employees CONTAINS a3_ AND a1_ != a3_)) AS a1_ GROUP BY a1_.a4_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a4_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a3_, a3_.name AS a4_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Employee AS a3_ WHERE (a1_.department CONTAINS a2_ AND a2_.employees CONTAINS a3_ AND a1_ != a3_)) AS a1_ GROUP BY a1_.a4_ ORDER BY COUNT(*) DESC");
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
                "SELECT DISTINCT a1_.a4_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a3_.name AS a4_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_)) AS a1_ GROUP BY a1_.a4_ ORDER BY COUNT(*) DESC");
    }

    public void test17() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Company.name Company:departments.name Company:departments:employees.name\"/>",
                "SELECT DISTINCT a1_, a1_.departments(SELECT default, default.employees(SELECT default)) AS a2_ FROM org.intermine.model.testmodel.Company AS a1_ ORDER BY a1_.name",
                "SELECT DISTINCT a1_.a2_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a2_ FROM org.intermine.model.testmodel.Company AS a1_) AS a1_ GROUP BY a1_.a2_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a2_.name AS a3_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a2_ WHERE a1_.departments CONTAINS a2_) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a4_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a3_.name AS a4_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Employee AS a3_ WHERE (a1_.departments CONTAINS a2_ AND a2_.employees CONTAINS a3_)) AS a1_ GROUP BY a1_.a4_ ORDER BY COUNT(*) DESC");
    }

    public void test18() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Company.name\"><constraint path=\"Company\" op=\"LOOKUP\" value=\"CompanyAkjhadf\"/></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Company AS a1_ WHERE a1_.id IN ? ORDER BY a1_.name 1: []",
                "SELECT DISTINCT a1_.a2_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a2_ FROM org.intermine.model.testmodel.Company AS a1_ WHERE a1_.id IN ? 1: []) AS a1_ GROUP BY a1_.a2_ ORDER BY COUNT(*) DESC");
    }

    public void test19() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Company.name Company:departments.name\"><join path=\"Company.departments\" style=\"OUTER\"/><constraint path=\"Company.departments.name\" op=\"=\" value=\"*1\"/></query>",
                "SELECT DISTINCT a1_, a1_.departments(SELECT default WHERE LOWER(default.name) LIKE '%1') AS a2_ FROM org.intermine.model.testmodel.Company AS a1_ ORDER BY a1_.name",
                "SELECT DISTINCT a1_.a2_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a2_ FROM org.intermine.model.testmodel.Company AS a1_) AS a1_ GROUP BY a1_.a2_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a2_.name AS a3_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a2_ WHERE (a1_.departments CONTAINS a2_ AND LOWER(a2_.name) LIKE '%1')) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC");
    }

    public void test20() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Company.name Company:departments.name Company:departments.employees.name\"/>",
                "SELECT DISTINCT a1_, a1_.departments(SELECT default, a1_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE default.employees CONTAINS a1_) AS a2_ FROM org.intermine.model.testmodel.Company AS a1_ ORDER BY a1_.name",
                "SELECT DISTINCT a1_.a2_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a2_ FROM org.intermine.model.testmodel.Company AS a1_) AS a1_ GROUP BY a1_.a2_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a4_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a2_.name AS a4_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Employee AS a3_ WHERE (a1_.departments CONTAINS a2_ AND a2_.employees CONTAINS a3_)) AS a1_ GROUP BY a1_.a4_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a4_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a3_.name AS a4_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Employee AS a3_ WHERE (a1_.departments CONTAINS a2_ AND a2_.employees CONTAINS a3_)) AS a1_ GROUP BY a1_.a4_ ORDER BY COUNT(*) DESC");
    }

    public void test21() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Department.name Department.company.name Department.company.departments.name\"><join path=\"Department.company\" style=\"OUTER\"/></query>",
                "SELECT DISTINCT a1_, a1_.company(SELECT default, a2_ FROM org.intermine.model.testmodel.Department AS a2_ WHERE default.departments CONTAINS a2_) AS a2_ FROM org.intermine.model.testmodel.Department AS a1_ ORDER BY a1_.name",
                "SELECT DISTINCT a1_.a2_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a2_ FROM org.intermine.model.testmodel.Department AS a1_) AS a1_ GROUP BY a1_.a2_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a4_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a2_.name AS a4_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Company AS a2_, org.intermine.model.testmodel.Department AS a3_ WHERE (a1_.company CONTAINS a2_ AND a2_.departments CONTAINS a3_)) AS a1_ GROUP BY a1_.a4_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a4_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a3_.name AS a4_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Company AS a2_, org.intermine.model.testmodel.Department AS a3_ WHERE (a1_.company CONTAINS a2_ AND a2_.departments CONTAINS a3_)) AS a1_ GROUP BY a1_.a4_ ORDER BY COUNT(*) DESC");
    }

    public void test22() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee:department:company.name\"/>",
                "SELECT DISTINCT a1_, a2_.0 AS a3_, a2_.1 AS a4_ FROM org.intermine.model.testmodel.Employee AS a1_ ORDER BY a1_.name PATH a1_.department(SELECT default, default.company) AS a2_",
                "SELECT DISTINCT a1_.a4_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a4_ FROM org.intermine.model.testmodel.Employee AS a1_) AS a1_ GROUP BY a1_.a4_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a4_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a3_.name AS a4_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_)) AS a1_ GROUP BY a1_.a4_ ORDER BY COUNT(*) DESC");
    }

    public void test23() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Department.name Department.company.name Department.company.departments.name\"><join path=\"Department.company\" style=\"OUTER\"/><constraint path=\"Department\" op=\"=\" loopPath=\"Department.company.departments\"/></query>",
                "PathQuery is invalid: [Loop constraint Department = Department.company.departments crosses an outer join]",
                "PathQuery is invalid: [Loop constraint Department = Department.company.departments crosses an outer join]",
                "PathQuery is invalid: [Loop constraint Department = Department.company.departments crosses an outer join]",
                "PathQuery is invalid: [Loop constraint Department = Department.company.departments crosses an outer join]");
    }

    public void test24() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Department.name Department.company.name Department.company.departments.name\"><constraint path=\"Department\" op=\"=\" loopPath=\"Department.company.departments\"/></query>",
                "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Company AS a2_ WHERE (a1_.company CONTAINS a2_ AND a2_.departments CONTAINS a1_) ORDER BY a1_.name, a2_.name",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a1_.name AS a3_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Company AS a2_ WHERE (a1_.company CONTAINS a2_ AND a2_.departments CONTAINS a1_)) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a2_.name AS a3_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Company AS a2_ WHERE (a1_.company CONTAINS a2_ AND a2_.departments CONTAINS a1_)) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a1_.name AS a3_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Company AS a2_ WHERE (a1_.company CONTAINS a2_ AND a2_.departments CONTAINS a1_)) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC");
    }

    public void test25() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Department.name Department:company.name Department:company:departments.name\"/>",
                "SELECT DISTINCT a1_, a2_.0 AS a3_, a2_.1 AS a4_ FROM org.intermine.model.testmodel.Department AS a1_ ORDER BY a1_.name PATH a1_.company(SELECT default, default.departments(SELECT default)) AS a2_",
                "SELECT DISTINCT a1_.a4_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a4_ FROM org.intermine.model.testmodel.Department AS a1_) AS a1_ GROUP BY a1_.a4_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a5_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a4_, a4_.name AS a5_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Company AS a4_ WHERE a1_.company CONTAINS a4_) AS a1_ GROUP BY a1_.a5_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a4_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a3_.name AS a4_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Company AS a2_, org.intermine.model.testmodel.Department AS a3_ WHERE (a1_.company CONTAINS a2_ AND a2_.departments CONTAINS a3_)) AS a1_ GROUP BY a1_.a4_ ORDER BY COUNT(*) DESC");
    }

    public void test26() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Company.name Company:departments.name Company:departments:company.name\"/>",
                "SELECT DISTINCT a1_, a1_.departments(SELECT default, default.company) AS a2_ FROM org.intermine.model.testmodel.Company AS a1_ ORDER BY a1_.name",
                "SELECT DISTINCT a1_.a2_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a2_ FROM org.intermine.model.testmodel.Company AS a1_) AS a1_ GROUP BY a1_.a2_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a2_.name AS a3_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a2_ WHERE a1_.departments CONTAINS a2_) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a4_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a3_.name AS a4_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_ WHERE (a1_.departments CONTAINS a2_ AND a2_.company CONTAINS a3_)) AS a1_ GROUP BY a1_.a4_ ORDER BY COUNT(*) DESC");
    }

    public void test27() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Company.name Company:contractors.name Company:departments.name Company:departments:employees.name\"/>",
                "SELECT DISTINCT a1_, a1_.contractors(SELECT default) AS a2_, a1_.departments(SELECT default, default.employees(SELECT default)) AS a3_ FROM org.intermine.model.testmodel.Company AS a1_ ORDER BY a1_.name",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a3_ FROM org.intermine.model.testmodel.Company AS a1_) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a4_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a3_, a3_.name AS a4_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Contractor AS a3_ WHERE a1_.contractors CONTAINS a3_) AS a1_ GROUP BY a1_.a4_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a4_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a3_, a3_.name AS a4_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a3_ WHERE a1_.departments CONTAINS a3_) AS a1_ GROUP BY a1_.a4_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a4_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a3_.name AS a4_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Employee AS a3_ WHERE (a1_.departments CONTAINS a2_ AND a2_.employees CONTAINS a3_)) AS a1_ GROUP BY a1_.a4_ ORDER BY COUNT(*) DESC");
    }

    public void test28() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.department.employees.name Employee:address.address\" sortOrder=\"Employee.name asc\"><constraint path=\"Employee.department.employees\" op=\"=\" loopPath=\"Employee\" code=\"A\"/></query>",
                "SELECT DISTINCT a1_, a2_, a1_.address AS a3_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_ WHERE (a1_.department CONTAINS a2_ AND a2_.employees CONTAINS a1_) ORDER BY a1_.name, a2_.name",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a1_.name AS a3_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_ WHERE (a1_.department CONTAINS a2_ AND a2_.employees CONTAINS a1_)) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a2_.name AS a3_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_ WHERE (a1_.department CONTAINS a2_ AND a2_.employees CONTAINS a1_)) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a1_.name AS a3_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_ WHERE (a1_.department CONTAINS a2_ AND a2_.employees CONTAINS a1_)) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a4_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a2_, a3_, a3_.address AS a4_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Address AS a3_ WHERE (a1_.department CONTAINS a2_ AND a2_.employees CONTAINS a1_ AND a1_.address CONTAINS a3_)) AS a1_ GROUP BY a1_.a4_ ORDER BY COUNT(*) DESC");
    }

    public void test29() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name Employee.age\" sortOrder=\"Employee.name asc\" constraintLogic=\"(A or B) and (C or D)\"><constraint path=\"Employee.name\" op=\"=\" value=\"EmployeeA1\" code=\"A\"/><constraint path=\"Employee.name\" op=\"=\" value=\"EmployeeA2\" code=\"B\"/><constraint path=\"Employee.age\" op=\"=\" value=\"10\" code=\"C\"/><constraint path=\"Employee.age\" op=\"=\" value=\"30\" code=\"D\"/></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE ((LOWER(a1_.name) LIKE 'employeea1' OR LOWER(a1_.name) LIKE 'employeea2') AND (a1_.age = 10 OR a1_.age = 30)) ORDER BY a1_.name, a1_.age",
                "SELECT DISTINCT a1_.a2_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a2_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE ((LOWER(a1_.name) LIKE 'employeea1' OR LOWER(a1_.name) LIKE 'employeea2') AND (a1_.age = 10 OR a1_.age = 30))) AS a1_ GROUP BY a1_.a2_ ORDER BY COUNT(*) DESC",
                "SELECT DISTINCT a1_.a6_ AS a2_, a1_.a5_ AS a3_, a1_.a7_ AS a4_, a1_.a8_ AS a5_, a1_.a10_ AS a6_, a1_.a9_ AS a7_, SUM(a1_.a3_) AS a8_ FROM (SELECT COUNT(*) AS a3_, a1_.a2_ AS a4_, a2_.a3_ AS a5_, a2_.a2_ AS a6_, a2_.a4_ AS a7_, a2_.a5_ AS a8_, WIDTH_BUCKET(a1_.a2_, (a2_.a3_)::BigDecimal * (1.01)::BigDecimal, a2_.a2_, a2_.a6_) AS a9_, a2_.a6_ AS a10_ FROM (SELECT a1_.a2_ AS a2_ FROM (SELECT DISTINCT a1_, a1_.age AS a2_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE ((LOWER(a1_.name) LIKE 'employeea1' OR LOWER(a1_.name) LIKE 'employeea2') AND (a1_.age = 10 OR a1_.age = 30))) AS a1_) AS a1_, (SELECT DISTINCT MIN(a1_.a2_) AS a2_, MAX(a1_.a2_) AS a3_, AVG(a1_.a2_) AS a4_, STDDEV(a1_.a2_) AS a5_, LEAST(20,MAX(a1_.a2_) - MIN(a1_.a2_)) AS a6_ FROM (SELECT DISTINCT a1_, a1_.age AS a2_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE ((LOWER(a1_.name) LIKE 'employeea1' OR LOWER(a1_.name) LIKE 'employeea2') AND (a1_.age = 10 OR a1_.age = 30))) AS a1_) AS a2_ GROUP BY a1_.a2_, a2_.a3_, a2_.a2_, a2_.a4_, a2_.a5_, a2_.a6_ ORDER BY a9_, a1_.a2_) AS a1_ GROUP BY a1_.a6_, a1_.a5_, a1_.a7_, a1_.a8_, a1_.a9_, a1_.a10_ ORDER BY a1_.a9_");
    }

    public void test30() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Department.name\" sortOrder=\"Department.name asc\"><constraint path=\"Department.employees\" type=\"CEO\"/></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.CEO AS a2_ WHERE a1_.employees CONTAINS a2_ ORDER BY a1_.name",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a3_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.CEO AS a2_ WHERE a1_.employees CONTAINS a2_) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC");
    }

    public void testRangeConstraintToSQL() throws Exception {
        MainHelper.RangeConfig.reset();
        MainHelper.RangeConfig.rangeHelpers.put(EmploymentPeriod.class, new DummyHelper());

        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name\" sortOrder=\"Employee.name asc\"><constraint path=\"Employee.employmentPeriod\" op=\"WITHIN\"><value>FOO</value></constraint></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.EmploymentPeriod AS a2_ WHERE (a1_.employmentPeriod CONTAINS a2_ AND 'Foo' = 'Bar') ORDER BY a1_.name",
                "SELECT DISTINCT a1_.a3_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a3_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.EmploymentPeriod AS a2_ WHERE (a1_.employmentPeriod CONTAINS a2_ AND 'Foo' = 'Bar')) AS a1_ GROUP BY a1_.a3_ ORDER BY COUNT(*) DESC");
    }

    public void testMultiTypeToSQL() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee.name\" sortOrder=\"Employee.name asc\"><constraint path=\"Employee\" op=\"ISA\"><value>CEO</value><value>Manager</value></constraint></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE a1_.class IN ? ORDER BY a1_.name 1: [class org.intermine.model.testmodel.CEO, class org.intermine.model.testmodel.Manager]",
                "SELECT DISTINCT a1_.a2_ AS a2_, COUNT(*) AS a3_ FROM (SELECT DISTINCT a1_, a1_.name AS a2_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE a1_.class IN ? 1: [class org.intermine.model.testmodel.CEO, class org.intermine.model.testmodel.Manager]) AS a1_ GROUP BY a1_.a2_ ORDER BY COUNT(*) DESC");
    }

    public void doQuery(String web, String iql, String ... summaries) throws Exception {
        Exception stacktrace = new Exception();
        stacktrace.fillInStackTrace();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        stacktrace.printStackTrace(pw);
        pw.flush();
        pw.close();
        StringReader sr = new StringReader(sw.toString());
        BufferedReader br = new BufferedReader(sr);
        br.readLine();
        br.readLine();
        String caller = br.readLine();
        System.out.println("Executing doQuery " + caller);
        try {
            Map<String, PathQuery> parsed = PathQueryBinding.unmarshalPathQueries(new StringReader(web), PathQuery.USERPROFILE_VERSION);
            PathQuery pq = parsed.get("test");
            Query q = MainHelper.makeQuery(pq, new HashMap(), null, bagQueryRunner, new HashMap());
            String got = q.toString();
            assertEquals("Expected: " + iql + ", but was: " + got, iql, got);
        } catch (Exception e) {
            if (!Arrays.asList(StringUtil.split(iql, "|")).contains(e.getMessage())) {
                throw e;
            }
        }
        int columnNo = 0;
        String summaryPath = null;
        try {
            Map<String, PathQuery> parsed = PathQueryBinding.unmarshalPathQueries(new StringReader(web), PathQuery.USERPROFILE_VERSION);
            PathQuery pq = parsed.get("test");
            for (String summary : summaries) {
                try {
                    summaryPath = pq.getView().get(columnNo);
                    Query q = MainHelper.makeSummaryQuery(pq, summaryPath,
                            new HashMap(), new HashMap(), bagQueryRunner);
                    String got = q.toString();
                    assertEquals("Failed for summaryPath " + summaryPath + ". Expected: " + summary + ", but was; " + got, summary, got);
                    summaryPath = null;
                } catch (Exception e) {
                    if (!Arrays.asList(StringUtil.split(summary, "|")).contains(e.getMessage())) {
                        throw e;
                    }
                } finally {
                    columnNo++;
                }
            }
            assertEquals("Columns do not have summary tests", pq.getView().size(), columnNo);
        } catch (Exception e) {
            throw new Exception("Exception while testing summaryPath " + summaryPath, e);
        }
    }
}
