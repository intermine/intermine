package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.HashSet;

import org.intermine.api.InterMineAPI;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.OldPathQuery;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.query.DisplayConstraint;
import org.intermine.web.logic.session.SessionMethods;

import servletunit.struts.MockStrutsTestCase;

public class DisplayConstraintTest extends MockStrutsTestCase
{
    DisplayConstraint dc;

    public DisplayConstraintTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();

        OldPathQuery query = new OldPathQuery(Model.getInstanceByName("testmodel"));
        PathNode node = query.addNode("Employee.name");

        InterMineAPI im = SessionMethods.getInterMineAPI(getActionServlet().getServletContext());
        dc = new DisplayConstraint(node, Model.getInstanceByName("testmodel"),
                im.getObjectStoreSummary(), null, null);
    }

    public void testValidOps() throws Exception {
        assertEquals(7, dc.getValidOps().size());
    }

    public void testOptionsList() throws Exception {
        assertEquals(6, dc.getOptionsList().size());
    }

    public void testFixedOpsIndeces() throws Exception {
        assertEquals(new HashSet(Arrays.asList(new Object[]{new Integer(0), new Integer(1)})),
                     new HashSet(dc.getFixedOpIndices()));
    }
}
