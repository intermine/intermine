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

import servletunit.struts.MockStrutsTestCase;

import javax.servlet.http.HttpSession;

public class RestartQueryActionTest extends MockStrutsTestCase {

    public RestartQueryActionTest(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testRestart() throws Exception {
        setRequestPathInfo("/restartQuery");
        HttpSession session = getSession();
        session.setAttribute("query", "query");
        session.setAttribute("queryClass", "queryClass");
        session.setAttribute("ops", "ops");
        actionPerform();
        verifyForward("buildquery");
        assertNull(session.getAttribute("query"));
        assertNull(session.getAttribute("queryClass"));
        assertNull(session.getAttribute("ops"));
    }

}
