package org.flymine.web.results;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import servletunit.struts.MockStrutsTestCase;
import org.apache.struts.tiles.ComponentContext;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.Results;
import org.flymine.metadata.Model;

import org.flymine.model.FlyMineBusinessObject;
import org.flymine.model.testmodel.*;

public class ResultsCellControllerTest extends MockStrutsTestCase
{
    public ResultsCellControllerTest(String arg1) {
        super(arg1);
    }

    public void testBusinessObject() throws Exception {
        ComponentContext context = new ComponentContext();
        ComponentContext.setContext(context, getRequest());
        setRequestPathInfo("/initResultsCell");

        getRequest().setAttribute("object", new Department());
        actionPerform();

        Model model = Model.getInstanceByName("testmodel");
        assertNotNull(context.getAttribute("clds"));
        assertTrue(((Set) context.getAttribute("clds")).contains(model.getClassDescriptorByName(Department.class.getName())));
        assertTrue(((Set) context.getAttribute("clds")).contains(model.getClassDescriptorByName(RandomInterface.class.getName())));
        assertTrue(((Set) context.getAttribute("clds")).contains(model.getClassDescriptorByName(FlyMineBusinessObject.class.getName())));

        assertNotNull(context.getAttribute("leafClds"));
        assertTrue(((Set) context.getAttribute("leafClds")).contains(model.getClassDescriptorByName(Department.class.getName())));
        assertFalse(((Set) context.getAttribute("leafClds")).contains(model.getClassDescriptorByName(RandomInterface.class.getName())));
        assertFalse(((Set) context.getAttribute("leafClds")).contains(model.getClassDescriptorByName(FlyMineBusinessObject.class.getName())));
    }


    public void testNonBusinessObject() throws Exception {
        ComponentContext context = new ComponentContext();
        ComponentContext.setContext(context, getRequest());
        setRequestPathInfo("/initResultsCell");

        getRequest().setAttribute("object", "test string");
        actionPerform();
        assertEquals(0, ((Set) context.getAttribute("clds")).size());
    }

}
