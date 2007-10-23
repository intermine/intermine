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
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.DataSetLdr;
import org.intermine.web.logic.widget.GraphDataSet;

import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.MRNALocalisationResult;

import org.jfree.data.category.DefaultCategoryDataset;

/**
 * @author Julie Sullivan
 *
 */
public class FlyFishDataSetLdr implements DataSetLdr
{

    private Results results;
    private Object[] geneCategoryArray;
    private HashMap<String, GraphDataSet> dataSets = new HashMap<String, GraphDataSet>();
    /**
     * Creates a FlyAtlasDataSetLdr used to retrieve, organise
     * and structure the FlyAtlas data to create a graph
     * @param bag the bag
     * @param os the ObjectStore
     */
    public FlyFishDataSetLdr(InterMineBag bag, ObjectStore os) {
        super();
        DefaultCategoryDataset dataSet = new DefaultCategoryDataset();

        Query q = new Query();
        QueryClass mrnaResult = new QueryClass(MRNALocalisationResult.class);
        QueryClass gene = new QueryClass(Gene.class);
       
        q.addFrom(mrnaResult);
        q.addFrom(gene);
        
        QueryField stageName = new QueryField(mrnaResult, "stage");

        q.addToSelect(new QueryField(mrnaResult, "expressed"));
        q.addToSelect(stageName);
        q.addToSelect(new QueryField(gene, "identifier"));

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryField qf = new QueryField(gene, "id");

        cs.addConstraint(new BagConstraint(qf, ConstraintOp.IN, bag.getOsb()));

        QueryCollectionReference r = new QueryCollectionReference(gene, "mRNALocalisations");
        cs.addConstraint(new ContainsConstraint(r, ConstraintOp.CONTAINS, mrnaResult));
        
        q.setConstraint(cs);

        
        results = os.execute(q);
        results.setBatchSize(100000);
        Iterator iter = results.iterator();
        LinkedHashMap<String, int[]> callTable = new LinkedHashMap<String, int[]>();
        LinkedHashMap<String, ArrayList<String>> geneMap 
                                                = new LinkedHashMap<String, ArrayList<String>>();
        while (iter.hasNext()) {
            ResultsRow resRow = (ResultsRow) iter.next();
            Boolean expressed = (Boolean) resRow.get(0);
            String stage = (String) resRow.get(1);
            String identifier = (String) resRow.get(2);
            if (expressed != null) {
                if (callTable.get(stage) != null) {
                    if (expressed.booleanValue()) {
                        (callTable.get(stage))[0]++;
                        (geneMap.get(stage + "_Up")).add(identifier);
                    } else if (!expressed.booleanValue()) {
                        (callTable.get(stage))[1]--;
                        (geneMap.get(stage + "_Down")).add(identifier);
                    }
                } else {
                    int[] count = new int[2];
                    ArrayList<String> genesArray = new ArrayList<String>();
                    genesArray.add(identifier);
                    if (expressed.booleanValue()) {
                        count[0]++;
                        geneMap.put(stage + "_Up", genesArray);
                        geneMap.put(stage + "_Down", new ArrayList<String>());
                    } else if (!expressed.booleanValue()) {
                        count[1]--;
                        geneMap.put(stage + "_Up", new ArrayList<String>());
                        geneMap.put(stage + "_Down", genesArray);
                    } 
                    callTable.put(stage, count);
                }
            }
        }

        // Build a map from tissue/UpDown to gene list
        geneCategoryArray = new Object[callTable.size()];
        int i = 0;
        for (Iterator iterator = callTable.keySet().iterator(); iterator.hasNext();) {
            String stage = (String) iterator.next();
            if ((callTable.get(stage))[0] > 0) {
              dataSet.addValue((callTable.get(stage))[0], "Up", stage);
            } else {
              dataSet.addValue(0.0001, "Up", stage);
            }
            if ((callTable.get(stage))[1] < 0) {
                dataSet.addValue((callTable.get(stage))[1], "Down", stage);
            } else {
              dataSet.addValue(-0.0001, "Down", stage);
            }
            Object[] geneSeriesArray = new Object[2];
            geneSeriesArray[0] = geneMap.get(stage + "_Up");
            geneSeriesArray[1] = geneMap.get(stage + "_Down");
            geneCategoryArray[i] = geneSeriesArray;
            i++;
        }
        GraphDataSet graphDataSet = new GraphDataSet(dataSet, geneCategoryArray);
        if (results.size() > 0) {
            dataSets.put("anyOrganism", graphDataSet);
        }
    }

    /**
     * @see org.intermine.web.logic.widget.DataSetLdr#getDataSet()
     */
    public Map getDataSets() {
        return dataSets;
    }

    
}
