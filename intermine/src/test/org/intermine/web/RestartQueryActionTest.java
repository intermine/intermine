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

import java.util.Map;
import java.util.HashMap;

public class RestartQueryActionTest extends MockStrutsTestCase
{
    public RestartQueryActionTest(String testName) {
        super(testName);
    }

    public void testRestart() throws Exception {
        HttpSession session = getSession();
        setRequestPathInfo("/query");
        addRequestParameter("action", "Reset query");

        String anAlias = "ClassAlias_0";

        Map queryClasses = new HashMap();
        DisplayQueryClass displayQueryClass = new DisplayQueryClass();

        displayQueryClass.setType("org.flymine.model.testmodel.Department");

        queryClasses.put(anAlias, displayQueryClass);

        session.setAttribute(Constants.QUERY_CLASSES, queryClasses);
        session.setAttribute(Constants.EDITING_ALIAS, anAlias);

        actionPerform();
        
        verifyForward("buildquery");
        assertNull(session.getAttribute(Constants.QUERY_CLASSES));
        assertNull(session.getAttribute(Constants.EDITING_ALIAS));
    }
}
