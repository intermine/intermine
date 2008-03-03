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
import org.intermine.web.logic.query.PathNode;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.widget.WidgetURLQuery;

/**
 * Builds a query to get all the genes (in bag) associated with specified go term.
 * @author Julie Sullivan
 */
public class TiffinURLQuery implements WidgetURLQuery
{

    InterMineBag bag;
    String key;
    ObjectStore os;
    /**
     * @param key which bar the user clicked on
     * @param bag bag
     * @param os object store
     */
    public TiffinURLQuery(ObjectStore os, InterMineBag bag, String key) {
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
//        QueryClass qcIntergenicRegion = new QueryClass(IntergenicRegion.class);
//        QueryClass qcTFBindingSite = new QueryClass(TFBindingSite.class);
//        QueryClass qcDataSet = new QueryClass(DataSet.class);
//        QueryClass qcMotif = new QueryClass(Motif.class);
//        QueryClass qcOrganism = new QueryClass(Organism.class);
//
//        QueryField qfGeneId = new QueryField(qcGene, "id");
//        QueryField qfOrganismName = new QueryField(qcOrganism, "name");
//        QueryField qfDataSet = new QueryField(qcDataSet, "title");
//        QueryField qfMotif = new QueryField(qcMotif, "primaryIdentifier");
//
//        q.addFrom(qcGene);
//        q.addFrom(qcIntergenicRegion);
//        q.addFrom(qcTFBindingSite);
//        q.addFrom(qcDataSet);
//        q.addFrom(qcMotif);
//        q.addFrom(qcOrganism);
//
//        q.addToSelect(qcGene);
//
//        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
//
//        // genes must be in bag
//        BagConstraint bc1 = new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb());
//        cs.addConstraint(bc1);
//
//        // get organisms
//        Collection organisms = BioUtil.getOrganisms(os, bag);
//
//        // limit to organisms in the bag
//        BagConstraint bc2 = new BagConstraint(qfOrganismName, ConstraintOp.IN, organisms);
//        cs.addConstraint(bc2);
//
//        // gene is from organism
//        QueryObjectReference qr1 = new QueryObjectReference(qcGene, "organism");
//        ContainsConstraint cc1 =
//            new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcOrganism);
//        cs.addConstraint(cc1);
//
//        QueryObjectReference qr2 =
//            new QueryObjectReference(qcGene, "upstreamIntergenicRegion");
//        ContainsConstraint cc2 =
//            new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qcIntergenicRegion);
//        cs.addConstraint(cc2);
//
//        QueryCollectionReference qr3 =
//            new QueryCollectionReference(qcIntergenicRegion, "overlappingFeatures");
//        ContainsConstraint cc3 =
//            new ContainsConstraint(qr3, ConstraintOp.CONTAINS, qcTFBindingSite);
//        cs.addConstraint(cc3);
//
//        QueryCollectionReference qr4 =
//            new QueryCollectionReference(qcTFBindingSite, "evidence");
//        ContainsConstraint cc4 = new ContainsConstraint(qr4, ConstraintOp.CONTAINS, qcDataSet);
//        cs.addConstraint(cc4);
//
//        QueryObjectReference qr5 =
//            new QueryObjectReference(qcTFBindingSite, "motif");
//        ContainsConstraint cc5 = new ContainsConstraint(qr5, ConstraintOp.CONTAINS, qcMotif);
//        cs.addConstraint(cc5);
//
//        SimpleConstraint sc1 =
//            new SimpleConstraint(qfDataSet, ConstraintOp.EQUALS, new QueryValue("Tiffin"));
//        cs.addConstraint(sc1);
//
//        SimpleConstraint sc2 =
//            new SimpleConstraint(qfMotif, ConstraintOp.EQUALS, new QueryValue(key));
//        cs.addConstraint(sc2);
//
//        q.setConstraint(cs);

        Model model = os.getModel();
        PathQuery q = new PathQuery(model);

        List<Path> view = new ArrayList<Path>();
        view.add(MainHelper.makePath(model, q, "Gene.identifier"));
        view.add(MainHelper.makePath(model, q, "Gene.primaryIdentifier"));
        view.add(MainHelper.makePath(model, q, "Gene.allGoAnnotation.identifier"));

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
        PathNode orgNode = q.addNode("Gene.allGoAnnotation.qualifier");
        Constraint orgConstraint
                        = new Constraint(constraintOp, null, false, label, code, id, null);
        orgNode.getConstraints().add(orgConstraint);

        // go term
        constraintOp = ConstraintOp.LOOKUP;
        code = q.getUnusedConstraintCode();
        PathNode goTermNode = q.addNode("Gene.allGoAnnotation");
        Constraint goTermConstraint
                        = new Constraint(constraintOp, key, false, label, code, id, null);
        goTermNode.getConstraints().add(goTermConstraint);

        q.setConstraintLogic("A and B and C");
        q.syncLogicExpression("and");

        return q;
    }
}

