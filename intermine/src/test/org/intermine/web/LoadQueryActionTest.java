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

import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.presentation.DisplayModel;
import org.flymine.model.testmodel.Company;
import org.flymine.model.testmodel.Employee;


public class LoadQueryActionTest extends MockStrutsTestCase
{
    Map savedQueries = new HashMap();

    public LoadQueryActionTest (String testName) {
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
    }

    /**
     * Test loading a saved query when there is no current query
     */
    public void testNewSuccessful() throws Exception {
        setRequestPathInfo("/loadQuery");
        HttpSession session = getSession();
        addRequestParameter("action", "Load from saved queries");

        session.setAttribute(Constants.SAVED_QUERIES, savedQueries);

        LoadQueryForm form = new LoadQueryForm();
        form.setQueryName("query1");
        setActionForm(form);

        actionPerform();
        verifyForward("buildquery");
        verifyNoActionErrors();

        assertNotNull(session.getAttribute(Constants.QUERY));
        assertEquals(1, ((Query) session.getAttribute(Constants.QUERY)).getFrom().size());
    }

    /**
     * Test loading a saved query when there is a current query
     */
    public void testReplaceSuccessful() throws Exception {
        setRequestPathInfo("/loadQuery");
        HttpSession session = getSession();
        addRequestParameter("action", "Load from saved queries");

        session.setAttribute(Constants.SAVED_QUERIES, savedQueries);
        session.setAttribute(Constants.QUERY, new Query());

        LoadQueryForm form = new LoadQueryForm();
        form.setQueryName("query2");
        setActionForm(form);

        actionPerform();
        verifyForward("buildquery");
        verifyNoActionErrors();
        assertNotNull(session.getAttribute(Constants.QUERY));
        assertEquals(2, ((Query) session.getAttribute(Constants.QUERY)).getFrom().size());
    }
}
