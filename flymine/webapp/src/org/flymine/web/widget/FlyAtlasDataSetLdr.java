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
import java.util.Map;

import org.flymine.model.genomic.FlyAtlasResult;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.MicroArrayAssay;
import org.intermine.objectstore.ObjectStore;
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
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.DataSetLdr;
import org.intermine.web.logic.widget.GraphDataSet;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * @author Xavier Watkins
 *
 */
public class FlyAtlasDataSetLdr implements DataSetLdr
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
    public FlyAtlasDataSetLdr(InterMineBag bag, ObjectStore os) {
        super();
        DefaultCategoryDataset dataSet = new DefaultCategoryDataset();

        Query q = new Query();
        QueryClass far = new QueryClass(FlyAtlasResult.class);
        QueryClass maa = new QueryClass(MicroArrayAssay.class);
        QueryClass gene = new QueryClass(Gene.class);
        q.addFrom(far);
        q.addFrom(maa);
        q.addFrom(gene);
        
        QueryField name = new QueryField(maa, "name");
        
        // q.addToSelect(new QueryField(far,"enrichment"));
        q.addToSelect(new QueryField(far, "affyCall"));
        q.addToSelect(name);
        q.addToSelect(new QueryField(gene, "identifier"));

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryField qf = new QueryField(gene, "id");
        BagConstraint bagC = new BagConstraint(qf, ConstraintOp.IN, bag.getOsb()); 
        cs.addConstraint(bagC);

        QueryCollectionReference r = new QueryCollectionReference(far, "genes");
        ContainsConstraint cc = new ContainsConstraint(r, ConstraintOp.CONTAINS, gene);
        QueryCollectionReference r2 = new QueryCollectionReference(far, "assays");
        ContainsConstraint cc2 = new ContainsConstraint(r2, ConstraintOp.CONTAINS, maa);

        cs.addConstraint(cc);
        cs.addConstraint(cc2);
        q.setConstraint(cs);
        q.addToOrderBy(name);
        
        results = os.execute(q);
        results.setBatchSize(100000);
        Iterator iter = results.iterator();
        HashMap<String, int[]> callTable = new HashMap<String, int[]>();
        HashMap<String, ArrayList<String>> geneMap = new HashMap<String, ArrayList<String>>();
        while (iter.hasNext()) {
            ResultsRow resRow = (ResultsRow) iter.next();
            // Double enrichment = (Double)resRow.get(0);
            String affyCall = (String) resRow.get(0);
            String tissue = (String) resRow.get(1);
            String identifier = (String) resRow.get(2);
            if (affyCall != null) {
                if (callTable.get(tissue) != null) {
                    if (affyCall.equals("Up")) {
                        (callTable.get(tissue))[0]++;
                        (geneMap.get(tissue + "_Up")).add(identifier);
                    } else
                        if (affyCall.equals("Down")) {
                            (callTable.get(tissue))[1]--;
                            (geneMap.get(tissue + "_Down")).add(identifier);
                        }
                } else {
                    int[] count = new int[2];
                    ArrayList<String> genesArray = new ArrayList<String>();
                    genesArray.add(identifier);
                    if (affyCall.equals("Up")) {
                        count[0]++;
                        geneMap.put(tissue + "_Up", genesArray);
                        geneMap.put(tissue + "_Down", new ArrayList<String>());
                    } else
                        if (affyCall.equals("Down")) {
                            count[1]--;
                            geneMap.put(tissue + "_Up", new ArrayList<String>());
                            geneMap.put(tissue + "_Down", genesArray);
                        } else
                            if (affyCall.equals("None")) {
                                geneMap.put(tissue + "_Up", new ArrayList<String>());
                                geneMap.put(tissue + "_Down", new ArrayList<String>());
                            }
                    callTable.put(tissue, count);
                }
            }
        }

        // Build a map from tissue/UpDown to gene list
        geneCategoryArray = new Object[callTable.size()];
        int i = 0;
        for (Iterator iterator = callTable.keySet().iterator(); iterator.hasNext();) {
            String tissue = (String) iterator.next();
            if ((callTable.get(tissue))[0] > 0) {
              dataSet.addValue((callTable.get(tissue))[0], "Up", tissue);
            } else {
              dataSet.addValue(0.0001, "Up", tissue);
            }
            if ((callTable.get(tissue))[1] < 0) {
                dataSet.addValue((callTable.get(tissue))[1], "Down", tissue);
            } else {
              dataSet.addValue(-0.0001, "Down", tissue);
            }
            Object[] geneSeriesArray = new Object[2];
            geneSeriesArray[0] = geneMap.get(tissue + "_Up");
            geneSeriesArray[1] = geneMap.get(tissue + "_Down");
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
