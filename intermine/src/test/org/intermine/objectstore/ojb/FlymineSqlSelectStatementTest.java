package org.flymine.objectstore.ojb;

import junit.framework.*;
import java.util.*;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryExpression;
import org.flymine.objectstore.query.QueryFunction;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.query.ConstraintSet;
import org.flymine.objectstore.query.SubqueryConstraint;
import org.flymine.objectstore.query.ClassConstraint;
import org.flymine.sql.Database;
import org.flymine.sql.DatabaseFactory;
import org.flymine.objectstore.ObjectStore;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;

import org.flymine.model.testmodel.Department;
import org.flymine.model.testmodel.Company;

public class FlymineSqlSelectStatementTest extends QueryTestCase
{
    private DescriptorRepository dr;

    public FlymineSqlSelectStatementTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        super.setUp();
        Database db = DatabaseFactory.getDatabase("db.unittest");
        ObjectStoreOjbImpl os = ObjectStoreOjbImpl.getInstance(db);
        PersistenceBroker broker = os.getPersistenceBroker();
        dr = broker.getDescriptorRepository();
    }

    public void setUpResults() throws Exception {
        results.put("SubQuery", "SELECT DISTINCT a1_.a1_name AS a2_, a1_.a2_ AS a3_ FROM (SELECT DISTINCT a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, 5 AS a2_ FROM Company AS a1_) AS a1_ ORDER BY a1_.a1_name, a1_.a2_ LIMIT 10000 OFFSET 0");
        results.put("WhereSimpleEquals", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE a1_.vatNumber = 1234 ORDER BY a1_.name LIMIT 10000 OFFSET 0");
        results.put("WhereSimpleNotEquals", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE a1_.vatNumber != 5 ORDER BY a1_.name LIMIT 10000 OFFSET 0");
        results.put("WhereSimpleLike", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE a1_.name LIKE 'Company%' ORDER BY a1_.name LIMIT 10000 OFFSET 0");
        results.put("WhereEqualsString", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE a1_.name = 'CompanyA' ORDER BY a1_.name LIMIT 10000 OFFSET 0");
        results.put("WhereAndSet", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE (a1_.name LIKE 'Company%' AND a1_.vatNumber > 2000) ORDER BY a1_.name LIMIT 10000 OFFSET 0");
        results.put("WhereOrSet", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE (a1_.name LIKE 'CompanyA%' OR a1_.vatNumber > 2000) ORDER BY a1_.name LIMIT 10000 OFFSET 0");
        results.put("WhereNotSet", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE ( NOT (a1_.name LIKE 'Company%' AND a1_.vatNumber > 2000)) ORDER BY a1_.name LIMIT 10000 OFFSET 0");
        results.put("WhereSubQueryField", "SELECT DISTINCT a1_.ID AS a1_ID, a1_.companyId AS a1_companyId, a1_.managerId AS a1_managerId, a1_.name AS a1_name FROM Department AS a1_ WHERE a1_.name IN (SELECT DISTINCT a1_.name AS a2_ FROM Department AS a1_) ORDER BY a1_.ID LIMIT 10000 OFFSET 0");
        results.put("WhereSubQueryClass", "SELECT DISTINCT a1_.ID AS a1_ID, a1_.companyId AS a1_companyId, a1_.managerId AS a1_managerId, a1_.name AS a1_name FROM Department AS a1_ WHERE a1_.ID IN (SELECT DISTINCT a1_.ID AS a1_ID FROM Company AS a1_ WHERE a1_.name = 'CompanyA') ORDER BY a1_.ID LIMIT 10000 OFFSET 0");
        results.put("WhereNotSubQueryClass", "SELECT DISTINCT a1_.ID AS a1_ID, a1_.companyId AS a1_companyId, a1_.managerId AS a1_managerId, a1_.name AS a1_name FROM Department AS a1_ WHERE a1_.ID NOT IN (SELECT DISTINCT a1_.ID AS a1_ID FROM Company AS a1_ WHERE a1_.name = 'CompanyA') ORDER BY a1_.ID LIMIT 10000 OFFSET 0");
        results.put("WhereNegSubQueryClass", "SELECT DISTINCT a1_.ID AS a1_ID, a1_.companyId AS a1_companyId, a1_.managerId AS a1_managerId, a1_.name AS a1_name FROM Department AS a1_ WHERE a1_.ID NOT IN (SELECT DISTINCT a1_.ID AS a1_ID FROM Company AS a1_ WHERE a1_.name = 'CompanyA') ORDER BY a1_.ID LIMIT 10000 OFFSET 0");
        results.put("WhereClassClass", "SELECT DISTINCT a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, a2_.ID AS a2_ID, a2_.addressId AS a2_addressId, a2_.name AS a2_name, a2_.vatNumber AS a2_vatNumber FROM Company AS a1_, Company AS a2_ WHERE (a1_.ID = a2_.ID) ORDER BY a1_.ID, a2_.ID LIMIT 10000 OFFSET 0");
        results.put("WhereNotClassClass", "SELECT DISTINCT a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, a2_.ID AS a2_ID, a2_.addressId AS a2_addressId, a2_.name AS a2_name, a2_.vatNumber AS a2_vatNumber FROM Company AS a1_, Company AS a2_ WHERE ( NOT (a1_.ID = a2_.ID)) ORDER BY a1_.ID, a2_.ID LIMIT 10000 OFFSET 0");
        results.put("WhereNegClassClass", "SELECT DISTINCT a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, a2_.ID AS a2_ID, a2_.addressId AS a2_addressId, a2_.name AS a2_name, a2_.vatNumber AS a2_vatNumber FROM Company AS a1_, Company AS a2_ WHERE ( NOT (a1_.ID = a2_.ID)) ORDER BY a1_.ID, a2_.ID LIMIT 10000 OFFSET 0");
        results.put("WhereClassObject", "SELECT DISTINCT a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber FROM Company AS a1_ WHERE (a1_.ID = 2345) ORDER BY a1_.ID LIMIT 10000 OFFSET 0");

    }

    public void executeTest(String type) throws Exception {
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement((Query) queries.get(type), dr, 0, 10000);
        assertEquals(type + " has failed", results.get(type), s1.getStatement());
    }


    public void testSelectQueryValue() throws Exception {
        QueryValue v1 = new QueryValue(new Integer(5));
        QueryValue v2 = new QueryValue("Hello");
        QueryValue v3 = new QueryValue(new Date(1046275720000l));
        QueryValue v4 = new QueryValue(Boolean.TRUE);
        Query q1 = new Query();
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q1, null);
        assertEquals(s1.queryEvaluableToString(v1), "5");
        assertEquals(s1.queryEvaluableToString(v2), "'Hello'");
        assertEquals(s1.queryEvaluableToString(v3), "'2003-02-26 16:08:40.000'");
        assertEquals(s1.queryEvaluableToString(v4), "1");
    }

    public void testSelectQueryExpression() throws Exception {
        QueryValue v1 = new QueryValue(new Integer(5));
        QueryValue v2 = new QueryValue(new Integer(7));
        QueryExpression e1 = new QueryExpression(v1, QueryExpression.ADD, v2);
        QueryExpression e2 = new QueryExpression(v1, QueryExpression.SUBTRACT, v2);
        QueryExpression e3 = new QueryExpression(v1, QueryExpression.MULTIPLY, v2);
        QueryExpression e4 = new QueryExpression(v1, QueryExpression.DIVIDE, v2);
        Query q1 = new Query();
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q1, null);
        assertEquals(s1.queryEvaluableToString(e1), "(5 + 7)");
        assertEquals(s1.queryEvaluableToString(e2), "(5 - 7)");
        assertEquals(s1.queryEvaluableToString(e3), "(5 * 7)");
        assertEquals(s1.queryEvaluableToString(e4), "(5 / 7)");
    }

    public void testSelectQuerySubstringExpression() throws Exception {
        QueryValue v1 = new QueryValue("Hello");
        QueryValue v2 = new QueryValue(new Integer(3));
        QueryValue v3 = new QueryValue(new Integer(5));
        QueryExpression e1 = new QueryExpression(v1, v2, v3);
        Query q1 = new Query();
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q1, null);
        assertEquals(s1.queryEvaluableToString(e1), "Substr('Hello', 3, 5)");
    }

    public void testSelectQueryField() throws Exception {
        QueryClass c1 = new QueryClass(Department.class);
        QueryField f1 = new QueryField(c1, "name");
        Query q1 = new Query();
        q1.addFrom(c1);
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q1, dr);
        assertEquals("a1_.name", s1.queryEvaluableToString(f1));
    }

    public void testSelectQueryFunction() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryField v1 = new QueryField(c1, "vatNumber");
        QueryFunction f1 = new QueryFunction();
        QueryFunction f2 = new QueryFunction(v1, QueryFunction.SUM);
        QueryFunction f3 = new QueryFunction(v1, QueryFunction.AVERAGE);
        QueryFunction f4 = new QueryFunction(v1, QueryFunction.MIN);
        QueryFunction f5 = new QueryFunction(v1, QueryFunction.MAX);
        Query q1 = new Query();
        q1.addFrom(c1);
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q1, null);
        assertEquals("COUNT(*)", s1.queryEvaluableToString(f1));
        assertEquals("SUM(a1_.vatNumber)", s1.queryEvaluableToString(f2));
        assertEquals("AVG(a1_.vatNumber)", s1.queryEvaluableToString(f3));
        assertEquals("MIN(a1_.vatNumber)", s1.queryEvaluableToString(f4));
        assertEquals("MAX(a1_.vatNumber)", s1.queryEvaluableToString(f5));
    }

    public void testSelectQueryClass() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q1, dr);
        String result = s1.queryClassToString(c1, true, false);
        assertEquals("a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber", result);
    }

    public void testSelectComplex() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryClass c2 = new QueryClass(Department.class);
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        QueryField f3 = new QueryField(c2, "name");
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addFrom(c2);
        q1.addToSelect(c1);
        QueryExpression e1 = new QueryExpression(new QueryFunction(f2, QueryFunction.AVERAGE),
                QueryExpression.ADD, new QueryValue(new Integer(20)));
        q1.addToSelect(e1);
        q1.addToSelect(f3);
        q1.addToSelect(c2);
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q1, dr);
        assertEquals("a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, (AVG(a1_.vatNumber) + 20) AS a3_, a2_.name AS a4_, a2_.ID AS a2_ID, a2_.companyId AS a2_companyId, a2_.managerId AS a2_managerId, a2_.name AS a2_name", s1.buildSelectComponent());
    }

    public void testFromQueryClass() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q1, dr);
        assertEquals("Company AS a1_", s1.buildFromComponent());
    }

    public void testFromSubquery() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        Query q2 = new Query();
        q2.addFrom(q1);
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q2, dr);
        assertEquals("(SELECT DISTINCT a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber FROM Company AS a1_) AS a1_", s1.buildFromComponent());
    }

    public void testFromMulti() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryClass c2 = new QueryClass(Department.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addFrom(q1);
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q2, dr);
        org.flymine.sql.query.Query oq1 = new org.flymine.sql.query.Query("SELECT DISTINCT 5 as a from (SELECT DISTINCT a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber FROM Company AS a1_) AS a2_, Department as a1_");
        org.flymine.sql.query.Query oq2 = new org.flymine.sql.query.Query("SELECT DISTINCT 5 as a from " + s1.buildFromComponent());
        assertEquals(oq1, oq2);
    }

}
