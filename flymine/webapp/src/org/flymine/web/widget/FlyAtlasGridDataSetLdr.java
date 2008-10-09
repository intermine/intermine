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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
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
import org.intermine.web.logic.widget.GridDataSet;
import org.intermine.web.logic.widget.GridDataSetLdr;

/**
 * @author Dominik Grimm
 *
 */
public class FlyAtlasGridDataSetLdr implements GridDataSetLdr
{

    private Results results;
    private GridDataSet dataSet;
    private int widgetTotal = 0;
    private Set<String> genes = new HashSet<String>();
    protected static final Logger LOG = Logger.getLogger(FlyAtlasGridDataSetLdr.class);

    /**
     * Creates a FlyAtlasDataSetLdr used to retrieve, organise
     * and structure the FlyAtlas data to create a graph
     * @param bag the bag
     * @param os the ObjectStore
     * @param extra ignore
     */
    public FlyAtlasGridDataSetLdr(InterMineBag bag, ObjectStore os, String extra) {
        super();
        dataSet = new GridDataSet();

        Query q = createQuery(bag);

        results = os.execute(q);
        results.setBatchSize(100000);
        Iterator iter = results.iterator();
        //LinkedHashMap<String, int[]> callTable = new LinkedHashMap<String, int[]>();

        while (iter.hasNext()) {
            ResultsRow resRow = (ResultsRow) iter.next();

            String affyCall = (String) resRow.get(0);
            String tissue = (String) resRow.get(1);
            String identifier = (String) resRow.get(2);
            if (affyCall != null) {
                    if (affyCall.equals("Up")) {
                        dataSet.addValue(tissue, identifier, true);
                    } else if (affyCall.equals("Down")) {
                        dataSet.addValue(tissue, identifier, false);
                    }
                genes.add(identifier);
            }

        }
        widgetTotal = genes.size();
    }

    /**
     * {@inheritDoc}
     */
    public Results getResults() {
        return results;
    }


    private Query createQuery(InterMineBag bag) {

        QueryClass far = new QueryClass(FlyAtlasResult.class);
        QueryClass maa = new QueryClass(MicroArrayAssay.class);
        QueryClass gene = new QueryClass(Gene.class);

        QueryField tissueName = new QueryField(maa, "name");

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryField qf = new QueryField(gene, "id");
        cs.addConstraint(new BagConstraint(qf, ConstraintOp.IN, bag.getOsb()));

        QueryCollectionReference r1 = new QueryCollectionReference(far, "genes");
        QueryCollectionReference r2 = new QueryCollectionReference(far, "assays");
        cs.addConstraint(new ContainsConstraint(r1, ConstraintOp.CONTAINS, gene));
        cs.addConstraint(new ContainsConstraint(r2, ConstraintOp.CONTAINS, maa));

        Query q = new Query();

        q.addToSelect(new QueryField(far, "affyCall"));
        q.addToSelect(tissueName);
        q.addToSelect(new QueryField(gene, "primaryIdentifier"));

        q.addFrom(far);
        q.addFrom(maa);
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
    /**
     * {@inheritDoc}
     */
    public GridDataSet getGridDataSet() {
        return dataSet;
    }

}
