package org.intermine.web.results;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collection;

import org.apache.struts.tiles.ComponentContext;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.util.DynamicUtil;
import org.intermine.web.Constants;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.*;

import servletunit.struts.MockStrutsTestCase;

/**
 * Tests for ObjectViewController.
 */
public class ObjectViewControllerTest extends MockStrutsTestCase
{
    ObjectStore os;

    public void setUp() throws Exception {
        super.setUp();
        Department d = new Department();
        Set classes = new HashSet();
        classes.add(Company.class);
        d.setCompany((Company) DynamicUtil.createObject(classes));
        os = new ObjectStoreDummyImpl();
        os.cacheObjectById(new Integer(42), d);
        ((ObjectStoreDummyImpl) os).setModel(Model.getInstanceByName("testmodel"));
    }

    public void tearDown() throws Exception {
        getActionServlet().destroy();
    }

    public ObjectViewControllerTest(String arg) {
        super(arg);
    }

    public void testObject() throws Exception {
        ComponentContext context = new ComponentContext();
        ComponentContext.setContext(context, getRequest());
        setRequestPathInfo("/initObjectView");

        getActionServlet().getServletContext().setAttribute(Constants.OBJECTSTORE, os);
        addRequestParameter("id", "42");
        getRequest().setAttribute("viewType", "summary");

        actionPerform();
        
        verifyNoActionErrors();
        assertNotNull(context.getAttribute("object"));
        assertTrue(context.getAttribute("object") instanceof Department);
    }
    
    public void testField() throws Exception {
        ComponentContext context = new ComponentContext();
        ComponentContext.setContext(context, getRequest());
        setRequestPathInfo("/initObjectView");
        
        getActionServlet().getServletContext().setAttribute(Constants.OBJECTSTORE, os);
        addRequestParameter("id", "42");
        addRequestParameter("field", "company");
        getRequest().setAttribute("viewType", "summary");
        
        actionPerform();
        
        verifyNoActionErrors();
        assertNotNull(context.getAttribute("object"));
        assertTrue(context.getAttribute("object") instanceof Company);
    }
    
    public void testBusinessObject() throws Exception {
        ComponentContext context = new ComponentContext();
        ComponentContext.setContext(context, getRequest());
        setRequestPathInfo("/initObjectView");

        getRequest().setAttribute("object", new Department());
        getRequest().setAttribute("viewType", "summary");
        actionPerform();

        Model model = Model.getInstanceByName("testmodel");
        assertNotNull(context.getAttribute("leafClds"));
        assertTrue(((Collection) context.getAttribute("leafClds")).contains(model.getClassDescriptorByName(Department.class.getName())));
        assertFalse(((Collection) context.getAttribute("leafClds")).contains(model.getClassDescriptorByName(RandomInterface.class.getName())));
        assertFalse(((Collection) context.getAttribute("leafClds")).contains(model.getClassDescriptorByName(InterMineObject.class.getName())));
        Map primaryKeyFields = (Map) context.getAttribute("primaryKeyFields");
        assertNotNull(primaryKeyFields);
        LinkedHashMap testFieldNames = new LinkedHashMap();
        testFieldNames.put("name","name");
        assertEquals(testFieldNames, primaryKeyFields);
    }

    public void testNonBusinessObject() throws Exception {
        ComponentContext context = new ComponentContext();
        ComponentContext.setContext(context, getRequest());
        setRequestPathInfo("/initObjectView");

        getRequest().setAttribute("object", "test string");
        actionPerform();

        assertEquals(0, ((Collection) context.getAttribute("leafClds")).size());
    }
}
