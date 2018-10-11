package org.intermine.objectstore.querygen;

import java.util.*;

import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Manager;
import org.intermine.objectstore.*;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.*;
import org.intermine.objectstore.query.iql.IqlQuery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class QueryGenUtilTest {
    ObjectStore os;

    @Before
    public void setUp() throws Exception {
        ObjectStoreWriter osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        os = osw.getObjectStore();
        Map data = ObjectStoreTestUtils.getTestData("testmodel", "testmodel_data.xml");
        ObjectStoreTestUtils.storeData(osw, data);
        osw.close();
    }

    @Test
    public void testConstructQuerySingleRef() throws Exception {
        Query actual = QueryGenUtil.constructQuery(os.getModel(), "Employee department Department");

        Query q = new Query();
        q.setDistinct(false);
        QueryClass qcEmpl = new QueryClass(Employee.class);
        q.addToSelect(qcEmpl);
        q.addFrom(qcEmpl);

        QueryClass qcDept = new QueryClass(Department.class);
        q.addToSelect(qcDept);
        q.addFrom(qcDept);

        QueryObjectReference ref = new QueryObjectReference(qcEmpl, "department");
        ContainsConstraint cc = new ContainsConstraint(ref, ConstraintOp.CONTAINS, qcDept);
        q.setConstraint(cc);

        QueryAssert.assertEquals(q, actual);
    }

    @Test
    public void testConstructQueryTwoRefs() throws Exception {
        Query actual = QueryGenUtil.constructQuery(os.getModel(), "Company departments Department manager Manager");

        Query q = new Query();
        q.setDistinct(false);
        QueryClass qcCom = new QueryClass(Company.class);
        q.addToSelect(qcCom);
        q.addFrom(qcCom);

        QueryClass qcDept = new QueryClass(Department.class);
        q.addToSelect(qcDept);
        q.addFrom(qcDept);

        QueryClass qcMan = new QueryClass(Manager.class);
        q.addToSelect(qcMan);
        q.addFrom(qcMan);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference col = new QueryCollectionReference(qcCom, "departments");
        ContainsConstraint cc1 = new ContainsConstraint(col, ConstraintOp.CONTAINS, qcDept);
        cs.addConstraint(cc1);

        QueryObjectReference ref = new QueryObjectReference(qcDept, "manager");
        ContainsConstraint cc2 = new ContainsConstraint(ref, ConstraintOp.CONTAINS, qcMan);
        cs.addConstraint(cc2);

        q.setConstraint(cs);

        QueryAssert.assertEquals(q, actual);
    }

    @Test
    public void testConstructQueryTwoRefsWithOrder() throws Exception {
        Query actual = QueryGenUtil.constructQuery(os.getModel(), "Company departments Department manager Manager ORDER BY 1.name 2.name 3.name");

        Query q = new Query();
        q.setDistinct(false);
        QueryClass qcCom = new QueryClass(Company.class);
        q.addToSelect(qcCom);
        q.addFrom(qcCom);

        QueryClass qcDept = new QueryClass(Department.class);
        q.addToSelect(qcDept);
        q.addFrom(qcDept);

        QueryClass qcMan = new QueryClass(Manager.class);
        q.addToSelect(qcMan);
        q.addFrom(qcMan);

        QueryField comName = new QueryField(qcCom, "name");
        q.addToSelect(comName);
        q.addToOrderBy(comName);
        QueryField deptName = new QueryField(qcDept, "name");
        q.addToSelect(deptName);
        q.addToOrderBy(deptName);
        QueryField manName = new QueryField(qcMan, "name");
        q.addToSelect(manName);
        q.addToOrderBy(manName);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference col = new QueryCollectionReference(qcCom, "departments");
        ContainsConstraint cc1 = new ContainsConstraint(col, ConstraintOp.CONTAINS, qcDept);
        cs.addConstraint(cc1);

        QueryObjectReference ref = new QueryObjectReference(qcDept, "manager");
        ContainsConstraint cc2 = new ContainsConstraint(ref, ConstraintOp.CONTAINS, qcMan);
        cs.addConstraint(cc2);

        q.setConstraint(cs);

        QueryAssert.assertEquals(q, actual);
    }

    @Test
    public void testConstructQueryReverse() throws Exception {
        Query actual = QueryGenUtil.constructQuery(os.getModel(), "Company reverse.company Department");

        Query q = new IqlQuery("SELECT a1_, a2_ FROM Company AS a1_, Department AS a2_ WHERE a2_.company CONTAINS a1_", "org.intermine.model.testmodel").toQuery();

        QueryAssert.assertEquals(q, actual);
    }

    @Test
    public void testConstructQueryNumber() throws Exception {
        Query actual = QueryGenUtil.constructQuery(os.getModel(), "Company departments Department 1.address Address");

        Query q = new IqlQuery("SELECT a1_, a2_, a3_ FROM Company AS a1_, Department AS a2_, Address AS a3_ WHERE a1_.departments CONTAINS a2_ AND a1_.address CONTAINS a3_", "org.intermine.model.testmodel").toQuery();

        QueryAssert.assertEquals(q, actual);
    }

    @Test
    public void testValidatePath() throws Exception {
        try {
            QueryGenUtil.validatePath("Company", os.getModel()); // too short
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            QueryGenUtil.validatePath("Company departments Department manager", os.getModel());  // wrong length
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            QueryGenUtil.validatePath("Department manager Monkey", os.getModel());  // no Monkeys
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            QueryGenUtil.validatePath("Department teaboy Employee", os.getModel());  // no teaboys
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            QueryGenUtil.validatePath("Company reverse.departments Department", os.getModel()); // no departments collection in Department
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            QueryGenUtil.validatePath("Company 1.departments Department", os.getModel()); // reference to future
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        // valid
        QueryGenUtil.validatePath("Company departments Department", os.getModel());
        QueryGenUtil.validatePath("Company address Address", os.getModel());  // inherited reference
        QueryGenUtil.validatePath("Company departments Department manager Manager", os.getModel());
        QueryGenUtil.validatePath("Company reverse.company Department", os.getModel());
        QueryGenUtil.validatePath("Company departments Department 1.address Address", os.getModel());
    }

    @Test
    public void testExpandPathStart() throws Exception {
        String original = "+Employee department Department";

        String exp1 = "Employee department Department";
        String exp2 = "CEO department Department";
        String exp3 = "Manager department Department";

        Set expected = new LinkedHashSet(Arrays.asList(new Object[] {exp1, exp2, exp3}));
        Assert.assertEquals(expected, QueryGenUtil.expandPath(os, original));
    }

    @Test
    public void testExpandPathEnd() throws Exception {
        String original = "Company departments Department employees +Employee";

        String exp1 = "Company departments Department employees Employee";
        String exp2 = "Company departments Department employees Manager";
        String exp3 = "Company departments Department employees CEO";

        Set expected = new LinkedHashSet(Arrays.asList(new Object[] {exp1, exp2, exp3}));
        Assert.assertEquals(expected, QueryGenUtil.expandPath(os, original));
    }

    @Test
    public void testExpandPathMultiple() throws Exception {
        String original = "+Employee department Department employees +Employee";

        String exp1 = "Employee department Department employees Employee";
        String exp2 = "Employee department Department employees Manager";
        String exp3 = "Employee department Department employees CEO";
        String exp4 = "Manager department Department employees Employee";
        String exp5 = "Manager department Department employees Manager";
        String exp6 = "Manager department Department employees CEO";
        String exp7 = "CEO department Department employees Employee";
        String exp8 = "CEO department Department employees Manager";
        String exp9 = "CEO department Department employees CEO";

        Assert.assertEquals(new LinkedHashSet(Arrays.asList(exp1, exp2, exp3, exp4, exp5, exp6, exp7, exp8, exp9)), QueryGenUtil.expandPath(os, original));
    }

    @Test
    public void testExpandPathMultipleWithOrder() throws Exception {
        String original = "+Employee department Department employees +Employee ORDER BY 1.name 2.name 3.name";

        String exp1 = "Employee department Department employees Employee ORDER BY 1.name 2.name 3.name";
        String exp2 = "Employee department Department employees Manager ORDER BY 1.name 2.name 3.name";
        String exp3 = "Employee department Department employees CEO ORDER BY 1.name 2.name 3.name";
        String exp4 = "Manager department Department employees Employee ORDER BY 1.name 2.name 3.name";
        String exp5 = "Manager department Department employees Manager ORDER BY 1.name 2.name 3.name";
        String exp6 = "Manager department Department employees CEO ORDER BY 1.name 2.name 3.name";
        String exp7 = "CEO department Department employees Employee ORDER BY 1.name 2.name 3.name";
        String exp8 = "CEO department Department employees Manager ORDER BY 1.name 2.name 3.name";
        String exp9 = "CEO department Department employees CEO ORDER BY 1.name 2.name 3.name";

        Assert.assertEquals(new LinkedHashSet(Arrays.asList(exp1, exp2, exp3, exp4, exp5, exp6, exp7, exp8, exp9)), QueryGenUtil.expandPath(os, original));
    }

    @Test
    public void testGetClassNames() throws Exception {
        String clsName = "+Employee";
        Set expected = new HashSet(Arrays.asList(new String[] {"Employee", "Manager", "CEO"}));
        Assert.assertEquals(expected, QueryGenUtil.getClassNames(os, clsName));
    }

    @Test
    public void testGetClassNames2() throws Exception {
        String clsName = "Employee,Department";
        Set expected = new HashSet(Arrays.asList("Employee", "Department"));
        Assert.assertEquals(expected, QueryGenUtil.getClassNames(os, clsName));
    }

    @Test
    public void testGetClassNames3() throws Exception {
        String clsName = "+Employee,Department";
        Set expected = new HashSet(Arrays.asList("Employee", "Manager", "CEO", "Department"));
        Assert.assertEquals(expected, QueryGenUtil.getClassNames(os, clsName));
    }

    @Test
    public void testGetClassNames4() throws Exception {
        String clsName = "Department.employees.class";
        Set expected = new HashSet(Arrays.asList(new String[] {"Employee", "Manager", "CEO"}));
        Assert.assertEquals(expected, QueryGenUtil.getClassNames(os, clsName));
    }

    @Test
    public void testCreateQuery() throws Exception {
        String clsName = "Department.employees.class";
        Assert.assertEquals("SELECT DISTINCT a2_.class AS a3_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Employee AS a2_ WHERE a1_.employees CONTAINS a2_", QueryGenUtil.createClassFindingQuery(os.getModel(), clsName).getQuery().toString());
    }
}
