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
import org.intermine.web.logic.widget.WidgetURLQuery;

/**
 * Builds a pathquery.  Used when a user clicks on a results record in an enrichment widget.
 * @author Julie Sullivan
 */

public class GeneticInteractionURLQuery implements WidgetURLQuery
{

    InterMineBag bag;
    String key;
    ObjectStore os;

    /**
     * @param key value selected by user to display
     * @param bag bag included in query
     * @param os object store
     */
    public GeneticInteractionURLQuery(ObjectStore os, InterMineBag bag, String key) {
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

        Path genePrimaryIdentifier = PathQuery.makePath(model, q, "Gene.primaryIdentifier");
        Path geneSymbol = PathQuery.makePath(model, q, "Gene.symbol");
        Path organismName = PathQuery.makePath(model, q, "Gene.organism.shortName");

        Path interactionName = PathQuery.makePath(model,
                                                   q, "Gene.geneticInteractions.shortName");
        Path interactionType = PathQuery.makePath(model,
                                                   q, "Gene.geneticInteractions.type");
        Path interactionRole = PathQuery.makePath(model,
                                                   q, "Gene.geneticInteractions.geneRole");
        Path interactor = PathQuery.makePath(model, q,
        "Gene.geneticInteractions.interactingGenes.primaryIdentifier");
        Path experimentName = PathQuery.makePath(model, q,
        "Gene.geneticInteractions.experiment.name");

        view.add(genePrimaryIdentifier);
        view.add(geneSymbol);
        view.add(organismName);
        view.add(interactionName);
        view.add(interactionType);
        view.add(interactionRole);
        view.add(interactor);
        view.add(experimentName);

        q.setViewPaths(view);

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
            q.addNode(bagType).getConstraints().add(c);
        } else {
            constraintOp = ConstraintOp.LOOKUP;
            code = q.getUnusedConstraintCode();
            PathNode geneNode = q.addNode("Gene.geneticInteractions.interactingGenes");
            c = new Constraint(constraintOp, key, false, label, code, id, null);
            geneNode.getConstraints().add(c);
        }

        q.setConstraintLogic("A and B");
        q.syncLogicExpression("and");

        List<OrderBy>  sortOrder = new ArrayList<OrderBy>();
        sortOrder.add(new OrderBy(organismName, "asc"));
        sortOrder.add(new OrderBy(genePrimaryIdentifier, "asc"));
        sortOrder.add(new OrderBy(interactionName, "asc"));
        sortOrder.add(new OrderBy(interactor, "asc"));
        q.setSortOrder(sortOrder);

        return q;
    }
}

