package org.intermine.web;

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
import java.util.IdentityHashMap;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Employee;

public class SaveQueryActionTest extends MockStrutsTestCase
{
    Map savedQueries = new HashMap();
    Map savedQueriesInverse = new IdentityHashMap();

    public SaveQueryActionTest (String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();

        Query q1 = new Query();
        Query q2 = new Query();
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Employee.class);
        q1.addFrom(qc1);
        q2.addFrom(qc1);
        q2.addFrom(qc2);

        savedQueries.put("query1", q1);
        savedQueries.put("query2", q2);
        savedQueriesInverse.put(q1, "query1");
        savedQueriesInverse.put(q2, "query2");
    }

    /**
     * Test saving a query when there are no saved queries.
     */
    public void testSuccessfulNoSavedQueries() throws Exception {
        setRequestPathInfo("/saveQuery");
        HttpSession session = getRequest().getSession();
        addRequestParameter("action", "Save a query");

        session.setAttribute(Constants.SAVED_QUERIES, new HashMap());
        session.setAttribute(Constants.SAVED_QUERIES_INVERSE, new IdentityHashMap());
        session.setAttribute(Constants.QUERY, new Query());

        SaveQueryForm form = new SaveQueryForm();
        form.setQueryName("query1");
        setActionForm(form);

        actionPerform();
        verifyForward("results");
        verifyNoActionErrors();
        assertNull(session.getAttribute(Constants.QUERY));
        assertEquals(1, ((Map)session.getAttribute(Constants.SAVED_QUERIES)).size());
    }

    /**
     * Test saving a query when there are saved queries.
     */
    public void testSuccessfulSavedQueries() throws Exception {
        setRequestPathInfo("/saveQuery");
        HttpSession session = getRequest().getSession();
        addRequestParameter("action", "Save a query");

        session.setAttribute(Constants.SAVED_QUERIES,
                             new HashMap(savedQueries));
        session.setAttribute(Constants.SAVED_QUERIES_INVERSE,
                             new IdentityHashMap(savedQueriesInverse));
        session.setAttribute(Constants.QUERY, new Query());

        SaveQueryForm form = new SaveQueryForm();
        form.setQueryName("query3");
        setActionForm(form);

        actionPerform();
        verifyForward("results");
        verifyNoActionErrors();
        assertNull(session.getAttribute(Constants.QUERY));
        Map savedQueriesFromSession =
            (Map)session.getAttribute(Constants.SAVED_QUERIES);
        assertEquals(3, savedQueriesFromSession.size());
        assertNotNull(savedQueriesFromSession.get("query1"));
        assertNotNull(savedQueriesFromSession.get("query2"));
        assertNotNull(savedQueriesFromSession.get("query3"));
    }

    /**
     * Test saving a query when there are saved queries are a name clash -
     * the new queryName is the same as an existing one.
     */
    public void testSuccessfulSavedQueriesNameClash() throws Exception {
        setRequestPathInfo("/saveQuery");
        HttpSession session = getRequest().getSession();
        addRequestParameter("action", "Save a query");

        session.setAttribute(Constants.SAVED_QUERIES,
                             new HashMap(savedQueries));
        session.setAttribute(Constants.SAVED_QUERIES_INVERSE,
                             new IdentityHashMap(savedQueriesInverse));
        session.setAttribute(Constants.QUERY, new Query());

        SaveQueryForm form = new SaveQueryForm();
        form.setQueryName("query2");
        setActionForm(form);

        actionPerform();
        verifyForward("results");
        verifyNoActionErrors();
        assertNull(session.getAttribute(Constants.QUERY));
        Map savedQueriesFromSession =
            (Map)session.getAttribute(Constants.SAVED_QUERIES);
        assertEquals(2, savedQueries.size ());
        assertEquals(2, savedQueriesFromSession.size());
        assertNotNull(savedQueriesFromSession.get("query1"));
        assertNotNull(savedQueriesFromSession.get("query2"));
    }
}
