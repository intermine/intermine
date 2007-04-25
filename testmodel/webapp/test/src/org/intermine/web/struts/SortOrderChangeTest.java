package org.intermine.web.struts;

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
import org.intermine.path.Path;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.query.PathQuery;

import servletunit.struts.MockStrutsTestCase;

/**
 * Tests for SortOrderChange.
 *
 * @author Julie Sullivan
 */

public class SortOrderChangeTest extends MockStrutsTestCase
{
    public SortOrderChangeTest (String arg) {
        super(arg);
    }

    public void tearDown() throws Exception {
         getActionServlet().destroy();
    }

    public void testRemove() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery query = new PathQuery(model);
        query.getView().add(MainHelper.makePath(model, query, "Employee.name"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.age"));
        getSession().setAttribute(Constants.QUERY, query);

        addRequestParameter("pathString", "Employee.age");
        addRequestParameter("method", "addToSortOrder");

        //necessary to work-round struts test case not invoking our SessionListener
        getSession().setAttribute(Constants.PROFILE,
                                  new Profile(null, null, null, null,
                                              new HashMap(), new HashMap(), new HashMap()));

        setRequestPathInfo("/sortOrderChange");

        actionPerform();
        verifyNoActionErrors();
        //verifyForward("query");

        ArrayList<Path> expected = new ArrayList<Path>();
        expected.add(MainHelper.makePath(model, query, "Employee.age"));
        assertEquals(expected.get(0), ((PathQuery) getSession().getAttribute(Constants.QUERY)).getSortOrder().get(0).getField());
    }
}
