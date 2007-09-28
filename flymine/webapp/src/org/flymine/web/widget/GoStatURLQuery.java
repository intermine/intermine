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

import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;

import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.EnrichmentWidgetURLQuery;

import org.flymine.model.genomic.GOAnnotation;
import org.flymine.model.genomic.GOTerm;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Organism;
/**
 * Builds a query to get all the genes (in bag) associated with specified go term.
 * @author Julie Sullivan
 */
public class GoStatURLQuery implements EnrichmentWidgetURLQuery
{

    InterMineBag bag;
    String key;
    /**
     * @param os
     * @param key
     * @param bag
     */
     public GoStatURLQuery(@SuppressWarnings("unused") ObjectStore os, 
                           InterMineBag bag, String key) {
         this.bag = bag;
         this.key = key;
     }
    
    /**
     * @return Query a query to generate the results needed
     */
    public Query getQuery() {

        Query q = new Query();

        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcGoAnnotation = new QueryClass(GOAnnotation.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);
        QueryClass qcGo = new QueryClass(GOTerm.class);

        QueryField qfQualifier = new QueryField(qcGoAnnotation, "qualifier");
        QueryField qfGeneId = new QueryField(qcGene, "id");
        QueryField qfGoTerm = new QueryField(qcGo, "identifier");

        q.addFrom(qcGene);
        q.addFrom(qcGoAnnotation);
        q.addFrom(qcOrganism);
        q.addFrom(qcGo);

        q.addToSelect(qcGene);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        // genes must be in bag
        BagConstraint bc1 = new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb());
        cs.addConstraint(bc1);

        // gene.goAnnotation CONTAINS GOAnnotation
        QueryCollectionReference qr1 = new QueryCollectionReference(qcGene, "allGoAnnotation");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcGoAnnotation);
        cs.addConstraint(cc1);

        // gene is from organism
        QueryObjectReference qr2 = new QueryObjectReference(qcGene, "organism");
        ContainsConstraint cc2 = new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qcOrganism);
        cs.addConstraint(cc2);

        // goannotation contains go term
        QueryObjectReference qr3 = new QueryObjectReference(qcGoAnnotation, "property");
        ContainsConstraint cc3 = new ContainsConstraint(qr3, ConstraintOp.CONTAINS, qcGo);
        cs.addConstraint(cc3);

        // can't be a NOT relationship!
        SimpleConstraint sc1 = new SimpleConstraint(qfQualifier,
                                                    ConstraintOp.IS_NULL);
        cs.addConstraint(sc1);

        SimpleConstraint sc2 = new SimpleConstraint(qfGoTerm,
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue(key));
        cs.addConstraint(sc2);
        q.setConstraint(cs);
        return q;
    }
}
