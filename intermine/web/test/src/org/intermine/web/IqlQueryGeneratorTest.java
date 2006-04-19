package org.intermine.web;

/*
 * Copyright (C) 2002-2006 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.intermine.objectstore.query.Query;

public class IqlQueryGeneratorTest extends TestCase
{
    public void test1() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee\"></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_");
    }

    public void test2() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.age\" type=\"int\"><constraint op=\"&gt;=\" value=\"10\" description=\"\" identifier=\"\" code=\"A\"></constraint></node></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE a1_.age >= 10");
    }

    public void test3() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee\" constraintLogic=\"A and B\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.age\" type=\"int\"><constraint op=\"&gt;=\" value=\"10\" description=\"\" identifier=\"\" code=\"A\"></constraint></node><node path=\"Employee.fullTime\" type=\"boolean\"><constraint op=\"=\" value=\"true\" description=\"\" identifier=\"\" code=\"B\"></constraint></node></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE (a1_.age >= 10 AND a1_.fullTime = true)");
    }
    
    public void test4() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee\" constraintLogic=\"A or B\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.age\" type=\"int\"><constraint op=\"&gt;=\" value=\"10\" description=\"\" identifier=\"\" code=\"A\"></constraint></node><node path=\"Employee.fullTime\" type=\"boolean\"><constraint op=\"=\" value=\"true\" description=\"\" identifier=\"\" code=\"B\"></constraint></node></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE (a1_.age >= 10 OR a1_.fullTime = true)");
    }

    public void test5() throws Exception {
        doQuery("<query name=\"test\" model=\"testmodel\" view=\"Employee\" constraintLogic=\"(A or B) and C\"><node path=\"Employee\" type=\"Employee\"></node><node path=\"Employee.age\" type=\"int\"><constraint op=\"&gt;=\" value=\"10\" description=\"\" identifier=\"\" code=\"A\"></constraint></node><node path=\"Employee.fullTime\" type=\"boolean\"><constraint op=\"=\" value=\"true\" description=\"\" identifier=\"\" code=\"B\"></constraint></node><node path=\"Employee.name\" type=\"String\"><constraint op=\"=\" value=\"EmployeeA2\" description=\"\" identifier=\"\" code=\"C\"></constraint></node></query>",
                "SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE ((a1_.age >= 10 OR a1_.fullTime = true) AND LOWER(a1_.name) = 'employeea2')");
    }

    public void doQuery(String web, String iql) throws Exception {
        Map parsed = PathQueryBinding.unmarshal(new StringReader(web));
        PathQuery pq = (PathQuery) parsed.get("test");
        Query q = MainHelper.makeQuery(pq, Collections.EMPTY_MAP);
        String got = q.toString();
        assertEquals(iql, got);
    }
}
