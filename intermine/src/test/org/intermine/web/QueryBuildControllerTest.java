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

import org.flymine.objectstore.query.*;
import org.flymine.model.testmodel.Company;
import org.flymine.model.testmodel.Department;


public class QueryBuildControllerTest extends MockStrutsTestCase
{
    public QueryBuildControllerTest(String arg1) {
        super(arg1);
    }


    public void testPopulateQueryBuildForm() throws Exception {
        QueryBuildForm form = new QueryBuildForm();
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        q.addFrom(qc1, "qc1");
        q.addFrom(qc2, "qc2");

        SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc1, "name"),
                                                  SimpleConstraint.NOT_EQUALS,
                                                  new QueryValue("name1"));
        ContainsConstraint cc1 = new ContainsConstraint(new QueryCollectionReference(qc1, "departments"),
                                                        ContainsConstraint.DOES_NOT_CONTAIN,
                                                        qc2);

        ConstraintSet c = new ConstraintSet(ConstraintSet.AND);
        c.addConstraint(sc1);
        c.addConstraint(cc1);
        q.setConstraint(c);

        QueryBuildController qbc = new QueryBuildController();
        qbc.populateQueryBuildForm(form, q, qc1);
        assertEquals(2, form.getFieldValues().size());
        assertEquals(2, form.getFieldOps().size());
        assertEquals("name1", (String) form.getFieldValues().get("name"));
        assertEquals(sc1.getType().getIndex(), (Integer) form.getFieldOps().get("name"));
        assertEquals("qc2", (String) form.getFieldValues().get("departments"));
        assertEquals(cc1.getType().getIndex(), (Integer) form.getFieldOps().get("departments"));

    }


}
