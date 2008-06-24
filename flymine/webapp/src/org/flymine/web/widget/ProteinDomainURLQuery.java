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
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.OrderBy;
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.MainHelper;
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
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(Collection<InterMineObject> keys) {

        Model model = os.getModel();
        PathQuery q = new PathQuery(model);

        List<Path> view = new ArrayList<Path>();
        String bagType = bag.getType();

        Path primaryIdentifier = null, primaryAccession = null;
        Path organism = null, domainIdentifier = null, domainName = null;
        Path geneIdentifier = null, secondaryIdentifier = null;

        if (bagType.equals("Gene")) {

            primaryIdentifier = MainHelper.makePath(model, q, "Gene.proteins.primaryIdentifier");
            primaryAccession = MainHelper.makePath(model, q, "Gene.proteins.primaryAccession");
            organism = MainHelper.makePath(model, q, "Gene.proteins.organism.name");
            geneIdentifier = MainHelper.makePath(model, q, "Gene.primaryIdentifier");
            secondaryIdentifier = MainHelper.makePath(model, q, "Gene.secondaryIdentifier");
            domainIdentifier
            =  MainHelper.makePath(model, q, "Gene.proteins.proteinDomains.primaryIdentifier");
            domainName =  MainHelper.makePath(model, q, "Gene.proteins.proteinDomains.name");


        } else if (bagType.equals("Protein")) {

            primaryIdentifier = MainHelper.makePath(model, q, "Protein.primaryIdentifier");
            primaryAccession = MainHelper.makePath(model, q, "Protein.primaryAccession");
            organism = MainHelper.makePath(model, q, "Protein.organism.name");
            domainIdentifier
            =  MainHelper.makePath(model, q, "Protein.proteinDomains.primaryIdentifier");
            domainName =  MainHelper.makePath(model, q, "Protein.proteinDomains.name");
        }

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
            if (bagType.equals("Gene")) {
                constraintOp = ConstraintOp.LOOKUP;
                code = q.getUnusedConstraintCode();
                PathNode interproNode = q.addNode("Gene.proteins.proteinDomains");
                c = new Constraint(constraintOp, key, false, label, code, id, null);
                interproNode.getConstraints().add(c);
            } else if (bagType.equals("Protein")) {
                constraintOp = ConstraintOp.LOOKUP;
                code = q.getUnusedConstraintCode();
                PathNode interproNode = q.addNode("Protein.proteinDomains");
                c = new Constraint(constraintOp, key, false, label, code, id, null);
                interproNode.getConstraints().add(c);
            }
        }

        view.add(primaryIdentifier);
        view.add(primaryAccession);
        view.add(organism);
        if (bagType.equals("Gene")) {
            view.add(geneIdentifier);
            view.add(secondaryIdentifier);
        }

        if (keys == null) {
            view.add(domainIdentifier);
            view.add(domainName);
        }
        q.setView(view);
        q.setConstraintLogic("A and B");
        q.syncLogicExpression("and");

        List<OrderBy>  sortOrder = new ArrayList<OrderBy>();
        if (keys == null) {
            sortOrder.add(new OrderBy(domainIdentifier, "asc"));
            sortOrder.add(new OrderBy(domainName, "asc"));
        }
        sortOrder.add(new OrderBy(primaryIdentifier, "asc"));
        sortOrder.add(new OrderBy(primaryAccession, "asc"));
        sortOrder.add(new OrderBy(organism, "asc"));
        if (bagType.equals("Gene")) {
            sortOrder.add(new OrderBy(geneIdentifier, "asc"));
            sortOrder.add(new OrderBy(secondaryIdentifier, "asc"));
        }
        q.setSortOrder(sortOrder);

        return q;
    }
}
