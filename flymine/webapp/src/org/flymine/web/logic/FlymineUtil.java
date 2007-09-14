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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.web.logic.SortableMap;
import org.intermine.web.logic.bag.InterMineBag;

import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Organism;
import org.flymine.web.Bonferroni;
import org.flymine.web.Hypergeometric;



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
     * @exception No bag
     * @exception ClassNotFoundException
     */
    public static Collection getOrganisms(ObjectStore os, InterMineBag bag) 
        throws ClassNotFoundException {

        Query q = new Query();
        Model model = os.getModel();
        QueryClass qcGene 
            = new QueryClass(Class.forName(model.getPackageName() + "." + bag.getType()));
        QueryClass qcOrganism = new QueryClass(Organism.class);

        QueryField qfOrganismName = new QueryField(qcOrganism, "name");     
        QueryField qfGeneId = new QueryField(qcGene, "id");

        q.addFrom(qcGene);
        q.addFrom(qcOrganism);

        q.addToSelect(qfOrganismName);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        BagConstraint bc = new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb());
        cs.addConstraint(bc);

        QueryObjectReference qr = new QueryObjectReference(qcGene, "organism");
        ContainsConstraint cc = new ContainsConstraint(qr, ConstraintOp.CONTAINS, qcOrganism);
        cs.addConstraint(cc);

        q.setConstraint(cs);
        
        q.addToOrderBy(qfOrganismName);
        
        Results r = os.execute(q);
        Iterator it = r.iterator();
        Collection<String> organismNames = new ArrayList<String>();
        
        while (it.hasNext()) {
            ResultsRow rr =  (ResultsRow) it.next();
            organismNames.add((String) rr.get(0));
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
        // TODO this may well go away once chromosomes sorted out in #1186
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
        
        Results r = os.execute(q);
        Iterator it = r.iterator();
        Collection<String> chromosomes = new ArrayList<String>();
        
        while (it.hasNext()) {
            ResultsRow rr =  (ResultsRow) it.next();
            chromosomes.add((String) rr.get(0));
        }
        return chromosomes;
    }   
    
/**
 * Takes two queries.  Runs both and compares the results.
 * @param os
 * @param queryPopulation The query to get the entire population, ie all genes in the database
 * @param querySample The query to get the sample, ie all genes in the bag
 * @param bag the bag we are analysing
 * @param organisms organisms of interest
 * @param maxValue maximum value to return
 * @param significanceValue significance value
 * @return array of three results maps
 * @throws Exception
 */
    public static ArrayList statsCalc(ObjectStoreInterMineImpl os, 
                         Query queryPopulation, 
                         Query querySample, 
                         InterMineBag bag,
                         Collection organisms,
                         Double maxValue)      
    throws Exception {
      
        
        ArrayList<Map> maps = new ArrayList<Map>();
        int numberOfGenesInBag;
        try {
            numberOfGenesInBag = bag.size();
        } catch (Exception e) {
            return null;
        }
        
        // run bag query
        Results r = os.execute(querySample);
        r.setBatchSize(10000);
        Iterator iter = r.iterator();
        HashMap<String, Long> countMap = new HashMap<String, Long>();
        HashMap<String, String> idMap = new HashMap<String, String>();

        while (iter.hasNext()) {

            // extract results
            ResultsRow rr =  (ResultsRow) iter.next();

            // id of item
            String id = (String) rr.get(0);

            // count of item
            Long count = (java.lang.Long) rr.get(1);  

            // id & count
            countMap.put(id, count);

            // id & label
            idMap.put(id, (String) rr.get(2));

        }
        
        // run population query
        Results rAll = os.execute(queryPopulation);
        rAll.setBatchSize(10000);

        Iterator itAll = rAll.iterator();

        // total number of genes for all organisms of interest 
        int total = getGeneTotal(os, organisms);
        
        Hypergeometric h = new Hypergeometric(total);
        HashMap<String, Double> resultsMap = new HashMap<String, Double>();

        while (itAll.hasNext()) {

            ResultsRow rrAll =  (ResultsRow) itAll.next();

            String id = (String) rrAll.get(0);

            if (countMap.containsKey(id)) {

                Long countBag = countMap.get(id);
                Long countAll = (java.lang.Long) rrAll.get(1);

                double p =
                    h.calculateP(numberOfGenesInBag, countBag.intValue(), 
                                 countAll.intValue(), total);
                resultsMap.put(id, new Double(p));
            }
        }
        
        Bonferroni b = new Bonferroni(resultsMap);
        b.calculate(maxValue);
        HashMap adjustedResultsMap = b.getAdjustedMap();

        SortableMap sortedMap = new SortableMap(adjustedResultsMap);
        sortedMap.sortValues();
        
        maps.add(0, sortedMap);  
        maps.add(1, countMap); 
        maps.add(2, idMap);   
        return maps;
    }
    
    /* gets total number of genes */
    private static int getGeneTotal(ObjectStore os, Collection organisms) {

           Query q = new Query();
           q.setDistinct(false);
           QueryClass qcGene = new QueryClass(Gene.class);
           QueryClass qcOrganism = new QueryClass(Organism.class);

           QueryField qfOrganism = new QueryField(qcOrganism, "name");
           QueryFunction geneCount = new QueryFunction();

           q.addFrom(qcGene);

           q.addFrom(qcOrganism);

           q.addToSelect(geneCount);

           ConstraintSet cs;
           cs = new ConstraintSet(ConstraintOp.AND);

           /* organism is in bag */
           BagConstraint bc2 = new BagConstraint(qfOrganism, ConstraintOp.IN, organisms);
           cs.addConstraint(bc2);

           /* gene is from organism */
           QueryObjectReference qr2 = new QueryObjectReference(qcGene, "organism");
           ContainsConstraint cc2 = new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qcOrganism);
           cs.addConstraint(cc2);

           q.setConstraint(cs);

           Results r = os.execute(q);
           Iterator it = r.iterator();
           ResultsRow rr =  (ResultsRow) it.next();
           Long l = (java.lang.Long) rr.get(0);
           int n = l.intValue();
           return n;
       }
    
}
