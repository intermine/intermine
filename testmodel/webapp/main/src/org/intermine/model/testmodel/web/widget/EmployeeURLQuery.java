package org.intermine.model.testmodel.web.widget;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.WidgetURLQuery;

/**
 * Builds a query to get all the employees (in bag) associated with specified dept.
 * @author Julie Sullivan
 */
public class EmployeeURLQuery implements WidgetURLQuery
{

    InterMineBag bag;
    String key;
    ObjectStore os;
    /**
     * @param key
     * @param bag
     * @param os
     */
    public EmployeeURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * @return Query a query to generate the results needed
     */
    public PathQuery generatePathQuery() {
        Model model = os.getModel();
        PathQuery q = new PathQuery(model);
        q.setView("Employee.name,Employee.department.name,Employee.department.company.name,"
                  + "Employee.fullTime");
        q.addConstraint(bag.getType(),  Constraints.in(bag.getName()));
        q.addConstraint("Employee.department",  Constraints.eq(key));
        q.setConstraintLogic("A and B");
        q.syncLogicExpression("and");
        return q;
    }
}

