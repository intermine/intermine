package org.intermine.bio.web.widget;

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

import org.intermine.bio.web.logic.BioUtil;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.DataSetLdr;
import org.intermine.web.logic.widget.GraphDataSet;

import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.Organism;

import org.jfree.data.category.DefaultCategoryDataset;
/**
 *
 * @author Julie Sullivan
 */
public class ChromosomeDistributionDataSetLdr implements DataSetLdr
{
   
    private Object[] geneCategoryArray;
    private LinkedHashMap<String, GraphDataSet> dataSets 
                                                        = new LinkedHashMap<String, GraphDataSet>();
    private ObjectStore os;
    private Model model;
    private String bagType;
    
    /**
     * Creates a ChromosomeDistributionDataSetLdr used to retrieve, organise
     * and structure the data to create a graph
     * @param bag the bag
     * @param os the ObjectStore
     * @param c the class being compared
     * @throws Exception
     */
    
    public ChromosomeDistributionDataSetLdr(InterMineBag bag, ObjectStore os) 
        throws Exception {
        super();
        this.os = os;
        model = os.getModel();
        bagType = bag.getType();
        Collection organisms = null;
        try {
            organisms = BioUtil.getOrganisms(os, bag);
        } catch (Exception e) {
            throw new Exception("Can't render chromosome view without a bag.");
        }

        for (Iterator it = organisms.iterator(); it.hasNext();) {
            
            String organismName = (String) it.next();
            DefaultCategoryDataset dataSet = new DefaultCategoryDataset();
                        
            // chromosome, count of genes
            LinkedHashMap<String, int[]> resultsTable = new LinkedHashMap<String, int[]> (); 
            // chromosome --> list of genes
            HashMap<String, ArrayList<String>> geneMap = new HashMap<String, ArrayList<String>>();
            
            // get all chromosomes for this organism 
            ArrayList<String> chromosomes 
                = (ArrayList<String>) BioUtil.getChromosomes(os, organismName);
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
            Results results = os.execute(q);
            results.setBatchSize(50000);
            boolean hasResults = false;
            if (results.size() > 0) {
                hasResults = true;
            }
            
            // find out how many genes in the bag have a chromosome location, use this
            // to work out the expected number for each chromosome.  This is a hack to
            // deal with the proportion of genes not assigned to a chromosome, it would
            // be easier of they were located on an 'unknown' chromosome.
            int totalWithLocation = 0;
            
            // put results in maps
            iter = results.iterator();
            while (iter.hasNext()) {
                ResultsRow resRow = (ResultsRow) iter.next();
                String chromosome = (String) resRow.get(0);     // chromosome
                String geneIdentifier = (String) resRow.get(1); // gene
                if (resultsTable.get(chromosome) != null) { 
                    (resultsTable.get(chromosome))[0]++;    
                    (geneMap.get(chromosome)).add(geneIdentifier);
                    totalWithLocation++;
                }
            }

            // update results with expected results
            addExpected(resultsTable, totalWithLocation, organismName);
            
            // Build a map from chromosome to gene list
            geneCategoryArray = new Object[resultsTable.size()];
            int i = 0;
            for (Iterator iterator = resultsTable.keySet().iterator(); iterator.hasNext();) {
                String chromosome = (String) iterator.next();
                dataSet.addValue((resultsTable.get(chromosome))[0], "Actual", chromosome);
                dataSet.addValue((resultsTable.get(chromosome))[1], "Expected", chromosome);
                Object[] geneSeriesArray = new Object[2];              
                geneSeriesArray[0] = geneMap.get(chromosome);   // actual
                // expected shouldn't be a link
                // geneSeriesArray[1] = geneMap.get(chromosome);   // expected
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
    private long getTotal(String organismName) 
    throws ClassNotFoundException {

        Query q = createQuery(organismName, "total", null);        
        Results results = os.execute(q);          
        Iterator iter = results.iterator();
        ResultsRow rr = (ResultsRow) iter.next();
        return ((Long) rr.get(0)).longValue();
    }

    private void addExpected(HashMap resultsTable, 
                             int bagSize, String organismName) 
        throws ClassNotFoundException {
        // totals
        long total = getTotal(organismName);

        // get expected results
        Query q = createQuery(organismName, "expected", null);
        Results results = os.execute(q);
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
                ((int[]) resultsTable.get(chromosome))[1] = 
                    (int) Math.round(expectedValue);             
            }
            i++;
        }
    }    
    
    private Query createQuery(String organismName, String resultsType, InterMineBag bag) 
        throws ClassNotFoundException {
        
        Query q = new Query();
        
        QueryClass chromosomeQC = new QueryClass(Chromosome.class);
        QueryClass geneQC 
            = new QueryClass(Class.forName(model.getPackageName() + "." + bagType)); 
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
        
        // constrain to be in chosen list of chomosomes, this may not be all chromosomes
        Collection chrs = BioUtil.getChromosomes(os, organismName);
        if (chrs != null && !chrs.isEmpty()) {
            QueryField qfChrId = new QueryField(chromosomeQC, "identifier");
            BagConstraint bagChr = new BagConstraint(qfChrId, ConstraintOp.IN, 
                                                     BioUtil.getChromosomes(os, organismName)); 
            cs.addConstraint(bagChr);
        }
        
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
