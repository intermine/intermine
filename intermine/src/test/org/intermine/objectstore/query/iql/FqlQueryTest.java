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
        results2.put("SelectSimpleObject", new FqlQuery("SELECT a1_ FROM org.flymine.model.testmodel.Company AS a1_", null));
        results2.put("SubQuery", new FqlQuery("SELECT a1_.a1_.name AS a2_, a1_.a2_ AS a3_ FROM (SELECT a1_, 5 AS a2_ FROM org.flymine.model.testmodel.Company AS a1_) AS a1_", null));
        results2.put("WhereSimpleEquals", new FqlQuery("SELECT a1_.name AS a2_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.vatNumber = 1234", null));
        results2.put("WhereSimpleNotEquals", new FqlQuery("SELECT a1_.name AS a2_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.vatNumber != 1234", null));
        results2.put("WhereSimpleLike", new FqlQuery("SELECT a1_.name AS a2_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.name LIKE 'Company%'", null));
        results2.put("WhereEqualsString", new FqlQuery("SELECT a1_.name AS a2_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.name = 'CompanyA'", null));
        results2.put("WhereAndSet", new FqlQuery("SELECT a1_.name AS a2_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE (a1_.name LIKE 'Company%' AND a1_.vatNumber > 2000)", null));
        results2.put("WhereOrSet", new FqlQuery("SELECT a1_.name AS a2_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE (a1_.name LIKE 'CompanyA%' OR a1_.vatNumber > 2000)", null));
        results2.put("WhereNotSet", new FqlQuery("SELECT a1_.name AS a2_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE ( NOT (a1_.name LIKE 'Company%' AND a1_.vatNumber > 2000))", null));
        results2.put("WhereSubQueryField", new FqlQuery("SELECT a1_ FROM org.flymine.model.testmodel.Department AS a1_ WHERE a1_.name IN (SELECT a1_.name AS a2_ FROM org.flymine.model.testmodel.Department AS a1_) ORDER BY a1_.name", null));
        results2.put("WhereSubQueryClass", new FqlQuery("SELECT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_ IN (SELECT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.name = 'CompanyA')", null));
        results2.put("WhereNotSubQueryClass", new FqlQuery("SELECT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_ NOT IN (SELECT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.name = 'CompanyA')", null));
        results2.put("WhereNegSubQueryClass", new FqlQuery("SELECT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_ NOT IN (SELECT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.name = 'CompanyA')", null));
        //results2.put("WhereNegSubQueryClass", new FqlQuery("SELECT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE NOT (a1_ IN (SELECT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.name = 'CompanyA'))", null));
        results2.put("WhereClassClass", new FqlQuery("SELECT a1_, a2_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Company AS a2_ WHERE a1_ = a2_", null));
        results2.put("WhereNotClassClass", new FqlQuery("SELECT a1_, a2_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Company AS a2_ WHERE a1_ != a2_", null));
        results2.put("WhereNegClassClass", new FqlQuery("SELECT a1_, a2_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Company AS a2_ WHERE a1_ != a2_", null));
        //results2.put("WhereNegClassClass", new FqlQuery("SELECT a1_, a2_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Company AS a2_ WHERE NOT a1_ = a2_", null));
        results2.put("Contains11", new FqlQuery("SELECT a1_, a2_ FROM org.flymine.model.testmodel.Department AS a1_, org.flymine.model.testmodel.Manager AS a2_ WHERE (a1_.manager CONTAINS a2_ AND a1_.name = 'DepartmentA1')", null));
        results2.put("ContainsNot11", new FqlQuery("SELECT a1_, a2_ FROM org.flymine.model.testmodel.Department AS a1_, org.flymine.model.testmodel.Manager AS a2_ WHERE (a1_.manager DOES NOT CONTAIN a2_ AND a1_.name = 'DepartmentA1')", null));
        results2.put("ContainsNeg11", new FqlQuery("SELECT a1_, a2_ FROM org.flymine.model.testmodel.Department AS a1_, org.flymine.model.testmodel.Manager AS a2_ WHERE (a1_.manager DOES NOT CONTAIN a2_ AND a1_.name = 'DepartmentA1')", null));
        //results2.put("ContainsNeg11", new FqlQuery("SELECT a1_, a2_ FROM org.flymine.model.testmodel.Department AS a1_, org.flymine.model.testmodel.Manager AS a2_ WHERE NOT a1_.manager CONTAINS a2_ AND a1_.name = 'DepartmentA1'", null));
        results2.put("Contains1N", new FqlQuery("SELECT a1_, a2_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Department AS a2_ WHERE (a1_.departments CONTAINS a2_ AND a1_.name = 'CompanyA')", null));
        results2.put("ContainsN1", new FqlQuery("SELECT a1_, a2_ FROM org.flymine.model.testmodel.Department AS a1_, org.flymine.model.testmodel.Company AS a2_ WHERE (a1_.company CONTAINS a2_ AND a2_.name = 'CompanyA')", null));
        results2.put("ContainsMN", new FqlQuery("SELECT a1_, a2_ FROM org.flymine.model.testmodel.Contractor AS a1_, org.flymine.model.testmodel.Company AS a2_ WHERE (a1_.companys CONTAINS a2_ AND a1_.name = 'ContractorA')", null));
        results2.put("ContainsDuplicatesMN", new FqlQuery("SELECT a1_, a2_ FROM org.flymine.model.testmodel.Contractor AS a1_, org.flymine.model.testmodel.Company AS a2_ WHERE a1_.oldComs CONTAINS a2_", null));
        results2.put("SimpleGroupBy", new FqlQuery("SELECT a1_, COUNT(*) AS a2_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Department AS a3_ WHERE a1_.departments CONTAINS a3_ GROUP BY a1_", null));
        results2.put("MultiJoin", new FqlQuery("SELECT a1_, a2_, a3_, a4_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Department AS a2_, org.flymine.model.testmodel.Manager AS a3_, org.flymine.model.testmodel.Address AS a4_ WHERE (a1_.departments CONTAINS a2_ AND a2_.manager CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a3_.name = 'EmployeeA1')", null));
        results2.put("SelectComplex", new FqlQuery("SELECT AVG(a1_.vatNumber) + 20 AS a3_, a2_.name AS a4_, a2_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Department AS a2_ GROUP BY a2_", null));
        results2.put("SelectClassAndSubClasses", new FqlQuery("SELECT a1_ FROM org.flymine.model.testmodel.Employee AS a1_ ORDER BY a1_.name", null));
        results2.put("SelectInterfaceAndSubClasses", new FqlQuery("SELECT a1_ FROM org.flymine.model.testmodel.Employable AS a1_", null));
        results2.put("SelectInterfaceAndSubClasses2", new FqlQuery("SELECT a1_ FROM org.flymine.model.testmodel.RandomInterface AS a1_", null));
        results2.put("SelectInterfaceAndSubClasses3", new FqlQuery("SELECT a1_ FROM org.flymine.model.testmodel.ImportantPerson AS a1_", null));
        results2.put("OrderByAnomaly", new FqlQuery("SELECT 5 AS a2_, a1_.name AS a3_ FROM org.flymine.model.testmodel.Company AS a1_", null));
    }

    public void executeTest(String type) throws Exception {
        Query orig = ((Query) queries.get(type));
        FqlQuery fq = (FqlQuery) results2.get(type);

        if (fq != null) {
            // This is testing whether new FqlQuery(Query) gives the FqlQueries above
            FqlQuery fqGenerated = new FqlQuery(orig);
            assertEquals(type + " has failed", fq.getQueryString(), fqGenerated.getQueryString());;
            assertEquals(type + " has failed", fq.getParameters(), fqGenerated.getParameters());;

            // This is testing whether the FqlQueries above are parsed into the correct query
            Query parsed = FqlQueryParser.parse(fq);
            assertEquals(type + " has failed", orig, parsed);
        }

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
