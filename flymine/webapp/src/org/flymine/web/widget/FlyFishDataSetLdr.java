package org.flymine.web.widget;

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
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;

import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.DataSetLdr;
import org.intermine.web.logic.widget.GraphDataSet;

import org.flymine.model.genomic.DataSet;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.MRNAExpressionResult;

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
     * {@inheritDoc}
     */
    public Map getDataSets() {
        return dataSets;
    }

    private void buildDataSets(InterMineBag bag, String geneIdentifier, ObjectStore os) {
        

        Query q = new Query();
        
        QueryClass mrnaResult = new QueryClass(MRNAExpressionResult.class);
        QueryClass gene = new QueryClass(Gene.class);
        QueryClass ds = new QueryClass(DataSet.class);

        q.addFrom(mrnaResult);
        q.addFrom(gene);
        q.addFrom(ds);
        
        QueryField stageName = new QueryField(mrnaResult, "stageRange");

        q.addToSelect(new QueryField(mrnaResult, "expressed"));
        q.addToSelect(stageName);
        q.addToSelect(new QueryField(gene, "primaryIdentifier"));
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        if (bag != null) {
            QueryField qf = new QueryField(gene, "id");
            cs.addConstraint(new BagConstraint(qf, ConstraintOp.IN, bag.getOsb()));
        }
        if (geneIdentifier != null) {
            cs.addConstraint(new SimpleConstraint(new QueryField(gene, "primaryIdentifier"),
                                                  ConstraintOp.EQUALS,
                                                  new QueryValue(geneIdentifier)));
        }

        QueryCollectionReference r = new QueryCollectionReference(gene, "mRNAExpressionResults");
        cs.addConstraint(new ContainsConstraint(r, ConstraintOp.CONTAINS, mrnaResult));

        QueryObjectReference qcr = new QueryObjectReference(mrnaResult, "source");
        cs.addConstraint(new ContainsConstraint(qcr, ConstraintOp.CONTAINS, ds));
        
        
        cs.addConstraint(new SimpleConstraint(new QueryField(ds, "title"), ConstraintOp.EQUALS,
        new QueryValue("fly-Fish data set of Drosophila embryo mRNA localization patterns")));
        
        q.setConstraint(cs);
        q.addToOrderBy(stageName);


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

                if (callTable.get(stage) != null) {
                    if (expressed.booleanValue()) {
                        (callTable.get(stage))[0]++;
                        (geneMap.get(stage + "_Expressed")).add(identifier);
                    } else {
                        (callTable.get(stage))[1]++;
                        (geneMap.get(stage + "_NotExpressed")).add(identifier);
                    }
                } else {
                    int[] count = new int[2];
                    ArrayList<String> genesArray = new ArrayList<String>();
                    genesArray.add(identifier);
                    if (expressed.booleanValue()) {
                        count[0]++;
                        geneMap.put(stage + "_Expressed", genesArray);
                        geneMap.put(stage + "_NotExpressed", new ArrayList<String>());
                    } else {
                        count[1]++;
                        geneMap.put(stage + "_Expressed", new ArrayList<String>());
                        geneMap.put(stage + "_NotExpressed", genesArray);
                    }
                    callTable.put(stage, count);
                }

        }
        DefaultCategoryDataset dataSet = new DefaultCategoryDataset();
        // Build a map from tissue/UpDown to gene list
        geneCategoryArray = new Object[callTable.size()];
        int i = 0;
        for (Iterator iterator = callTable.keySet().iterator(); iterator.hasNext();) {
            String stage = (String) iterator.next();

            dataSet.addValue((callTable.get(stage))[0], "Expressed", stage);
            dataSet.addValue((callTable.get(stage))[1], "NotExpressed", stage);

            Object[] geneSeriesArray = new Object[2];
            geneSeriesArray[0] = geneMap.get(stage + "_Expressed");
            geneSeriesArray[1] = geneMap.get(stage + "_NotExpressed");
            geneCategoryArray[i] = geneSeriesArray;
            i++;
        }
        GraphDataSet graphDataSet = new GraphDataSet(dataSet, geneCategoryArray);
        if (results.size() > 0) {
            dataSets.put("any", graphDataSet);
        }
    }

}
