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

import junit.framework.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.flymine.testing.OneTimeTestCase;
import org.flymine.objectstore.SetupDataTestCase;

/**
 * Test case that sets up various FqlQueries.
 * This class extends SetupDataTestCase because we use various example
 * objects as part of the tests. These are set up in
 * SetupDataTestCase. We do not actually contact the database in this
 * test.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public abstract class FqlQueryTestCase extends SetupDataTestCase
{
    public FqlQueryTestCase(String arg) {
        super(arg);
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(FqlQueryTestCase.class);
    }

    public static void oneTimeSetUp() throws Exception {
        SetupDataTestCase.oneTimeSetUp();

        setUpResults();
    }

    /**
     * Set up all the results expected for a given subset of queries
     */
    public static void setUpResults() {
        results.put("SelectSimpleObject", new FqlQuery("SELECT Company FROM org.flymine.model.testmodel.Company AS Company", null));
        results.put("SubQuery", new FqlQuery("SELECT DISTINCT a1_.a1_.name AS a2_, a1_.Alias AS a3_ FROM (SELECT DISTINCT a1_, 5 AS Alias FROM org.flymine.model.testmodel.Company AS a1_) AS a1_", null));
        results.put("WhereSimpleEquals", new FqlQuery("SELECT DISTINCT a1_.name AS a2_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.vatNumber = 1234", null));
        results.put("WhereSimpleNotEquals", new FqlQuery("SELECT DISTINCT a1_.name AS a2_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.vatNumber != 1234", null));
        results.put("WhereSimpleLike", new FqlQuery("SELECT DISTINCT a1_.name AS a2_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.name LIKE 'Company%'", null));
        results.put("WhereEqualsString", new FqlQuery("SELECT DISTINCT a1_.name AS a2_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.name = 'CompanyA'", null));
        results.put("WhereAndSet", new FqlQuery("SELECT DISTINCT a1_.name AS a2_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE (a1_.name LIKE 'Company%' AND a1_.vatNumber > 2000)", null));
        results.put("WhereOrSet", new FqlQuery("SELECT DISTINCT a1_.name AS a2_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE (a1_.name LIKE 'CompanyA%' OR a1_.vatNumber > 2000)", null));
        results.put("WhereNotSet", new FqlQuery("SELECT DISTINCT a1_.name AS a2_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE ( NOT (a1_.name LIKE 'Company%' AND a1_.vatNumber > 2000))", null));
        results.put("WhereSubQueryField", new FqlQuery("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Department AS a1_ WHERE a1_.name IN (SELECT DISTINCT a1_.name AS a2_ FROM org.flymine.model.testmodel.Department AS a1_) ORDER BY a1_.name", null));
        results.put("WhereSubQueryClass", new FqlQuery("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_ IN (SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.name = 'CompanyA')", null));
        results.put("WhereNotSubQueryClass", new FqlQuery("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_ NOT IN (SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.name = 'CompanyA')", null));
        results.put("WhereClassClass", new FqlQuery("SELECT DISTINCT a1_, a2_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Company AS a2_ WHERE a1_ = a2_", null));
        FqlQuery fq = new FqlQuery("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_ = ?", null);
        fq.setParameters(Collections.singletonList(data.get("CompanyA")));
        results.put("WhereClassObject", fq);
        results.put("WhereNotClassClass", new FqlQuery("SELECT DISTINCT a1_, a2_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Company AS a2_ WHERE a1_ != a2_", null));
        results.put("Contains11", new FqlQuery("SELECT DISTINCT a1_, a2_ FROM org.flymine.model.testmodel.Department AS a1_, org.flymine.model.testmodel.Manager AS a2_ WHERE (a1_.manager CONTAINS a2_ AND a1_.name = 'DepartmentA1')", null));
        results.put("ContainsNot11", new FqlQuery("SELECT DISTINCT a1_, a2_ FROM org.flymine.model.testmodel.Department AS a1_, org.flymine.model.testmodel.Manager AS a2_ WHERE (a1_.manager DOES NOT CONTAIN a2_ AND a1_.name = 'DepartmentA1')", null));
        results.put("Contains1N", new FqlQuery("SELECT DISTINCT a1_, a2_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Department AS a2_ WHERE (a1_.departments CONTAINS a2_ AND a1_.name = 'CompanyA')", null));
        results.put("ContainsN1", new FqlQuery("SELECT DISTINCT a1_, a2_ FROM org.flymine.model.testmodel.Department AS a1_, org.flymine.model.testmodel.Company AS a2_ WHERE (a1_.company CONTAINS a2_ AND a2_.name = 'CompanyA')", null));
        results.put("ContainsMN", new FqlQuery("SELECT DISTINCT a1_, a2_ FROM org.flymine.model.testmodel.Contractor AS a1_, org.flymine.model.testmodel.Company AS a2_ WHERE (a1_.companys CONTAINS a2_ AND a1_.name = 'ContractorA')", null));
        results.put("ContainsDuplicatesMN", new FqlQuery("SELECT DISTINCT a1_, a2_ FROM org.flymine.model.testmodel.Contractor AS a1_, org.flymine.model.testmodel.Company AS a2_ WHERE a1_.oldComs CONTAINS a2_", null));
        results.put("SimpleGroupBy", new FqlQuery("SELECT DISTINCT a1_, COUNT(*) AS a2_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Department AS a3_ WHERE a1_.departments CONTAINS a3_ GROUP BY a1_", null));
        results.put("MultiJoin", new FqlQuery("SELECT DISTINCT a1_, a2_, a3_, a4_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Department AS a2_, org.flymine.model.testmodel.Manager AS a3_, org.flymine.model.testmodel.Address AS a4_ WHERE (a1_.departments CONTAINS a2_ AND a2_.manager CONTAINS a3_ AND a3_.address CONTAINS a4_ AND a3_.name = 'EmployeeA1')", null));
        results.put("SelectComplex", new FqlQuery("SELECT DISTINCT AVG(a1_.vatNumber) + 20 AS a3_, a2_.name AS a4_, a2_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Department AS a2_ GROUP BY a2_", null));
        results.put("SelectClassAndSubClasses", new FqlQuery("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Employee AS a1_ ORDER BY a1_.name", null));
        results.put("SelectInterfaceAndSubClasses", new FqlQuery("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Employable AS a1_", null));
        results.put("SelectInterfaceAndSubClasses2", new FqlQuery("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.RandomInterface AS a1_", null));
        results.put("SelectInterfaceAndSubClasses3", new FqlQuery("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.ImportantPerson AS a1_", null));
        results.put("OrderByAnomaly", new FqlQuery("SELECT DISTINCT 5 AS a2_, a1_.name AS a3_ FROM org.flymine.model.testmodel.Company AS a1_", null));
        results.put("SelectUnidirectionalCollection", new FqlQuery("SELECT DISTINCT a2_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Secretary AS a2_ WHERE (a1_.name = 'CompanyA' AND a1_.secretarys CONTAINS a2_)", null));
        fq = new FqlQuery("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Department AS a2_ WHERE (a1_ = ? AND a1_.departments CONTAINS a2_ AND a2_ IN (SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Department AS a1_ WHERE a1_ = ?))", null);
        fq.setParameters(Arrays.asList(new Object [] {data.get("CompanyA"), data.get("DepartmentA1")}));
        results.put("SelectClassObjectSubquery", fq);
        results.put("EmptyAndConstraintSet", new FqlQuery("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE true", null));
        results.put("EmptyOrConstraintSet", new FqlQuery("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE false", null));
        Set bag1 = new HashSet();
        bag1.add("hello");
        bag1.add("goodbye");
        bag1.add("CompanyA");
        bag1.add(new Integer(5));
        fq = new FqlQuery("SELECT DISTINCT Company FROM org.flymine.model.testmodel.Company AS Company WHERE Company.name IN ?", null);
        fq.setParameters(Collections.singletonList(bag1));
        results.put("BagConstraint", fq);
        Set bag2 = new HashSet();
        bag2.add("hello");
        bag2.add("goodbye");
        bag2.add("CompanyA");
        bag2.add(new Integer(5));
        bag2.add(data.get("CompanyA"));
        fq = new FqlQuery("SELECT DISTINCT Company FROM org.flymine.model.testmodel.Company AS Company WHERE Company IN ?", null);
        fq.setParameters(Collections.singletonList(bag2));
        results.put("BagConstraint2", fq);
        results.put("InterfaceField", new FqlQuery("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Employable AS a1_ WHERE a1_.name = 'EmployeeA1'", null));
        fq = new FqlQuery("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.HasAddress AS a1_, org.flymine.model.testmodel.Address AS a2_ WHERE (a1_.address CONTAINS a2_ AND a2_ = ?)", null);
        fq.setParameters(Collections.singletonList(data.get("Employee Street, AVille")));
        results.put("InterfaceReference", fq);
        fq = new FqlQuery("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.HasSecretarys AS a1_, org.flymine.model.testmodel.Secretary AS a2_ WHERE (a1_.secretarys CONTAINS a2_ AND a2_ = ?)", null);
        fq.setParameters(Collections.singletonList(data.get("Secretary1")));
        results.put("InterfaceCollection", fq);
        Set res = new HashSet();
        res.add(new FqlQuery("SELECT DISTINCT a1_, a1_.debt AS a2_, a1_.vatNumber AS a3_ FROM (org.flymine.model.testmodel.Company, org.flymine.model.testmodel.Broke) AS a1_ WHERE (a1_.debt > 0 AND a1_.vatNumber > 0)", null));
        res.add(new FqlQuery("SELECT DISTINCT a1_, a1_.debt AS a2_, a1_.vatNumber AS a3_ FROM (org.flymine.model.testmodel.Broke, org.flymine.model.testmodel.Company) AS a1_ WHERE (a1_.debt > 0 AND a1_.vatNumber > 0)", null));
        results.put("DynamicInterfacesAttribute", res);
        res = new HashSet();
        res.add(new FqlQuery("SELECT DISTINCT a1_ FROM (org.flymine.model.testmodel.Employable, org.flymine.model.testmodel.Broke) AS a1_", null));
        res.add(new FqlQuery("SELECT DISTINCT a1_ FROM (org.flymine.model.testmodel.Broke, org.flymine.model.testmodel.Employable) AS a1_", null));
        results.put("DynamicClassInterface", res);
        res = new HashSet();
        res.add(new FqlQuery("SELECT DISTINCT a1_, a2_, a3_ FROM (org.flymine.model.testmodel.Department, org.flymine.model.testmodel.Broke) AS a1_, org.flymine.model.testmodel.Company AS a2_, org.flymine.model.testmodel.Bank AS a3_ WHERE (a2_.departments CONTAINS a1_ AND a3_.debtors CONTAINS a1_)", null));
        res.add(new FqlQuery("SELECT DISTINCT a1_, a2_, a3_ FROM (org.flymine.model.testmodel.Broke, org.flymine.model.testmodel.Department) AS a1_, org.flymine.model.testmodel.Company AS a2_, org.flymine.model.testmodel.Bank AS a3_ WHERE (a2_.departments CONTAINS a1_ AND a3_.debtors CONTAINS a1_)", null));
        results.put("DynamicClassRef1", res);
        res = new HashSet();
        res.add(new FqlQuery("SELECT DISTINCT a1_, a2_, a3_ FROM (org.flymine.model.testmodel.Department, org.flymine.model.testmodel.Broke) AS a1_, org.flymine.model.testmodel.Company AS a2_, org.flymine.model.testmodel.Bank AS a3_ WHERE (a1_.company CONTAINS a2_ AND a1_.bank CONTAINS a3_)", null));
        res.add(new FqlQuery("SELECT DISTINCT a1_, a2_, a3_ FROM (org.flymine.model.testmodel.Broke, org.flymine.model.testmodel.Department) AS a1_, org.flymine.model.testmodel.Company AS a2_, org.flymine.model.testmodel.Bank AS a3_ WHERE (a1_.company CONTAINS a2_ AND a1_.bank CONTAINS a3_)", null));
        results.put("DynamicClassRef2", res);
        res = new HashSet();
        res.add(new FqlQuery("SELECT DISTINCT a1_, a2_, a3_ FROM (org.flymine.model.testmodel.Company, org.flymine.model.testmodel.Bank) AS a1_, org.flymine.model.testmodel.Department AS a2_, org.flymine.model.testmodel.Broke AS a3_ WHERE (a1_.departments CONTAINS a2_ AND a1_.debtors CONTAINS a3_)", null));
        res.add(new FqlQuery("SELECT DISTINCT a1_, a2_, a3_ FROM (org.flymine.model.testmodel.Bank, org.flymine.model.testmodel.Company) AS a1_, org.flymine.model.testmodel.Department AS a2_, org.flymine.model.testmodel.Broke AS a3_ WHERE (a1_.departments CONTAINS a2_ AND a1_.debtors CONTAINS a3_)", null));
        results.put("DynamicClassRef3", res);
        res = new HashSet();
        res.add(new FqlQuery("SELECT DISTINCT a1_, a2_, a3_ FROM (org.flymine.model.testmodel.Company, org.flymine.model.testmodel.Bank) AS a1_, org.flymine.model.testmodel.Department AS a2_, org.flymine.model.testmodel.Broke AS a3_ WHERE (a2_.company CONTAINS a1_ AND a3_.bank CONTAINS a1_)", null));
        res.add(new FqlQuery("SELECT DISTINCT a1_, a2_, a3_ FROM (org.flymine.model.testmodel.Bank, org.flymine.model.testmodel.Company) AS a1_, org.flymine.model.testmodel.Department AS a2_, org.flymine.model.testmodel.Broke AS a3_ WHERE (a2_.company CONTAINS a1_ AND a3_.bank CONTAINS a1_)", null));
        results.put("DynamicClassRef4", res);
        res = new HashSet();
        res.add(new FqlQuery("SELECT DISTINCT a1_ FROM (org.flymine.model.testmodel.Employable, org.flymine.model.testmodel.Broke) AS a1_, (org.flymine.model.testmodel.HasAddress, org.flymine.model.testmodel.Broke) AS a2_ WHERE a1_ = a2_", null));
        res.add(new FqlQuery("SELECT DISTINCT a1_ FROM (org.flymine.model.testmodel.Broke, org.flymine.model.testmodel.Employable) AS a1_, (org.flymine.model.testmodel.HasAddress, org.flymine.model.testmodel.Broke) AS a2_ WHERE a1_ = a2_", null));
        res.add(new FqlQuery("SELECT DISTINCT a1_ FROM (org.flymine.model.testmodel.Employable, org.flymine.model.testmodel.Broke) AS a1_, (org.flymine.model.testmodel.Broke, org.flymine.model.testmodel.HasAddress) AS a2_ WHERE a1_ = a2_", null));
        res.add(new FqlQuery("SELECT DISTINCT a1_ FROM (org.flymine.model.testmodel.Broke, org.flymine.model.testmodel.Employable) AS a1_, (org.flymine.model.testmodel.Broke, org.flymine.model.testmodel.HasAddress) AS a2_ WHERE a1_ = a2_", null));
        results.put("DynamicClassConstraint", res);
        results.put("ContainsConstraintNull", new FqlQuery("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Employee AS a1_ WHERE a1_.address IS NULL", null));
        results.put("ContainsConstraintNotNull", new FqlQuery("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Employee AS a1_ WHERE a1_.address IS NOT NULL", null));
        results.put("SimpleConstraintNull", new FqlQuery("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Manager AS a1_ WHERE a1_.title IS NULL", null));
        results.put("SimpleConstraintNotNull", new FqlQuery("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Manager AS a1_ WHERE a1_.title IS NOT NULL", null));
    }
}
