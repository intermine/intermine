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


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.MainHelper;
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
    public PathQuery generatePathQuery(Collection<InterMineObject> keys) {


        Model model = os.getModel();
        PathQuery q = new PathQuery(model);

        List view = new ArrayList();
        view.add(MainHelper.makePath(model, q, "Employee.name"));
        view.add(MainHelper.makePath(model, q, "Employee.department.name"));
        view.add(MainHelper.makePath(model, q, "Employee.department.company.name"));
        view.add(MainHelper.makePath(model, q, "Employee.fullTime"));

        q.setView(view);

        String bagType = bag.getType();
        ConstraintOp constraintOp = ConstraintOp.IN;
        String constraintValue = bag.getName();
        String label = null, id = null, code = q.getUnusedConstraintCode();
        Constraint c = new Constraint(constraintOp, constraintValue, false, label, code, id, null);
        q.addNode(bagType).getConstraints().add(c);

        // dept
        constraintOp = ConstraintOp.LOOKUP;
        code = q.getUnusedConstraintCode();
        PathNode deptNode = q.addNode("Employee.department");
        Constraint deptConstraint
                        = new Constraint(constraintOp, key, false, label, code, id, null);
        deptNode.getConstraints().add(deptConstraint);

        q.setConstraintLogic("A and B");
        q.syncLogicExpression("and");

        return q;
    }
}

