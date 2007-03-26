package org.intermine.web.results;

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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.RandomInterface;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.util.DynamicUtil;
import org.intermine.web.Constants;

import org.apache.struts.tiles.ComponentContext;

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
        ComponentContext componentContext = new ComponentContext();
        ComponentContext.setContext(componentContext, getRequest());
        setRequestPathInfo("/initObjectView");

        getActionServlet().getServletContext().setAttribute(Constants.OBJECTSTORE, os);
        addRequestParameter("id", "42");
        getRequest().setAttribute("viewType", "summary");

        actionPerform();
        
        verifyNoActionErrors();
        assertNotNull(componentContext.getAttribute("object"));
        assertTrue(componentContext.getAttribute("object") instanceof Department);
    }
    
    public void testField() throws Exception {
        ComponentContext componentContext = new ComponentContext();
        ComponentContext.setContext(componentContext, getRequest());
        setRequestPathInfo("/initObjectView");
        
        getActionServlet().getServletContext().setAttribute(Constants.OBJECTSTORE, os);
        addRequestParameter("id", "42");
        addRequestParameter("field", "company");
        getRequest().setAttribute("viewType", "summary");
        
        actionPerform();
        
        verifyNoActionErrors();
        assertNotNull(componentContext.getAttribute("object"));
        assertTrue(componentContext.getAttribute("object") instanceof Company);
    }
    
    public void testBusinessObject() throws Exception {
        ComponentContext componentContext = new ComponentContext();
        ComponentContext.setContext(componentContext, getRequest());
        setRequestPathInfo("/initObjectView");

        Department department = new Department();
        getRequest().setAttribute("object", department);
        getRequest().setAttribute("viewType", "summary");
        actionPerform();

        Model model = Model.getInstanceByName("testmodel");
        
        Map leafDescs = (Map) getSession().getServletContext().getAttribute(Constants.LEAF_DESCRIPTORS_MAP);
        assertNotNull(leafDescs);
        assertTrue(((Set) leafDescs.get(department)).contains(model.getClassDescriptorByName(Department.class.getName())));
        assertFalse(((Set) leafDescs.get(department)).contains(model.getClassDescriptorByName(RandomInterface.class.getName())));
        assertFalse(((Set) leafDescs.get(department)).contains(model.getClassDescriptorByName(InterMineObject.class.getName())));
        Map primaryKeyFields = (Map) componentContext.getAttribute("primaryKeyFields");
        assertNotNull(primaryKeyFields);
        LinkedHashMap testFieldNames = new LinkedHashMap();
        testFieldNames.put("name","name");
        assertEquals(testFieldNames, primaryKeyFields);
    }

    public void testNonBusinessObject() throws Exception {
        ComponentContext componentContext = new ComponentContext();
        ComponentContext.setContext(componentContext, getRequest());
        setRequestPathInfo("/initObjectView");

        getRequest().setAttribute("object", "test string");
        actionPerform();

        Map leafDescs = (Map) getSession().getServletContext().getAttribute(Constants.LEAF_DESCRIPTORS_MAP);
        assertNotNull(leafDescs);
        assertEquals(0, ((Set) leafDescs.get("test string")).size());
    }
}
