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

import java.util.Arrays;
import java.util.HashSet;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreSummary;

import servletunit.struts.MockStrutsTestCase;

public class DisplayConstraintTest extends MockStrutsTestCase
{
    DisplayConstraint dc;
    
    public DisplayConstraintTest(String arg) {
        super(arg);
    }
    
    public void setUp() throws Exception {
        super.setUp();
        
        PathQuery query = new PathQuery(Model.getInstanceByName("testmodel"));
        PathNode node = query.addNode("Employee.name");
        
        dc = new DisplayConstraint(node, Model.getInstanceByName("testmodel"),
                (ObjectStoreSummary) getActionServlet().getServletContext().getAttribute(Constants.OBJECT_STORE_SUMMARY),
                null);
    }
    
    public void testValidOps() throws Exception {        
        assertEquals(8, dc.getValidOps().size());
    }
    
    public void testOptionsList() throws Exception {
        assertEquals(6, dc.getOptionsList().size());
    }
    
    public void testFixedOpsIndeces() throws Exception {
        assertEquals(new HashSet(Arrays.asList(new Object[]{new Integer(0), new Integer(1)})),
                     new HashSet(dc.getFixedOpIndices()));
    }
}
