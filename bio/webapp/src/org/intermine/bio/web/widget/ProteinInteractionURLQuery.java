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
 * @author Dominik Grimm & Michael Menden
 */
public class ProteinInteractionURLQuery implements WidgetURLQuery
{

    InterMineBag bag;
    String key;
    ObjectStore os;

    /**
     * @param key value selected by user to display
     * @param bag bag included in query
     * @param os object store
     */
    public ProteinInteractionURLQuery(ObjectStore os, InterMineBag bag, String key) {
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

        Path primaryIdentifier = PathQuery.makePath(model, q, "Protein.primaryIdentifier");
        Path primaryAccesion = PathQuery.makePath(model, q, "Protein.primaryAccession");
        Path interactPrimaryIdentifier = PathQuery.makePath(model, q,
                "Protein.proteinInteractions.interactingProteins.primaryIdentifier");
        Path interactPrimaryAccession = PathQuery.makePath(model, q,
                "Protein.proteinInteractions.interactingProteins.primaryAccession");
        Path interactName = PathQuery.makePath(model, q,
                "Protein.proteinInteractions.interactingProteins.name");
        Path interactShortname = PathQuery.makePath(model, q,
                "Protein.proteinInteractions.shortName");
        Path interactPathRole = PathQuery.makePath(model, q,
                "Protein.proteinInteractions.proteinRole");
        Path interactPubMedId = PathQuery.makePath(model, q,
                "Protein.proteinInteractions.experiment.publication.pubMedId");

        view.add(primaryIdentifier);
        view.add(primaryAccesion);
        view.add(interactPrimaryIdentifier);
        view.add(interactPathRole);
        view.add(interactPrimaryAccession);
        view.add(interactName);
        view.add(interactPubMedId);
        view.add(interactShortname);

        q.setViewPaths(view);

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
        } else {
            constraintOp = ConstraintOp.LOOKUP;
            code = q.getUnusedConstraintCode();
            PathNode node = q.addNode("Protein.proteinInteractions.interactingProteins");
            c = new Constraint(constraintOp, key, false, label, code, id, null);
            node.getConstraints().add(c);
        }

        q.setConstraintLogic("A and B");
        q.syncLogicExpression("and");

        List<OrderBy>  sortOrder = new ArrayList<OrderBy>();
        sortOrder.add(new OrderBy(primaryIdentifier));
        sortOrder.add(new OrderBy(primaryAccesion));
        sortOrder.add(new OrderBy(interactName));

        q.setSortOrder(sortOrder);

        return q;
    }
}
