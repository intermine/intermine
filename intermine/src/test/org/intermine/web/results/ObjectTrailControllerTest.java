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

import java.util.List;
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
 * Tests for ObjectTrailController.
 */
public class ObjectTrailControllerTest extends MockStrutsTestCase
{
    ObjectStore os;
    
    public void setUp() throws Exception {
        super.setUp();
        Department d = new Department();
        /*Set classes = new HashSet();
        classes.add(Company.class);
        d.setCompany((Company) DynamicUtil.createObject(classes));*/
        os = new ObjectStoreDummyImpl();
        os.cacheObjectById(new Integer(42), d);
        d = new Department();
        os.cacheObjectById(new Integer(43), d);
        d = new Department();
        os.cacheObjectById(new Integer(44), d);
        ((ObjectStoreDummyImpl) os).setModel(Model.getInstanceByName("testmodel"));
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
        ComponentContext context = new ComponentContext();
        ComponentContext.setContext(context, getRequest());
        setRequestPathInfo("/initObjectTrail");

        getActionServlet().getServletContext().setAttribute(Constants.OBJECTSTORE, os);
        addRequestParameter("trail", "_42_43_44");

        actionPerform();
        
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
        
        
        verifyNoActionErrors();
    }
}
