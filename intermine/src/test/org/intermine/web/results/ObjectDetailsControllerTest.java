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

import java.util.Set;

import org.apache.struts.tiles.ComponentContext;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.query.fql.FqlQuery;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsRow;

import org.flymine.model.testmodel.Department;

import servletunit.struts.MockStrutsTestCase;

public class ObjectDetailsControllerTest extends MockStrutsTestCase
{
    public ObjectDetailsControllerTest(String arg1) {
        super(arg1);
    }

    public void testObject() throws Exception {
        ComponentContext context = new ComponentContext();
        ComponentContext.setContext(context, getRequest());
        setRequestPathInfo("/initObjectDetails");

        ObjectStore os = ObjectStoreFactory.getObjectStore();
        Results r = os.execute(new FqlQuery("select Department from Department", "org.flymine.model.testmodel").toQuery());
        Department d = (Department) ((ResultsRow) r.get(0)).get(0);
        System.out.println(d);

        addRequestParameter("id", d.getId().toString());

        actionPerform();

        assertNotNull(context.getAttribute("leafClds"));
        assertTrue(((Set) context.getAttribute("leafClds")).contains(os.getModel().getClassDescriptorByName("org.flymine.model.testmodel.Department")));
    }

    public void testField() throws Exception {
        ComponentContext context = new ComponentContext();
        ComponentContext.setContext(context, getRequest());
        setRequestPathInfo("/initObjectDetails");

        ObjectStore os = ObjectStoreFactory.getObjectStore();
        Results r = os.execute(new FqlQuery("select Department from Department", "org.flymine.model.testmodel").toQuery());
        Department d = (Department) ((ResultsRow) r.get(0)).get(0);
        System.out.println(d);

        addRequestParameter("id", d.getId().toString());
        addRequestParameter("field", "company");

        actionPerform();

        assertNotNull(context.getAttribute("leafClds"));
        assertTrue(((Set) context.getAttribute("leafClds")).contains(os.getModel().getClassDescriptorByName("org.flymine.model.testmodel.Company")));
    }
}
