package org.intermine.web.results;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;

import servletunit.struts.MockStrutsTestCase;

import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.DynamicUtil;
import org.intermine.web.Constants;

public class ChangeResultsActionTest extends MockStrutsTestCase
{
    public ChangeResultsActionTest(String arg1) {
        super(arg1);
    }

    private Results results;
    private PagedResults dr;

    public void setUp() throws Exception {
        super.setUp();
        ObjectStoreDummyImpl os = new ObjectStoreDummyImpl();
        os.setResultsSize(15);
        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        Department d = new Department();
        d.setId(new Integer(21));
        ResultsRow row = new ResultsRow();
        row.add(c);
        row.add(d);
        os.addRow(row);
        IqlQuery fq = new IqlQuery("select c, d from Company as c, Department as d order by c", "org.intermine.model.testmodel");
        results = os.execute(fq.toQuery());
        dr = new PagedResults(results);
        dr.setPageSize(10);
    }

    public void testNext() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "next");

        getSession().setAttribute("results", results);
        getSession().setAttribute(Constants.RESULTS_TABLE, dr);
        dr.setStart(0);

        actionPerform();

        verifyForward("results");
        verifyNoActionErrors();
        assertEquals(10, dr.getStart());
    }

    public void testPrevious() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "previous");

        getSession().setAttribute("results", results);
        getSession().setAttribute(Constants.RESULTS_TABLE, dr);
        dr.setStart(10);

        actionPerform();

        verifyForward("results");
        verifyNoActionErrors();
        assertEquals(0, dr.getStart());
    }

    public void testFirst() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "first");

        getSession().setAttribute("results", results);
        getSession().setAttribute(Constants.RESULTS_TABLE, dr);
        dr.setStart(10);

        actionPerform();

        verifyForward("results");
        verifyNoActionErrors();
        assertEquals(0, dr.getStart());
    }

    public void testLast() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "last");

        getSession().setAttribute("results", results);
        getSession().setAttribute(Constants.RESULTS_TABLE, dr);
        dr.setStart(0);

        actionPerform();

        verifyForward("results");
        verifyNoActionErrors();
        assertEquals(10, dr.getStart());
    }

    public void testHide() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "hideColumn");
        addRequestParameter("columnAlias", "c");

        getSession().setAttribute("results", results);
        getSession().setAttribute(Constants.RESULTS_TABLE, dr);
        dr.getColumnByName("c").setVisible(true);
        assertTrue(dr.getColumnByName("c").isVisible());

        actionPerform();

        assertFalse(dr.getColumnByName("c").isVisible());
        verifyForward("results");
        verifyNoActionErrors();
    }

    public void testShow() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "showColumn");
        addRequestParameter("columnAlias", "c");

        getSession().setAttribute("results", results);
        getSession().setAttribute(Constants.RESULTS_TABLE, dr);
        dr.getColumnByName("c").setVisible(false);
        assertFalse(dr.getColumnByName("c").isVisible());

        actionPerform();

        assertTrue(dr.getColumnByName("c").isVisible());
        verifyForward("results");
        verifyNoActionErrors();
    }

    /**
     * No need to test every single thing about moving columns up or
     * down - that is done in PagedResultsTest. Here we just do
     * a quick check to see if a couple of operations work, and that
     * we are forwarded to the correct page
     */
    public void testMoveColumnUp() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "moveColumnUp");
        addRequestParameter("columnAlias", "d");

        getSession().setAttribute("results", results);
        getSession().setAttribute(Constants.RESULTS_TABLE, dr);

        assertEquals(dr.getColumnByName("c"), dr.getColumns().get(0));

        actionPerform();

        assertEquals(dr.getColumnByName("d"), dr.getColumns().get(0));
        verifyForward("results");
        verifyNoActionErrors();
    }

    public void testMoveColumnDown() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "moveColumnDown");
        addRequestParameter("columnAlias", "c");

        getSession().setAttribute("results", results);
        getSession().setAttribute(Constants.RESULTS_TABLE, dr);

        assertEquals(dr.getColumnByName("c"), dr.getColumns().get(0));

        actionPerform();

        assertEquals(dr.getColumnByName("d"), dr.getColumns().get(0));
        verifyForward("results");
        verifyNoActionErrors();
    }
}
