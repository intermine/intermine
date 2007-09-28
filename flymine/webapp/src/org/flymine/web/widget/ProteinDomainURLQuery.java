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

import java.util.ArrayList;

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

import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.ProteinFeature;
import org.flymine.web.logic.BioUtil;


/**
 * Builds a query to get all the genes (in bag) associated with specified go term.
 * @author Julie Sullivan
 */
public class ProteinDomainURLQuery implements EnrichmentWidgetURLQuery
{

    InterMineBag bag;
    String key;
    ObjectStore os;
    /**
     * @param key
     * @param bag
     * 
     */
    public ProteinDomainURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * @return Query a query to generate the results needed
     */
    public Query getQuery() {

        Query q = new Query();
        q.setDistinct(true);
        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcProtein = new QueryClass(Protein.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);
        QueryClass qcProteinFeature = new QueryClass(ProteinFeature.class);

        QueryField qfGeneId = new QueryField(qcGene, "id");
        QueryField qfOrganismName = new QueryField(qcOrganism, "name");
        QueryField qfInterpro = new QueryField(qcProteinFeature, "identifier");

        q.addFrom(qcGene);
        q.addFrom(qcProtein);
        q.addFrom(qcOrganism);
        q.addFrom(qcProteinFeature);

        q.addToSelect(qcGene);

        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);

        // genes must be in bag
        BagConstraint bc1 = new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb());
        cs1.addConstraint(bc1);

        // get organisms
        ArrayList organisms = (ArrayList) BioUtil.getOrganisms(os, bag);

        // limit to organisms in the bag
        BagConstraint bc2 = new BagConstraint(qfOrganismName, ConstraintOp.IN, organisms);
        cs1.addConstraint(bc2);

        // gene is from organism
        QueryObjectReference qr1 = new QueryObjectReference(qcGene, "organism");
        ContainsConstraint cc1 
        = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcOrganism);
        cs1.addConstraint(cc1);

        // gene.Proteins CONTAINS protein
        QueryCollectionReference qr2 = new QueryCollectionReference(qcGene, "proteins");
        ContainsConstraint cc2 =
            new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qcProtein);
        cs1.addConstraint(cc2);

        // protein.ProteinFeatures CONTAINS proteinFeature
        QueryCollectionReference qr3 
        = new QueryCollectionReference(qcProtein, "proteinFeatures");
        ContainsConstraint cc3 =
            new ContainsConstraint(qr3, ConstraintOp.CONTAINS, qcProteinFeature);
        cs1.addConstraint(cc3);

        SimpleConstraint sc = 
            new SimpleConstraint(qfInterpro, ConstraintOp.MATCHES, new QueryValue(key));
        cs1.addConstraint(sc);

        q.setConstraint(cs1);

        return q;
    }
}
