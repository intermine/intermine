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

import org.flymine.metadata.Model;
import org.flymine.metadata.presentation.DisplayModel;

public class RestartQueryActionTest extends MockStrutsTestCase {
    protected Model model;

    public RestartQueryActionTest(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();
        model = Model.getInstanceByName("testmodel");
    }

    public void tearDown() throws Exception {
        super.tearDown();
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

        session.setAttribute("queryClasses", queryClasses);
        session.setAttribute("editingAlias", anAlias);
        session.setAttribute("model", new DisplayModel(model));

        actionPerform();
        
        verifyForward("buildquery");
        assertNull(session.getAttribute("queryClasses"));
        assertNull(session.getAttribute("editingAlias"));
        assertNotNull(session.getAttribute("model"));
    }
}
