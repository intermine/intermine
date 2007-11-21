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
public class GoStatURLQuery implements EnrichmentWidgetURLQuery
{
    ObjectStore os;
    InterMineBag bag;
    String key;
    /**
     * @param os
     * @param key
     * @param bag
     */
     public GoStatURLQuery(ObjectStore os, InterMineBag bag, String key) {
         this.bag = bag;
         this.key = key;
         this.os = os;
     }
    
    /**
     * @return Query a query to generate the results needed
     */
     public PathQuery generatePathQuery() {

//        Query q = new Query();
//
//        QueryClass qcGene = new QueryClass(Gene.class);
//        QueryClass qcGoAnnotation = new QueryClass(GOAnnotation.class);
//        QueryClass qcOrganism = new QueryClass(Organism.class);
//        QueryClass qcGo = new QueryClass(GOTerm.class);
//
//        QueryField qfQualifier = new QueryField(qcGoAnnotation, "qualifier");
//        QueryField qfGeneId = new QueryField(qcGene, "id");
//        QueryField qfGoTerm = new QueryField(qcGo, "identifier");
//
//        q.addFrom(qcGene);
//        q.addFrom(qcGoAnnotation);
//        q.addFrom(qcOrganism);
//        q.addFrom(qcGo);
//
//        q.addToSelect(qcGene);
//
//        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
//
//        // genes must be in bag
//        BagConstraint bc1 = new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb());
//        cs.addConstraint(bc1);
//
//        // gene.goAnnotation CONTAINS GOAnnotation
//        QueryCollectionReference qr1 = new QueryCollectionReference(qcGene, "allGoAnnotation");
//        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcGoAnnotation);
//        cs.addConstraint(cc1);
//
//        // gene is from organism
//        QueryObjectReference qr2 = new QueryObjectReference(qcGene, "organism");
//        ContainsConstraint cc2 = new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qcOrganism);
//        cs.addConstraint(cc2);
//
//        // goannotation contains go term
//        QueryObjectReference qr3 = new QueryObjectReference(qcGoAnnotation, "property");
//        ContainsConstraint cc3 = new ContainsConstraint(qr3, ConstraintOp.CONTAINS, qcGo);
//        cs.addConstraint(cc3);
//
//        // can't be a NOT relationship!
//        SimpleConstraint sc1 = new SimpleConstraint(qfQualifier,
//                                                    ConstraintOp.IS_NULL);
//        cs.addConstraint(sc1);
//
//        SimpleConstraint sc2 = new SimpleConstraint(qfGoTerm,
//                                                    ConstraintOp.EQUALS,
//                                                    new QueryValue(key));
//        cs.addConstraint(sc2);
//        q.setConstraint(cs);
        
         Model model = os.getModel();
         PathQuery q = new PathQuery(model);
         
         List view = new ArrayList();
         view.add(MainHelper.makePath(model, q, "Gene.identifier"));
         view.add(MainHelper.makePath(model, q, "Gene.organismDbId"));
         view.add(MainHelper.makePath(model, q, "Gene.name"));
         view.add(MainHelper.makePath(model, q, "Gene.organism.name"));
         view.add(MainHelper.makePath(model, q, "Gene.allGoAnnotation.identifier"));
         view.add(MainHelper.makePath(model, q, "Gene.allGoAnnotation.name"));
//         (1)  GO term
//         (2)  GO term ID 
         
         q.setView(view);
         
         String bagType = bag.getType();
         ConstraintOp constraintOp = ConstraintOp.IN;
         String constraintValue = bag.getName();        
         String label = null, id = null, code = q.getUnusedConstraintCode();
         Constraint c = new Constraint(constraintOp, constraintValue, false, label, code, id, null);
         q.addNode(bagType).getConstraints().add(c);
         
         // can't be a NOT relationship!
         constraintOp = ConstraintOp.IS_NULL;
         code = q.getUnusedConstraintCode();
         PathNode qualifierNode = q.addNode("Gene.allGoAnnotation.qualifier");
         Constraint qualifierConstraint 
                         = new Constraint(constraintOp, null, false, label, code, id, null);
         qualifierNode.getConstraints().add(qualifierConstraint);
         
         // go term
         constraintOp = ConstraintOp.EQUALS;
         code = q.getUnusedConstraintCode();
         PathNode goTermNode = q.addNode("Gene.allGoAnnotation.identifier");
         Constraint goTermConstraint 
                         = new Constraint(constraintOp, key, false, label, code, id, null);
         goTermNode.getConstraints().add(goTermConstraint);
         
         q.setConstraintLogic("A and B and C");
         q.syncLogicExpression("and");
        
        return q;
    }
}
