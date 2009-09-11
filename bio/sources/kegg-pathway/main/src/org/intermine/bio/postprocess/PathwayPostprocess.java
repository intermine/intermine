package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.intermine.bio.util.Constants;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Pathway;
import org.intermine.model.bio.Protein;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.postprocess.PostProcessor;

/**
 * class to populate pathways.genes and pathways.proteins collections.
 * 
 * KEGG populates pathway.genes. and BioPAX populates pathways.proteins.
 * @author Julie Sullivan
 */
public class PathwayPostprocess extends PostProcessor
{
//    private static final Logger LOG = Logger.getLogger(PathwayPostprocess.class);
    protected ObjectStore os;
    private Map<Gene, Set<Pathway>> genesToPathways = new HashMap();
    private Map<Protein, Set<Pathway>> proteinsToPathways = new HashMap();
    
    /**
     * @param osw writer on genomic ObjectStore
     */
    public PathwayPostprocess(ObjectStoreWriter osw) {
        super(osw);
        this.os = osw.getObjectStore();
    }


    /**
     * Copy all pathways from the Protein objects to the corresponding Gene(s)
     * @throws ObjectStoreException if anything goes wrong
     */
    public void postProcess()
    throws ObjectStoreException {

//        long startTime = System.currentTimeMillis();

        osw.beginTransaction();
        
        int count = 0;
        // get biopax (metacyc, reactome, etc) pathways

        Iterator iter = getProteinPathways();
        
        while (iter.hasNext()) {
            ResultsRow rr = (ResultsRow) iter.next();
            Gene gene = (Gene) rr.get(0);
            Pathway pathway = (Pathway) rr.get(1);
            if (genesToPathways.get(gene) == null) {
                genesToPathways.put(gene, new HashSet());
            }
            genesToPathways.get(gene).add(pathway);
            count++;
        }
        
        // get all kegg pathways
        iter = getGenePathways();
        
        while (iter.hasNext()) {
            ResultsRow rr = (ResultsRow) iter.next();
            Protein protein = (Protein) rr.get(0);
            Pathway pathway = (Pathway) rr.get(1);
            if (proteinsToPathways.get(protein) == null) {
                proteinsToPathways.put(protein, new HashSet());
            }
            proteinsToPathways.get(protein).add(pathway);
            count++;
        }
        
        // store all gene.pathways and protein.pathways 
        for (Map.Entry<Gene, Set<Pathway>> entry : genesToPathways.entrySet()) {
            Gene gene = entry.getKey();
            Set<Pathway> pathwayCollection = entry.getValue();
            gene.setPathways(pathwayCollection);
            osw.store(gene);
        }
        for (Map.Entry<Protein, Set<Pathway>> entry : proteinsToPathways.entrySet()) {
            Protein protein = entry.getKey();
            Set<Pathway> pathwayCollection = entry.getValue();
            protein.setPathways(pathwayCollection);
            osw.store(protein);
        }
        osw.commitTransaction();
    }


    /**
     * Query Protein->Pathway
     */
    private Iterator getProteinPathways()
    throws ObjectStoreException {
        Query q = new Query();

        q.setDistinct(false);

        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);
        q.addToOrderBy(qcGene);

        QueryClass qcProtein = new QueryClass(Protein.class);
        q.addFrom(qcProtein);

        QueryClass qcPathway = new QueryClass(Pathway.class);
        q.addFrom(qcPathway);
        q.addToSelect(qcPathway);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        // protein.genes
        QueryCollectionReference qc1 = new QueryCollectionReference(qcProtein, "genes");
        cs.addConstraint(new ContainsConstraint(qc1, ConstraintOp.CONTAINS, qcGene));

        // protein.pathways
        QueryCollectionReference qc2 = new QueryCollectionReference(qcProtein, "pathways");
        cs.addConstraint(new ContainsConstraint(qc2, ConstraintOp.CONTAINS, qcPathway));

        q.setConstraint(cs);
        ((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);
        Results res = os.execute(q, 5000, true, true, true);
        return res.iterator();
    }
    
    /**
     * get Gene->Pathway
     */
    private Iterator getGenePathways()
    throws ObjectStoreException {
        Query q = new Query();
        
        q.setDistinct(false);

        QueryClass qcProtein = new QueryClass(Protein.class);
        q.addFrom(qcProtein);
        q.addToSelect(qcProtein);
        q.addToOrderBy(qcProtein);
        
        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
       
        QueryClass qcPathway = new QueryClass(Pathway.class);
        q.addFrom(qcPathway);
        q.addToSelect(qcPathway);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        // gene.proteins
        QueryCollectionReference qc1 = new QueryCollectionReference(qcGene, "proteins");
        cs.addConstraint(new ContainsConstraint(qc1, ConstraintOp.CONTAINS, qcProtein));

        // gene.pathways
        QueryCollectionReference qc2 = new QueryCollectionReference(qcGene, "pathways");
        cs.addConstraint(new ContainsConstraint(qc2, ConstraintOp.CONTAINS, qcPathway));

        q.setConstraint(cs);

        ((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);
        Results res = os.execute(q, 5000, true, true, true);
        return res.iterator();
    }
    
}
