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

import java.util.List;

import servletunit.struts.MockStrutsTestCase;

import org.flymine.objectstore.query.Query;

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
        addRequestParameter("action", "Run");
        actionPerform();
        verifyForward("runquery");
        assertNotNull(getSession().getAttribute(Constants.QUERY));
        verifyNoActionErrors();
    }

    public void testSubmitEmptyQuery() {
        setRequestPathInfo("/fqlquery");
        addRequestParameter("querystring","");
        addRequestParameter("action", "Run");
        actionPerform();
        verifyForward("error");
        assertNull(getSession().getAttribute(Constants.QUERY));
    }

    public void testSubmitRubbishQuery() {
        setRequestPathInfo("/fqlquery");
        addRequestParameter("querystring","some rubbish");
        addRequestParameter("action", "Run");
        actionPerform();
        verifyForward("error");
        assertNull(getSession().getAttribute(Constants.QUERY));
    }

    public void testViewSuccessfulQuery() {
        setRequestPathInfo("/fqlquery");
        addRequestParameter("querystring","select a1_ from Company as a1_");
        addRequestParameter("action", "View");
        actionPerform();
        verifyForward("buildquery");
        assertEquals("SELECT a1_ FROM org.flymine.model.testmodel.Company AS a1_", ((Query) getSession().getAttribute(Constants.QUERY)).toString());
        verifyNoActionErrors();
    }

    public void testViewEmptyQuery() {
        setRequestPathInfo("/fqlquery");
        addRequestParameter("querystring","");
        addRequestParameter("action", "View");
        actionPerform();
        verifyForward("error");
        assertNull((String) getSession().getAttribute(Constants.QUERY));
    }

    public void testViewRubbishQuery() {
        setRequestPathInfo("/fqlquery");
        addRequestParameter("querystring","some rubbish");
        addRequestParameter("action", "View");
        actionPerform();
        verifyForward("error");
        assertNull((String) getSession().getAttribute(Constants.QUERY));
    }
}
