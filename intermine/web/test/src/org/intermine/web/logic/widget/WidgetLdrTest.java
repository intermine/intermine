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

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
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
import org.intermine.pathquery.PathQuery;
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
        QueryClass employeeQc = new QueryClass(Class.forName("org.intermine.model.testmodel.Employee"));
        query.addFrom(employeeQc);
        QueryClass departmentQc = new QueryClass(Class.forName("org.intermine.model.testmodel.Department"));
        query.addFrom(departmentQc);
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(employeeQc, "department"),
                        ConstraintOp.CONTAINS, departmentQc));
        QueryClass companyQc = new QueryClass(Class.forName("org.intermine.model.testmodel.Company"));
        query.addFrom(companyQc);
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(departmentQc, "company"),
                ConstraintOp.CONTAINS, companyQc));
        QueryClass contractorQc = new QueryClass(Class.forName("org.intermine.model.testmodel.Contractor"));
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
        widgetLdr.createQueryFieldByPath("department.company.contractors.name", q, true);
        assertEquals(query.toString(), q.toString());

        widgetLdr.createQueryFieldByPath("department.company.name", q, false);
        assertEquals(query.toString(), q.toString());
        
    }

    /**
     * Add a contains constraint to Query (q) from qcStart using path
     */
/*    protected void testAddReference(Query query, QueryClass qc, String path) {
        ConstraintSet cs = (ConstraintSet) query.getConstraint();
        QueryReference qr = null;
        String type = "";
        boolean useSubClass = false;
        if (WidgetConfigUtil.isPathContainingSubClass(os.getModel(), path)) {
            useSubClass = true;
            type = path.substring(path.indexOf("[") + 1, path.indexOf("]"));
            path = path.substring(0, path.indexOf("["));
        }
        QueryClass qcTmp = null;
        try {
            qr = new QueryObjectReference(qc, path);
            if (useSubClass) {
                try {
                    qcTmp = new QueryClass(Class.forName(os.getModel().getPackageName()
                                                      + "." + type));
                } catch (ClassNotFoundException cnfe) {
                    fail("The type " + type + " doesn't exist in the model.");
                }
            } else {
                qcTmp = new QueryClass(qr.getType());
            }
        } catch (IllegalArgumentException e) {
            // Not a reference - try collection instead
            qr = new QueryCollectionReference(qc, path);
            if (useSubClass) {
                try {
                    qcTmp = new QueryClass(Class.forName(os.getModel().getPackageName()
                                                      + "." + type));
                } catch (ClassNotFoundException cnfe) {
                    fail("The type " + type + " doesn't exist in the model.");
                }
            } else {
                qcTmp = new QueryClass(TypeUtil.getElementType(qc.getType(), path));
            }
        }
        if (addQueryClassInQuery(qcTmp, qc)) {
            String key = generateKeyForQueryClassInQuery(qcTmp, qc);
            qc = qcTmp;
            query.addFrom(qc);
            cs.addConstraint(new ContainsConstraint(qr, ConstraintOp.CONTAINS, qc));
            queryClassInQuery.put(key, qc);
        } else {
            //retrieve qc from queryClassInQuery map
            String key = generateKeyForQueryClassInQuery(qcTmp, qc);
            qc = queryClassInQuery.get(key);
        }
        return qc;
    }*/

    public void testAddQueryClassInQuery() throws ClassNotFoundException {
        QueryClass queryClassParent = new QueryClass(Class.forName("org.intermine.model.testmodel.Department"));
        QueryClass queryClass = new QueryClass(Class.forName("org.intermine.model.testmodel.Employee"));
        assertEquals(false, widgetLdr.isQueryClassInQuery(queryClass, queryClassParent));
        assertEquals(false, widgetLdr.isQueryClassInQuery(queryClass, null));
    }

    public void testGenerateKeyForQueryClassInQuery() throws ClassNotFoundException {
        QueryClass queryClass = new QueryClass(Class.forName("org.intermine.model.testmodel.Department"));
        QueryClass queryClassParent = new QueryClass(Class.forName("org.intermine.model.testmodel.Employee"));
        assertEquals("Department_Employee", widgetLdr.generateKeyForQueryClassInQuery(queryClass, queryClassParent));
        assertEquals("Employee_", widgetLdr.generateKeyForQueryClassInQuery(queryClassParent, null));
    }

    public void testCreatePathQueryView() {
        PathQuery pathQuery = new PathQuery(os.getModel());
        pathQuery.addView("Employee.name");
        pathQuery.addView("Employee.age");
        pathQuery.addView("Employee.department.name");
        assertEquals(pathQuery, widgetLdr.createPathQueryView(os,
                webConfig.getWidgets().get(("contractor_enrichment"))));
    }
    
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
