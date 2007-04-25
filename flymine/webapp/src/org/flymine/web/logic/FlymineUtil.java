package org.flymine.web.logic;

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
import java.util.Collection;
import java.util.Iterator;

import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;

import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.bag.InterMineBag;

import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Organism;



/**
 * Utility methods for the flymine package.
 * @author Julie Sullivan
 */
public abstract class FlymineUtil
{
    /**
     * For a bag of genes, returns a list of organisms
     * @param os ObjectStore
     * @param bag InterMineBag
     * @return collection of organism names
     */
    public static Collection getOrganisms(ObjectStore os, InterMineBag bag) {

        Query q = new Query();

        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);

        QueryField qfOrganismName = new QueryField(qcOrganism, "name");     
        QueryField qfGeneId = new QueryField(qcGene, "id");

        q.addFrom(qcGene);
        q.addFrom(qcOrganism);

        q.addToSelect(qfOrganismName);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        if (bag != null) {
            BagConstraint bc = new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb());
            cs.addConstraint(bc);
        }

        QueryObjectReference qr = new QueryObjectReference(qcGene, "organism");
        ContainsConstraint cc = new ContainsConstraint(qr, ConstraintOp.CONTAINS, qcOrganism);
        cs.addConstraint(cc);

        q.setConstraint(cs);
        
        q.addToOrderBy(qfOrganismName);
        
        Results r = new Results(q, os, os.getSequence());
        Iterator it = r.iterator();
        Collection organismNames = new ArrayList();
        
        while (it.hasNext()) {
            ResultsRow rr =  (ResultsRow) it.next();
            organismNames.add(rr.get(0));
        }
        return organismNames;
    }   
    
    /**
     * Return a list of chromosomes for specified organism
     * @param os ObjectStore
     * @param organism Organism name
     * @return collection of chromosome names
     */
    public static Collection getChromosomes(ObjectStore os, String organism) {

//        SELECT DISTINCT o
//        FROM org.flymine.model.genomic.Chromosome AS c, 
//        org.flymine.model.genomic.Organism AS o 
//        WHERE c.organism CONTAINS o 
            
        /* TODO put this in a config file */
        if (organism.equals("Drosophila melanogaster")) {
            
            ArrayList<String> chromosomes = new ArrayList<String>();
            chromosomes.add("2L");
            chromosomes.add("2R");
            chromosomes.add("3L");
            chromosomes.add("3R");
            chromosomes.add("4");
            chromosomes.add("U");
            chromosomes.add("X");
            
            return chromosomes;
        }
        
        Query q = new Query();

        QueryClass qcChromosome = new QueryClass(Chromosome.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);
        QueryField qfChromosome = new QueryField(qcChromosome, "identifier");
        QueryField organismNameQF = new QueryField(qcOrganism, "name");
        q.addFrom(qcChromosome);
        q.addFrom(qcOrganism);

        q.addToSelect(qfChromosome);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryObjectReference qr = new QueryObjectReference(qcChromosome, "organism");
        ContainsConstraint cc = new ContainsConstraint(qr, ConstraintOp.CONTAINS, qcOrganism);
        cs.addConstraint(cc);

        SimpleConstraint sc = new SimpleConstraint(organismNameQF,
                                                   ConstraintOp.EQUALS,
                                                   new QueryValue(organism));
        cs.addConstraint(sc); 
        
        q.setConstraint(cs);
        
        q.addToOrderBy(qfChromosome);
        
        Results r = new Results(q, os, os.getSequence());
        Iterator it = r.iterator();
        Collection<String> chromosomes = new ArrayList<String>();
        
        while (it.hasNext()) {
            ResultsRow rr =  (ResultsRow) it.next();
            chromosomes.add((String) rr.get(0));
        }
        return chromosomes;
    }   
    
}
