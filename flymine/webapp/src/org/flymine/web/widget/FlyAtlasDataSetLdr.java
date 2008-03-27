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
import java.util.Iterator;
import java.util.LinkedHashMap;

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
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * @author Xavier Watkins
 *
 */
public class FlyAtlasDataSetLdr implements DataSetLdr
{

    private Results results;
    private DefaultCategoryDataset dataSet;
    
    /**
     * Creates a FlyAtlasDataSetLdr used to retrieve, organise
     * and structure the FlyAtlas data to create a graph
     * @param bag the bag
     * @param os the ObjectStore
     */
    public FlyAtlasDataSetLdr(InterMineBag bag, ObjectStore os, String extra) {
        super();
        dataSet = new DefaultCategoryDataset();

        Query q = new Query();
        QueryClass far = new QueryClass(FlyAtlasResult.class);
        QueryClass maa = new QueryClass(MicroArrayAssay.class);
        QueryClass gene = new QueryClass(Gene.class);
        q.addFrom(far);
        q.addFrom(maa);
        q.addFrom(gene);

        QueryField tissueName = new QueryField(maa, "name");

        q.addToSelect(new QueryField(far, "affyCall"));
        q.addToSelect(tissueName);
        q.addToSelect(new QueryField(gene, "primaryIdentifier"));

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
        q.addToOrderBy(tissueName);

        results = os.execute(q);
        results.setBatchSize(100000);
        Iterator iter = results.iterator();
        LinkedHashMap<String, int[]> callTable = new LinkedHashMap<String, int[]>();

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
                    } else if (affyCall.equals("Down")) {
                        (callTable.get(tissue))[1]--;
                    }
                } else {
                    int[] count = new int[2];
                    ArrayList<String> genesArray = new ArrayList<String>();
                    genesArray.add(identifier);
                    if (affyCall.equals("Up")) {
                        count[0]++;
                    } else if (affyCall.equals("Down")) {
                            count[1]--;
                    } 
                    callTable.put(tissue, count);
                }
            }
        }

        for (Iterator<String> iterator = callTable.keySet().iterator(); iterator.hasNext();) {
            String tissue = iterator.next();
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
        }
        
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
    public void setExtraAttributes(String extra) {
    }
    
    /**
     * {@inheritDoc}
     */
    public Results getResults() {
        return results;
    }

}
