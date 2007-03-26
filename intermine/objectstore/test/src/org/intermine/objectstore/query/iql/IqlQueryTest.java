package org.intermine.objectstore.query.iql;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Iterator;

import junit.framework.Test;

import org.intermine.objectstore.query.Query;
import org.intermine.testing.OneTimeTestCase;

public class IqlQueryTest extends IqlQueryTestCase
{
    public IqlQueryTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(IqlQueryTest.class);
    }

    public static void oneTimeSetUp() throws Exception {
        IqlQueryTestCase.oneTimeSetUp();

        results.put("WhereSimpleNegEquals", new IqlQuery("SELECT DISTINCT a1_.name AS a2_ FROM org.intermine.model.testmodel.Company AS a1_ WHERE a1_.vatNumber != 1234", null));
        results.put("WhereNegSubQueryClass", new IqlQuery("SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Company AS a1_ WHERE a1_ NOT IN (SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Company AS a1_ WHERE a1_.name = 'CompanyA')", null));
        results.put("WhereNegClassClass", new IqlQuery("SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Company AS a2_ WHERE a1_ != a2_", null));
        results.put("ContainsNeg11", new IqlQuery("SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Manager AS a2_ WHERE (a1_.manager DOES NOT CONTAIN a2_ AND a1_.name = 'DepartmentA1')", null));
        results.put("EmptyNandConstraintSet", new IqlQuery("SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Company AS a1_ WHERE false", null));
        results.put("EmptyNorConstraintSet", new IqlQuery("SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Company AS a1_ WHERE true", null));
    }

    public void executeTest(String type) throws Exception {
        Query orig = ((Query) queries.get(type));
        Object res = results.get(type);
        IqlQuery fqGenerated = new IqlQuery(orig);
        if (res instanceof IqlQuery) {
            IqlQuery fq = (IqlQuery) res;

            // This is testing whether new IqlQuery(Query) gives the IqlQueries above
            assertEquals(type + " has failed: " + fqGenerated.getQueryString(), fq.getQueryString(), fqGenerated.getQueryString());
            assertEquals(type + " has failed: " + fq.getParameters().getClass().getName() + " versus " + fqGenerated.getParameters().getClass().getName(), fq.getParameters(), fqGenerated.getParameters());
        } else {
            Iterator resIter = ((Collection) res).iterator();
            boolean passed = false;
            while (resIter.hasNext()) {
                IqlQuery fq = (IqlQuery) resIter.next();
                passed = passed || ((fq.getQueryString().equals(fqGenerated.getQueryString())) && (fq.getParameters().equals(fqGenerated.getParameters())));
            }
            assertTrue(type + " has failed: " + fqGenerated.toString(), passed);
        }
    }

    public void testConstructNullQuery() throws Exception {
        try {
            IqlQuery fq = new IqlQuery(null, "org.intermine.model.testmodel");
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testConstructEmptyQuery() throws Exception {
        try {
            IqlQuery fq = new IqlQuery("", "org.intermine.model.testmodel");
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testConstructEmptyPackageName() throws Exception {
        try {
            IqlQuery fq = new IqlQuery("select a1_ from Company as a1_", "");
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }
}
