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
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.ConstraintSet;
import org.flymine.testing.OneTimeTestCase;

import org.flymine.model.testmodel.Company;

public class FqlQueryTest extends FqlQueryTestCase
{
    public FqlQueryTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(FqlQueryTest.class);
    }

    public static void oneTimeSetUp() throws Exception {
        FqlQueryTestCase.oneTimeSetUp();

        results.put("WhereNegSubQueryClass", new FqlQuery("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_ NOT IN (SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE a1_.name = 'CompanyA')", null));
        results.put("WhereNegClassClass", new FqlQuery("SELECT DISTINCT a1_, a2_ FROM org.flymine.model.testmodel.Company AS a1_, org.flymine.model.testmodel.Company AS a2_ WHERE a1_ != a2_", null));
        results.put("ContainsNeg11", new FqlQuery("SELECT DISTINCT a1_, a2_ FROM org.flymine.model.testmodel.Department AS a1_, org.flymine.model.testmodel.Manager AS a2_ WHERE (a1_.manager DOES NOT CONTAIN a2_ AND a1_.name = 'DepartmentA1')", null));
        results.put("EmptyNandConstraintSet", new FqlQuery("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE false", null));
        results.put("EmptyNorConstraintSet", new FqlQuery("SELECT DISTINCT a1_ FROM org.flymine.model.testmodel.Company AS a1_ WHERE true", null));
    }

    public void executeTest(String type) throws Exception {
        Query orig = ((Query) queries.get(type));
        FqlQuery fq = (FqlQuery) results.get(type);

        if (fq == null) {
            fail("No results set up for test " + type);
        }

        // This is testing whether new FqlQuery(Query) gives the FqlQueries above
        FqlQuery fqGenerated = new FqlQuery(orig);
        assertEquals(type + " has failed", fq.getQueryString(), fqGenerated.getQueryString());;
        assertEquals(type + " has failed", fq.getParameters(), fqGenerated.getParameters());;

        // This is testing whether the FqlQueries above are parsed into the correct query
        // This really duplicates FqlQueryParserTest but here we call the FqlQuery.toQuery() method
        //Query parsed = fq.toQuery();
        //assertEquals(type + " has failed", orig, parsed);
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
