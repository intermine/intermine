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

import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.fql.FqlQuery;

public class RunQueryActionTest extends MockStrutsTestCase {

    public RunQueryActionTest(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testRunValidQuery() {
        setRequestPathInfo("/runQuery");
        getSession().setAttribute("query", new FqlQuery("select c from Company as c", "org.flymine.model.testmodel").toQuery());
        actionPerform();
        verifyForward("results");
        assertNotNull(getSession().getAttribute("results"));
        verifyNoActionErrors();
    }

    public void testRunNoQueryPresent() {
        setRequestPathInfo("/runQuery");
        actionPerform();
        verifyForward("error");
        assertNull(getSession().getAttribute("results"));
    }
}
