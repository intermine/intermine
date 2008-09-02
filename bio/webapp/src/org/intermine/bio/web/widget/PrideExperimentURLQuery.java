package org.intermine.bio.web.widget;

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

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.WidgetURLQuery;

/**
 * Builds a query to get all the employees (in bag) associated with specified dept.
 * @author Dominik Grimm and Michael Menden
 */

public class PrideExperimentURLQuery implements WidgetURLQuery
{
    InterMineBag bag;
    String key;
    ObjectStore os;

    /**
     * @param key value selected by user to display
     * @param bag bag included in query
     * @param os object store
     */
    public PrideExperimentURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(Collection<InterMineObject> keys) {
        PathQuery q = new PathQuery(os.getModel());
        q.setView("Protein.proteinIdentifications.prideExperiment.title,Protein.primaryIdentifier,"
                  + "Protein.primaryAccession,Protein.name");
        String bagType = bag.getType();
        q.addConstraint(bagType,  Constraints.in(bag.getName()));
        if (keys != null) {
            q.addConstraint(bagType,  Constraints.notIn(new ArrayList(keys)));
        } else {
            q.addConstraint("Protein.proteinIdentifications.prideExperiment.title",
                            Constraints.eq(key));
        }
        q.setConstraintLogic("A and B");
        q.syncLogicExpression("and");
        q.setOrderBy("Protein.proteinIdentifications.prideExperiment.title");
        return q;
    }
}
