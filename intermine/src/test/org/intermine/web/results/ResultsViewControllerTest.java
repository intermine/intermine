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

import java.util.HashMap;
import java.util.Map;

import org.apache.struts.tiles.ComponentContext;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.Results;
import org.flymine.web.Constants;

import servletunit.struts.MockStrutsTestCase;

public class ResultsViewControllerTest extends MockStrutsTestCase
{
    public ResultsViewControllerTest(String arg1) {
        super(arg1);
    }

    public void testNoExisting() throws Exception {
        ComponentContext context = new ComponentContext();
        ComponentContext.setContext(context, getRequest());
        setRequestPathInfo("/initResultsView");

        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        Results results = new Results(new Query(), os, os.getSequence());
        getSession().setAttribute("results", results);
        actionPerform();

        verifyNoActionErrors();
        DisplayableResults dr = (DisplayableResults) getSession().getAttribute(Constants.RESULTS_TABLE);
        assertTrue(results == dr.getResults());
    }

    public void testExisting() throws Exception {
        ComponentContext context = new ComponentContext();
        ComponentContext.setContext(context, getRequest());
        setRequestPathInfo("/initResultsView");

        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        Results results = new Results(new Query(), os, os.getSequence());
        getSession().setAttribute("results", results);
        DisplayableResults dr = new DisplayableResults(results);
        dr.setStart(3);
        getSession().setAttribute(Constants.RESULTS_TABLE, dr);
        actionPerform();

        verifyNoActionErrors();
        dr = (DisplayableResults) getSession().getAttribute(Constants.RESULTS_TABLE);
        assertTrue(results == dr.getResults());
        assertEquals(3, dr.getStart());
    }
}
