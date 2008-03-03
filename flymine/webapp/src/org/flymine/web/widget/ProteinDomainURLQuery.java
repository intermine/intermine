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

        List<Path> view = new ArrayList<Path>();
        String bagType = bag.getType();

        ConstraintOp constraintOp = ConstraintOp.IN;
        String constraintValue = bag.getName();
        String label = null, id = null, code = q.getUnusedConstraintCode();
        Constraint c = new Constraint(constraintOp, constraintValue, false, label, code, id, null);
        q.addNode(bagType).getConstraints().add(c);
        
        Path secondaryIdentifier = null;
        Path primaryIdentifier = MainHelper.makePath(model, q, bagType + ".primaryIdentifier");
        Path name = MainHelper.makePath(model, q, bagType + ".name");
        Path organism = MainHelper.makePath(model, q, bagType + ".organism.name");
        Path domainIdentifier = null;
        Path domainName = null;

        if (bagType.equalsIgnoreCase("gene")) {
            
            secondaryIdentifier = MainHelper.makePath(model, q, "Gene.secondaryIdentifier");
            domainIdentifier 
            =  MainHelper.makePath(model, q, "Gene.proteins.proteinDomains.primaryIdentifier");
            domainName =  MainHelper.makePath(model, q, "Gene.proteins.proteinDomains.name");
            
            constraintOp = ConstraintOp.LOOKUP;
            code = q.getUnusedConstraintCode();
            PathNode interproNode = q.addNode("Gene.proteins.proteinDomains");
            Constraint interproConstraint
            = new Constraint(constraintOp, key, false, label, code, id, null);
            interproNode.getConstraints().add(interproConstraint);
            
        } else if (bagType.equalsIgnoreCase("protein")) {

            secondaryIdentifier = MainHelper.makePath(model, q, "Protein.primaryAccession");
            domainIdentifier 
            =  MainHelper.makePath(model, q, "Protein.proteinDomains.primaryIdentifier");
            domainName =  MainHelper.makePath(model, q, "Protein.proteinDomains.name");
            
            constraintOp = ConstraintOp.LOOKUP;
            code = q.getUnusedConstraintCode();
            PathNode interproNode = q.addNode("Protein.proteinDomains");
            Constraint interproConstraint
            = new Constraint(constraintOp, key, false, label, code, id, null);
            interproNode.getConstraints().add(interproConstraint);
        }

        view.add(primaryIdentifier);
        view.add(secondaryIdentifier);
        view.add(name);
        view.add(organism);
        view.add(domainIdentifier);
        view.add(domainName);
        q.setView(view);
        

        q.setConstraintLogic("A and B");
        q.syncLogicExpression("and");
        
        List<OrderBy>  sortOrder = new ArrayList<OrderBy>();   
        sortOrder.add(new OrderBy(domainIdentifier, "asc"));
        sortOrder.add(new OrderBy(domainName, "asc"));
        sortOrder.add(new OrderBy(primaryIdentifier, "asc"));
        sortOrder.add(new OrderBy(secondaryIdentifier, "asc"));
        sortOrder.add(new OrderBy(name, "asc"));
        sortOrder.add(new OrderBy(organism, "asc"));
        q.setSortOrder(sortOrder);
        
        return q;
    }
}
