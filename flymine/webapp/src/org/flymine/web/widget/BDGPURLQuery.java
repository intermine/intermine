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
import java.util.List;

import org.intermine.objectstore.query.ConstraintOp;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
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
     * @return Query a query to generate the results needed
     */
    public PathQuery generatePathQuery() {

        Model model = os.getModel();
        PathQuery q = new PathQuery(model);

        List<Path> view = new ArrayList<Path>();
        
        Path primaryIdentifier = MainHelper.makePath(model, q, "Gene.primaryIdentifier");
        Path stage  =  MainHelper.makePath(model, q, "Gene.mRNAExpressionResults.stageRange");
        Path term =  MainHelper.makePath(model, q, 
                                         "Gene.mRNAExpressionResults.mRNAExpressionTerms.name");
        
        ConstraintOp constraintOp = ConstraintOp.IN;
        String constraintValue = bag.getName();
        String label = null, id = null, code = q.getUnusedConstraintCode();
        Constraint c = new Constraint(constraintOp, constraintValue, false, label, code, id, null);
        q.addNode(bag.getType()).getConstraints().add(c);
        
        
        constraintOp = ConstraintOp.EQUALS;
        code = q.getUnusedConstraintCode();
        PathNode node = q.addNode("Gene.mRNAExpressionResults.mRNAExpressionTerms.name");
        node.getConstraints().add(new Constraint(constraintOp, key, false, label, code, id, null));

        view.add(primaryIdentifier);
        view.add(stage);
        view.add(term);

        q.setView(view);
        
        q.setConstraintLogic("A and B");
        q.syncLogicExpression("and");
        
        List<OrderBy>  sortOrder = new ArrayList<OrderBy>();   
        sortOrder.add(new OrderBy(stage, "asc"));
        sortOrder.add(new OrderBy(term, "asc"));
        sortOrder.add(new OrderBy(primaryIdentifier, "asc"));
        q.setSortOrder(sortOrder);
        
        return q;
    }
}
