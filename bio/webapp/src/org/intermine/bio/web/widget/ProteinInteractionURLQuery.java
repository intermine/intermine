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
    public PathQuery generatePathQuery() {

        Model model = os.getModel();
        PathQuery q = new PathQuery(model);

        List<Path> view = new ArrayList<Path>();
        
        Path proteinPrimaryIdentifier = MainHelper.makePath(model, q, "Protein.primaryIdentifier");
        Path proteinPrimaryAccesion = MainHelper.makePath(model, q, "Protein.primaryAccession");
        Path interactingProteinPrimaryIdentifier = MainHelper.makePath(model, q,
                "Protein.proteinInteractions.interactingProteins.primaryIdentifier");
        Path interactingProteinPrimaryAccession = MainHelper.makePath(model, q, 
                "Protein.proteinInteractions.interactingProteins.primaryAccession");
        Path interactingProteinName = MainHelper.makePath(model, q, 
                "Protein.proteinInteractions.interactingProteins.name");
        Path interactingProteinPubMedId = MainHelper.makePath(model, q, 
                "Protein.proteinInteractions.experiment.publication.pubMedId");
        
        view.add(proteinPrimaryIdentifier);
        view.add(proteinPrimaryAccesion);
        view.add(interactingProteinPrimaryIdentifier);
        view.add(interactingProteinPrimaryAccession);
        view.add(interactingProteinName);
        view.add(interactingProteinPubMedId);

        q.setView(view);

        String bagType = bag.getType();
        ConstraintOp constraintOp = ConstraintOp.IN;
        String constraintValue = bag.getName();
        String label = null, 
               id    = null, 
               code  = q.getUnusedConstraintCode();
        Constraint c = new Constraint(constraintOp, constraintValue, false, label, code, id, null);
        q.addNode(bagType).getConstraints().add(c);


        constraintOp = ConstraintOp.EQUALS;
        code = q.getUnusedConstraintCode();
        PathNode geneNode = 
        q.addNode("Protein.proteinInteractions.interactingProteins.primaryAccession");
        Constraint geneConstraint = new Constraint(constraintOp, key, false, label, code, id, null);
        geneNode.getConstraints().add(geneConstraint);

        q.setConstraintLogic("A and B");
        q.syncLogicExpression("and");

        List<OrderBy>  sortOrder = new ArrayList<OrderBy>();
        sortOrder.add(new OrderBy(proteinPrimaryIdentifier, "asc"));
        sortOrder.add(new OrderBy(proteinPrimaryAccesion, "asc"));
        sortOrder.add(new OrderBy(interactingProteinName, "asc"));
        
        q.setSortOrder(sortOrder);
        
        return q;
    }
}
