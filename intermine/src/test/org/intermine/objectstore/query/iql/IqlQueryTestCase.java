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


import org.flymine.objectstore.query.Query;
import org.flymine.testing.OneTimeTestCase;
import org.flymine.objectstore.ObjectStoreQueriesTestCase;

/**
 * Test for testing the parser on the flymine query object.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public abstract class FqlQueryTestCase extends ObjectStoreQueriesTestCase
{
    public FqlQueryTestCase(String arg) {
        super(arg);
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(FqlQueryTestCase.class);
    }

    public static void oneTimeSetUp() throws Exception {
        ObjectStoreQueriesTestCase.oneTimeSetUp();

        setUpResults();
    }

    /**
     * Set up all the results expected for a given subset of queries
     */
    public static void setUpResults() {
        results.put("SelectSimpleObject", "select a1_ from Company as a1_");
        results.put("SubQuery", "select a1_.a1_.name AS a2_, a1_.a2_ AS a3_ from (select a1_, 5 as a2_ from Company as a1_) as a1_");
        results.put("WhereSimpleEquals", "select a1_.name as a2_ from Company as a1_ where a1_.vatNumber = 1234");
        results.put("WhereSimpleNotEquals", "select a1_.name as a2_ from Company as a1_ where a1_.vatNumber != 1234");
        results.put("WhereSimpleLike", "select a1_.name as a2_ from Company as a1_ where a1_.name like 'Company%'");
        results.put("WhereEqualsString", "select a1_.name as a2_ from Company as a1_ where a1_.name = 'CompanyA'");
        results.put("WhereAndSet", "select a1_.name as a2_ from Company as a1_ where a1_.name like 'Company%' and a1_.vatNumber > 2000");
        results.put("WhereOrSet", "select a1_.name as a2_ from Company as a1_ where a1_.name like 'CompanyA%' or a1_.vatNumber > 2000");
        results.put("WhereNotSet", "select a1_.name as a2_ from Company as a1_ where not (a1_.name like 'Company%' and a1_.vatNumber > 2000)");
        results.put("WhereSubQueryField", "select a1_ from Department as a1_ where a1_.name in (select a1_.name as a2_ from Department as a1_) order by a1_.name");
        results.put("WhereSubQueryClass", "select a1_ from Company as a1_ where a1_ in (select a1_ from Company as a1_ where a1_.name = 'CompanyA')");
        results.put("WhereNotSubQueryClass", "select a1_ from Company as a1_ where a1_ not in (select a1_ from Company as a1_ where a1_.name = 'CompanyA')");
        results.put("WhereNegSubQueryClass", "select a1_ from Company as a1_ where not (a1_ in (select a1_ from Company as a1_ where a1_.name = 'CompanyA'))");
        results.put("WhereClassClass", "select a1_, a2_ from Company as a1_, Company as a2_ where a1_ = a2_");
        results.put("WhereNotClassClass", "select a1_, a2_ from Company as a1_, Company as a2_ where a1_ != a2_");
        results.put("WhereNegClassClass", "select a1_, a2_ from Company as a1_, Company as a2_ where not a1_ = a2_");
        results.put("Contains11", "select a1_, a2_ from Department as a1_, Manager as a2_ where a1_.manager contains a2_ and a1_.name = 'DepartmentA1'");
        results.put("ContainsNot11", "select a1_, a2_ from Department as a1_, Manager as a2_ where a1_.manager does not contain a2_ and a1_.name = 'DepartmentA1'");
        results.put("ContainsNeg11", "select a1_, a2_ from Department as a1_, Manager as a2_ where not a1_.manager contains a2_ and a1_.name = 'DepartmentA1'");
        results.put("Contains1N", "select a1_, a2_ from Company as a1_, Department as a2_ where a1_.departments contains a2_ and a1_.name = 'CompanyA'");
        results.put("ContainsN1", "select a1_, a2_ from Department as a1_, Company as a2_ where a1_.company contains a2_ and a2_.name = 'CompanyA'");
        results.put("ContainsMN", "select a1_, a2_ from Contractor as a1_, Company as a2_ where a1_.companys contains a2_ and a1_.name = 'ContractorA'");
        results.put("ContainsDuplicatesMN", "select a1_, a2_ from Contractor as a1_, Company as a2_ where a1_.oldComs contains a2_");
        results.put("SimpleGroupBy", "select a1_, count(*) as a2_ from Company as a1_, Department as a3_ where a1_.departments contains a3_ group by a1_");
        results.put("MultiJoin", "select a1_, a2_, a3_, a4_ from Company as a1_, Department as a2_, Manager as a3_, Address as a4_ where a1_.departments contains a2_ and a2_.manager contains a3_ and a3_.address contains a4_ and a3_.name = 'EmployeeA1'");
        results.put("SelectComplex", "select avg(a1_.vatNumber) + 20 as a3_, a2_.name as a4_, a2_ from Company as a1_, Department as a2_ group by a2_");
        results.put("SelectClassAndSubClasses", "select a1_ from Employee as a1_ order by a1_.name");
        results.put("SelectInterfaceAndSubClasses", "select a1_ from Employable as a1_");
        results.put("SelectInterfaceAndSubClasses2", "select a1_ from RandomInterface as a1_");
        results.put("SelectInterfaceAndSubClasses3", "select a1_ from ImportantPerson as a1_");
        results.put("OrderByAnomaly", "select 5 as a2_, a1_.name as a3_ from Company as a1_");
    }

}
