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

import java.util.Arrays;
import java.util.HashSet;

import org.intermine.api.InterMineAPI;
import org.intermine.metadata.Model;
import org.intermine.pathquery.PathQuery;
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

        PathQuery query = new PathQuery(Model.getInstanceByName("testmodel"));

        InterMineAPI im = SessionMethods.getInterMineAPI(getActionServlet().getServletContext());

    }

    public void testValidOps() throws Exception {
        assertEquals(7, dc.getValidOps().size());
    }


}
