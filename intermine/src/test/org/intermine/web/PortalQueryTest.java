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

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.util.MessageResources;
import org.intermine.web.results.PagedResults;

import servletunit.struts.MockStrutsTestCase;

public class PortalQueryTest extends MockStrutsTestCase
{
    public PortalQueryTest(String arg1) {
        super(arg1);
    }

    public void tearDown() throws Exception {
        getActionServlet().destroy();
    }
    
    public void testGoodExternalId() throws Exception {
        
        setRequestPathInfo("/portal");
        addRequestParameter("externalid", "EmployeeA1");
        
        actionPerform();
        verifyNoActionErrors();
        
        // would be nice to test that we forward to the correct
        // object details URL but I can't figure out how to get
        // the forward returned.
    }

    public void testBadExternalId() throws Exception {
        setRequestPathInfo("/portal");
        addRequestParameter("externalid", "wibble");

        actionPerform();
        verifyForward("results");
        verifyNoActionErrors();
    }
    
   /* public void testMultipleResults() throws Exception {
        setRequestPathInfo("/portal");
        addRequestParameter("externalid", "Employee");

        actionPerform();
        verifyForward("results");
        
        PagedResults pr = (PagedResults) getSession().getAttribute(Constants.RESULTS_TABLE);
        assertTrue("" + pr.getSize() + " > 1", pr.getSize() > 1);
        verifyNoActionErrors();
    }*/
}
