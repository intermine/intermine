package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import org.apache.struts.tiles.ComponentContext;
import org.intermine.web.Constants;
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
        setRequestPathInfo("/initBegin");

        actionPerform();
        verifyNoActionErrors();
        
        List cats = (List) getActionServlet().getServletContext().getAttribute(Constants.CATEGORIES);
        Map subcats = (Map) getActionServlet().getServletContext().getAttribute(Constants.CATEGORY_CLASSES);
        
        assertNotNull(cats);
        assertNotNull(subcats);
        
        assertEquals(3, cats.size());
        assertEquals(3, subcats.keySet().size());
        assertEquals("People", cats.get(0));
        assertEquals(Arrays.asList(new Object[]
                    {"Employee","Manager","CEO","Contractor","Secretary"}), subcats.get("People"));
        
    }
}
