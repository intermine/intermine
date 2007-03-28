package org.intermine.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;

import org.intermine.metadata.Model;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.PathQuery;

import servletunit.struts.MockStrutsTestCase;

/**
 * Tests for ViewChange.
 *
 * @author Kim Rutherford
 */

public class ViewChangeTest extends MockStrutsTestCase
{
    public ViewChangeTest (String arg) {
        super(arg);
    }

    public void tearDown() throws Exception {
         getActionServlet().destroy();
    }

    public void testRemove() throws Exception {
        PathQuery query = new PathQuery(Model.getInstanceByName("testmodel"));
        query.getView().add("Employee.age");
        query.getView().add("Employee.name");
        getSession().setAttribute(Constants.QUERY, query);

        addRequestParameter("path", "Employee.age");
        addRequestParameter("method", "removeFromView");

        //necessary to work-round struts test case not invoking our SessionListener
        getSession().setAttribute(Constants.PROFILE,
                                  new Profile(null, null, null, null,
                                              new HashMap(), new HashMap(), new HashMap()));

        setRequestPathInfo("/viewChange");

        actionPerform();
        verifyNoActionErrors();
        //verifyForward("query");

        ArrayList expected = new ArrayList();
        expected.add("Employee.name");
        assertEquals(expected, ((PathQuery) getSession().getAttribute(Constants.QUERY)).getView());
    }
}
