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

import javax.servlet.http.HttpSession;

import servletunit.struts.MockStrutsTestCase;

import org.flymine.objectstore.query.Query;
import org.flymine.model.testmodel.Company;
import org.flymine.metadata.Model;

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

    public void testSelectSuccessful() throws Exception {

    }

    /*    public void testSubmitSuccessfulQuery() {
        setRequestPathInfo("/query");
        addRequestParameter("querystring","select a1_ from Company as a1_");
        addRequestParameter("action", "Submit");
        actionPerform();
        verifyForward("results");
        assertNotNull((List) getRequest().getAttribute("results"));
        verifyNoActionErrors();
    }

    public void testSubmitEmptyQuery() {
        setRequestPathInfo("/query");
        addRequestParameter("querystring","");
        addRequestParameter("action", "Submit");
        actionPerform();
        verifyForward("error");
        assertNull((String) getRequest().getAttribute("results"));
    }

    public void testSubmitRubbishQuery() {
        setRequestPathInfo("/query");
        addRequestParameter("querystring","some rubbish");
        addRequestParameter("action", "Submit");
        actionPerform();
        verifyForward("error");
        assertNull((String) getRequest().getAttribute("results"));
    }

    public void testViewSuccessfulQuery() {
        setRequestPathInfo("/query");
        addRequestParameter("querystring","select a1_ from Company as a1_");
        addRequestParameter("action", "View");
        actionPerform();
        verifyForward("buildquery");
        assertEquals("SELECT a1_ FROM org.flymine.model.testmodel.Company AS a1_", ((Query) getRequest().getAttribute("query")).toString());
        verifyNoActionErrors();
    }

    public void testViewEmptyQuery() {
        setRequestPathInfo("/query");
        addRequestParameter("querystring","");
        addRequestParameter("action", "View");
        actionPerform();
        verifyForward("error");
        assertNull((String) getRequest().getAttribute("query"));
    }

    public void testViewRubbishQuery() {
        setRequestPathInfo("/query");
        addRequestParameter("querystring","some rubbish");
        addRequestParameter("action", "View");
        actionPerform();
        verifyForward("error");
        assertNull((String) getRequest().getAttribute("query"));
    }
    */
}
