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
import org.flymine.metadata.presentation.DisplayModel;

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
        DisplayModel model = new DisplayModel(Model.getInstanceByName("testmodel"));
        session.setAttribute("model", model);
        addRequestParameter("cldName", "org.flymine.model.testmodel.Company");
        addRequestParameter("action", "Select");
        actionPerform();
        verifyForward("buildquery");
        assertNotNull(session.getAttribute("cld"));
    }

    public void testNoModelOnSession() throws Exception {
        setRequestPathInfo("/queryClassSelect");
        HttpSession session = getSession();
        addRequestParameter("cldName", "org.flymine.model.testmodel.Company");
        addRequestParameter("action", "Select");
        actionPerform();
        verifyForward("error");
        assertNull(session.getAttribute("cld"));
    }

    public void testNoCldNameSet() throws Exception {
        setRequestPathInfo("/queryClassSelect");
        HttpSession session = getSession();
        DisplayModel model = new DisplayModel(Model.getInstanceByName("testmodel"));
        session.setAttribute("model", model);
        addRequestParameter("action", "Select");
        actionPerform();
        verifyForward("error");
        assertNull(session.getAttribute("cld"));
    }


}
