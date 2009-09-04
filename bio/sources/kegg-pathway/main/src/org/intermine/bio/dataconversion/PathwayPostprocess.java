package org.intermine.bio.dataconversion;

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

import org.apache.log4j.Logger;
import org.intermine.bio.postprocess.PostProcessUtil;
import org.intermine.bio.util.Constants;
import org.intermine.model.bio.GOAnnotation;
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
import org.intermine.objectstore.query.QueryObjectReference;
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
    private static final Logger LOG = Logger.getLogger(PathwayPostprocess.class);
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

        long startTime = System.currentTimeMillis();

        osw.beginTransaction();
        
        // get all kegg pathways
        Iterator iter = getGenePathways();

        int count = 0;
        
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
        
        
        LOG.info("Created " + count + " new pathway objects for proteins"
                 + " - took " + (System.currentTimeMillis() - startTime) + " ms.");
        osw.commitTransaction();
        
        osw.beginTransaction();
        
        while (iter.hasNext()) {
            ResultsRow rr = (ResultsRow) iter.next();
            Gene gene = (Gene) rr.get(0);
            Pathway pathway = (Pathway) rr.get(1);


            if (genesToPathways.get(gene) == null) {
                genesToPathways.put(gene, new HashSet());
            }
            
            
            count++;
        }



        LOG.info("Created " + count + " new pathway objects for Genes"
                + " - took " + (System.currentTimeMillis() - startTime) + " ms.");
        osw.commitTransaction();
        
        // get all biopax pathways
        Iterator resIter = getProteinPathways();
        
    }


    /**
     * Query Gene->Protein->Annotation->GOTerm and return an iterator over the Gene,
     *  Protein and GOTerm.
     *
     * @param restrictToPrimaryGoTermsOnly Only get primary Annotation items linking the gene
     *  and the go term.
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

        QueryClass qcAnnotation = new QueryClass(GOAnnotation.class);
        q.addFrom(qcAnnotation);
        q.addToSelect(qcAnnotation);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference geneProtRef = new QueryCollectionReference(qcProtein, "genes");
        ContainsConstraint geneProtConstraint =
            new ContainsConstraint(geneProtRef, ConstraintOp.CONTAINS, qcGene);
        cs.addConstraint(geneProtConstraint);

        QueryObjectReference annSubjectRef =
            new QueryObjectReference(qcAnnotation, "subject");
        ContainsConstraint annSubjectConstraint =
            new ContainsConstraint(annSubjectRef, ConstraintOp.CONTAINS, qcProtein);
        cs.addConstraint(annSubjectConstraint);


        q.setConstraint(cs);

        ObjectStore os = osw.getObjectStore();

        ((ObjectStoreInterMineImpl) os).precompute(q, Constants
                                                   .PRECOMPUTE_CATEGORY);
        Results res = os.execute(q, 5000, true, true, true);
        return res.iterator();
    }
    
    /**
     * return all the genes and pathways
     */
    private Iterator getGenePathways()
    throws ObjectStoreException {
        Query q = new Query();

        q.setDistinct(false);

        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);
        q.addToOrderBy(qcGene);

        QueryClass qcPathway = new QueryClass(Pathway.class);
        q.addFrom(qcPathway);
        q.addToSelect(qcPathway);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference qc = new QueryCollectionReference(qcGene, "pathways");
        cs.addConstraint(new ContainsConstraint(qc, ConstraintOp.CONTAINS, qcGene));

//        QueryObjectReference annSubjectRef =
//            new QueryObjectReference(qcAnnotation, "subject");
//        ContainsConstraint annSubjectConstraint =
//            new ContainsConstraint(annSubjectRef, ConstraintOp.CONTAINS, qcProtein);
//        cs.addConstraint(annSubjectConstraint);


        q.setConstraint(cs);

        ObjectStore os = osw.getObjectStore();

        ((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);
        Results res = os.execute(q, 5000, true, true, true);
        return res.iterator();
    }
    
}
