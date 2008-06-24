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
import java.util.List;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.path.Path;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.OrderBy;
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.widget.WidgetURLQuery;

/**
 * Generates the query to run when a user clicks on a results record in an enrichment widget.
 * @author Julie Sullivan
 */
public class UniProtFeaturesURLQuery implements WidgetURLQuery
{

    InterMineBag bag;
    String key;
    ObjectStore os;

    /**
     * @param key value selected by user to display
     * @param bag bag included in query
     * @param os object store
     */
    public UniProtFeaturesURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(Collection<InterMineObject> keys) {

        Model model = os.getModel();
        PathQuery q = new PathQuery(model);

        List<Path> view = new ArrayList<Path>();

        Path identifier = MainHelper.makePath(model, q, "Protein.primaryIdentifier");
        Path sec = MainHelper.makePath(model, q, "Protein.primaryAccession");
        Path organism = MainHelper.makePath(model, q, "Protein.organism.name");
        Path name = MainHelper.makePath(model, q, "Protein.features.feature.name");
        Path descr =  MainHelper.makePath(model, q, "Protein.features.description");
        Path begin = MainHelper.makePath(model, q, "Protein.features.begin");
        Path end = MainHelper.makePath(model, q, "Protein.features.end");

        view.add(identifier);
        view.add(sec);
        view.add(organism);
        if (keys == null) {
            view.add(name);
            view.add(descr);
            view.add(begin);
            view.add(end);
        }
        q.setView(view);

        String bagType = bag.getType();

        ConstraintOp constraintOp = ConstraintOp.IN;
        String constraintValue = bag.getName();
        String label = null, id = null, code = q.getUnusedConstraintCode();
        Constraint c = new Constraint(constraintOp, constraintValue, false, label, code, id, null);
        q.addNode(bagType).getConstraints().add(c);

        if (keys != null) {
            constraintOp = ConstraintOp.NOT_IN;
            code = q.getUnusedConstraintCode();
            c = new Constraint(constraintOp, keys, false, label, code, id, null);
            q.getNode(bagType).getConstraints().add(c);
        } else {
            constraintOp = ConstraintOp.LOOKUP;
            code = q.getUnusedConstraintCode();
            PathNode node = q.addNode("Protein.features.feature");
            c = new Constraint(constraintOp, key, false, label, code, id, null);
            node.getConstraints().add(c);
        }

        q.setConstraintLogic("A and B");
        q.syncLogicExpression("and");

        List<OrderBy>  sortOrder = new ArrayList<OrderBy>();
        if (keys == null) {
            sortOrder.add(new OrderBy(name, "asc"));
        }
        sortOrder.add(new OrderBy(identifier, "asc"));
        q.setSortOrder(sortOrder);

        return q;
    }
}
