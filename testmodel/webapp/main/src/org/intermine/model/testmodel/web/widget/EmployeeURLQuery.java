package org.intermine.model.testmodel.web.widget;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.WidgetURLQuery;

/**
 * Builds a query to get all the employees (in bag) associated with specified dept.
 * @author Julie Sullivan
 */
public class EmployeeURLQuery implements WidgetURLQuery
{
    private InterMineBag bag;
    private String key;
    private ObjectStore os;

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
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(boolean showAll) {
        PathQuery q = new PathQuery(os.getModel());
        // add columns to be displayed in the results
        q.addView("Employee.name,Employee.department.name,Employee.department.company.name");
        // restrict results to objects in list
        q.addConstraint(Constraints.in(bag.getType(), bag.getName()));
        // only display objects selected
        if (!showAll) {
            q.addConstraint(Constraints.eq("Employee.department", key));
        }

        return q;
    }
}

