package org.intermine.bio.web.widget;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import org.intermine.web.logic.query.PathNode;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.widget.EnrichmentWidgetURLQuery;

/**
 * Builds a query to get all the genes (in bag) associated with specified go term.
 * @author Julie Sullivan
 */
public class UniProtFeaturesURLQuery implements EnrichmentWidgetURLQuery
{

    InterMineBag bag;
    String key;
    ObjectStore os;
    /**
     * @param key
     * @param bag
     * @param os
     */
    public UniProtFeaturesURLQuery(ObjectStore os, InterMineBag bag, String key) {
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
        view.add(MainHelper.makePath(model, q, "Protein.identifier"));
        view.add(MainHelper.makePath(model, q, "Protein.primaryAccession"));
        view.add(MainHelper.makePath(model, q, "Protein.organism.name"));
        view.add(MainHelper.makePath(model, q, "Protein.features.type"));
        view.add(MainHelper.makePath(model, q, "Protein.features.description"));
        view.add(MainHelper.makePath(model, q, "Protein.features.begin"));
        view.add(MainHelper.makePath(model, q, "Protein.features.end"));
        q.setView(view);

        String bagType = bag.getType();
        ConstraintOp constraintOp = ConstraintOp.IN;
        String constraintValue = bag.getName();
        String label = null, id = null, code = q.getUnusedConstraintCode();
        Constraint bc = new Constraint(constraintOp, constraintValue, false, label, code, id, null);
        q.addNode(bagType).getConstraints().add(bc);

        constraintOp = ConstraintOp.EQUALS;
        code = q.getUnusedConstraintCode();
        PathNode keywordNode = q.addNode("Protein.features.type");
        Constraint c = new Constraint(constraintOp, key, false, label, code, id, null);
        keywordNode.getConstraints().add(c);

        q.setConstraintLogic("A and B");
        q.syncLogicExpression("and");

        return q;
    }
}
