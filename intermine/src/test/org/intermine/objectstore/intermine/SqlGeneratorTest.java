package org.flymine.objectstore.flymine;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.Test;


import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.flymine.metadata.Model;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.SetupDataTestCase;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryExpression;
import org.flymine.objectstore.query.QueryFunction;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.sql.Database;
import org.flymine.sql.DatabaseFactory;
import org.flymine.testing.OneTimeTestCase;
import org.flymine.util.TypeUtil;

import org.flymine.model.testmodel.*;

public class SqlGeneratorTest extends SetupDataTestCase
{
    private static Database db;

    public SqlGeneratorTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(SqlGeneratorTest.class);
    }

    public static void oneTimeSetUp() throws Exception {
        SetupDataTestCase.oneTimeSetUp();
        setUpResults();
        db = DatabaseFactory.getDatabase("db.unittest");
    }

    public static void setUpResults() throws Exception {
        results.put("SelectSimpleObject", "SELECT Company.OBJECT AS \"Company\", Company.id AS \"Companyid\" FROM Company AS Company ORDER BY Company.id");
        results.put("SubQuery", "SELECT DISTINCT a1_.a1_name AS a2_, a1_.Alias AS a3_ FROM (SELECT DISTINCT a1_.OBJECT AS a1_, a1_.addressId AS a1_addressId, a1_.cEOId AS a1_cEOId, a1_.id AS a1_id, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, 5 AS Alias FROM Company AS a1_) AS a1_ ORDER BY a1_.a1_name, a1_.Alias");
        results.put("WhereSimpleEquals", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE a1_.vatNumber = 1234 ORDER BY a1_.name");
        results.put("WhereSimpleNotEquals", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE a1_.vatNumber != 1234 ORDER BY a1_.name");
        results.put("WhereSimpleNegEquals", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE a1_.vatNumber != 1234 ORDER BY a1_.name");
        results.put("WhereSimpleLike", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE a1_.name LIKE 'Company%' ORDER BY a1_.name");
        results.put("WhereEqualsString", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE a1_.name = 'CompanyA' ORDER BY a1_.name");
        results.put("WhereAndSet", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE (a1_.name LIKE 'Company%' AND a1_.vatNumber > 2000) ORDER BY a1_.name");
        results.put("WhereOrSet", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE (a1_.name LIKE 'CompanyA%' OR a1_.vatNumber > 2000) ORDER BY a1_.name");
        results.put("WhereNotSet", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE ( NOT (a1_.name LIKE 'Company%' AND a1_.vatNumber > 2000)) ORDER BY a1_.name");
        results.put("WhereSubQueryField", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a1_.name AS orderbyfield0 FROM Department AS a1_ WHERE a1_.name IN (SELECT DISTINCT a1_.name FROM Department AS a1_) ORDER BY a1_.name, a1_.id");
        results.put("WhereSubQueryClass", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_ WHERE a1_.id IN (SELECT DISTINCT a1_.id FROM Company AS a1_ WHERE a1_.name = 'CompanyA') ORDER BY a1_.id");
        results.put("WhereNotSubQueryClass", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_ WHERE a1_.id NOT IN (SELECT DISTINCT a1_.id FROM Company AS a1_ WHERE a1_.name = 'CompanyA') ORDER BY a1_.id");
        results.put("WhereNegSubQueryClass", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_ WHERE a1_.id NOT IN (SELECT DISTINCT a1_.id FROM Company AS a1_ WHERE a1_.name = 'CompanyA') ORDER BY a1_.id");
        results.put("WhereClassClass", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Company AS a1_, Company AS a2_ WHERE a1_.id = a2_.id ORDER BY a1_.id, a2_.id");
        results.put("WhereNotClassClass", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Company AS a1_, Company AS a2_ WHERE a1_.id != a2_.id ORDER BY a1_.id, a2_.id");
        results.put("WhereNegClassClass", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Company AS a1_, Company AS a2_ WHERE a1_.id != a2_.id ORDER BY a1_.id, a2_.id");
        Integer id1 = (Integer) TypeUtil.getFieldValue(data.get("CompanyA"), "id");
        results.put("WhereClassObject", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_ WHERE a1_.id = " + id1 + " ORDER BY a1_.id");
        results.put("Contains11", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Department AS a1_, Manager AS a2_ WHERE (a1_.managerId = a2_.id AND a1_.name = 'DepartmentA1') ORDER BY a1_.id, a2_.id");
        results.put("ContainsNot11", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Department AS a1_, Manager AS a2_ WHERE (a1_.managerId != a2_.id AND a1_.name = 'DepartmentA1') ORDER BY a1_.id, a2_.id");
        results.put("ContainsNeg11", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Department AS a1_, Manager AS a2_ WHERE (a1_.managerId != a2_.id AND a1_.name = 'DepartmentA1') ORDER BY a1_.id, a2_.id");
        results.put("Contains1N", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Company AS a1_, Department AS a2_ WHERE (a1_.id = a2_.companyId AND a1_.name = 'CompanyA') ORDER BY a1_.id, a2_.id");
        results.put("ContainsN1", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Department AS a1_, Company AS a2_ WHERE (a1_.companyId = a2_.id AND a2_.name = 'CompanyA') ORDER BY a1_.id, a2_.id");
        results.put("ContainsMN", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Contractor AS a1_, Company AS a2_, CompanysContractors AS indirect0 WHERE ((a1_.id = indirect0.Companys AND indirect0.Contractors = a2_.id) AND a1_.name = 'ContractorA') ORDER BY a1_.id, a2_.id");
        results.put("ContainsDuplicatesMN", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Contractor AS a1_, Company AS a2_, OldComsOldContracts AS indirect0 WHERE (a1_.id = indirect0.OldComs AND indirect0.OldContracts = a2_.id) ORDER BY a1_.id, a2_.id");
        id1 = (Integer) TypeUtil.getFieldValue(data.get("EmployeeA1"), "id");
        results.put("ContainsObject", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Department AS a1_ WHERE a1_.managerId = " + id1 + " ORDER BY a1_.id");
        results.put("SimpleGroupBy", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, COUNT(*) AS a2_ FROM Company AS a1_, Department AS a3_ WHERE a1_.id = a3_.companyId GROUP BY a1_.OBJECT, a1_.addressId, a1_.cEOId, a1_.id, a1_.name, a1_.vatNumber ORDER BY a1_.id, COUNT(*)");
        results.put("MultiJoin", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id, a4_.OBJECT AS a4_, a4_.id AS a4_id FROM Company AS a1_, Department AS a2_, Manager AS a3_, Address AS a4_ WHERE (a1_.id = a2_.companyId AND a2_.managerId = a3_.id AND a3_.addressId = a4_.id AND a3_.name = 'EmployeeA1') ORDER BY a1_.id, a2_.id, a3_.id, a4_.id");
        results.put("SelectComplex", "SELECT DISTINCT (AVG(a1_.vatNumber) + 20) AS a3_, a2_.name AS a4_, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Company AS a1_, Department AS a2_ GROUP BY a2_.OBJECT, a2_.companyId, a2_.id, a2_.managerId, a2_.name ORDER BY (AVG(a1_.vatNumber) + 20), a2_.name, a2_.id");
        results.put("SelectClassAndSubClasses", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a1_.name AS orderbyfield0 FROM Employee AS a1_ ORDER BY a1_.name, a1_.id");
        results.put("SelectInterfaceAndSubClasses", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employable AS a1_ ORDER BY a1_.id");
        results.put("SelectInterfaceAndSubClasses2", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM RandomInterface AS a1_ ORDER BY a1_.id");
        results.put("SelectInterfaceAndSubClasses3", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM ImportantPerson AS a1_ ORDER BY a1_.id");
        results.put("OrderByAnomaly", "SELECT DISTINCT 5 AS a2_, a1_.name AS a3_ FROM Company AS a1_ ORDER BY a1_.name");
        Integer id2 = (Integer) TypeUtil.getFieldValue(data.get("CompanyA"), "id");
        Integer id3 = (Integer) TypeUtil.getFieldValue(data.get("DepartmentA1"), "id");
        results.put("SelectClassObjectSubquery", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_, Department AS a2_ WHERE (a1_.id = " + id2 + " AND a1_.id = a2_.companyId AND a2_.id IN (SELECT DISTINCT a1_.id FROM Department AS a1_ WHERE a1_.id = " + id3 + ")) ORDER BY a1_.id");
        results.put("SelectUnidirectionalCollection", "SELECT DISTINCT a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Company AS a1_, Secretary AS a2_, HasSecretarysSecretarys AS indirect0 WHERE (a1_.name = 'CompanyA' AND (a1_.id = indirect0.Secretarys AND indirect0.HasSecretarys = a2_.id)) ORDER BY a2_.id");
        results.put("EmptyAndConstraintSet", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_ WHERE true ORDER BY a1_.id");
        results.put("EmptyOrConstraintSet", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_ WHERE false ORDER BY a1_.id");
        results.put("EmptyNandConstraintSet", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_ WHERE false ORDER BY a1_.id");
        results.put("EmptyNorConstraintSet", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_ WHERE true ORDER BY a1_.id");
        results.put("BagConstraint", "SELECT DISTINCT Company.OBJECT AS \"Company\", Company.id AS \"Companyid\" FROM Company AS Company WHERE (Company.name = 'CompanyA' OR Company.name = 'goodbye' OR Company.name = 'hello') ORDER BY Company.id");
        results.put("BagConstraint2", "SELECT DISTINCT Company.OBJECT AS \"Company\", Company.id AS \"Companyid\" FROM Company AS Company WHERE (Company.id = " + id2 + ") ORDER BY Company.id");
        results.put("InterfaceField", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employable AS a1_ WHERE a1_.name = 'EmployeeA1' ORDER BY a1_.id");
        results.put("InterfaceReference", NO_RESULT);
        results.put("InterfaceCollection", NO_RESULT);
        Set res = new HashSet();
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a1__1.debt AS a2_, a1_.vatNumber AS a3_ FROM Company AS a1_, Broke AS a1__1 WHERE a1_.id = a1__1.id AND (a1__1.debt > 0 AND a1_.vatNumber > 0) ORDER BY a1_.id, a1__1.debt, a1_.vatNumber");
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a1_.debt AS a2_, a1__1.vatNumber AS a3_ FROM Broke AS a1_, Company AS a1__1 WHERE a1_.id = a1__1.id AND (a1_.debt > 0 AND a1__1.vatNumber > 0) ORDER BY a1_.id, a1_.debt, a1__1.vatNumber");
        results.put("DynamicInterfacesAttribute", res);
        res = new HashSet();
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employable AS a1_, Broke AS a1__1 WHERE a1_.id = a1__1.id ORDER BY a1_.id");
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Broke AS a1_, Employable AS a1__1 WHERE a1_.id = a1__1.id ORDER BY a1_.id");
        results.put("DynamicClassInterface", res);
        res = new HashSet();
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM Department AS a1_, Broke AS a1__1, Company AS a2_, Bank AS a3_ WHERE a1_.id = a1__1.id AND (a2_.id = a1_.companyId AND a3_.id = a1__1.bankId) ORDER BY a1_.id, a2_.id, a3_.id");
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM Broke AS a1_, Department AS a1__1, Company AS a2_, Bank AS a3_ WHERE a1_.id = a1__1.id AND (a2_.id = a1__1.companyId AND a3_.id = a1_.bankId) ORDER BY a1_.id, a2_.id, a3_.id");
        results.put("DynamicClassRef1", res);
        res = new HashSet();
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM Department AS a1_, Broke AS a1__1, Company AS a2_, Bank AS a3_ WHERE a1_.id = a1__1.id AND (a1_.companyId = a2_.id AND a1__1.bankId = a3_.id) ORDER BY a1_.id, a2_.id, a3_.id");
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM Broke AS a1_, Department AS a1__1, Company AS a2_, Bank AS a3_ WHERE a1_.id = a1__1.id AND (a1__1.companyId = a2_.id AND a1_.bankId = a3_.id) ORDER BY a1_.id, a2_.id, a3_.id");
        results.put("DynamicClassRef2", res);
        res = new HashSet();
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM Company AS a1_, Bank AS a1__1, Department AS a2_, Broke AS a3_ WHERE a1_.id = a1__1.id AND (a1_.id = a2_.companyId AND a1_.id = a3_.bankId) ORDER BY a1_.id, a2_.id, a3_.id");
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM Bank AS a1_, Company AS a1__1, Department AS a2_, Broke AS a3_ WHERE a1_.id = a1__1.id AND (a1_.id = a2_.companyId AND a1_.id = a3_.bankId) ORDER BY a1_.id, a2_.id, a3_.id");
        results.put("DynamicClassRef3", res);
        res = new HashSet();
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM Company AS a1_, Bank AS a1__1, Department AS a2_, Broke AS a3_ WHERE a1_.id = a1__1.id AND (a2_.companyId = a1_.id AND a3_.bankId = a1_.id) ORDER BY a1_.id, a2_.id, a3_.id");
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM Bank AS a1_, Company AS a1__1, Department AS a2_, Broke AS a3_ WHERE a1_.id = a1__1.id AND (a2_.companyId = a1_.id AND a3_.bankId = a1_.id) ORDER BY a1_.id, a2_.id, a3_.id");
        results.put("DynamicClassRef4", res);
        res = new HashSet();
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employable AS a1_, Broke AS a1__1, HasAddress AS a2_, Broke AS a2__1 WHERE a1_.id = a1__1.id AND a2_.id = a2__1.id AND a1_.id = a2_.id ORDER BY a1_.id");
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employable AS a1_, Broke AS a1__1, Broke AS a2_, HasAddress AS a2__1 WHERE a1_.id = a1__1.id AND a2_.id = a2__1.id AND a1_.id = a2_.id ORDER BY a1_.id");
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Broke AS a1_, Employable AS a1__1, HasAddress AS a2_, Broke AS a2__1 WHERE a1_.id = a1__1.id AND a2_.id = a2__1.id AND a1_.id = a2_.id ORDER BY a1_.id");
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Broke AS a1_, Employable AS a1__1, Broke AS a2_, HasAddress AS a2__1 WHERE a1_.id = a1__1.id AND a2_.id = a2__1.id AND a1_.id = a2_.id ORDER BY a1_.id");
        results.put("DynamicClassConstraint", res);
        results.put("ContainsConstraintNull", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employee AS a1_ WHERE a1_.addressId IS NULL ORDER BY a1_.id");
        results.put("ContainsConstraintNotNull", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employee AS a1_ WHERE a1_.addressId IS NOT NULL ORDER BY a1_.id");
        results.put("SimpleConstraintNull", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Manager AS a1_ WHERE a1_.title IS NULL ORDER BY a1_.id");
        results.put("SimpleConstraintNotNull", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Manager AS a1_ WHERE a1_.title IS NOT NULL ORDER BY a1_.id");
        results.put("TypeCast", "SELECT DISTINCT (a1_.age)::TEXT AS a2_ FROM Employee AS a1_ ORDER BY (a1_.age)::TEXT");
        results.put("IndexOf", "SELECT STRPOS(a1_.name, 'oy') AS a2_ FROM Employee AS a1_ ORDER BY STRPOS(a1_.name, 'oy')");
        results.put("Substring", "SELECT SUBSTR(a1_.name, 2, 2) AS a2_ FROM Employee AS a1_ ORDER BY SUBSTR(a1_.name, 2, 2)");
        results.put("Substring2", "SELECT SUBSTR(a1_.name, 2) AS a2_ FROM Employee AS a1_ ORDER BY SUBSTR(a1_.name, 2)");
    }

    public void executeTest(String type) throws Exception {
        Query q = (Query) queries.get(type);
        String generated = SqlGenerator.generate(q, 0, Integer.MAX_VALUE, model, db);
        Object expected = results.get(type);
        if (expected instanceof String) {
            assertEquals(type + " has failed", results.get(type), generated);
        } else if (expected instanceof Collection) {
            boolean hasEqual = false;
            Iterator expectedIter = ((Collection) expected).iterator();
            while ((!hasEqual) && expectedIter.hasNext()) {
                String expectedString = (String) expectedIter.next();
                hasEqual = expectedString.equals(generated);
            }
            assertTrue(type + " has failed: " + generated, hasEqual);
        }

        // TODO: extend sql so that it can represent these
        if (!("TypeCast".equals(type) || "IndexOf".equals(type) || "Substring".equals(type) || "Substring2".equals(type))) {
            // And check that the SQL generated is high enough quality to be parsed by the optimiser.
            org.flymine.sql.query.Query sql = new org.flymine.sql.query.Query(generated);
        }
    }

    public void testSelectQueryValue() throws Exception {
        QueryValue v1 = new QueryValue(new Integer(5));
        QueryValue v2 = new QueryValue("Hello");
        QueryValue v3 = new QueryValue(new Date(1046275720000l));
        QueryValue v4 = new QueryValue(Boolean.TRUE);
        StringBuffer buffer = new StringBuffer();
        SqlGenerator.queryEvaluableToString(buffer, v1, null, null);
        SqlGenerator.queryEvaluableToString(buffer, v2, null, null);
        SqlGenerator.queryEvaluableToString(buffer, v3, null, null);
        SqlGenerator.queryEvaluableToString(buffer, v4, null, null);
        assertEquals("5'Hello''2003-02-26 16:08:40.000''true'", buffer.toString());
    }

    public void testSelectQueryExpression() throws Exception {
        QueryValue v1 = new QueryValue(new Integer(5));
        QueryValue v2 = new QueryValue(new Integer(7));
        QueryExpression e1 = new QueryExpression(v1, QueryExpression.ADD, v2);
        QueryExpression e2 = new QueryExpression(v1, QueryExpression.SUBTRACT, v2);
        QueryExpression e3 = new QueryExpression(v1, QueryExpression.MULTIPLY, v2);
        QueryExpression e4 = new QueryExpression(v1, QueryExpression.DIVIDE, v2);
        StringBuffer buffer = new StringBuffer();
        SqlGenerator.queryEvaluableToString(buffer, e1, null, null);
        SqlGenerator.queryEvaluableToString(buffer, e2, null, null);
        SqlGenerator.queryEvaluableToString(buffer, e3, null, null);
        SqlGenerator.queryEvaluableToString(buffer, e4, null, null);
        assertEquals("(5 + 7)(5 - 7)(5 * 7)(5 / 7)", buffer.toString());
    }

    public void testSelectQuerySubstringExpression() throws Exception {
        QueryValue v1 = new QueryValue("Hello");
        QueryValue v2 = new QueryValue(new Integer(3));
        QueryValue v3 = new QueryValue(new Integer(5));
        QueryExpression e1 = new QueryExpression(v1, v2, v3);
        StringBuffer buffer = new StringBuffer();
        SqlGenerator.queryEvaluableToString(buffer, e1, null, null);
        assertEquals("SUBSTR('Hello', 3, 5)", buffer.toString());
    }

    /* TODO
    public void testSelectQueryField() throws Exception {
        QueryClass c1 = new QueryClass(Department.class);
        QueryField f1 = new QueryField(c1, "name");
        Query q1 = new Query();
        q1.addFrom(c1);
        StringBuffer buffer = new StringBuffer();
        SqlGenerator.queryEvaluableToString(buffer, f1, q1);
        assertEquals("a1_.name", buffer.toString());
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
        StringBuffer buffer = new StringBuffer();
        SqlGenerator.queryEvaluableToString(buffer, f1, q1);
        SqlGenerator.queryEvaluableToString(buffer, f2, q1);
        SqlGenerator.queryEvaluableToString(buffer, f3, q1);
        SqlGenerator.queryEvaluableToString(buffer, f4, q1);
        SqlGenerator.queryEvaluableToString(buffer, f5, q1);
        assertEquals("COUNT(*)SUM(a1_.vatNumber)AVG(a1_.vatNumber)MIN(a1_.vatNumber)MAX(a1_.vatNumber)", buffer.toString());
    }
*/

    public void testInvalidClass() throws Exception {
        Query q = new Query();
        QueryClass c1 = new QueryClass(Company.class);
        q.addFrom(c1);
        q.addToSelect(c1);
        try {
            SqlGenerator.generate(q, 0, Integer.MAX_VALUE, new Model("nothing", "http://www.flymine.org/model/testmodel",
                                                                     new HashSet()), db);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
        }
    }
}

