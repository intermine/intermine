package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import servletunit.struts.MockStrutsTestCase;

import org.intermine.objectstore.query.iql.IqlQuery;

public class RunQueryActionTest extends MockStrutsTestCase
{
    public RunQueryActionTest(String testName) {
        super(testName);
    }

    public void testRunValidQuery() {
        setRequestPathInfo("/runQuery");
        getSession().setAttribute(Constants.QUERY, new IqlQuery("select c from Company as c", "org.intermine.model.testmodel").toQuery());
        actionPerform();
        verifyForward("results");
        assertNotNull(getRequest().getAttribute("results"));
        verifyNoActionErrors();
    }

    public void testRunNoQueryPresent() {
        setRequestPathInfo("/runQuery");
        actionPerform();
        verifyForward("buildquery");
        assertNull(getSession().getAttribute("results"));
        verifyNoActionErrors();
    }
}
