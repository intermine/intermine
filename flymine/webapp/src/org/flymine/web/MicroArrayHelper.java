package org.flymine.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;

import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.MicroArrayExperiment;
import org.flymine.model.genomic.MicroArrayResult;
import org.flymine.model.genomic.MicroArrayAssay;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SimpleConstraint;

/**
 * Some static methods that query microarray data.
 *
 * @author Thomas Riley
 * @author Richard Smith
 */
public class MicroArrayHelper
{
    private static Map queries = new HashMap();

    /**
     * For a given gene and experiment retrieve results and assays and displayOrder attribute.
     * Order by displatOrder.
     * @param experiment MicroArrayExperiment.identifier
     * @param gene Gene.identifier
     * @param os ObjectStore to query
     * @return results: [MicroArrayResult, MicroArrayAssay, MicroArrayAssay.displayOrder]
     */
    public static Results queryMicroArrayResults(String experiment, String gene, ObjectStore os) {
        Query q = new Query();
        QueryClass mar = new QueryClass(MicroArrayResult.class);
        QueryClass mae = new QueryClass(MicroArrayExperiment.class);
        QueryClass g = new QueryClass(Gene.class);
        QueryClass maa = new QueryClass(MicroArrayAssay.class);
        q.addFrom(mar);
        q.addFrom(mae);
        q.addFrom(g);
        q.addFrom(maa);
        q.addToSelect(mar);
        q.addToSelect(maa);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        // MicroArrayResult.genes
        QueryCollectionReference marG = new QueryCollectionReference(mar, "genes");
        ContainsConstraint marGC = new ContainsConstraint(marG, ConstraintOp.CONTAINS, g);
        cs.addConstraint(marGC);

        // MicroArrayResult.genes.identifier
        QueryField qf = new QueryField(g, "identifier");
        SimpleConstraint gid = new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue(gene));
        cs.addConstraint(gid);

        // MicroArrayResult.experiement
        QueryObjectReference marE = new QueryObjectReference(mar, "experiment");
        ContainsConstraint marEC = new ContainsConstraint(marE, ConstraintOp.CONTAINS, mae);
        cs.addConstraint(marEC);

        // MicroArrayResult.experiment.identifier
        qf = new QueryField(mae, "identifier");
        SimpleConstraint eid = new SimpleConstraint(qf, ConstraintOp.EQUALS,
                                                    new QueryValue(experiment));
        cs.addConstraint(eid);

        // MicroArrayResult.assays.displayOrder
        QueryCollectionReference marMaa = new QueryCollectionReference(mar, "assays");
        ContainsConstraint marMaaC = new ContainsConstraint(marMaa, ConstraintOp.CONTAINS, maa);
        cs.addConstraint(marMaaC);
        QueryField marMaaDo = new QueryField(maa, "displayOrder");
        q.addToSelect(marMaaDo);
        q.addToOrderBy(marMaaDo);

        q.setConstraint(cs);

        Results results = os.execute(q);

        return results;
    }


    public static Results queryExperimentsInvolvingGene(String gene, ObjectStore os) {
        Query q = new Query();
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        q.setConstraint(cs);
        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        QueryClass qcMar = new QueryClass(MicroArrayResult.class);
        q.addFrom(qcMar);
        QueryClass qcExpt = new QueryClass(MicroArrayExperiment.class);
        q.addFrom(qcExpt);
        q.addToSelect(qcExpt);

        QueryField qfGeneIdentifier = new QueryField(qcGene, "identifier");
        SimpleConstraint sc = new SimpleConstraint(qfGeneIdentifier, ConstraintOp.EQUALS,
                                                   new QueryValue(gene));
        cs.addConstraint(sc);

        QueryCollectionReference maResults =
            new QueryCollectionReference(qcGene, "microArrayResults");
        ContainsConstraint cc1 = new ContainsConstraint(maResults, ConstraintOp.CONTAINS, qcMar);
        cs.addConstraint(cc1);

        QueryObjectReference experiment = new QueryObjectReference(qcMar, "experiment");
        ContainsConstraint cc2 = new ContainsConstraint(experiment, ConstraintOp.CONTAINS, qcExpt);
        QueryField qfExptIdentifier = new QueryField(qcExpt, "identifier");

        cs.addConstraint(cc2);

        Results results = os.execute(q);
        return results;
    }
}
