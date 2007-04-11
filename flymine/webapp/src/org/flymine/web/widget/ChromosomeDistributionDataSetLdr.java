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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.DataSetLdr;

import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.Gene;

import org.jfree.data.category.DefaultCategoryDataset;
/**
 *
 * @author Julie Sullivan
 */
public class ChromosomeDistributionDataSetLdr implements DataSetLdr
{
    private DefaultCategoryDataset dataSet;
    private Results results;
    private Object[] geneCategoryArray;

    /**
     * Creates a ChromosomeDistributionDataSetLdr used to retrieve, organise
     * and structure the data to create a graph
     * @param bag the bag
     * @param os the ObjectStore
     */
    
    
    //SELECT DISTINCT a1_, a2_ FROM org.flymine.model.genomic.Gene AS a1_, 
    //org.flymine.model.genomic.Chromosome AS a2_ WHERE a1_.chromosome CONTAINS a2_ 


    
    public ChromosomeDistributionDataSetLdr(InterMineBag bag, ObjectStore os) {
        super();
        dataSet = new DefaultCategoryDataset();
        Collection geneList = bag.getListOfIds();

        Query q = new Query();
        QueryClass chromosomeQC = new QueryClass(Chromosome.class);
        QueryClass geneQC = new QueryClass(Gene.class); 
        
        QueryField chromoQF = new QueryField(chromosomeQC, "identifier");
        QueryField geneIdentifierQF = new QueryField(geneQC, "identifier");
        
        q.addFrom(chromosomeQC);
        q.addFrom(geneQC);
        
        q.addToSelect(chromoQF);
        q.addToSelect(geneIdentifierQF);
        
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        
        // gene has to be in bag
        QueryField qf = new QueryField(geneQC, "id");
        BagConstraint bagC = new BagConstraint(qf, ConstraintOp.IN, geneList); 
        cs.addConstraint(bagC);

        // gene.chromosome CONTAINS chromosome.identifier
        QueryObjectReference r = new QueryObjectReference(geneQC, "chromosome");
        ContainsConstraint cc = new ContainsConstraint(r, ConstraintOp.CONTAINS, chromosomeQC);
        cs.addConstraint(cc);
        
        q.setConstraint(cs);
        results = new Results(q, os, os.getSequence());
        
        Iterator iter = results.iterator();
        HashMap resultsTable = new HashMap();               // chromosome, count of genes
        HashMap geneMap = new HashMap();                    // chromosome --> list of genes
        while (iter.hasNext()) {
            ResultsRow resRow = (ResultsRow) iter.next();
        
            String chromosome = (String) resRow.get(0);     // chromosome
            String geneIdentifier = (String) resRow.get(1); // gene

            if (resultsTable.get(chromosome) != null) {  
                ((int[]) resultsTable.get(chromosome))[0]++;
                ((ArrayList) geneMap.get(chromosome)).add(geneIdentifier);
            } else {
                ArrayList genesArray = new ArrayList();
                genesArray.add(geneIdentifier);
                geneMap.put(chromosome, genesArray);       
                int[] count = new int[2];
                count[0] = 1;
                resultsTable.put(chromosome, count);
            }
        }

        // update results with expected results
        addExpected(os, resultsTable, bag.getSize());

        // Build a map from chromosome to gene list
        geneCategoryArray = new Object[resultsTable.size()];
        int i = 0;
        for (Iterator iterator = resultsTable.keySet().iterator(); iterator.hasNext();) {
            String chromosome = (String) iterator.next();
            dataSet.addValue(((int[]) resultsTable.get(chromosome))[0], "Actual", chromosome);
            dataSet.addValue(((int[]) resultsTable.get(chromosome))[1], "Expected", chromosome);
            Object[] geneSeriesArray = new Object[2];
            geneSeriesArray[0] = geneMap.get(chromosome);
            geneSeriesArray[1] = geneMap.get(chromosome);
            geneCategoryArray[i] = geneSeriesArray;

            i++;
        }
    }

    /**
     * @see org.intermine.web.widget.DataSetLdr#getDataSet()
     */
    public DefaultCategoryDataset getDataSet() {
        return dataSet;
    }

    /**
     * @see org.intermine.web.widget.DataSetLdr#getGeneCategoryArray()
     */
    public Object[] getGeneCategoryArray() {
        return geneCategoryArray;
    }
    
    /**
     * @see org.intermine.web.widget.DataSetLdr#getResultsSize()
     */
    public int getResultsSize() {
        return results.size();
    }
    
    /* select count(*) from genes where chromosomeLocation != null; */
    private int getTotal(ObjectStore os) {

        Query q = new Query();
        QueryClass chromosomeQC = new QueryClass(Chromosome.class);
        QueryClass geneQC = new QueryClass(Gene.class); 
        

        QueryFunction geneQF = new QueryFunction();
        
        q.addFrom(chromosomeQC);
        q.addFrom(geneQC);

        q.addToSelect(geneQF);
        
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        
        // gene.chromosome CONTAINS chromosome.identifier
        QueryObjectReference r = new QueryObjectReference(geneQC, "chromosome");
        ContainsConstraint cc = new ContainsConstraint(r, ConstraintOp.CONTAINS, chromosomeQC);
        cs.addConstraint(cc);
        
        q.setConstraint(cs);
        results = new Results(q, os, os.getSequence());
        
        Iterator it = results.iterator();
        ResultsRow rr =  (ResultsRow) it.next();

        Long n = (java.lang.Long) rr.get(0);
        return n.intValue();

    }

    private void addExpected(ObjectStore os, HashMap resultsTable, int bagSize) {
        
        int total = getTotal(os);
        
        // select count(*) from genes where chromosomeLocation != null;
        Query q = new Query();
        QueryClass chromosomeQC = new QueryClass(Chromosome.class);
        QueryClass geneQC = new QueryClass(Gene.class); 
        
        QueryField chromoQF = new QueryField(chromosomeQC, "identifier");
        QueryFunction geneQF = new QueryFunction();
        
        q.addFrom(chromosomeQC);
        q.addFrom(geneQC);
        
        q.addToSelect(chromoQF);
        q.addToSelect(geneQF);
        
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        
        // gene.chromosome CONTAINS chromosome.identifier
        QueryObjectReference r = new QueryObjectReference(geneQC, "chromosome");
        ContainsConstraint cc = new ContainsConstraint(r, ConstraintOp.CONTAINS, chromosomeQC);
        cs.addConstraint(cc);
        
        q.setConstraint(cs);
        
        q.addToGroupBy(chromoQF);
        results = new Results(q, os, os.getSequence());
        
        Iterator iter = results.iterator();

        while (iter.hasNext()) {
            ResultsRow resRow = (ResultsRow) iter.next();
        
            String chromosome = (String) resRow.get(0);         // chromosome
            Long geneCount = (java.lang.Long) resRow.get(1);    // genecount
            
            double expectedValue = 0;
            double proportion = 0.0000000000; 
            if (total > 0) { 
                proportion = geneCount.intValue() / total;
            } 
            expectedValue = bagSize * proportion;

            // if the chromosome isn't there, we aren't interested in it
            if (resultsTable.get(chromosome) != null) {  
                ((int[]) resultsTable.get(chromosome))[1] = (int) expectedValue;             
            }
        }
    }    
}
