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

import org.intermine.bio.web.logic.BioUtil;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
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
public class PathwayURLQuery implements EnrichmentWidgetURLQuery
{

    InterMineBag bag;
    String key;
    ObjectStore os;
    /**
     * @param key
     * @param bag
     * @param os
     */
    public PathwayURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * @return Query a query to generate the results needed
     */
    public PathQuery generatePathQuery() {
                       
//        Query q = new Query();

//        QueryClass qcGene = new QueryClass(Gene.class);
//        QueryClass qcPub = new QueryClass(Publication.class);
//        QueryClass qcOrganism = new QueryClass(Organism.class);
//        
//        QueryField qfGeneId = new QueryField(qcGene, "id");
//        QueryField qfId = new QueryField(qcPub, "pubMedId");
//        QueryField qfOrganismName = new QueryField(qcOrganism, "name");
//        
//        q.addFrom(qcGene);
//        q.addFrom(qcPub);
//        q.addFrom(qcOrganism);
//        
//        q.addToSelect(qcGene);
//    
//        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
//
//        // genes must be in bag
//        BagConstraint bc1 =
//            new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb());
//        cs1.addConstraint(bc1);
//
//
        // get organisms
        ArrayList organisms = (ArrayList) BioUtil.getOrganisms(os, bag);
//
//        // limit to organisms in the bag
//        BagConstraint bc2 = new BagConstraint(qfOrganismName, ConstraintOp.IN, organisms);
//        cs1.addConstraint(bc2);
//        
//        // gene is from organism
//        QueryObjectReference qr1 = new QueryObjectReference(qcGene, "organism");
//        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcOrganism);
//        cs1.addConstraint(cc1);
//        
//        // gene.Publications CONTAINS pub
//        QueryCollectionReference qr2 = new QueryCollectionReference(qcGene, "publications");
//        ContainsConstraint cc2 =
//            new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qcPub);
//        cs1.addConstraint(cc2);
//        
//        SimpleConstraint sc = 
//            new SimpleConstraint(qfId, ConstraintOp.MATCHES, new QueryValue(key));
//        cs1.addConstraint(sc);
//
//        q.setConstraint(cs1);
        
        Model model = os.getModel();
        PathQuery q = new PathQuery(model);
        
        List view = new ArrayList();
        view.add(MainHelper.makePath(model, q, "Gene.identifier"));
        view.add(MainHelper.makePath(model, q, "Gene.organismDbId"));
        view.add(MainHelper.makePath(model, q, "Gene.name"));
        view.add(MainHelper.makePath(model, q, "Gene.organism.name"));
        view.add(MainHelper.makePath(model, q, "Gene.pathways.identifier"));
        view.add(MainHelper.makePath(model, q, "Gene.pathways.name"));
        
        q.setView(view);
        
        String bagType = bag.getType();
        ConstraintOp constraintOp = ConstraintOp.IN;
        String constraintValue = bag.getName();        
        String label = null, id = null, code = q.getUnusedConstraintCode();
        Constraint c = new Constraint(constraintOp, constraintValue, false, label, code, id, null);
        q.addNode(bagType).getConstraints().add(c);
        
        // pubmedid
        constraintOp = ConstraintOp.EQUALS;
        code = q.getUnusedConstraintCode();
        PathNode expressedNode = q.addNode("Gene.pathways.identifier");
        Constraint expressedConstraint 
                        = new Constraint(constraintOp, key, false, label, code, id, null);
        expressedNode.getConstraints().add(expressedConstraint);
        
        q.setConstraintLogic("A and B");
        q.syncLogicExpression("and");
                
        return q;
    }
}

