package org.flymine.web;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.HashMap;

import servletunit.struts.MockStrutsTestCase;

public class QueryClassSelectActionTest extends MockStrutsTestCase
{
    public QueryClassSelectActionTest(String testName) {
        super(testName);
    }

    public void testSelectValidClassName() throws Exception {
        setRequestPathInfo("/queryClassSelect");
        getSession().setAttribute("queryClasses", new HashMap());
        addRequestParameter("className", "org.flymine.model.testmodel.Company");
        addRequestParameter("action", "Add");
        actionPerform();

        verifyForward("buildquery");
        assertEquals(1, ((Map) getSession().getAttribute("queryClasses")).size());
    }

    public void testSelectNullClassName() throws Exception {
        setRequestPathInfo("/queryClassSelect");
        getSession().setAttribute("queryClasses", new HashMap());
        addRequestParameter("action", "Add");
        actionPerform();

        verifyForward("error");
        assertEquals(0, ((Map) getSession().getAttribute("queryClasses")).size());
    }
}
