package org.flymine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.MicroArrayExperiment;
import org.flymine.model.genomic.MicroArrayResult;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
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
import org.intermine.web.MainHelper;
import org.intermine.web.PathNode;
import org.intermine.web.PathQuery;
import org.intermine.web.PathQueryBinding;

/**
 * Some static methods that query microarray data.
 */
public class MicroArrayHelper
{
    private static Map queries = new HashMap();
    
    static
    {
        Map pathQueries = PathQueryBinding.unmarshal(new InputStreamReader(
                MicroArrayHelper.class.getResourceAsStream("microarray-queries.xml")));
        Iterator iter = pathQueries.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            PathQuery pq = (PathQuery) entry.getValue();
            //Query q = MainHelper.makeQuery(pq, Collections.EMPTY_MAP);
            queries.put((String) entry.getKey(), pq);
        }
    }
    
    public static Results queryMicroArrayResults(String experiment, String gene, ObjectStore os) {
        
        PathQuery pq = (PathQuery) queries.get("resultsForGeneAndExperiment");
        PathNode pn = pq.getNode("Gene.identifier");
        pn.setConstraintValue(pn.getConstraint(0), gene);
        pn = pq.getNode("Gene.microArrayResults.experiment.identifier");
        pn.setConstraintValue(pn.getConstraint(0), experiment);

        Query q = MainHelper.makeQuery(pq, Collections.EMPTY_MAP);
        
        Results results = new Results(q, os, os.getSequence());
        
        return results;        
/*
        Query q = MainHelper.makeQuery(pq, Collections.EMPTY_MAP);
        
        Results results = new Results(q, os, os.getSequence());
        
        Query q = new Query();
        QueryClass mar = new QueryClass(MicroArrayResult.class);
        QueryClass mae = new QueryClass(MicroArrayExperiment.class);
        QueryClass g = new QueryClass(Gene.class);
        q.addFrom(mar);
        q.addFrom(mae);
        q.addFrom(g);
        q.addToSelect(mar);
        
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        
        // MicroArrayResult.genes
        QueryCollectionReference mar_g = new QueryCollectionReference(mar, "genes");
        ContainsConstraint mar_g_c = new ContainsConstraint(mar_g, ConstraintOp.CONTAINS, g);
        cs.addConstraint(mar_g_c);
        
        // MicroArrayResult.genes.identifier
        QueryField qf = new QueryField(g, "identifier");
        SimpleConstraint gid = new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue(gene));
        cs.addConstraint(gid);
        
        // MicroArrayResult.experiement
        QueryObjectReference mar_e = new QueryObjectReference(mar, "experiment");
        ContainsConstraint mar_e_c = new ContainsConstraint(mar_e, ConstraintOp.CONTAINS, mae);
        cs.addConstraint(mar_e_c);
        
        // MicroArrayResult.genes.identifier
        qf = new QueryField(mae, "identifier");
        SimpleConstraint eid = new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue(experiment));
        cs.addConstraint(eid);
        
        q.setConstraint(cs);
        
        Results results = new Results(q, os, os.getSequence());
        
        return results;*/
    }
    
    public static Results queryExperimentsInvolvingGene(String gene, ObjectStore os) {
        PathQuery pq = (PathQuery) queries.get("experimentsForGene");
        PathNode pn = pq.getNode("MicroArrayExperiment.results.genes.identifier");
        pn.setConstraintValue(pn.getConstraint(0), gene);

        Query q = MainHelper.makeQuery(pq, Collections.EMPTY_MAP);
        
        Results results = new Results(q, os, os.getSequence());
        
        return results;
    }
}
