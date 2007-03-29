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

import java.util.HashSet;
import java.util.Set;

import org.apache.struts.tiles.ComponentContext;
import org.intermine.web.logic.Constants;

import servletunit.struts.MockStrutsTestCase;

/**
 * @author Thomas Riley
 */
public class BeginControllerTest extends MockStrutsTestCase
{
    public BeginControllerTest(String arg1) {
        super(arg1);
    }

    public void tearDown() throws Exception {
         getActionServlet().destroy();
    }

    public void testCategoryLoad() throws Exception {
        ComponentContext context = new ComponentContext();
        ComponentContext.setContext(context, getRequest());
        setRequestPathInfo("/begin");

        actionPerform();
        verifyNoActionErrors();

        Set cats = (Set) getActionServlet().getServletContext().getAttribute(Constants.CATEGORIES);

        assertNotNull(cats);

        Set expecting = new HashSet();
        expecting.add("People");
        expecting.add("Entities");

        assertEquals(2, cats.size());
        assertEquals(expecting, cats);
    }
}
