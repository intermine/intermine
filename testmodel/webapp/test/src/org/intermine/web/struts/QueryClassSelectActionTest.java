package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import servletunit.struts.MockStrutsTestCase;

public class QueryClassSelectActionTest extends MockStrutsTestCase
{
    public QueryClassSelectActionTest(String testName) {
        super(testName);
    }

    public void tearDown() throws Exception {
         getActionServlet().destroy();
    }

    public void testSelectValidClassName() throws Exception {
        setRequestPathInfo("/queryClassSelect");
        addRequestParameter("className", "org.intermine.model.testmodel.Company");
        addRequestParameter("action", "Select");

        actionPerform();

        verifyNoActionErrors();
        verifyForward("query");
        //assertEquals("org.intermine.model.testmodel.Company",
        //           getRequest().getAttribute("class"));
    }
}