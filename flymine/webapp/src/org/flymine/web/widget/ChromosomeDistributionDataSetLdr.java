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
import java.util.LinkedHashMap;
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

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.DataSetLdr;
import org.intermine.web.logic.widget.GraphDataSet;

import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Organism;
import org.flymine.web.logic.FlymineUtil;

import org.jfree.data.category.DefaultCategoryDataset;
/**
 *
 * @author Julie Sullivan
 */
public class ChromosomeDistributionDataSetLdr implements DataSetLdr
{
    //private DefaultCategoryDataset dataSet;
    private Results results;
    private Object[] geneCategoryArray;
    private LinkedHashMap dataSets = new LinkedHashMap();
    private ObjectStore os;
    
    /**
     * Creates a ChromosomeDistributionDataSetLdr used to retrieve, organise
     * and structure the data to create a graph
     * @param bag the bag
     * @param os the ObjectStore
     */
    
    public ChromosomeDistributionDataSetLdr(InterMineBag bag, ObjectStore os) {
        super();
        this.os = os;
        
        Collection organisms = FlymineUtil.getOrganisms(os, bag);
    
        organisms.remove("Drosophila pseudoobscura");
        organisms.remove("Apis mellifera");
        
        for (Iterator it = organisms.iterator(); it.hasNext();) {
            
            String organismName = (String) it.next();
            DefaultCategoryDataset dataSet = new DefaultCategoryDataset();

                        
            // chromosome, count of genes
            LinkedHashMap<String, int[]> resultsTable = new LinkedHashMap<String, int[]> (); 
            // chromosome --> list of genes
            HashMap<String, ArrayList> geneMap = new HashMap<String, ArrayList>();
            
            // get all chromosomes for this organism 
            ArrayList<String> chromosomes 
                = (ArrayList<String>) FlymineUtil.getChromosomes(os, organismName);
            Iterator iter = chromosomes.iterator();
            
            while (iter.hasNext()) {
                int[] count = new int[2];
                count[0] = 0;
                String chromosome = (String) iter.next();
                resultsTable.put(chromosome, count);
                ArrayList<String> genesArray = new ArrayList<String>();
                geneMap.put(chromosome, genesArray);
            }
            
            // run query 
            Query q = createQuery(organismName, "actual", bag);            
            results = os.execute(q);
            boolean hasResults = false;
            if (results.size() > 0) {
                hasResults = true;
            }
            
            // put results in maps
            iter = results.iterator();
            while (iter.hasNext()) {
                ResultsRow resRow = (ResultsRow) iter.next();
                String chromosome = (String) resRow.get(0);     // chromosome
                String geneIdentifier = (String) resRow.get(1); // gene
                if (resultsTable.get(chromosome) != null) { 
                    (resultsTable.get(chromosome))[0]++;    
                    (geneMap.get(chromosome)).add(geneIdentifier);
                }
            }

            // update results with expected results
            try {
                // TODO handle this exception more sensibly, changes need to be made to
                // BagDetailsController
                addExpected(os, resultsTable, bag.getSize(), organismName);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("Error getting size of bag. ", e);
            }
            
            // Build a map from chromosome to gene list
            geneCategoryArray = new Object[resultsTable.size()];
            int i = 0;
            for (Iterator iterator = resultsTable.keySet().iterator(); iterator.hasNext();) {
                String chromosome = (String) iterator.next();
                dataSet.addValue((resultsTable.get(chromosome))[0], "Actual", chromosome);
                dataSet.addValue((resultsTable.get(chromosome))[1], "Expected", chromosome);
                Object[] geneSeriesArray = new Object[2];
                /* why are there two? */
                geneSeriesArray[0] = geneMap.get(chromosome);   // actual
                geneSeriesArray[1] = geneMap.get(chromosome);   // expected
                geneCategoryArray[i] = geneSeriesArray;

                i++;
            }

            GraphDataSet graphDataSet = new GraphDataSet(dataSet, geneCategoryArray);
            if (hasResults) {
                dataSets.put(organismName, graphDataSet);
            }
        }       
    }

