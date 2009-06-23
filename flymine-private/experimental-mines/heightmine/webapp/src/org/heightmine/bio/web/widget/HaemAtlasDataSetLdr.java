package org.heightmine.bio.web.widget;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.DataSetLdr;

import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.HaemAtlasProbeSet;
import org.flymine.model.genomic.HaemAtlasResult;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * @author Dominik Grimm
 *
 */
public class HaemAtlasDataSetLdr implements DataSetLdr
{

    private Results results;
    private DefaultCategoryDataset dataSet;
    private int widgetTotal = 0;
    private Set<String> genes = new HashSet<String>();
    
    /**
     * Creates a HaemAtlasDataSetLdr used to retrieve, organise
     * and structure the HaemAtlas data to create a graph
     * @param bag the bag
     * @param os the ObjectStore
     * @param extra ignore
     */
    public HaemAtlasDataSetLdr(InterMineBag bag, ObjectStore os, String extra) {
        super();
        dataSet = new DefaultCategoryDataset();

        Query q = createQuery(bag);

        results = os.execute(q);
        results.setBatchSize(20000);
        Iterator<?> iter = results.iterator();
        LinkedHashMap<String, int[]> callTable = new LinkedHashMap<String, int[]>();

        while (iter.hasNext()) {
            ResultsRow resRow = (ResultsRow) iter.next();

            double  detectionProbabilities = Double.parseDouble(resRow.get(0).toString());
            String sampleName = (String) resRow.get(1);
            String identifier = (String) resRow.get(2);
            if (detectionProbabilities < Double.valueOf(extra)) {
                if (callTable.get(sampleName) != null) {
                        (callTable.get(sampleName))[0]++;
                } else {
                    int[] count = new int[2];
                    ArrayList<String> genesArray = new ArrayList<String>();
                    genesArray.add(identifier);
                    count[0]++;
                    callTable.put(sampleName, count);
                }
                genes.add(identifier);
            }
        }

        for (Iterator<String> iterator = callTable.keySet().iterator(); iterator.hasNext();) {
            String sampleName = iterator.next();
            dataSet.addValue((callTable.get(sampleName))[0], "Expressed", sampleName);
        } 
      
        widgetTotal = genes.size();
    }

    /**
     * {@inheritDoc}
     */
    public CategoryDataset getDataSet() {
        return dataSet;
    }
    
    /**
     * {@inheritDoc}
     */
    public Results getResults() {
        return results;
    }

    
    private Query createQuery(InterMineBag bag) {
                
        QueryClass had = new QueryClass(HaemAtlasResult.class);
        QueryClass prs = new QueryClass(HaemAtlasProbeSet.class);
        QueryClass gene = new QueryClass(Gene.class);

        QueryField sampleName = new QueryField(had, "sampleName");

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryField qf = new QueryField(gene, "id");
        cs.addConstraint(new BagConstraint(qf, ConstraintOp.IN, bag.getOsb()));

        QueryCollectionReference r1 = new QueryCollectionReference(gene, "probeSets");  
        QueryCollectionReference r2 = new QueryCollectionReference(prs, "haemAtlasResults");  
        cs.addConstraint(new ContainsConstraint(r1, ConstraintOp.CONTAINS, prs));
        cs.addConstraint(new ContainsConstraint(r2, ConstraintOp.CONTAINS, had));
        Query q = new Query();

        q.addToSelect(new QueryField(had, "detectionProbabilities"));
        q.addToSelect(sampleName);
        q.addToSelect(new QueryField(gene, "symbol"));

        q.addFrom(had);
        q.addFrom(prs);
        q.addFrom(gene);
        
        q.setConstraint(cs);
        q.addToOrderBy(sampleName);

        return q;
    }
    
    /**
     * {@inheritDoc}
     */
    public int getWidgetTotal() {
        return widgetTotal;
    }
   
}
