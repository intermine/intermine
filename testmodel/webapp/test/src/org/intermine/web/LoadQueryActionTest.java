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

import java.util.HashMap;

import org.intermine.web.logic.Constants;

import servletunit.struts.MockStrutsTestCase;

/**
 * Tests for the LoadQueryAction class
 *
 * @author Kim Rutherford
 */
public class LoadQueryActionTest extends MockStrutsTestCase
{
    public LoadQueryActionTest(String arg) {
        super(arg);
    }


    public void tearDown() throws Exception {
         getActionServlet().destroy();
    }

    public void testLoadXml() {
        String xml = "<query name=\"\" model=\"testmodel\" view=\"Employee Employee.name\">\n" +
                "</query>";

        addRequestParameter("method", "xml");
        addRequestParameter("query", xml);
        addRequestParameter("skipBuilder", "false");

        setRequestPathInfo("/loadQuery");

        actionPerform();
        verifyNoActionErrors();
        verifyForward("query");
        assertNotNull(getSession().getAttribute(Constants.QUERY));
    }

    public void testLoadXmlSkipBuilder() {
        String xml = "<query name=\"\" model=\"testmodel\" view=\"Employee Employee.name\">\n" +
                "</query>";

        addRequestParameter("method", "xml");
        addRequestParameter("query", xml);
        addRequestParameter("skipBuilder", "true");

        setRequestPathInfo("/loadQuery");

        actionPerform();
        verifyNoActionErrors();
        assertEquals("/pollBrowseQuery.do?qid=0", getActualForward());
        assertNotNull(getSession().getAttribute(Constants.QUERY));
    }
}
