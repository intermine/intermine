package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.web.logic.widget.config.WidgetConfig;

public class WidgetConfigTest extends WidgetConfigTestCase
{
    public void testGetFiltersValues() throws Exception {
        //create employee's list 
        Profile superUser = im.getProfileManager().getSuperuserProfile();
        Employee e1 = new Employee();
        e1.setName("Employee1");
        Employee e2 = new Employee();
        e2.setName("Employee2");
        Department d1 = new Department();
        d1.setName("department");
        ObjectStoreWriter osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        osw.store(d1);
        e1.setDepartment(d1);
        e2.setDepartment(d1);
        osw.store(e1);
        osw.store(e2);
        InterMineBag list = superUser.createBag("employeeList", "Employee", "", im.getClassKeys());
        Collection<Integer> ids = new ArrayList<Integer>();
        ids.add(e1.getId()); ids.add(e2.getId());
        list.addIdsToBag(ids, "Employee");

        Map<String, WidgetConfig> widgets = webConfig.getWidgets();
        assertNull(widgets.get("contractor_enrichment").getFiltersValues(os, list));
        assertEquals("ContractorA, ContractorB", widgets.get("contractor_enrichment_with_filter2").getFiltersValues(os, list));
        assertEquals("department", widgets.get("contractor_enrichment_with_filter1").getFiltersValues(os, list));
    }
}
