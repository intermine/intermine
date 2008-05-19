package org.flymine.web.widget;

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
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.Constraint;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.query.OrderBy;
import org.intermine.web.logic.query.PathNode;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.widget.WidgetURLQuery;

/**
 * @author Julie Sullivan
 */
public class BDGPURLQuery implements WidgetURLQuery
{

    InterMineBag bag;
    String key;
    ObjectStore os;

    /**
     * @param key which record the user clicked on
     * @param bag bag
     * @param os object store
     */
    public BDGPURLQuery(ObjectStore os, InterMineBag bag, String key) {
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

        Path secondaryIdentifier = MainHelper.makePath(model, q, "Gene.secondaryIdentifier");
        Path name = MainHelper.makePath(model, q, "Gene.name");
        Path organism = MainHelper.makePath(model, q, "Gene.organism.name");
        Path primaryIdentifier = MainHelper.makePath(model, q, "Gene.primaryIdentifier");
        Path stage  =  MainHelper.makePath(model, q, "Gene.mRNAExpressionResults.stageRange");
        Path dataset = MainHelper.makePath(model, q, "Gene.mRNAExpressionResults.source.title");
        Path term =  MainHelper.makePath(model, q,
                                         "Gene.mRNAExpressionResults.mRNAExpressionTerms.name");

        String bagType = bag.getType();

        ConstraintOp constraintOp = ConstraintOp.IN;
        String constraintValue = bag.getName();
        String label = null, id = null, code = q.getUnusedConstraintCode();
        Constraint c = new Constraint(constraintOp, constraintValue, false, label, code, id, null);
        q.addNode(bagType).getConstraints().add(c);

        if (keys != null) {
            code = q.getUnusedConstraintCode();
            constraintOp = ConstraintOp.NOT_IN;
            c = new Constraint(constraintOp, keys, false, label, code, id, null);
            q.getNode(bagType).getConstraints().add(c);
            q.setConstraintLogic("A and B");
        } else {
            constraintOp = ConstraintOp.LOOKUP;
            code = q.getUnusedConstraintCode();
            PathNode nameNode = q.addNode("Gene.mRNAExpressionResults.mRNAExpressionTerms");
            nameNode.getConstraints().add(new Constraint(constraintOp, key, false, label,
                                                         code, id, null));

            constraintOp = ConstraintOp.EQUALS;
            code = q.getUnusedConstraintCode();
            PathNode expressedNode = q.addNode("Gene.mRNAExpressionResults.expressed");
            expressedNode.getConstraints().add(new Constraint(constraintOp, Boolean.TRUE,
                                                              false, label, code, id, null));

            constraintOp = ConstraintOp.EQUALS;
            code = q.getUnusedConstraintCode();
            PathNode datasetNode = q.addNode("Gene.mRNAExpressionResults.source.title");
            datasetNode.getConstraints().add(new Constraint(constraintOp, "BDGP in situ data set",
                                                            false, label, code, id, null));
            q.setConstraintLogic("A and B and C and D");
        }


        view.add(primaryIdentifier);
        view.add(secondaryIdentifier);
        view.add(name);
        view.add(organism);

        if (keys == null) {
            view.add(stage);
            view.add(term);
            view.add(dataset);
        }

        q.setView(view);
        q.syncLogicExpression("and");

        List<OrderBy>  sortOrder = new ArrayList<OrderBy>();
        if (keys == null) {
            sortOrder.add(new OrderBy(term, "asc"));
            sortOrder.add(new OrderBy(primaryIdentifier, "asc"));
            sortOrder.add(new OrderBy(stage, "asc"));
        } else {
            sortOrder.add(new OrderBy(primaryIdentifier, "asc"));
        }
        q.setSortOrder(sortOrder);

        return q;
    }
}
