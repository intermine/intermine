package org.intermine.web;

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
import org.intermine.web.SessionMethods;

public class PortalQueryTest extends MockStrutsTestCase
{
    public PortalQueryTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        super.setUp();
        SessionMethods.initSession(this.getSession());
    }

    public void tearDown() throws Exception {
        getActionServlet().destroy();
    }

    public void testGoodExternalId() throws Exception {
        setRequestPathInfo("/portal");
        addRequestParameter("externalid", "EmployeeA1");

        actionPerform();
        System.out.println("stacktrace: " + getRequest().getAttribute("stacktrace"));
        verifyNoActionErrors();

        // would be nice to test that we forward to the correct
        // object details URL but I can't figure out how to get
        // the forward returned.
    }
}
