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


public class QueryClassSelectActionTest extends MockStrutsTestCase {

    public QueryClassSelectActionTest(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSelectSuccessful() throws Exception {
        setRequestPathInfo("/queryClassSelect");
        HttpSession session = getSession();
        QueryClassSelectForm form = new QueryClassSelectForm();
        form.setClassName("org.flymine.model.testmodel.Company");
        setActionForm(form);
        addRequestParameter("action", "Select");
        actionPerform();
        verifyForward("buildquery");
        assertNotNull(session.getAttribute("queryClass"));
    }


    public void testNoClassNameSet() throws Exception {
        setRequestPathInfo("/queryClassSelect");
        HttpSession session = getSession();
        QueryClassSelectForm form = new QueryClassSelectForm();
        setActionForm(form);
        addRequestParameter("action", "Select");
        actionPerform();
        verifyForward("error");
        assertNull(session.getAttribute("queryClass"));
    }


}
