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
    private PagedResults pr;

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
        pr = new PagedResults(results);
        pr.setPageSize(10);
    }

    public void testNext() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "next");

        getSession().setAttribute("results", results);
        getSession().setAttribute(Constants.RESULTS_TABLE, pr);
        pr.setStart(0);

        actionPerform();

        verifyForward("results");
        verifyNoActionErrors();
        assertEquals(10, pr.getStart());
    }

    public void testPrevious() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "previous");

        getSession().setAttribute("results", results);
        getSession().setAttribute(Constants.RESULTS_TABLE, pr);
        pr.setStart(10);

        actionPerform();

        verifyForward("results");
        verifyNoActionErrors();
        assertEquals(0, pr.getStart());
    }

    public void testFirst() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "first");

        getSession().setAttribute("results", results);
        getSession().setAttribute(Constants.RESULTS_TABLE, pr);
        pr.setStart(10);

        actionPerform();

        verifyForward("results");
        verifyNoActionErrors();
        assertEquals(0, pr.getStart());
    }

    public void testLast() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "last");

        getSession().setAttribute("results", results);
        getSession().setAttribute(Constants.RESULTS_TABLE, pr);
        pr.setStart(0);

        actionPerform();

        verifyForward("results");
        verifyNoActionErrors();
        assertEquals(10, pr.getStart());
    }

    public void testHide() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "hideColumn");
        addRequestParameter("index", "0");

        getSession().setAttribute("results", results);
        getSession().setAttribute(Constants.RESULTS_TABLE, pr);

        ((Column) pr.getColumns().get(0)).setVisible(true);
        assertTrue(((Column) pr.getColumns().get(0)).isVisible());

        actionPerform();
        verifyNoActionErrors();
        verifyForward("results");

        assertFalse(((Column) pr.getColumns().get(0)).isVisible());
    }

    public void testShow() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "showColumn");
        addRequestParameter("index", "0");

        getSession().setAttribute("results", results);
        getSession().setAttribute(Constants.RESULTS_TABLE, pr);

        ((Column) pr.getColumns().get(0)).setVisible(false);
        assertFalse(((Column) pr.getColumns().get(0)).isVisible());

        actionPerform();
        verifyNoActionErrors();
        verifyForward("results");

        assertTrue(((Column) pr.getColumns().get(0)).isVisible());
    }

    public void testMoveColumnLeft() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "moveColumnLeft");
        addRequestParameter("index", "1");

        getSession().setAttribute("results", results);
        getSession().setAttribute(Constants.RESULTS_TABLE, pr);

        actionPerform();

        verifyNoActionErrors();
        verifyForward("results");
        assertEquals("d", ((Column) pr.getColumns().get(0)).getName());
    }

    public void testMoveColumnRight() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "moveColumnRight");
        addRequestParameter("index", "0");

        getSession().setAttribute("results", results);
        getSession().setAttribute(Constants.RESULTS_TABLE, pr);

        actionPerform();

        verifyNoActionErrors();
        verifyForward("results");
        assertEquals("c", ((Column) pr.getColumns().get(1)).getName());
    }
}
