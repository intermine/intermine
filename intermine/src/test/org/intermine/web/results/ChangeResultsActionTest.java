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

import servletunit.struts.MockStrutsTestCase;
import org.apache.struts.tiles.ComponentContext;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.dummy.ObjectStoreDummyImpl;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.fql.FqlQuery;
import org.flymine.objectstore.query.Results;

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
        FqlQuery fq = new FqlQuery("select c, d from Company as c, Department as d", "org.flymine.model.testmodel");
        results = os.execute(fq.toQuery());
        dr = new DisplayableResults(results);
        dr.setPageSize(10);
    }


    public void testNext() throws Exception {
        setRequestPathInfo("/changeResults");
        addRequestParameter("method", "next");

        getSession().setAttribute("results", results);
        getSession().setAttribute("resultsTable", dr);
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
        getSession().setAttribute("resultsTable", dr);
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
        getSession().setAttribute("resultsTable", dr);
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
        getSession().setAttribute("resultsTable", dr);
        dr.setStart(0);

        actionPerform();

        verifyForward("results");
        verifyNoActionErrors();
        assertEquals(10, dr.getStart());
    }

}
