package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashSet;

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

    public void testRemove() throws Exception {
        ArrayList view = new ArrayList();
        view.add("Employee.age");
        view.add("Employee.name");
        getSession().setAttribute(Constants.VIEW, view);

        addRequestParameter("path", "Employee.age");
        addRequestParameter("method", "removeFromView");

        setRequestPathInfo("/viewChange");

        actionPerform();
        verifyNoActionErrors();
        verifyForward("query");
        
        ArrayList expected = new ArrayList();
        expected.add("Employee.name");
        assertEquals(expected, getSession().getAttribute(Constants.VIEW));
    }
}
