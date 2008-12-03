package org.intermine.objectstore.querygen;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import junit.framework.Test;

import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Manager;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.StoreDataTestCase;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryObjectReference;

public class QueryGenUtilTest extends StoreDataTestCase {
    ObjectStore os;

    public QueryGenUtilTest (String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();

        os = ObjectStoreFactory.getObjectStore("os.unittest");
    }

    public static void oneTimeSetUp() throws Exception {
        StoreDataTestCase.oneTimeSetUp();
    }

    public void executeTest(String type) {
    }

    public void testQueries() throws Throwable {
    }

    public static Test suite() {
        return buildSuite(QueryGenUtilTest.class);
    }
    public void testConstructQuerySingleRef() throws Exception {
        Query actual = QueryGenUtil.constructQuery(os.getModel(), "Employee department Department");

        Query q = new Query();
        QueryClass qcEmpl = new QueryClass(Employee.class);
        q.addToSelect(qcEmpl);
        q.addFrom(qcEmpl);
        q.addToOrderBy(qcEmpl);

        QueryClass qcDept = new QueryClass(Department.class);
        q.addToSelect(qcDept);
        q.addFrom(qcDept);
        q.addToOrderBy(qcDept);

        QueryObjectReference ref = new QueryObjectReference(qcEmpl, "department");
        ContainsConstraint cc = new ContainsConstraint(ref, ConstraintOp.CONTAINS, qcDept);
        q.setConstraint(cc);

        assertEquals(q, actual);
    }


    public void testConstructQueryTwoRefs() throws Exception {
        Query actual = QueryGenUtil.constructQuery(os.getModel(), "Company departments Department manager Manager");

        Query q = new Query();
        QueryClass qcCom = new QueryClass(Company.class);
        q.addToSelect(qcCom);
        q.addFrom(qcCom);
        q.addToOrderBy(qcCom);

        QueryClass qcDept = new QueryClass(Department.class);
        q.addToSelect(qcDept);
        q.addFrom(qcDept);
        q.addToOrderBy(qcDept);

        QueryClass qcMan = new QueryClass(Manager.class);
        q.addToSelect(qcMan);
        q.addFrom(qcMan);
        q.addToOrderBy(qcMan);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference col = new QueryCollectionReference(qcCom, "departments");
        ContainsConstraint cc1 = new ContainsConstraint(col, ConstraintOp.CONTAINS, qcDept);
        cs.addConstraint(cc1);

        QueryObjectReference ref = new QueryObjectReference(qcDept, "manager");
        ContainsConstraint cc2 = new ContainsConstraint(ref, ConstraintOp.CONTAINS, qcMan);
        cs.addConstraint(cc2);

        q.setConstraint(cs);

        assertEquals(q, actual);
    }



    public void testValidatePath() throws Exception {
        try {
            QueryGenUtil.validatePath("Company", os.getModel()); // too short
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            QueryGenUtil.validatePath("Company departments Department manager", os.getModel());  // wrong length
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            QueryGenUtil.validatePath("Department manager Monkey", os.getModel());  // no Monkeys
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            QueryGenUtil.validatePath("Department teaboy Employee", os.getModel());  // no teaboys
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        // valid
        QueryGenUtil.validatePath("Company departments Department", os.getModel());
        QueryGenUtil.validatePath("Company address Address", os.getModel());  // inherited reference
        QueryGenUtil.validatePath("Company departments Department manager Manager", os.getModel());
    }


    public void testExpandPathStart() throws Exception {
        String original = "+Employee department Department";

        String exp1 = "Employee department Department";
        String exp2 = "CEO department Department";
        String exp3 = "Manager department Department";

        Set expected = new LinkedHashSet(Arrays.asList(new Object[] {exp1, exp2, exp3}));
        assertEquals(expected, QueryGenUtil.expandPath(os.getModel(), original));
    }


    public void testExpandPathEnd() throws Exception {
        String original = "Company departments Department employees +Employee";

        String exp1 = "Company departments Department employees Employee";
        String exp2 = "Company departments Department employees Manager";
        String exp3 = "Company departments Department employees CEO";

        Set expected = new LinkedHashSet(Arrays.asList(new Object[] {exp1, exp2, exp3}));
        assertEquals(expected, QueryGenUtil.expandPath(os.getModel(), original));
    }



    public void testGetClassNames() throws Exception {
        String clsName = "+Employee";
        Set expected = new HashSet(Arrays.asList(new String[] {"Employee", "Manager", "CEO"}));
        assertEquals(expected, QueryGenUtil.getClassNames(os.getModel(), clsName));
    }

}
