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

import org.flymine.model.genomic.FlyAtlasResult;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.MicroArrayAssay;

import org.jfree.data.category.DefaultCategoryDataset;

/**
 * @author Xavier Watkins
 *
 */
public class FlyAtlasDataSetLdr implements DataSetLdr
{

    private Results results;
    private Object[] geneCategoryArray;
    private HashMap dataSets = new HashMap();
    /**
     * Creates a FlyAtlasDataSetLdr used to retrieve, organise
     * and structure the FlyAtlas data to create a graph
     * @param bag the bag
     * @param os the ObjectStore
     */
    public FlyAtlasDataSetLdr(InterMineBag bag, ObjectStore os) {
        super();
        DefaultCategoryDataset dataSet = new DefaultCategoryDataset();
        Collection geneList = bag.getListOfIds();

        Query q = new Query();
        QueryClass far = new QueryClass(FlyAtlasResult.class);
        QueryClass maa = new QueryClass(MicroArrayAssay.class);
        QueryClass gene = new QueryClass(Gene.class);
        q.addFrom(far);
        q.addFrom(maa);
        q.addFrom(gene);
        // q.addToSelect(new QueryField(far,"enrichment"));
        q.addToSelect(new QueryField(far, "affyCall"));
        q.addToSelect(new QueryField(maa, "name"));
        q.addToSelect(new QueryField(gene, "identifier"));

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryField qf = new QueryField(gene, "id");
        BagConstraint bagC = new BagConstraint(qf, ConstraintOp.IN, geneList); 
        cs.addConstraint(bagC);

        QueryCollectionReference r = new QueryCollectionReference(far, "genes");
        ContainsConstraint cc = new ContainsConstraint(r, ConstraintOp.CONTAINS, gene);
        QueryCollectionReference r2 = new QueryCollectionReference(far, "assays");
        ContainsConstraint cc2 = new ContainsConstraint(r2, ConstraintOp.CONTAINS, maa);

        cs.addConstraint(cc);
        cs.addConstraint(cc2);
        q.setConstraint(cs);

        results = new Results(q, os, os.getSequence());
        Iterator iter = results.iterator();
        HashMap callTable = new HashMap();
        HashMap geneMap = new HashMap();
        while (iter.hasNext()) {
            ResultsRow resRow = (ResultsRow) iter.next();
            // Double enrichment = (Double)resRow.get(0);
            String affyCall = (String) resRow.get(0);
            String tissue = (String) resRow.get(1);
            String identifier = (String) resRow.get(2);
            if (affyCall != null) {
                if (callTable.get(tissue) != null) {
                    if (affyCall.equals("Up")) {
                        ((int[]) callTable.get(tissue))[0]++;
                        ((ArrayList) geneMap.get(tissue + "_Up")).add(identifier);
                    } else
                        if (affyCall.equals("Down")) {
                            ((int[]) callTable.get(tissue))[1]--;
                            ((ArrayList) geneMap.get(tissue + "_Down")).add(identifier);
                        }
                } else {
                    int[] count = new int[2];
                    ArrayList genesArray = new ArrayList();
                    genesArray.add(identifier);
                    if (affyCall.equals("Up")) {
                        count[0]++;
                        geneMap.put(tissue + "_Up", genesArray);
                        ;
                        geneMap.put(tissue + "_Down", new ArrayList());
                    } else
                        if (affyCall.equals("Down")) {
                            count[1]--;
                            geneMap.put(tissue + "_Up", new ArrayList());
                            geneMap.put(tissue + "_Down", genesArray);
                        } else
                            if (affyCall.equals("None")) {
                                geneMap.put(tissue + "_Up", new ArrayList());
                                geneMap.put(tissue + "_Down", new ArrayList());
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
            dataSet.addValue(((int[]) callTable.get(tissue))[0], "Up", tissue);
            dataSet.addValue(((int[]) callTable.get(tissue))[1], "Down", tissue);
            Object[] geneSeriesArray = new Object[2];
            geneSeriesArray[0] = (ArrayList) geneMap.get(tissue + "_Up");
            geneSeriesArray[1] = (ArrayList) geneMap.get(tissue + "_Down");
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
