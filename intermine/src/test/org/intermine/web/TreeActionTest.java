package org.flymine.web;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;
import java.util.HashSet;

import org.flymine.objectstore.query.QueryClass;

import org.flymine.model.testmodel.Department;

import servletunit.struts.MockStrutsTestCase;

public class TreeActionTest extends MockStrutsTestCase
{
    public TreeActionTest(String arg1) {
        super(arg1);
    }

    public void testExpand() throws Exception {
        Set openClasses = new HashSet();
        openClasses.add("org.flymine.model.testmodel.Thing");
        getSession().setAttribute("openClasses", openClasses);

        addRequestParameter("node", "org.flymine.model.testmodel.Department");

        setRequestPathInfo("/changeTree");
        addRequestParameter("method", "expand");

        actionPerform();
        verifyNoActionErrors();
        verifyForward("renderTree");
        
        Set expected = new HashSet();
        expected.add("org.flymine.model.testmodel.Thing");
        expected.add("org.flymine.model.testmodel.Department");
        assertEquals(expected, getSession().getAttribute("openClasses"));
    }

    public void testCollapse() throws Exception {
        Set openClasses = new HashSet();
        openClasses.add("org.flymine.model.testmodel.Thing");
        openClasses.add("org.flymine.model.testmodel.Department");
        getSession().setAttribute("openClasses", openClasses);

        addRequestParameter("node", "org.flymine.model.testmodel.Department");

        setRequestPathInfo("/changeTree");
        addRequestParameter("method", "collapse");

        actionPerform();
        verifyNoActionErrors();
        verifyForward("renderTree");
        
        Set expected = new HashSet();
        expected.add("org.flymine.model.testmodel.Thing");
        assertEquals(expected, getSession().getAttribute("openClasses"));
    }

//     public void testSelect() throws Exception {
//         addRequestParameter("node", "org.flymine.model.testmodel.Department");

//         setRequestPathInfo("/changeTree");
//         addRequestParameter("method", "select");

//         actionPerform();
//         verifyNoActionErrors();
//         verifyForward("buildquery");

//         assertEquals(Department.class, ((QueryClass) getSession().getAttribute("queryClass")).getType());
//     }
}