    /**
     * @see org.intermine.web.widget.DataSetLdr#getDataSet()
     */
    public Map getDataSets() {
        return dataSets;
    }

    /* select count(*) from genes where chromosomeLocation != null; */
    private long getTotal(ObjectStore os, String organismName) {

        Query q = createQuery(organismName, "total", null);        
        results = os.execute(q);          
        Iterator iter = results.iterator();
        ResultsRow rr = (ResultsRow) iter.next();
        return (Long) rr.get(0);
    }

    private void addExpected(ObjectStore os, HashMap resultsTable, 
                             int bagSize, String organismName) {
        // totals
        long total = getTotal(os, organismName);
        
        // get expected results
        Query q = createQuery(organismName, "expected", null);
        results = os.execute(q);
        Iterator iter = results.iterator();
        int i = 0;
        
        // loop through, calc, and put in map
        while (iter.hasNext()) {
            ResultsRow resRow = (ResultsRow) iter.next();
        
            String chromosome = (String) resRow.get(0);         // chromosome
            Long geneCount = (java.lang.Long) resRow.get(1);    // genecount
            
            double expectedValue = 0;
            double proportion = 0.0000000000; 
            double count = geneCount.intValue();            
            if (total > 0) { 
                proportion = count / total;
            } 
            expectedValue = bagSize * proportion;
                      
            if (resultsTable.get(chromosome) != null) {  
                ((int[]) resultsTable.get(chromosome))[1] = (int) expectedValue;             
            }
            i++;
        }
    }    
    
    private Query createQuery(String organismName, String resultsType, InterMineBag bag) {
        
        Query q = new Query();
        
        QueryClass chromosomeQC = new QueryClass(Chromosome.class);
        QueryClass geneQC = new QueryClass(Gene.class); 
        QueryClass organismQC = new QueryClass(Organism.class);
        
        QueryField chromoQF = new QueryField(chromosomeQC, "identifier");
        QueryFunction countQF = new QueryFunction();
        QueryField organismNameQF = new QueryField(organismQC, "name");
        QueryField geneIdentifierQF = new QueryField(geneQC, "identifier");

        if (resultsType.equals("actual")) {
            q.addToSelect(chromoQF);
            q.addToSelect(geneIdentifierQF);            
        } else if (resultsType.equals("expected")) {
            q.addToSelect(chromoQF);
            q.addToSelect(countQF);
        } else if (resultsType.equals("total")) {
            q.addToSelect(countQF);
        }

        q.addFrom(chromosomeQC);
        q.addFrom(geneQC);
        q.addFrom(organismQC);
        
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryObjectReference r = new QueryObjectReference(geneQC, "chromosome");
        ContainsConstraint cc = new ContainsConstraint(r, ConstraintOp.CONTAINS, chromosomeQC);
        cs.addConstraint(cc);
        
        QueryObjectReference r2 = new QueryObjectReference(geneQC, "organism");
        ContainsConstraint cc2 = new ContainsConstraint(r2, ConstraintOp.CONTAINS, organismQC);
        cs.addConstraint(cc2);
        
        SimpleConstraint sc = new SimpleConstraint(organismNameQF,
                                                   ConstraintOp.EQUALS,
                                                   new QueryValue(organismName));
        cs.addConstraint(sc);       
       
        if (resultsType.equals("actual")) {
            QueryField qf = new QueryField(geneQC, "id");
            BagConstraint bagC = new BagConstraint(qf, ConstraintOp.IN, bag.getOsb()); 
            cs.addConstraint(bagC);
        }
        
        q.setConstraint(cs);       
        
        if (resultsType.equals("expected")) {
            q.addToGroupBy(chromoQF);
        }
        
        if (!resultsType.equals("total")) {
          q.addToOrderBy(chromoQF);
        }
        return q;
    }    
}
