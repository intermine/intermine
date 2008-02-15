package org.flymine.web.widget;

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
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.Constraint;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.query.PathNode;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.widget.WidgetURLQuery;

/**
 * Builds a query to get all the genes (in bag) associated with specified go term.
 * @author Julie Sullivan
 */
public class ProteinDomainURLQuery implements WidgetURLQuery
{

    InterMineBag bag;
    String key;
    ObjectStore os;

    /**
     * @param key which protein domain the user clicked on
     * @param bag bag
     * @param os object store
     */
    public ProteinDomainURLQuery(ObjectStore os, InterMineBag bag, String key) {
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

        List view = new ArrayList();
        String bagType = bag.getType();

        ConstraintOp constraintOp = ConstraintOp.IN;
        String constraintValue = bag.getName();
        String label = null, id = null, code = q.getUnusedConstraintCode();
        Constraint c = new Constraint(constraintOp, constraintValue, false, label, code, id, null);
        q.addNode(bagType).getConstraints().add(c);

        if (bagType.equalsIgnoreCase("gene")) {

            view.add(MainHelper.makePath(model, q, "Gene.secondaryIdentifier"));
            view.add(MainHelper.makePath(model, q, "Gene.primaryIdentifier"));
            view.add(MainHelper.makePath(model, q, "Gene.name"));
            view.add(MainHelper.makePath(model, q, "Gene.organism.name"));
            view.add(MainHelper.makePath(model, q, "Gene.proteins.proteinDomains.identifier"));
            view.add(MainHelper.makePath(model, q, "Gene.proteins.proteinDomains.name"));
            q.setView(view);

            constraintOp = ConstraintOp.LOOKUP;
            code = q.getUnusedConstraintCode();
            PathNode interproNode = q.addNode("Gene.proteins.proteinDomains");
            Constraint interproConstraint
            = new Constraint(constraintOp, key, false, label, code, id, null);
            interproNode.getConstraints().add(interproConstraint);
        }
        else if (bagType.equalsIgnoreCase("protein")) {
            view.add(MainHelper.makePath(model, q, "Protein.primaryIdentifier"));
            view.add(MainHelper.makePath(model, q, "Protein.primaryAccession"));
            view.add(MainHelper.makePath(model, q, "Protein.name"));
            view.add(MainHelper.makePath(model, q, "Protein.organism.name"));
            view.add(MainHelper.makePath(model, q, "Protein.proteinDomains.identifier"));
            view.add(MainHelper.makePath(model, q, "Protein.proteinDomains.name"));
            q.setView(view);

            constraintOp = ConstraintOp.LOOKUP;
            code = q.getUnusedConstraintCode();
            PathNode interproNode = q.addNode("Protein.proteinDomains");
            Constraint interproConstraint
            = new Constraint(constraintOp, key, false, label, code, id, null);
            interproNode.getConstraints().add(interproConstraint);
        }
        else {
            //?
        }

        q.setConstraintLogic("A and B");
        q.syncLogicExpression("and");

        return q;
    }
}
