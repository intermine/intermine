package org.flymine.web.results;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;

import servletunit.struts.MockStrutsTestCase;

import org.flymine.model.testmodel.Company;
import org.flymine.model.testmodel.Department;
import org.flymine.objectstore.dummy.ObjectStoreDummyImpl;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryNode;
import org.flymine.objectstore.query.fql.FqlQuery;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsRow;
import org.flymine.util.DynamicUtil;
import org.flymine.web.Constants;

public class ChangeResultsActionTest extends MockStrutsTestCase
{
    public ChangeResultsActionTest(String arg1) {
        super(arg1);
    }

    private Results results;
    private DisplayableResults dr;

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
        FqlQuery fq = new FqlQuery("select c, d from Company as c, Department as d order by c", "org.flymine.model.testmodel");
        results = os.execute(fq.toQuery());
        dr = new DisplayableResults(results);
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
        dr.getColumn("c").setVisible(true);
        assertTrue(dr.getColumn("c").isVisible());

        actionPerform();

        assertFalse(dr.getColumn("c").isVisible());
        verifyForward("results");
        verifyNoActionErrors();
    }

    public void testShow() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "showColumn");
        addRequestParameter("columnAlias", "c");

        getSession().setAttribute("results", results);
        getSession().setAttribute(Constants.RESULTS_TABLE, dr);
        dr.getColumn("c").setVisible(false);
        assertFalse(dr.getColumn("c").isVisible());

        actionPerform();

        assertTrue(dr.getColumn("c").isVisible());
        verifyForward("results");
        verifyNoActionErrors();
    }

    /**
     * No need to test every single thing about moving columns up or
     * down - that is done in DisplayableResultsTest. Here we just do
     * a quick check to see if a couple of operations work, and that
     * we are forwarded to the correct page
     */
    public void testMoveColumnUp() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "moveColumnUp");
        addRequestParameter("columnAlias", "d");

        getSession().setAttribute("results", results);
        getSession().setAttribute(Constants.RESULTS_TABLE, dr);

        assertEquals(dr.getColumn("c"), dr.getColumns().get(0));

        actionPerform();

        assertEquals(dr.getColumn("d"), dr.getColumns().get(0));
        verifyForward("results");
        verifyNoActionErrors();
    }

    public void testMoveColumnDown() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "moveColumnDown");
        addRequestParameter("columnAlias", "c");

        getSession().setAttribute("results", results);
        getSession().setAttribute(Constants.RESULTS_TABLE, dr);

        assertEquals(dr.getColumn("c"), dr.getColumns().get(0));

        actionPerform();

        assertEquals(dr.getColumn("d"), dr.getColumns().get(0));
        verifyForward("results");
        verifyNoActionErrors();
    }

    public void testOrderByColumn() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "orderByColumn");
        addRequestParameter("columnAlias", "d");

        getSession().setAttribute(Constants.RESULTS_TABLE, dr);

        Query q = results.getQuery();
        QueryNode qnc = (QueryNode) q.getReverseAliases().get("c");
        QueryNode qnd = (QueryNode) q.getReverseAliases().get("d");

        assertEquals(1, q.getOrderBy().size());
        assertEquals(qnc, q.getOrderBy().get(0));

        actionPerform();

        assertEquals(1, q.getOrderBy().size());
        assertEquals(qnd, q.getOrderBy().get(0));

        assertEquals(q, getSession().getAttribute(Constants.QUERY));
        verifyForward("runquery");
        verifyNoActionErrors();
    }

    public void testDetails() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "details");
        addRequestParameter("columnIndex", "1");
        addRequestParameter("rowIndex", "0");

        getSession().setAttribute("results", results);
        getSession().setAttribute(Constants.RESULTS_TABLE, dr);

        Object obj = ((ResultsRow) results.get(0)).get(1);

        actionPerform();

        assertEquals(obj, getRequest().getAttribute("object"));
        verifyForward("details");
        verifyNoActionErrors();
    }
}
