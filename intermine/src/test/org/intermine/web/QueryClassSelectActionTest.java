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

import java.util.Map;
import java.util.HashMap;

import servletunit.struts.MockStrutsTestCase;

public class QueryClassSelectActionTest extends MockStrutsTestCase
{
    public QueryClassSelectActionTest(String testName) {
        super(testName);
    }

    public void testSelectValidClassName() throws Exception {
        setRequestPathInfo("/queryClassSelect");
        QueryClassSelectForm form = new QueryClassSelectForm();
        form.setClassName("org.intermine.model.testmodel.Company");
        setActionForm(form);
        addRequestParameter("action", "Add");
        actionPerform();

        verifyForward("query");
        assertEquals("org.intermine.model.testmodel.Company",
                     getRequest().getAttribute("class"));
    }

    public void testSelectNullClassName() throws Exception {
        setRequestPathInfo("/queryClassSelect");
        getSession().setAttribute(Constants.QUERY_CLASSES, new HashMap());
        actionPerform();

        verifyForward("error");
        assertEquals(0, ((Map) getSession().getAttribute(Constants.QUERY_CLASSES)).size());
    }
}
