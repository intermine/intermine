package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;

import org.intermine.api.profile.Profile;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.OldPathQuery;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.session.SessionMethods;

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
        Model model = Model.getInstanceByName("testmodel");
        OldPathQuery query = new OldPathQuery(model);
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.name"));
        query.getView().add(OldPathQuery.makePath(model, query, "Employee.age"));
        SessionMethods.getQuery(getSession());

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

        ArrayList<Path> expected = new ArrayList<Path>();
        expected.add(OldPathQuery.makePath(model, query, "Employee.name"));
        assertEquals(expected, SessionMethods.getQuery(getSession()).getView());
    }
}
