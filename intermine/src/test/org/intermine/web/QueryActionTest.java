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

public class QueryActionTest extends MockStrutsTestCase {

    public QueryActionTest(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSuccessfulQuery() {
       setRequestPathInfo("/query");
       addRequestParameter("querystring","select a1_ from Company as a1_");
       actionPerform();
       verifyForward("results");
       assertNotNull((List) getRequest().getAttribute("results"));
       verifyNoActionErrors();
    }

    public void testEmptyQuery() {
        setRequestPathInfo("/query");
        addRequestParameter("querystring","");
        actionPerform();
        verifyForward("error");
        assertNull((String) getRequest().getAttribute("results"));
    }

    public void testRubbishQuery() {
        setRequestPathInfo("/query");
        addRequestParameter("querystring","some rubbish");
        actionPerform();
        verifyForward("error");
        assertNull((String) getRequest().getAttribute("results"));
    }
}
