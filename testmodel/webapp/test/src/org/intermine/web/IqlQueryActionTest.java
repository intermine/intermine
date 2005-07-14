package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import servletunit.struts.MockStrutsTestCase;

public class IqlQueryActionTest extends MockStrutsTestCase {

    public IqlQueryActionTest(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
         getActionServlet().destroy();
    }

    public void testSubmitSuccessfulQuery() {
        setRequestPathInfo("/iqlQueryAction");
        addRequestParameter("querystring","select a1_ from Company as a1_");
        addRequestParameter("action", "Run query");
        actionPerform();
        verifyNoActionErrors();
        //verifyForward("/results.do?table=results");
        //assertNotNull(getSession().getAttribute(Constants.QUERY_RESULTS));
        verifyNoActionErrors();
    }

    public void testSubmitEmptyQuery() {
        setRequestPathInfo("/iqlQueryAction");
        addRequestParameter("querystring","");
        addRequestParameter("action", "Run query");
        actionPerform();
        verifyActionErrors(new String[] {"errors.iqlquery.illegalargument"});
        verifyForward("iqlQuery");
        //assertNull(getSession().getAttribute(Constants.QUERY_RESULTS));
    }

    public void testSubmitRubbishQuery() {
        setRequestPathInfo("/iqlQueryAction");
        addRequestParameter("querystring","some rubbish");
        addRequestParameter("action", "Run query");
        actionPerform();
        verifyActionErrors(new String[] {"errors.iqlquery.illegalargument"});
        verifyForward("iqlQuery");
        //assertNull(getSession().getAttribute(Constants.QUERY_RESULTS));
    }
}
