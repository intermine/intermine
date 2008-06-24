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
 * Builds a pathquery.  Used when a user clicks on a results record in an enrichment widget.
 * @author Julie Sullivan
 */

public class HomologueURLQuery implements WidgetURLQuery
{

    InterMineBag bag;
    String key;
    ObjectStore os;

    /**
     * @param key value selected by user to display
     * @param bag bag included in query
     * @param os object store
     */
    public HomologueURLQuery(ObjectStore os, InterMineBag bag, String key) {
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

        Path genePrimaryIdentifier = MainHelper.makePath(model, q, "Gene.primaryIdentifier");
        Path geneSymbol = MainHelper.makePath(model, q, "Gene.symbol");
        Path organismName = MainHelper.makePath(model, q, "Gene.organism.name");

        Path homologueIdentifier
        = MainHelper.makePath(model, q, "Gene.homologues.homologue.primaryIdentifier");
        Path homologueSymbol
        = MainHelper.makePath(model, q, "Gene.homologues.homologue.symbol");
        Path homologueOrganism
        = MainHelper.makePath(model, q, "Gene.homologues.homologue.organism.name");
        Path homologueType
        = MainHelper.makePath(model, q, "Gene.homologues.type");

        view.add(genePrimaryIdentifier);
        view.add(geneSymbol);
        view.add(organismName);
        view.add(homologueType);
        view.add(homologueIdentifier);
        view.add(homologueSymbol);
        view.add(homologueOrganism);

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
            q.addNode(bagType).getConstraints().add(c);
            q.setConstraintLogic("A and B");
        } else {

            // constraint the organism name
            constraintOp = ConstraintOp.LOOKUP;
            code = q.getUnusedConstraintCode();
            PathNode organismNode = q.addNode("Gene.homologues.homologue.organism");
            c = new Constraint(constraintOp, key, false, label, code, id, null);
            organismNode.getConstraints().add(c);

            // constrain homologue.type to be 'orthologue;
            constraintOp = ConstraintOp.EQUALS;
            code = q.getUnusedConstraintCode();
            PathNode typeNode = q.addNode("Gene.homologues.type");
            c = new Constraint(constraintOp, "orthologue", false, label, code, id, null);
            typeNode.getConstraints().add(c);
            q.setConstraintLogic("A and B and C");
        }

        q.syncLogicExpression("and");

        List<OrderBy>  sortOrder = new ArrayList<OrderBy>();
        sortOrder.add(new OrderBy(organismName, "asc"));
        sortOrder.add(new OrderBy(genePrimaryIdentifier, "asc"));
        sortOrder.add(new OrderBy(homologueOrganism, "asc"));
        sortOrder.add(new OrderBy(homologueIdentifier, "asc"));
        q.setSortOrder(sortOrder);

        return q;
    }
}

