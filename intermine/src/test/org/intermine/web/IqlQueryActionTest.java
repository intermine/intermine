package org.intermine.web;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.Query;

import servletunit.struts.MockStrutsTestCase;

public class FqlQueryActionTest extends MockStrutsTestCase {

    public FqlQueryActionTest(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSubmitSuccessfulQuery() {
        setRequestPathInfo("/fqlquery");
        addRequestParameter("querystring","select a1_ from Company as a1_");
        addRequestParameter("action", "Run query");
        actionPerform();
        verifyForward("runquery");
        assertNotNull(getSession().getAttribute(Constants.QUERY));
        verifyNoActionErrors();
    }

    public void testSubmitEmptyQuery() {
        setRequestPathInfo("/fqlquery");
        addRequestParameter("querystring","");
        addRequestParameter("action", "Run query");
        actionPerform();
        verifyForward("error");
        assertNull(getSession().getAttribute(Constants.QUERY));
    }

    public void testSubmitRubbishQuery() {
        setRequestPathInfo("/fqlquery");
        addRequestParameter("querystring","some rubbish");
        addRequestParameter("action", "Run query");
        actionPerform();
        verifyForward("error");
        assertNull(getSession().getAttribute(Constants.QUERY));
    }

    public void testViewSuccessfulQuery() {
        setRequestPathInfo("/fqlquery");
        addRequestParameter("querystring","select a1_ from Company as a1_");
        addRequestParameter("action", "Query composer");
        actionPerform();
        verifyForward("buildquery");
        assertEquals("SELECT a1_ FROM org.intermine.model.testmodel.Company AS a1_",
                     ((Query) getSession().getAttribute(Constants.QUERY)).toString());
        verifyNoActionErrors();
    }

    public void testViewEmptyQuery() {
        setRequestPathInfo("/fqlquery");
        addRequestParameter("querystring","");
        addRequestParameter("action", "Query composer");
        actionPerform();
        verifyForward("error");
        assertNull((String) getSession().getAttribute(Constants.QUERY));
    }

    public void testViewRubbishQuery() {
        setRequestPathInfo("/fqlquery");
        addRequestParameter("querystring","some rubbish");
        addRequestParameter("action", "Query composer");
        actionPerform();
        verifyForward("error");
        assertNull((String) getSession().getAttribute(Constants.QUERY));
    }
}
