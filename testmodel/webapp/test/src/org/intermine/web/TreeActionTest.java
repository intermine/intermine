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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;

import servletunit.struts.MockStrutsTestCase;

public class TreeActionTest extends MockStrutsTestCase
{
    public TreeActionTest(String arg1) {
        super(arg1);
    }

    public void tearDown() throws Exception {
        getActionServlet().destroy();
    }

    public void testExpand() throws Exception {
        Set openClasses = new HashSet();
        openClasses.add("org.intermine.model.testmodel.Thing");
        getSession().setAttribute("openClasses", openClasses);

        addRequestParameter("node", "org.intermine.model.testmodel.Department");

        setRequestPathInfo("/changeTree");
        addRequestParameter("method", "expand");

        //necessary to work-round struts test case not invoking our SessionListener
        getSession().setAttribute(Constants.PROFILE,
                                  new Profile(null, null, null, null,
                                              new HashMap(), new HashMap(), new HashMap()));

        actionPerform();
        verifyNoActionErrors();
        verifyForward("renderTree");

        Set expected = new HashSet();
        expected.add("org.intermine.model.testmodel.Thing");
        expected.add("org.intermine.model.testmodel.Department");
        assertEquals(expected, getSession().getAttribute("openClasses"));
    }

    public void testCollapse() throws Exception {
        Set openClasses = new HashSet();
        openClasses.add("org.intermine.model.testmodel.Thing");
        openClasses.add("org.intermine.model.testmodel.Department");
        getSession().setAttribute("openClasses", openClasses);

        addRequestParameter("node", "org.intermine.model.testmodel.Department");

        setRequestPathInfo("/changeTree");
        addRequestParameter("method", "collapse");

        //necessary to work-round struts test case not invoking our SessionListener
        getSession().setAttribute(Constants.PROFILE,
                                  new Profile(null, null, null, null,
                                              new HashMap(), new HashMap(), new HashMap()));

        actionPerform();
        verifyNoActionErrors();
        verifyForward("renderTree");

        Set expected = new HashSet();
        expected.add("org.intermine.model.testmodel.Thing");
        assertEquals(expected, getSession().getAttribute("openClasses"));
    }

//     public void testSelect() throws Exception {
//         addRequestParameter("node", "org.intermine.model.testmodel.Department");

//         setRequestPathInfo("/changeTree");
//         addRequestParameter("method", "select");

//         actionPerform();
//         verifyNoActionErrors();
//         verifyForward("buildquery");

//         assertEquals(Department.class, ((QueryClass) getSession().getAttribute("queryClass")).getType());
//     }
}
