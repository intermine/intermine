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
import org.flymine.objectstore.query.QueryClass;
import org.flymine.model.testmodel.Company;

public class QueryAliasSelectActionTest extends MockStrutsTestCase {

    private Query q;

    public QueryAliasSelectActionTest(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();
        q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSelectSuccessful() throws Exception {
        setRequestPathInfo("/queryAliasSelect");
        HttpSession session = getSession();
        session.setAttribute("query", q);

        QueryAliasSelectForm form = new QueryAliasSelectForm();
        form.setAlias("a1_");
        setActionForm(form);

        addRequestParameter("action", "Select");
        actionPerform();
        verifyForward("buildquery");
        verifyNoActionErrors();
        assertNotNull(session.getAttribute("queryClass"));
        assertEquals(Company.class, ((QueryClass) session.getAttribute("queryClass")).getType());
    }


    public void testNoQueryOnSession() throws Exception {
        setRequestPathInfo("/queryAliasSelect");
        HttpSession session = getSession();

        QueryAliasSelectForm form = new QueryAliasSelectForm();
        form.setAlias("a1_");
        setActionForm(form);

        addRequestParameter("action", "Select");
        actionPerform();
        verifyForward("error");
        assertNull(session.getAttribute("queryClass"));
    }

    public void testQueryClassNotInQuery() throws Exception {
        setRequestPathInfo("/queryAliasSelect");
        HttpSession session = getSession();
        session.setAttribute("query", new Query());

        QueryAliasSelectForm form = new QueryAliasSelectForm();
        form.setAlias("wrong_alias");
        setActionForm(form);

        addRequestParameter("action", "Select");
        actionPerform();
        verifyForward("error");
        assertNull(session.getAttribute("queryClass"));
    }


    public void testNoAliasSetInForm() throws Exception {
        setRequestPathInfo("/queryAliasSelect");
        HttpSession session = getSession();
        session.setAttribute("query", q);
        QueryAliasSelectForm form = new QueryAliasSelectForm();
        setActionForm(form);

        actionPerform();
        verifyForward("error");
        assertNull(session.getAttribute("queryClass"));
     }

}
