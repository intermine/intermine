package org.fishmine.web.widget;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.intermine.model.bio.ExpressionResult;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Stage;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.DataSetLdr;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * @author Julie Sullivan
 */
public class ZfinExprDataSetLdr implements DataSetLdr
{
    private static final Logger LOG = Logger.getLogger(ZfinExprDataSetLdr.class);
    private DefaultCategoryDataset dataSet = null;
    private Results results;
    private int widgetTotal = 0;

    /**
     * Creates a DataSetLdr used to retrieve, organise
     * and structure the data to create a graph
     * @param bag the bag
     * @param os the ObjectStore
     * @param extra extra attribute probably organism - currently only used for the chromosome
     * widget
     */
    public ZfinExprDataSetLdr(InterMineBag bag, ObjectStore os, String extra) {
        super();
        buildDataSets(bag, os);
    }

    /**
     * {@inheritDoc}
     */
    public CategoryDataset getDataSet() {
        return dataSet;
    }

    private void buildDataSets(InterMineBag bag, ObjectStore os) {

        Query q = createQuery(bag, false);
        results = os.execute(q);
        Iterator iter = results.iterator();
        LinkedHashMap<String, int[]> callTable = initCallTable();
        while (iter.hasNext()) {
            ResultsRow resRow = (ResultsRow) iter.next();
            String stage = (String) resRow.get(0);
            stage = (stage.split(":"))[0];
            Boolean expressed = (Boolean) resRow.get(1);
            Long geneCount = (Long) resRow.get(2);

            if (expressed.booleanValue()) {
                (callTable.get(stage.trim()))[0] += geneCount.intValue();
            } else {
                (callTable.get(stage.trim()))[1] += geneCount.intValue();
            }
        }

        dataSet = new DefaultCategoryDataset();

        for (Iterator<String> iterator = callTable.keySet().iterator(); iterator.hasNext();) {
            String stage = iterator.next();
            dataSet.addValue((callTable.get(stage))[0], "Expressed", stage);
            dataSet.addValue((callTable.get(stage))[1], "NotExpressed", stage);
        }

        calcTotal(bag, os);
    }

    private Query createQuery(InterMineBag bag, boolean calcTotal) {
        QueryClass mrnaResult = new QueryClass(ExpressionResult.class);
        QueryClass gene = new QueryClass(Gene.class);
        QueryClass stage = new QueryClass(Stage.class);

        QueryField qfStage = new QueryField(stage, "name");
        QueryField qfExpressed = new QueryField(mrnaResult, "expressed");
        QueryFunction qfCount = new QueryFunction();

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryField qf = new QueryField(gene, "id");
        cs.addConstraint(new BagConstraint(qf, ConstraintOp.IN, bag.getOsb()));

        QueryCollectionReference r = new QueryCollectionReference(gene, "expressionResults");
        cs.addConstraint(new ContainsConstraint(r, ConstraintOp.CONTAINS, mrnaResult));

        QueryCollectionReference r2 = new QueryCollectionReference(mrnaResult, "stages");
        cs.addConstraint(new ContainsConstraint(r2, ConstraintOp.CONTAINS, stage));

        Query q = new Query();
        q.setDistinct(false);

        if (!calcTotal) {
            q.addToSelect(qfStage);
            q.addToSelect(qfExpressed);
            q.addToSelect(qfCount);

            q.addFrom(mrnaResult);
            q.addFrom(gene);
            q.addFrom(stage);

            q.addToGroupBy(qfStage);
            q.addToGroupBy(qfExpressed);

            q.addToOrderBy(qfStage);
            q.setConstraint(cs);
        } else {
            Query subQ = new Query();
            subQ.setDistinct(true);

            subQ.addToSelect(qf);

            subQ.addFrom(mrnaResult);
            subQ.addFrom(gene);
            subQ.addFrom(stage);

            subQ.setConstraint(cs);

            q.addFrom(subQ);
            q.addToSelect(qfCount);
        }

        return q;
    }


    private LinkedHashMap<String, int[]> initCallTable() {
        LinkedHashMap<String, int[]> callTable = new LinkedHashMap();
        String[] stageLabels = new String[11];

        stageLabels[0] = "Unknown";
        stageLabels[1] = "Zygote";
        stageLabels[2] = "Cleavage";
        stageLabels[3] = "Blastula";
        stageLabels[4] = "Gastrula";
        stageLabels[5] = "Segmentation";
        stageLabels[6] = "Pharyngula";
        stageLabels[7] = "Hatching";
        stageLabels[8] = "Larval";
        stageLabels[9] = "Juvenile";
        stageLabels[10] = "Adult";

        for (String stage : stageLabels) {
            int[] count = new int[2];
            count[0] = 0;
            count[1] = 0;
            callTable.put(stage, count);
        }
        return callTable;
    }

    private void calcTotal(InterMineBag bag, ObjectStore os) {
        Results res = os.execute(createQuery(bag, true));
        Iterator iter = res.iterator();
        while (iter.hasNext()) {
            ResultsRow resRow = (ResultsRow) iter.next();
            widgetTotal = ((java.lang.Long) resRow.get(0)).intValue();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Results getResults() {
        return results;
    }

    /**
     * {@inheritDoc}
     */
    public int getWidgetTotal() {
        return widgetTotal;
    }
}
