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
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.Results;

public class ResultsViewControllerTest extends MockStrutsTestCase
{
    public ResultsViewControllerTest(String arg1) {
        super(arg1);
    }

    public void test1() throws Exception {
        ComponentContext context = new ComponentContext();
        ComponentContext.setContext(context, getRequest());
        setRequestPathInfo("/initResultsView");

        Query q = new Query();
        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");

        getRequest().setAttribute("results", new Results(q, os));
        actionPerform();
        assertEquals(q, context.getAttribute("query"));
        assertNotNull(getRequest().getSession().getAttribute(ResultsViewController.DISPLAYABLERESULTS_NAME));
    }

}
