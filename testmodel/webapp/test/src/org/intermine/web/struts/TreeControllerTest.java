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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.intermine.metadata.Model;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.TreeNode;
import org.intermine.web.logic.profile.Profile;

import org.apache.struts.tiles.ComponentContext;

import servletunit.struts.MockStrutsTestCase;

public class TreeControllerTest extends MockStrutsTestCase
{
    public TreeControllerTest(String arg1) {
        super(arg1);
    }

    public void tearDown() throws Exception {
         getActionServlet().destroy();
    }

    public void testExecute() throws Exception {

        Model model = Model.getInstanceByName("testmodel");
        ComponentContext componentContext = new ComponentContext();
        ComponentContext.setContext(componentContext, getRequest());
        setRequestPathInfo("/initTree");

        String pkg = "org.intermine.model.testmodel.";
        Set openClasses = new HashSet();
        openClasses.add(pkg + "Employable");
        openClasses.add(pkg + "Thing");
        getSession().setAttribute("openClasses", openClasses);
        getRequest().setAttribute("rootClass", pkg + "Thing");
        //necessary to work-round struts test case not invoking our SessionListener
        getSession().setAttribute(Constants.PROFILE,
                                  new Profile(null, null, null, null,
                                              new HashMap(), new HashMap(), new HashMap()));

        actionPerform();
        verifyNoActionErrors();

        List expected = new ArrayList();
        expected.add(new TreeNode(model.getClassDescriptorByName(pkg + "Thing"), "", 0, false, false, true));
        expected.add(new TreeNode(model.getClassDescriptorByName(pkg + "Address"), "", 1, false, true, false));
        expected.add(new TreeNode(model.getClassDescriptorByName(pkg + "Employable"), "", 1, false, false, true));
        expected.add(new TreeNode(model.getClassDescriptorByName(pkg + "Contractor"), "", 2, false, true, false));
        expected.add(new TreeNode(model.getClassDescriptorByName(pkg + "Employee"), "", 2, false, false, false));

        assertEquals(openClasses, getSession().getAttribute("openClasses"));
        assertEquals(expected, componentContext.getAttribute("nodes"));
    }
}
