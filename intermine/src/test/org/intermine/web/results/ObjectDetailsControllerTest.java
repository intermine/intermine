package org.intermine.web.results;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;
import java.util.HashSet;

import org.apache.struts.tiles.ComponentContext;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.util.DynamicUtil;
import org.intermine.web.Constants;

import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;

import servletunit.struts.MockStrutsTestCase;

public class ObjectDetailsControllerTest extends MockStrutsTestCase
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

    public ObjectDetailsControllerTest(String arg1) {
        super(arg1);
    }

    public void testObject() throws Exception {
        ComponentContext context = new ComponentContext();
        ComponentContext.setContext(context, getRequest());
        setRequestPathInfo("/initObjectDetails");

        getActionServlet().getServletContext().setAttribute(Constants.OBJECTSTORE, os);
        addRequestParameter("id", "42");

        actionPerform();

        verifyNoActionErrors();
        assertNotNull(context.getAttribute("object"));
        assertTrue(context.getAttribute("object") instanceof Department);
    }

    public void testField() throws Exception {
        ComponentContext context = new ComponentContext();
        ComponentContext.setContext(context, getRequest());
        setRequestPathInfo("/initObjectDetails");

        getActionServlet().getServletContext().setAttribute(Constants.OBJECTSTORE, os);
        addRequestParameter("id", "42");
        addRequestParameter("field", "company");

        actionPerform();

        verifyNoActionErrors();
        assertNotNull(context.getAttribute("object"));
        assertTrue(context.getAttribute("object") instanceof Company);
    }
}
