package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.ConstraintOp;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Contractor;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.web.logic.widget.config.WidgetConfig;

public class WidgetLdrTest extends WidgetConfigTestCase {
    MokaWidgetLdr widgetLdr;

    public void setUp() throws Exception {
        super.setUp();
        widgetLdr = new MokaWidgetLdr(null, os, null, webConfig.getWidgets().get(("contractor_enrichment")));
    }

    public void testBuildQueryValue() {
        PathConstraint pc1 = new PathConstraintAttribute("Employee.name", ConstraintOp.EQUALS, "EmployeeA1");
        QueryValue qv1 = widgetLdr.buildQueryValue(pc1);
        assertEquals(new QueryValue("EmployeeA1"), qv1);

        PathConstraint pc2 = new PathConstraintAttribute("Employee.fulltime", ConstraintOp.EQUALS, "true");
        QueryValue qv2 = widgetLdr.buildQueryValue(pc2);
        assertEquals(new QueryValue(true), qv2);

        PathConstraint pc3 = new PathConstraintAttribute("Employee.fulltime", ConstraintOp.EQUALS, "FALSE");
        QueryValue qv3 = widgetLdr.buildQueryValue(pc3);
        assertEquals(new QueryValue(false), qv3);

        PathConstraint pc4 = new PathConstraintAttribute("Employee.fulltime", ConstraintOp.EQUALS, "false");
        QueryValue qv4 = widgetLdr.buildQueryValue(pc4);
        assertEquals(new QueryValue(false), qv4);
    }

    public void testCreateQueryFieldByPath() throws ClassNotFoundException {
        Query query = new Query();
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        query.setConstraint(cs);
        QueryClass employeeQc = new QueryClass(Employee.class);
        query.addFrom(employeeQc);
        QueryClass departmentQc = new QueryClass(Department.class);
        query.addFrom(departmentQc);
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(employeeQc, "department"),
                        ConstraintOp.CONTAINS, departmentQc));
        QueryClass companyQc = new QueryClass(Company.class);
        query.addFrom(companyQc);
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(departmentQc, "company"),
                ConstraintOp.CONTAINS, companyQc));
        QueryClass contractorQc = new QueryClass(Contractor.class);
        query.addFrom(contractorQc);
        cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(companyQc, "contractors"),
                ConstraintOp.CONTAINS, contractorQc));
        QueryField name = new QueryField(contractorQc, "name");
        query.addToSelect(name);
        query.addToGroupBy(name);
        query.addToOrderBy(name);

        Query q = new Query();
        q.setConstraint(new ConstraintSet(ConstraintOp.AND));
        q.addFrom(widgetLdr.getStartQueryClass());
        QueryField contractorNameQf = widgetLdr.createQueryFieldByPath("department.company.contractors.name", q, true);
        assertEquals(name.toString(), contractorNameQf.toString());
        assertEquals(query.toString(), q.toString());

        widgetLdr.createQueryFieldByPath("department.company.name", q, false);
        assertEquals(query.toString(), q.toString());

    }

    public void testAddReference() throws ClassNotFoundException {
        Query query = new Query();
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        query.setConstraint(cs);
        QueryClass startQc = new QueryClass(Employee.class);
        query.addFrom(startQc);
        widgetLdr.addReference(query, startQc, "department", "Employee.department");
        ContainsConstraint cc = (ContainsConstraint) (cs.getConstraints().iterator().next());
        assertEquals("Department", cc.getQueryClass().getType().getSimpleName());
        QueryObjectReference qr = (QueryObjectReference) cc.getReference();
        assertEquals("department", qr.getFieldName());
        assertEquals("Employee", qr.getQcType().getSimpleName());
    }

    public void testCreateAttributePath() throws ClassNotFoundException {
        String[] splittedPath = {"department", "employee[CEO], name"};
        assertEquals("Employee.department.employee", widgetLdr.createAttributePath(splittedPath, 1));
    }

/*    */

    public class MokaWidgetLdr extends WidgetLdr
    {
        public MokaWidgetLdr(InterMineBag bag, ObjectStore os, String filter, WidgetConfig config) {
            super(bag, os, filter, config);
        }

        public QueryClass getStartQueryClass() {
            return startClass;
        }
    }
}
