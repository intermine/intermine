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
import java.util.List;
import java.util.Set;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Broke;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Department;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.SessionMethods;
import org.intermine.web.logic.results.ObjectTrailController;

import org.apache.struts.tiles.ComponentContext;

import servletunit.struts.MockStrutsTestCase;

/**
 * Tests for ObjectTrailController.
 */
public class ObjectTrailControllerTest extends MockStrutsTestCase
{
    ObjectStore os;
    
    public void setUp() throws Exception {
        super.setUp();
        Department d = new Department();
        d.setId(new Integer(42));
        /*Set classes = new HashSet();
        classes.add(Company.class);
        d.setCompany((Company) DynamicUtil.createObject(classes));*/
        os = new ObjectStoreDummyImpl();
        os.cacheObjectById(new Integer(42), d);
        d = new Department();
        d.setId(new Integer(43));
        os.cacheObjectById(new Integer(43), d);
        d = new Department();
        d.setId(new Integer(44));
        os.cacheObjectById(new Integer(44), d);
        ((ObjectStoreDummyImpl) os).setModel(Model.getInstanceByName("testmodel"));
        
        SessionMethods.initSession(getSession());
    }

    public void tearDown() throws Exception {
        getActionServlet().destroy();
        super.tearDown();
    }

    public ObjectTrailControllerTest(String arg) {
        super(arg);
    }

    public void testCreateTrailLabel() throws Exception {
        Department d = new Department();
        String label = ObjectTrailController.createTrailLabel(d, os.getModel());
        assertEquals("Department", label);
        
        Set classes = new HashSet();
        classes.add(CEO.class);
        classes.add(Broke.class);
        label = ObjectTrailController.createTrailLabel((InterMineObject) DynamicUtil.createObject(classes), os.getModel());
        assertEquals("CEO Broke", label);
        
    }
    
    public void testPopulateTrailElements() throws Exception {
        ComponentContext componentContext = new ComponentContext();
        ComponentContext.setContext(componentContext, getRequest());
        setRequestPathInfo("/initObjectTrail");

        getActionServlet().getServletContext().setAttribute(Constants.OBJECTSTORE, os);
        addRequestParameter("trail", "_42_43_44");

        actionPerform();
        
        verifyNoActionErrors();
        
        List c = (List) getRequest().getAttribute("trailElements");
        assertNotNull("trailElements attribute is null", c);
        assertEquals(3, c.size());
        ObjectTrailController.TrailElement e0 = (ObjectTrailController.TrailElement) c.get(0);
        ObjectTrailController.TrailElement e1 = (ObjectTrailController.TrailElement) c.get(1);
        ObjectTrailController.TrailElement e2 = (ObjectTrailController.TrailElement) c.get(2);
        assertNotNull(e0);
        assertNotNull(e1);
        assertNotNull(e2);
        
        assertEquals("_42", e0.getTrail());
        assertEquals("_42_43", e1.getTrail());
        assertEquals("_42_43_44", e2.getTrail());
        
        assertEquals("Department", e0.getLabel());
        assertEquals("Department", e1.getLabel());
        assertEquals("Department", e2.getLabel());
        
        assertEquals(42, e0.getObjectId());
        assertEquals(43, e1.getObjectId());
        assertEquals(44, e2.getObjectId());
    }
    
//    public void testTableParameter() {
//        ComponentContext componentContext = new ComponentContext();
//        ComponentContext.setContext(componentContext, getRequest());
//        setRequestPathInfo("/initObjectTrail");
//        addRequestParameter("table", "results.0");
//        SessionMethods.setResultsTable(getSession(), "results.0", new PagedObject("", null));
//        getActionServlet().getServletContext().setAttribute(Constants.OBJECTSTORE, os);
//
//        actionPerform();
//  
//        List c = (List) getRequest().getAttribute("trailElements");
//        assertEquals(1, c.size());
//        
//        ObjectTrailController.TrailElement e0 = (ObjectTrailController.TrailElement) c.get(0);
//        assertTrue(e0.isTable());
//        assertEquals("results.0", e0.getTableId());
//        
//        verifyNoActionErrors();
//    }
    
