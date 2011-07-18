package org.flymine.web.widget;

/*
 * Copyright (C) 2002-2011 FlyMine
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

import org.intermine.api.profile.InterMineBag;
import org.intermine.model.bio.FlyAtlasResult;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.ProbeSet;
import org.intermine.model.bio.Tissue;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
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
    private int widgetTotal = 0;
    private Set<String> genes = new HashSet<String>();

    /**
     * Creates a FlyAtlasDataSetLdr used to retrieve, organise
     * and structure the FlyAtlas data to create a graph
     * @param bag the bag
     * @param os the ObjectStore
     * @param extra ignore
     */
    public FlyAtlasDataSetLdr(InterMineBag bag, ObjectStore os, String extra) {
        super();
        dataSet = new DefaultCategoryDataset();

        Query q = createQuery(bag);

        results = os.execute(q, 100000, true, true, true);
        Iterator<?> iter = results.iterator();
        LinkedHashMap<String, int[]> callTable = new LinkedHashMap<String, int[]>();

        while (iter.hasNext()) {
            ResultsRow<?> resRow = (ResultsRow<?>) iter.next();

            String affyCall = (String) resRow.get(0);
            String tissue = (String) resRow.get(1);
            String identifier = (String) resRow.get(2);
            if (affyCall != null) {
                if (callTable.get(tissue) != null) {
                    if ("Up".equals(affyCall)) {
                        (callTable.get(tissue))[0]++;
                    } else if ("Down".equals(affyCall)) {
                        (callTable.get(tissue))[1]--;
                    }
                } else {
                    int[] count = new int[2];
                    ArrayList<String> genesArray = new ArrayList<String>();
                    genesArray.add(identifier);
                    if ("Up".equals(affyCall)) {
                        count[0]++;
                    } else if ("Down".equals(affyCall)) {
                        count[1]--;
                    }
                    callTable.put(tissue, count);
                }
                genes.add(identifier);
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

        String bagType = bag.getType();

        QueryClass far = new QueryClass(FlyAtlasResult.class);
        QueryClass tissue = new QueryClass(Tissue.class);
        QueryClass gene = new QueryClass(Gene.class);

        QueryField tissueName = new QueryField(tissue, "name");

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference r1 = new QueryCollectionReference(far, "genes");
        cs.addConstraint(new ContainsConstraint(r1, ConstraintOp.CONTAINS, gene));

        QueryObjectReference r2 = new QueryObjectReference(far, "tissue");
        cs.addConstraint(new ContainsConstraint(r2, ConstraintOp.CONTAINS, tissue));

        Query q = new Query();

        q.addToSelect(new QueryField(far, "affyCall"));
        q.addToSelect(tissueName);

        QueryField qf = null;

        if ("ProbeSet".equalsIgnoreCase(bagType)) {
            QueryClass probe = new QueryClass(ProbeSet.class);
            qf = new QueryField(probe, "id");


            q.addFrom(probe);
            q.addToSelect(new QueryField(probe, "primaryIdentifier"));

            QueryCollectionReference r3 = new QueryCollectionReference(gene, "probeSets");
            cs.addConstraint(new ContainsConstraint(r3, ConstraintOp.CONTAINS, probe));
        } else {
            qf = new QueryField(gene, "id");
            cs.addConstraint(new BagConstraint(qf, ConstraintOp.IN, bag.getOsb()));

            q.addToSelect(new QueryField(gene, "primaryIdentifier"));
        }
        cs.addConstraint(new BagConstraint(qf, ConstraintOp.IN, bag.getOsb()));

        q.addFrom(far);
        q.addFrom(tissue);
        q.addFrom(gene);

        q.setConstraint(cs);
        q.addToOrderBy(tissueName);

        return q;
    }

    /**
     * {@inheritDoc}
     */
    public int getWidgetTotal() {
        return widgetTotal;
    }

}
