package org.flymine.web;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpSession;

import servletunit.struts.MockStrutsTestCase;
import org.apache.struts.tiles.ComponentContext;

import java.util.List;
import java.util.Arrays;
import java.util.HashMap;

import org.flymine.objectstore.query.*;
import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.model.testmodel.Company;
import org.flymine.model.testmodel.Department;


public class QueryBuildControllerTest extends MockStrutsTestCase
{
    public QueryBuildControllerTest(String arg1) {
        super(arg1);
    }


    public void testPopulateQueryBuildForm() throws Exception {
        QueryBuildForm form = new QueryBuildForm();
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);

        SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc1, "name"),
                                                  SimpleConstraint.NOT_EQUALS,
                                                  new QueryValue("name1"));
        SimpleConstraint sc2 = new SimpleConstraint(new QueryField(qc1, "name"),
                                                  SimpleConstraint.EQUALS,
                                                  new QueryValue("name2"));
        ContainsConstraint cc1 = new ContainsConstraint(new QueryCollectionReference(qc1, "departments"),
                                                        ContainsConstraint.DOES_NOT_CONTAIN,
                                                        qc2);

        List constraints = Arrays.asList(new Object[] {sc1, sc2, cc1});
        ClassDescriptor cld = Model.getInstanceByName("testmodel")
            .getClassDescriptorByName("org.flymine.model.testmodel.Company");
        HashMap aliasMap = new HashMap();
        aliasMap.put(qc2, "qc2");

        QueryBuildController qbc = new QueryBuildController();
        qbc.populateQueryBuildForm(form, cld, constraints, aliasMap);
        assertEquals(3, form.getFieldValues().size());
        assertEquals(3, form.getFieldOps().size());
        assertEquals("name1", (String) form.getFieldValues().get("name#0"));
        assertEquals(sc1.getType().getIndex(), (Integer) form.getFieldOps().get("name#0"));
        assertEquals("name2", (String) form.getFieldValues().get("name#1"));
        assertEquals(sc2.getType().getIndex(), (Integer) form.getFieldOps().get("name#1"));
        assertEquals("qc2", (String) form.getFieldValues().get("departments#0"));
        assertEquals(cc1.getType().getIndex(), (Integer) form.getFieldOps().get("departments#0"));

    }


}
