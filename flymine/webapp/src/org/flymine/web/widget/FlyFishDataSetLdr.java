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
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;

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
     * @param identifier the gene to use in teh query
     * @param os the ObjectStore
     */
    public FlyFishDataSetLdr(String identifier, ObjectStore os) {
        super();
        buildDataSets(null, identifier, os);
    }

    /**
     * Creates a FlyAtlasDataSetLdr used to retrieve, organise
     * and structure the FlyAtlas data to create a graph
     * @param bag the bag
     * @param os the ObjectStore
     */
    public FlyFishDataSetLdr(InterMineBag bag, ObjectStore os) {
        super();
        buildDataSets(bag, null, os);
    }
    
    /**
     * @see org.intermine.web.logic.widget.DataSetLdr#getDataSet()
     */
    public Map getDataSets() {
        return dataSets;
    }

    private void buildDataSets(InterMineBag bag, String geneIdentifier, ObjectStore os) {
        DefaultCategoryDataset dataSet = new DefaultCategoryDataset();

        Query q = new Query();
        QueryClass mrnaResult = new QueryClass(MRNALocalisationResult.class);
        QueryClass gene = new QueryClass(Gene.class);
       
        q.addFrom(mrnaResult);
        q.addFrom(gene);
        
        QueryField stageName = new QueryField(mrnaResult, "stage");

        q.addToSelect(new QueryField(mrnaResult, "localisation"));
        q.addToSelect(stageName);
        q.addToSelect(new QueryField(gene, "identifier"));

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryField qf = new QueryField(gene, "id");
        
        if (bag != null) {
            cs.addConstraint(new BagConstraint(qf, ConstraintOp.IN, bag.getOsb()));
        }
        if (geneIdentifier != null) {
            cs.addConstraint(new SimpleConstraint(new QueryField(gene, "identifier"),
                                                  ConstraintOp.EQUALS, 
                                                  new QueryValue(geneIdentifier)));
        }

        QueryCollectionReference r = new QueryCollectionReference(gene, "mRNALocalisationResults");
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
            String loc = (String) resRow.get(0);
            String stage = (String) resRow.get(1);
            String identifier = (String) resRow.get(2);
            if (loc != null) {
                if (callTable.get(stage) != null) {
                    if (loc.equals("localised")) {
                        (callTable.get(stage))[0]++;
                        (geneMap.get(stage + "_Localised")).add(identifier);
                    } else if (loc.equals("not localised")) {
                        (callTable.get(stage))[1]++;
                        (geneMap.get(stage + "_NotLocalised")).add(identifier);
                    } else if (loc.equals("not expressed")) {
                        (callTable.get(stage))[2]++;
                        (geneMap.get(stage + "_NotExpressed")).add(identifier);
                    }
                } else {
                    int[] count = new int[3];
                    ArrayList<String> genesArray = new ArrayList<String>();
                    genesArray.add(identifier);
                    if (loc.equals("localised")) {
                        count[0]++;
                        geneMap.put(stage + "_Localised", genesArray);
                        geneMap.put(stage + "_NotLocalised", new ArrayList<String>());
                        geneMap.put(stage + "_NotExpressed", new ArrayList<String>());
                    } else if (loc.equals("not localised")) {
                        count[1]++;
                        geneMap.put(stage + "_Localised", new ArrayList<String>());
                        geneMap.put(stage + "_NotLocalised", genesArray);
                        geneMap.put(stage + "_NotExpressed", new ArrayList<String>());
                    } else if (loc.equals("not expressed")) {
                        count[2]++;
                        geneMap.put(stage + "_Localised", new ArrayList<String>());
                        geneMap.put(stage + "_NotLocalised", new ArrayList<String>());
                        geneMap.put(stage + "_NotExpressed", genesArray);
                    } else {
                        throw new RuntimeException("unknown term (" + loc + ") encountered");
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
   
            dataSet.addValue((callTable.get(stage))[0], "Localised", stage);
            dataSet.addValue((callTable.get(stage))[1], "NotLocalised", stage);
            dataSet.addValue((callTable.get(stage))[2], "NotExpressed", stage);
            
            Object[] geneSeriesArray = new Object[3];
            geneSeriesArray[0] = geneMap.get(stage + "_Localised");
            geneSeriesArray[1] = geneMap.get(stage + "_NotLocalised");
            geneSeriesArray[2] = geneMap.get(stage + "_NotExpressed");
            geneCategoryArray[i] = geneSeriesArray;
            i++;
        }
        GraphDataSet graphDataSet = new GraphDataSet(dataSet, geneCategoryArray);
        if (results.size() > 0) {
            dataSets.put("anyOrganism", graphDataSet);
        }
    }
    
}