    public void testTableParameterDoesNotExist() {
        ComponentContext componentContext = new ComponentContext();
        ComponentContext.setContext(componentContext, getRequest());
        setRequestPathInfo("/initObjectTrail");
        addRequestParameter("table", "results.0");
        
        getActionServlet().getServletContext().setAttribute(Constants.OBJECTSTORE, os);

        actionPerform();
        
        List c = (List) getRequest().getAttribute("trailElements");
        assertEquals(0, c.size());
        
        verifyNoActionErrors();
    }
    
//    public void testTableInTrail() {
//        ComponentContext componentContext = new ComponentContext();
//        ComponentContext.setContext(componentContext, getRequest());
//        setRequestPathInfo("/initObjectTrail");
//        addRequestParameter("trail", "_results.0_42");
//        
//        SessionMethods.setResultsTable(getSession(), "results.0", new PagedObject("", null));
//        getActionServlet().getServletContext().setAttribute(Constants.OBJECTSTORE, os);
//        
//        actionPerform();
//               
//        List c = (List) getRequest().getAttribute("trailElements");
//        assertEquals(2, c.size());
//        
//        ObjectTrailController.TrailElement e0 = (ObjectTrailController.TrailElement) c.get(0);
//        assertTrue(e0.isTable());
//        assertEquals("results.0", e0.getTableId());
//        
//        ObjectTrailController.TrailElement e1 = (ObjectTrailController.TrailElement) c.get(1);
//        assertFalse(e1.isTable());
//        assertEquals(42, e1.getObjectId());
//        
//        verifyNoActionErrors();
//    }
//    
    public void testTableInTrailDoesNotExist() {
        ComponentContext componentContext = new ComponentContext();
        ComponentContext.setContext(componentContext, getRequest());
        setRequestPathInfo("/initObjectTrail");
        addRequestParameter("trail", "|results.0|42");

        getActionServlet().getServletContext().setAttribute(Constants.OBJECTSTORE, os);

        actionPerform();
        
        List c = (List) getRequest().getAttribute("trailElements");
        assertEquals(1, c.size());
        
        ObjectTrailController.TrailElement e1 = (ObjectTrailController.TrailElement) c.get(0);
        assertFalse(e1.getType().equals("results"));
        assertEquals(42, e1.getObjectId());
        
        verifyNoActionErrors();
    }
    
    public void testNoTrailParameter() {
        ComponentContext componentContext = new ComponentContext();
        ComponentContext.setContext(componentContext, getRequest());
        setRequestPathInfo("/initObjectTrail");

        getActionServlet().getServletContext().setAttribute(Constants.OBJECTSTORE, os);

        actionPerform();
        
        verifyNoActionErrors();
    }
    
    public void testEmptyTrailParameter() {
        ComponentContext componentContext = new ComponentContext();
        ComponentContext.setContext(componentContext, getRequest());
        setRequestPathInfo("/initObjectTrail");

        getActionServlet().getServletContext().setAttribute(Constants.OBJECTSTORE, os);
        addRequestParameter("trail", "");

        actionPerform();
        
        verifyNoActionErrors();
    }
    
    public void testJunkTrailParameter() {
        ComponentContext componentContext = new ComponentContext();
        ComponentContext.setContext(componentContext, getRequest());
        setRequestPathInfo("/initObjectTrail");

        getActionServlet().getServletContext().setAttribute(Constants.OBJECTSTORE, os);
        addRequestParameter("trail", "sdfklksdkasdf");

        actionPerform();
        
        verifyNoActionErrors();
    }
    
    public void testBadIdsTrailParameter() {
        ComponentContext componentContext = new ComponentContext();
        ComponentContext.setContext(componentContext, getRequest());
        setRequestPathInfo("/initObjectTrail");

        getActionServlet().getServletContext().setAttribute(Constants.OBJECTSTORE, os);
        addRequestParameter("trail", "_234234_2345544");

        actionPerform();
        
        verifyNoActionErrors();
    }
}
