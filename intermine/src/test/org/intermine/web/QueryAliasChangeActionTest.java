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
import org.flymine.model.testmodel.Department;

public class QueryAliasChangeActionTest extends MockStrutsTestCase {

    private Query q;

    public QueryAliasChangeActionTest(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();
        q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc, "company1");
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testEditSuccessful() throws Exception {
        setRequestPathInfo("/changealias");
        HttpSession session = getSession();
        session.setAttribute(Constants.QUERY, q);

        addRequestParameter("alias", "company1");
        addRequestParameter("method", "edit");
        actionPerform();
        verifyForward("buildquery");
        verifyNoActionErrors();
        assertNotNull(session.getAttribute("queryClass"));
        assertEquals(Company.class, ((QueryClass) session.getAttribute("queryClass")).getType());
    }


    public void testEditNoQueryOnSession() throws Exception {
        setRequestPathInfo("/changealias");
        HttpSession session = getSession();

        addRequestParameter("alias", "company1");
        addRequestParameter("method", "edit");
        actionPerform();
        verifyForward("buildquery");
        verifyNoActionErrors();
        assertNull(session.getAttribute("queryClass"));
    }

    public void testEditQueryClassNotInQuery() throws Exception {
        setRequestPathInfo("/changealias");
        HttpSession session = getSession();
        session.setAttribute(Constants.QUERY, new Query());

        addRequestParameter("alias", "wrong_alias");
        addRequestParameter("method", "edit");

        actionPerform();
        verifyForward("buildquery");
        verifyNoActionErrors();
        assertNull(session.getAttribute("queryClass"));
    }


    public void testEditNoAliasSetInForm() throws Exception {
        setRequestPathInfo("/changealias");
        HttpSession session = getSession();
        session.setAttribute(Constants.QUERY, q);

        addRequestParameter("method", "edit");

        actionPerform();
        verifyForward("buildquery");
        verifyNoActionErrors();
        assertNull(session.getAttribute("queryClass"));
    }

    public void testRemoveSuccessful() throws Exception {
        setRequestPathInfo("/changealias");
        HttpSession session = getSession();
        session.setAttribute(Constants.QUERY, q);

        q.addFrom(new QueryClass(Department.class));

        addRequestParameter("alias", "company1");
        addRequestParameter("method", "remove");
        actionPerform();
        verifyForward("buildquery");
        verifyNoActionErrors();
        assertNull(session.getAttribute("queryClass"));
        assertNotNull(session.getAttribute(Constants.QUERY));
        assertEquals(1, q.getFrom().size());
    }
    
    public void testRemoveLastSuccessful() throws Exception {
        setRequestPathInfo("/changealias");
        HttpSession session = getSession();
        session.setAttribute(Constants.QUERY, q);

        addRequestParameter("alias", "company1");
        addRequestParameter("method", "remove");
        actionPerform();
        verifyForward("buildquery");
        verifyNoActionErrors();
        assertNull(session.getAttribute("queryClass"));
        assertNull(session.getAttribute(Constants.QUERY));
    }

    public void testRemoveNoQueryOnSession() throws Exception {
        setRequestPathInfo("/changealias");
        HttpSession session = getSession();

        addRequestParameter("alias", "company1");
        addRequestParameter("method", "remove");
        actionPerform();
        verifyForward("buildquery");
        verifyNoActionErrors();
        assertNull(session.getAttribute("queryClass"));
    }

    public void testRemoveQueryClassNotInQuery() throws Exception {
        setRequestPathInfo("/changealias");
        HttpSession session = getSession();
        session.setAttribute(Constants.QUERY, new Query());

        addRequestParameter("alias", "wrong_alias");
        addRequestParameter("method", "remove");

        actionPerform();
        verifyForward("buildquery");
        verifyNoActionErrors();
        assertNull(session.getAttribute("queryClass"));
    }


    public void testRemoveNoAliasSetInForm() throws Exception {
        setRequestPathInfo("/changealias");
        HttpSession session = getSession();
        session.setAttribute(Constants.QUERY, q);

        addRequestParameter("method", "remove");

        actionPerform();
        verifyForward("buildquery");
        verifyNoActionErrors();
        assertNull(session.getAttribute("queryClass"));
    }

    
}
