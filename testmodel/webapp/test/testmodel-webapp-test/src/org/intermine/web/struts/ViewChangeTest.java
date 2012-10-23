package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;

import org.intermine.metadata.Model;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Tests for ViewChange.
 *
 * @author Kim Rutherford
 */

public class ViewChangeTest extends WebappTestCase
{
    public ViewChangeTest (String arg) {
        super(arg);
    }

    public void tearDown() throws Exception {
         getActionServlet().destroy();
    }

    public void testRemove() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery query = new PathQuery(model);
        query.addView("Employee.age");
        query.addView("Employee.name");
        SessionMethods.getQuery(getSession());

        addRequestParameter("path", "Employee.age");
        addRequestParameter("method", "removeFromView");

        setRequestPathInfo("/viewChange");

        actionPerform();
        verifyNoActionErrors();
        //verifyForward("query");
        PathQuery q = new PathQuery(model);
        ArrayList<Path> expected = new ArrayList<Path>();
        expected.add(q.makePath("Employee.name"));
        assertEquals(expected, SessionMethods.getQuery(getSession()).getView());
    }
}
