package org.flymine.objectstore.query.fql;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;
import junit.framework.Test;
import org.flymine.objectstore.query.Query;
import org.flymine.testing.OneTimeTestCase;
import java.util.HashMap;
import java.util.Map;

public class FqlQueryTest extends FqlQueryTestCase
{
    public static Map results2 = new HashMap();

    public FqlQueryTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(FqlQueryTest.class);
    }

    public static void oneTimeSetUp() throws Exception {
        FqlQueryTestCase.oneTimeSetUp();

        setUpResults();
    }

    public static void setUpResults() {
        results2.put("SelectSimpleObject", "SELECT a1_ FROM org.flymine.model.testmodel.Company AS a1_");
        results2.put("SubQuery", "SELECT a1_.a1_.name AS a2_, a1_.a2_ AS a3_ FROM (SELECT a1_, 5 AS a2_ FROM org.flymine.model.testmodel.Company AS a1_) AS a1_");
        results2.put("WhereSimpleEquals", "SELECT a1_.name AS a2_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.vatNumber = 1234");
        results2.put("WhereSimpleNotEquals", "SELECT a1_.name AS a2_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.vatNumber != 1234");
        results2.put("WhereSimpleLike", "SELECT a1_.name AS a2_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.name LIKE 'Company%'");
        results2.put("WhereEqualsString", "SELECT a1_.name AS a2_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.name = 'CompanyA'");
        results2.put("WhereAndSet", "SELECT a1_.name AS a2_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE (a1_.name LIKE 'Company%' AND a1_.vatNumber > 2000)");
        results2.put("WhereOrSet", "SELECT a1_.name AS a2_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE (a1_.name LIKE 'CompanyA%' OR a1_.vatNumber > 2000)");
        results2.put("WhereNotSet", "SELECT a1_.name AS a2_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE ( NOT (a1_.name LIKE 'Company%' AND a1_.vatNumber > 2000))");
        results2.put("WhereSubQueryField", "SELECT a1_ FROM org.flymine.model.testmodel.Department AS a1_ WHERE a1_.name IN (SELECT a1_.name AS a2_ FROM org.flymine.model.testmodel.Department AS a1_) ORDER BY a1_.name");
        results2.put("WhereSubQueryClass", "SELECT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_ IN (SELECT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.name = 'CompanyA')");
        results2.put("WhereNotSubQueryClass", "SELECT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_ NOT IN (SELECT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.name = 'CompanyA')");
        results2.put("WhereNegSubQueryClass", "SELECT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_ NOT IN (SELECT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.name = 'CompanyA')");
        //results2.put("WhereNegSubQueryClass", "SELECT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE NOT (a1_ IN (SELECT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.name = 'CompanyA'))");
        results2.put("WhereClassClass", "SELECT a1_, a2_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Company AS a2_ WHERE a1_ = a2_");
        results2.put("WhereNotClassClass", "SELECT a1_, a2_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Company AS a2_ WHERE a1_ != a2_");
        results2.put("WhereNegClassClass", "SELECT a1_, a2_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Company AS a2_ WHERE a1_ != a2_");
        //results2.put("WhereNegClassClass", "SELECT a1_, a2_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Company AS a2_ WHERE NOT a1_ = a2_");
        results2.put("Contains11", "SELECT a1_, a2_ FROM org.flymine.model.testmodel.Department AS a1_, org.flymine.model.testmodel.Manager AS a2_ WHERE (a1_.manager CONTAINS a2_ AND a1_.name = 'DepartmentA1')");
        results2.put("ContainsNot11", "SELECT a1_, a2_ FROM org.flymine.model.testmodel.Department AS a1_, org.flymine.model.testmodel.Manager AS a2_ WHERE (a1_.manager DOES NOT CONTAIN a2_ AND a1_.name = 'DepartmentA1')");
        results2.put("ContainsNeg11", "SELECT a1_, a2_ FROM org.flymine.model.testmodel.Department AS a1_, org.flymine.model.testmodel.Manager AS a2_ WHERE (a1_.manager DOES NOT CONTAIN a2_ AND a1_.name = 'DepartmentA1')");
        //results2.put("ContainsNeg11", "SELECT a1_, a2_ FROM org.flymine.model.testmodel.Department AS a1_, org.flymine.model.testmodel.Manager AS a2_ WHERE NOT a1_.manager CONTAINS a2_ AND a1_.name = 'DepartmentA1'");
        results2.put("Contains1N", "SELECT a1_, a2_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Department AS a2_ WHERE (a1_.departments CONTAINS a2_ AND a1_.name = 'CompanyA')");
        results2.put("ContainsN1", "SELECT a1_, a2_ FROM org.flymine.model.testmodel.Department AS a1_, org.flymine.model.testmodel.Company AS a2_ WHERE (a1_.company CONTAINS a2_ AND a2_.name = 'CompanyA')");
        results2.put("ContainsMN", "SELECT a1_, a2_ FROM org.flymine.model.testmodel.Contractor AS a1_, org.flymine.model.testmodel.Company AS a2_ WHERE (a1_.companys CONTAINS a2_ AND a1_.name = 'ContractorA')");
        results2.put("ContainsDuplicatesMN", "SELECT a1_, a2_ FROM org.flymine.model.testmodel.Contractor AS a1_, org.flymine.model.testmodel.Company AS a2_ WHERE a1_.oldComs CONTAINS a2_");
        results2.put("SimpleGroupBy", "SELECT a1_, COUNT(*) AS a2_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Department AS a3_ WHERE a1_.departments CONTAINS a3_ GROUP BY a1_");
        results2.put("MultiJoin", "SELECT a1_, a2_, a3_, a4_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Department AS a2_, org.flymine.model.testmodel.Manager AS a3_, org.flymine.model.testmodel.Address AS a4_ WHERE (a1_.departments CONTAINS a2_ AND a2_.manager CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a3_.name = 'EmployeeA1')");
        results2.put("SelectComplex", "SELECT AVG(a1_.vatNumber) + 20 AS a3_, a2_.name AS a4_, a2_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Department AS a2_ GROUP BY a2_");
        results2.put("SelectClassAndSubClasses", "SELECT a1_ FROM org.flymine.model.testmodel.Employee AS a1_ ORDER BY a1_.name");
        results2.put("SelectInterfaceAndSubClasses", "SELECT a1_ FROM org.flymine.model.testmodel.Employable AS a1_");
        results2.put("SelectInterfaceAndSubClasses2", "SELECT a1_ FROM org.flymine.model.testmodel.RandomInterface AS a1_");
        results2.put("SelectInterfaceAndSubClasses3", "SELECT a1_ FROM org.flymine.model.testmodel.ImportantPerson AS a1_");
        results2.put("OrderByAnomaly", "SELECT 5 AS a2_, a1_.name AS a3_ FROM org.flymine.model.testmodel.Company AS a1_");
    }

    public void executeTest(String type) throws Exception {
        String generated = ((Query) queries.get(type)).toString();
        String fql = (String) results2.get(type);
        if (fql != null) {
            assertEquals(type + " has failed", fql, generated);
        }
        Query parsed = FqlQueryParser.parse(new FqlQuery(generated, null));
        assertEquals(type + " has failed", (Query) queries.get(type), parsed);
    }

    public void testConstructNullQuery() throws Exception {
        try {
            FqlQuery fq = new FqlQuery(null, "org.flymine.model.testmodel");
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testConstructEmptyQuery() throws Exception {
        try {
            FqlQuery fq = new FqlQuery("", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testConstructEmptyPackageName() throws Exception {
        try {
            FqlQuery fq = new FqlQuery("select a1_ from Company as a1_", "");
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

}
