package org.flymine.objectstore.ojb;

import java.util.Date;
import java.util.Vector;
import java.util.Iterator;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.*;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ObjectStoreQueriesTestCase;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryExpression;
import org.flymine.objectstore.query.QueryFunction;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.util.TypeUtil;

import org.flymine.model.testmodel.*;

public class FlymineSqlSelectStatementTest extends ObjectStoreQueriesTestCase
{
    protected DescriptorRepository dr;

    public FlymineSqlSelectStatementTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        PersistenceBroker pb = ((ObjectStoreOjbImpl) os).getPersistenceBroker();
        dr = ((PersistenceBrokerFlyMine) pb).getDescriptorRepository();
        super.setUp();
    }

    public void setUpResults() throws Exception {
        results.put("SelectSimpleObject", "SELECT DISTINCT a1_.CEOId AS a1_CEOId, a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber FROM Company AS a1_ ORDER BY a1_.ID");
        results.put("SubQuery", "SELECT DISTINCT a1_.a1_name AS a2_, a1_.a2_ AS a3_ FROM (SELECT DISTINCT a1_.CEOId AS a1_CEOId, a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, 5 AS a2_ FROM Company AS a1_) AS a1_ ORDER BY a1_.a1_name, a1_.a2_");
        results.put("WhereSimpleEquals", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE a1_.vatNumber = 1234 ORDER BY a1_.name");
        results.put("WhereSimpleNotEquals", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE a1_.vatNumber != 1234 ORDER BY a1_.name");
        results.put("WhereSimpleLike", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE a1_.name LIKE 'Company%' ORDER BY a1_.name");
        results.put("WhereEqualsString", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE a1_.name = 'CompanyA' ORDER BY a1_.name");
        results.put("WhereAndSet", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE (a1_.name LIKE 'Company%' AND a1_.vatNumber > 2000) ORDER BY a1_.name");
        results.put("WhereOrSet", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE (a1_.name LIKE 'CompanyA%' OR a1_.vatNumber > 2000) ORDER BY a1_.name");
        results.put("WhereNotSet", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE ( NOT (a1_.name LIKE 'Company%' AND a1_.vatNumber > 2000)) ORDER BY a1_.name");
        results.put("WhereSubQueryField", "SELECT DISTINCT a1_.ID AS a1_ID, a1_.companyId AS a1_companyId, a1_.managerId AS a1_managerId, a1_.name AS a1_name FROM Department AS a1_ WHERE a1_.name IN (SELECT DISTINCT a1_.name AS a2_ FROM Department AS a1_) ORDER BY a1_.name, a1_.ID");
        results.put("WhereSubQueryClass", "SELECT DISTINCT a1_.CEOId AS a1_CEOId, a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber FROM Company AS a1_ WHERE a1_.ID IN (SELECT DISTINCT a1_.ID AS a1_ID FROM Company AS a1_ WHERE a1_.name = 'CompanyA') ORDER BY a1_.ID");
        results.put("WhereNotSubQueryClass", "SELECT DISTINCT a1_.CEOId AS a1_CEOId, a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber FROM Company AS a1_ WHERE a1_.ID NOT IN (SELECT DISTINCT a1_.ID AS a1_ID FROM Company AS a1_ WHERE a1_.name = 'CompanyA') ORDER BY a1_.ID");
        results.put("WhereNegSubQueryClass", "SELECT DISTINCT a1_.CEOId AS a1_CEOId, a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber FROM Company AS a1_ WHERE a1_.ID NOT IN (SELECT DISTINCT a1_.ID AS a1_ID FROM Company AS a1_ WHERE a1_.name = 'CompanyA') ORDER BY a1_.ID");
        results.put("WhereClassClass", "SELECT DISTINCT a1_.CEOId AS a1_CEOId, a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, a2_.CEOId AS a2_CEOId, a2_.ID AS a2_ID, a2_.addressId AS a2_addressId, a2_.name AS a2_name, a2_.vatNumber AS a2_vatNumber FROM Company AS a1_, Company AS a2_ WHERE (a1_.ID = a2_.ID) ORDER BY a1_.ID, a2_.ID");
        results.put("WhereNotClassClass", "SELECT DISTINCT a1_.CEOId AS a1_CEOId, a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, a2_.CEOId AS a2_CEOId, a2_.ID AS a2_ID, a2_.addressId AS a2_addressId, a2_.name AS a2_name, a2_.vatNumber AS a2_vatNumber FROM Company AS a1_, Company AS a2_ WHERE ( NOT (a1_.ID = a2_.ID)) ORDER BY a1_.ID, a2_.ID");
        results.put("WhereNegClassClass", "SELECT DISTINCT a1_.CEOId AS a1_CEOId, a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, a2_.CEOId AS a2_CEOId, a2_.ID AS a2_ID, a2_.addressId AS a2_addressId, a2_.name AS a2_name, a2_.vatNumber AS a2_vatNumber FROM Company AS a1_, Company AS a2_ WHERE ( NOT (a1_.ID = a2_.ID)) ORDER BY a1_.ID, a2_.ID");
        Integer id = (Integer) TypeUtil.getFieldValue(data.get("CompanyA"), "id");
        results.put("WhereClassObject", "SELECT DISTINCT a1_.CEOId AS a1_CEOId, a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber FROM Company AS a1_ WHERE (a1_.ID = " + id + ") ORDER BY a1_.ID");
        results.put("Contains11", "SELECT DISTINCT a1_.ID AS a1_ID, a1_.companyId AS a1_companyId, a1_.managerId AS a1_managerId, a1_.name AS a1_name, a2_.CLASS AS a2_CLASS, a2_.ID AS a2_ID, a2_.addressId AS a2_addressId, a2_.age AS a2_age, a2_.companyId AS a2_companyId, a2_.departmentId AS a2_departmentId, a2_.departmentThatRejectedMeId AS a2_departmentThatRejectedMeId, a2_.fullTime AS a2_fullTime, a2_.name AS a2_name, a2_.salary AS a2_salary, a2_.title AS a2_title FROM Department AS a1_, Employee AS a2_ WHERE (a2_.CLASS = 'org.flymine.model.testmodel.CEO' OR a2_.CLASS = 'org.flymine.model.testmodel.Manager') AND ((a1_.managerId = a2_.ID) AND a1_.name = 'DepartmentA1') ORDER BY a1_.ID, a2_.ID");
        results.put("ContainsNot11", "SELECT DISTINCT a1_.ID AS a1_ID, a1_.companyId AS a1_companyId, a1_.managerId AS a1_managerId, a1_.name AS a1_name, a2_.CLASS AS a2_CLASS, a2_.ID AS a2_ID, a2_.addressId AS a2_addressId, a2_.age AS a2_age, a2_.companyId AS a2_companyId, a2_.departmentId AS a2_departmentId, a2_.departmentThatRejectedMeId AS a2_departmentThatRejectedMeId, a2_.fullTime AS a2_fullTime, a2_.name AS a2_name, a2_.salary AS a2_salary, a2_.title AS a2_title FROM Department AS a1_, Employee AS a2_ WHERE (a2_.CLASS = 'org.flymine.model.testmodel.CEO' OR a2_.CLASS = 'org.flymine.model.testmodel.Manager') AND (( NOT (a1_.managerId = a2_.ID)) AND a1_.name = 'DepartmentA1') ORDER BY a1_.ID, a2_.ID");
        results.put("ContainsNeg11", "SELECT DISTINCT a1_.ID AS a1_ID, a1_.companyId AS a1_companyId, a1_.managerId AS a1_managerId, a1_.name AS a1_name, a2_.CLASS AS a2_CLASS, a2_.ID AS a2_ID, a2_.addressId AS a2_addressId, a2_.age AS a2_age, a2_.companyId AS a2_companyId, a2_.departmentId AS a2_departmentId, a2_.departmentThatRejectedMeId AS a2_departmentThatRejectedMeId, a2_.fullTime AS a2_fullTime, a2_.name AS a2_name, a2_.salary AS a2_salary, a2_.title AS a2_title FROM Department AS a1_, Employee AS a2_ WHERE (a2_.CLASS = 'org.flymine.model.testmodel.CEO' OR a2_.CLASS = 'org.flymine.model.testmodel.Manager') AND (( NOT (a1_.managerId = a2_.ID)) AND a1_.name = 'DepartmentA1') ORDER BY a1_.ID, a2_.ID");
        results.put("Contains1N", "SELECT DISTINCT a1_.CEOId AS a1_CEOId, a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, a2_.ID AS a2_ID, a2_.companyId AS a2_companyId, a2_.managerId AS a2_managerId, a2_.name AS a2_name FROM Company AS a1_, Department AS a2_ WHERE ((a1_.ID = a2_.companyId) AND a1_.name = 'CompanyA') ORDER BY a1_.ID, a2_.ID");
        results.put("ContainsN1", "SELECT DISTINCT a1_.ID AS a1_ID, a1_.companyId AS a1_companyId, a1_.managerId AS a1_managerId, a1_.name AS a1_name, a2_.CEOId AS a2_CEOId, a2_.ID AS a2_ID, a2_.addressId AS a2_addressId, a2_.name AS a2_name, a2_.vatNumber AS a2_vatNumber FROM Department AS a1_, Company AS a2_ WHERE ((a1_.companyId = a2_.ID) AND a2_.name = 'CompanyA') ORDER BY a1_.ID, a2_.ID");
        results.put("ContainsMN", "SELECT DISTINCT a1_.ID AS a1_ID, a1_.businessAddressId AS a1_businessAddressId, a1_.name AS a1_name, a1_.personalAddressId AS a1_personalAddressId, a2_.CEOId AS a2_CEOId, a2_.ID AS a2_ID, a2_.addressId AS a2_addressId, a2_.name AS a2_name, a2_.vatNumber AS a2_vatNumber FROM Contractor AS a1_, Company AS a2_, CompanyContractor AS ind_a1_a2_CompanyContractor_ WHERE ((a1_.ID = ind_a1_a2_CompanyContractor_.contractorId AND a2_.ID = ind_a1_a2_CompanyContractor_.companyId) AND a1_.name = 'ContractorA') ORDER BY a1_.ID, a2_.ID");
        results.put("ContainsDuplicatesMN", "SELECT DISTINCT a1_.ID AS a1_ID, a1_.businessAddressId AS a1_businessAddressId, a1_.name AS a1_name, a1_.personalAddressId AS a1_personalAddressId, a2_.CEOId AS a2_CEOId, a2_.ID AS a2_ID, a2_.addressId AS a2_addressId, a2_.name AS a2_name, a2_.vatNumber AS a2_vatNumber FROM Contractor AS a1_, Company AS a2_, OldComOldContract AS ind_a1_a2_OldComOldContract_ WHERE (a1_.ID = ind_a1_a2_OldComOldContract_.oldContractId AND a2_.ID = ind_a1_a2_OldComOldContract_.oldComId) ORDER BY a1_.ID, a2_.ID");
        results.put("SimpleGroupBy", "SELECT DISTINCT a1_.CEOId AS a1_CEOId, a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, COUNT(*) AS a2_ FROM Company AS a1_, Department AS a3_ WHERE (a1_.ID = a3_.companyId) GROUP BY a1_.CEOId, a1_.ID, a1_.addressId, a1_.name, a1_.vatNumber ORDER BY a1_.ID, COUNT(*)");
        results.put("MultiJoin", "SELECT DISTINCT a1_.CEOId AS a1_CEOId, a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, a2_.ID AS a2_ID, a2_.companyId AS a2_companyId, a2_.managerId AS a2_managerId, a2_.name AS a2_name, a3_.CLASS AS a3_CLASS, a3_.ID AS a3_ID, a3_.addressId AS a3_addressId, a3_.age AS a3_age, a3_.companyId AS a3_companyId, a3_.departmentId AS a3_departmentId, a3_.departmentThatRejectedMeId AS a3_departmentThatRejectedMeId, a3_.fullTime AS a3_fullTime, a3_.name AS a3_name, a3_.salary AS a3_salary, a3_.title AS a3_title, a4_.ID AS a4_ID, a4_.address AS a4_address FROM Company AS a1_, Department AS a2_, Employee AS a3_, Address AS a4_ WHERE (a3_.CLASS = 'org.flymine.model.testmodel.CEO' OR a3_.CLASS = 'org.flymine.model.testmodel.Manager') AND ((a1_.ID = a2_.companyId) AND (a2_.managerId = a3_.ID) AND (a3_.addressId = a4_.ID) AND a3_.name = 'EmployeeA1') ORDER BY a1_.ID, a2_.ID, a3_.ID, a4_.ID");
        results.put("SelectComplex", "SELECT DISTINCT (AVG(a1_.vatNumber) + 20) AS a3_, a2_.name AS a4_, a2_.ID AS a2_ID, a2_.companyId AS a2_companyId, a2_.managerId AS a2_managerId, a2_.name AS a2_name FROM Company AS a1_, Department AS a2_ GROUP BY a2_.ID, a2_.companyId, a2_.managerId, a2_.name ORDER BY (AVG(a1_.vatNumber) + 20), a2_.name, a2_.ID");
        results.put("SelectClassAndSubClasses", "SELECT DISTINCT a1_.CLASS AS a1_CLASS, a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.age AS a1_age, a1_.companyId AS a1_companyId, a1_.departmentId AS a1_departmentId, a1_.departmentThatRejectedMeId AS a1_departmentThatRejectedMeId, a1_.fullTime AS a1_fullTime, a1_.name AS a1_name, a1_.salary AS a1_salary, a1_.title AS a1_title FROM Employee AS a1_ ORDER BY a1_.name, a1_.ID");
        results.put("SelectInterfaceAndSubClasses", "SELECT DISTINCT a1_.CLASS AS a1_CLASS, a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.age AS a1_age, a1_.businessAddressId AS a1_businessAddressId, a1_.companyId AS a1_companyId, a1_.departmentId AS a1_departmentId, a1_.departmentThatRejectedMeId AS a1_departmentThatRejectedMeId, a1_.fullTime AS a1_fullTime, a1_.name AS a1_name, a1_.personalAddressId AS a1_personalAddressId, a1_.salary AS a1_salary, a1_.title AS a1_title FROM (SELECT 'org.flymine.model.testmodel.Contractor' AS CLASS, ID, NULL AS addressId, NULL AS age, businessAddressId, NULL AS companyId, NULL AS departmentId, NULL AS departmentThatRejectedMeId, NULL AS fullTime, name, personalAddressId, NULL AS salary, NULL AS title FROM Contractor UNION SELECT CLASS, ID, addressId, age, NULL AS businessAddressId, companyId, departmentId, departmentThatRejectedMeId, fullTime, name, NULL AS personalAddressId, salary, title FROM Employee) AS a1_ ORDER BY a1_.ID");
        results.put("SelectInterfaceAndSubClasses2", "SELECT DISTINCT a1_.CEOId AS a1_CEOId, a1_.CLASS AS a1_CLASS, a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.companyId AS a1_companyId, a1_.managerId AS a1_managerId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber FROM (SELECT CEOId, 'org.flymine.model.testmodel.Company' AS CLASS, ID, addressId, NULL AS companyId, NULL AS managerId, name, vatNumber FROM Company UNION SELECT NULL AS CEOId, 'org.flymine.model.testmodel.Department' AS CLASS, ID, NULL AS addressId, companyId, managerId, name, NULL AS vatNumber FROM Department) AS a1_ ORDER BY a1_.ID");
        results.put("SelectInterfaceAndSubClasses3", "SELECT DISTINCT a1_.CLASS AS a1_CLASS, a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.age AS a1_age, a1_.businessAddressId AS a1_businessAddressId, a1_.companyId AS a1_companyId, a1_.departmentId AS a1_departmentId, a1_.departmentThatRejectedMeId AS a1_departmentThatRejectedMeId, a1_.fullTime AS a1_fullTime, a1_.name AS a1_name, a1_.personalAddressId AS a1_personalAddressId, a1_.salary AS a1_salary, a1_.title AS a1_title FROM (SELECT 'org.flymine.model.testmodel.Contractor' AS CLASS, ID, NULL AS addressId, NULL AS age, businessAddressId, NULL AS companyId, NULL AS departmentId, NULL AS departmentThatRejectedMeId, NULL AS fullTime, name, personalAddressId, NULL AS salary, NULL AS title FROM Contractor UNION SELECT CLASS, ID, addressId, age, NULL AS businessAddressId, companyId, departmentId, departmentThatRejectedMeId, fullTime, name, NULL AS personalAddressId, salary, title FROM Employee WHERE CLASS = 'org.flymine.model.testmodel.CEO' OR CLASS = 'org.flymine.model.testmodel.Manager') AS a1_ ORDER BY a1_.ID");
    }

    public void executeTest(String type) throws Exception {
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement((Query) queries.get(type), dr);
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

    public void testCountNoGroupByNotDistinct() throws Exception {
        Query q = (Query) queries.get("ContainsDuplicatesMN");
        q.setDistinct(false);
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q, dr, false, true);
        assertEquals(s1.getStatement(), "SELECT COUNT(*) AS count_ FROM Contractor AS a1_, Company AS a2_, OldComOldContract AS ind_a1_a2_OldComOldContract_ WHERE (a1_.ID = ind_a1_a2_OldComOldContract_.oldContractId AND a2_.ID = ind_a1_a2_OldComOldContract_.oldComId)");
    }

    public void testCountNoGroupByDistinct() throws Exception {
        Query q = (Query) queries.get("ContainsDuplicatesMN");
        q.setDistinct(true);
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q, dr, false, true);
        assertEquals("SELECT COUNT(*) AS count_ FROM (SELECT DISTINCT a1_.ID AS a1_ID, a1_.businessAddressId AS a1_businessAddressId, a1_.name AS a1_name, a1_.personalAddressId AS a1_personalAddressId, a2_.CEOId AS a2_CEOId, a2_.ID AS a2_ID, a2_.addressId AS a2_addressId, a2_.name AS a2_name, a2_.vatNumber AS a2_vatNumber FROM Contractor AS a1_, Company AS a2_, OldComOldContract AS ind_a1_a2_OldComOldContract_ WHERE (a1_.ID = ind_a1_a2_OldComOldContract_.oldContractId AND a2_.ID = ind_a1_a2_OldComOldContract_.oldComId)) AS fake_table", s1.getStatement());
    }

    public void testCountGroupByNotDistinct() throws Exception {
        Query q = (Query) queries.get("SimpleGroupBy");
        q.setDistinct(false);
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q, dr, false, true);
        assertEquals("SELECT COUNT(*) AS count_ FROM (SELECT 1 AS flibble FROM Company AS a1_, Department AS a3_ WHERE (a1_.ID = a3_.companyId) GROUP BY a1_.CEOId, a1_.ID, a1_.addressId, a1_.name, a1_.vatNumber) AS fake_table", s1.getStatement());
    }

    public void testCountGroupByDistinct() throws Exception {
        Query q = (Query) queries.get("SimpleGroupBy");
        q.setDistinct(true);
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q, dr, false, true);
        assertEquals(s1.getStatement(), "SELECT COUNT(*) AS count_ FROM (SELECT DISTINCT a1_.CEOId AS a1_CEOId, a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, COUNT(*) AS a2_ FROM Company AS a1_, Department AS a3_ WHERE (a1_.ID = a3_.companyId) GROUP BY a1_.CEOId, a1_.ID, a1_.addressId, a1_.name, a1_.vatNumber) AS fake_table");
    }





    // why is all this code commented out?  for deletion?
    /*
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
*/




    // Following tests throw exceptions containing ojb metadata values for various business
    // objects.  Uncomment for debug/research purposes.

    /*
    public void testEmployee() throws Exception {
        QueryClass qc1 = new QueryClass(Employee.class);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addToSelect(qc1);
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q1, dr);
        throw (new Exception(s1.getStatement()));
    }

    public void testEmployable() throws Exception {
        QueryClass qc1 = new QueryClass(Employable.class);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addToSelect(qc1);
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q1, dr);
        throw (new Exception(s1.getStatement()));
    }

    public void testManager() throws Exception {
        QueryClass qc1 = new QueryClass(Manager.class);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addToSelect(qc1);
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q1, dr);
        throw (new Exception(s1.getStatement()));
    }

    public void testRandomInterface() throws Exception {
        QueryClass qc1 = new QueryClass(RandomInterface.class);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addToSelect(qc1);
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q1, dr);
        throw (new Exception(s1.getStatement()));
    }

    public void testImportantPerson() throws Exception {
        QueryClass qc1 = new QueryClass(ImportantPerson.class);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addToSelect(qc1);
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q1, dr);
        throw (new Exception(s1.getStatement()));
    }

    public void testCEO() throws Exception {
        QueryClass qc1 = new QueryClass(CEO.class);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addToSelect(qc1);
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q1, dr);
        throw (new Exception(s1.getStatement()));
    }

    public void testEmployeeGetExtentClasses() throws Exception {
        ClassDescriptor cld = dr.getDescriptorFor(Employee.class);
        outputData(cld);
    }

    public void testEmployableGetExtentClasses() throws Exception {
        ClassDescriptor cld = dr.getDescriptorFor(Employable.class);
        outputData(cld);
    }

    public void testManagerGetExtentClasses() throws Exception {
        ClassDescriptor cld = dr.getDescriptorFor(Manager.class);
        outputData(cld);
    }

    public void testCEOGetExtentClasses() throws Exception {
        ClassDescriptor cld = dr.getDescriptorFor(CEO.class);
        outputData(cld);
    }

    public void testContractorGetExtentClasses() throws Exception {
        ClassDescriptor cld = dr.getDescriptorFor(Contractor.class);
        outputData(cld);
    }

    public void testCompanyGetExtentClasses() throws Exception {
        ClassDescriptor cld = dr.getDescriptorFor(Company.class);
        outputData(cld);
    }


   public void testDepartmentGetExtentClasses() throws Exception {
        ClassDescriptor cld = dr.getDescriptorFor(Department.class);
        outputData(cld);
    }

    private void outputData(ClassDescriptor cld) throws Exception {
        String retval = "";
        retval += "Full table name = \"" + cld.getFullTableName() + "\"\n";
        retval += "getExtentClasses() = \"" + cld.getExtentClasses() + "\"\n";
        FieldDescriptor classDesc = cld.getFieldDescriptorByName(ClassDescriptor.OJB_CONCRETE_CLASS);
        retval += "Class field name = " + (classDesc == null ? "null" : "\"" + classDesc.getColumnName() + "\"") + "\n";
        FieldDescriptor fields[] = cld.getFieldDescriptions();
        retval += "All fields = \"" + descriptionsToString(fields) + "\"\n";
        fields = cld.getFieldDescriptorsInHeirarchy();
        retval += "Fields in heirarchy = \"" + descriptionsToString(fields) + "\"\n";
        retval += "Superclass = \"" + cld.getSuperClass() + "\"\n";
        retval += "Class name = \"" + cld.getClassOfObject().toString() + "\"\n";
        retval += "Interface: " + (cld.isInterface() ? "Yes" : "No") + "\n";
        retval += "Collections: " + collectionsToString(cld.getCollectionDescriptors()) + "\n";
        throw (new Exception(retval));
    }

    private String descriptionsToString(FieldDescriptor fields[]) {
        if (fields == null) {
            return "null";
        }
        String retval = "";
        for (int i = 0; i < fields.length; i++) {
            retval += (i == 0 ? "" : ", ") + fields[i].getColumnName();
        }
        return retval;
    }

    private String collectionsToString(Vector collections) {
        if (collections == null) {
            return "null";
        }
        String retval = collections.size() + "\n";
        Iterator collectionIter = collections.iterator();
        while (collectionIter.hasNext()) {
            CollectionDescriptor c = (CollectionDescriptor) collectionIter.next();
            retval += "--Collection of " + c.getItemClassName() + "\n";
            ClassDescriptor cld = dr.getDescriptorFor(c.getItemClass());
            retval += "----isMtoNRelation: " + (c.isMtoNRelation() ? "Yes\n" : "No\n");
            if (c.isMtoNRelation()) {
                retval += "----getFksToItemClass: " + stringArrayToString(c.getFksToItemClass()) + "\n";
                retval += "----getFksToThisClass: " + stringArrayToString(c.getFksToThisClass()) + "\n";
                retval += "----getIndirectionTable: " + c.getIndirectionTable() + "\n";
            } else {
                retval += "----getForeignKeyFields: " + descriptionsToString(c.getForeignKeyFieldDescriptors(cld)) + "\n";
            }
        }
        return retval;
    }

    private String stringArrayToString(String in[]) {
        String retval = "";
        for (int i = 0; i < in.length; i++) {
            retval += (i == 0 ? "" : ", ") + in[i];
        }
        return retval;
    }
    */

}
