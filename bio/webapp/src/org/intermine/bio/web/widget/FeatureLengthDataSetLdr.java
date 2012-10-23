package org.intermine.bio.web.widget;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.Model;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.util.CacheMap;
import org.intermine.web.logic.widget.DataSetLdr;

/**
 *
 * @author Julie Sullivan
  */
public class FeatureLengthDataSetLdr implements DataSetLdr
{
    private static final Logger LOG = Logger.getLogger(FeatureLengthDataSetLdr.class);
    private Model model;
    private String bagType;
    private Results results;
    private int widgetTotal = 0;
    private static final double MINIMUM_VALUE = 0.0;

    /**
     * Creates a FeatureLengthDataSetLdr used to retrieve, organise
     * and structure the data to create a graph
     * @param bag the bag
     * @param os the ObjectStore
     * @param organismName the organism name
     * @throws Exception if getting the list of organisms fails
     *
     */
    public FeatureLengthDataSetLdr(InterMineBag bag, ObjectStore os, String organismName)
        throws Exception {
        super();

        model = os.getModel();
        bagType = bag.getType();

        if (organismName == null) {
            LOG.warn("can't render graph widgets without organism name");
            return;
        }

/*        XYSeries actual = getSeries(bag, os, organismName);
        XYSeries expected = getSeries(null, os, organismName);
        dataSet = new XYSeriesCollection();
        ((XYSeriesCollection) dataSet).addSeries(actual);
        ((XYSeriesCollection) dataSet).addSeries(expected);*/
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

    @SuppressWarnings("boxing")
/*    private XYSeries getSeries(InterMineBag bag, ObjectStore os,  String organismName)
        throws ClassNotFoundException {

        Query q = getQuery(organismName, bag);
        XYSeries series = featureLengthCache.get(q.toString());

        *//**
         * actual   = series will always be null for bag queries, we probably don't want to cache
         *            those, the bag could change
         * expected = series will only be null if this is the first time the query is run
         *//*
        if (series == null) {
            results = os.execute(q, 50000, true, true, true);
            DescriptiveStatistics stats = new DescriptiveStatistics();

            Iterator<?> iter = results.iterator();
            while (iter.hasNext()) {
                ResultsRow<?> resRow = (ResultsRow<?>) iter.next();
                Integer length = (java.lang.Integer) resRow.get(0);
                stats.addValue(length);
            }

            Double mean = stats.getMean();
            DecimalFormat twoDigits = new DecimalFormat("0.00");
            String prettyMean = twoDigits.format(mean);
            Function2D actual
                = new NormalDistributionFunction2D(mean, stats.getStandardDeviation());

            int total = (int) stats.getN();
            String legend = "[mean: " + prettyMean + " count:  " + String.valueOf(total) + "]";
            String seriesName = "All features " + legend;
            if (bag != null) {
                seriesName = "Features in this list " + legend;
                widgetTotal = total;
            }
            series = DatasetUtilities.sampleFunction2DToSeries(actual, MINIMUM_VALUE,
                                                                        stats.getMax(),
                                                                        total, seriesName);
            if (bag == null) {
                featureLengthCache.put(q.toString(), series);
                LOG.info("caching feature length results:" + q.toString());
            }
        } else {
            LOG.info("using cached feature length results:" + q.toString());
        }
        return series;
    }*/

    private Query getQuery(String organism, InterMineBag bag)
        throws ClassNotFoundException {

        QueryClass organismQC = new QueryClass(Organism.class);
        Class<?> bagCls = Class.forName(model.getPackageName() + "." + bagType);
        QueryClass featureQC = new QueryClass(bagCls);

        QueryField lengthQF = new QueryField(featureQC, "length");
        QueryField organismNameQF = new QueryField(organismQC, "name");

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryObjectReference r2 = new QueryObjectReference(featureQC, "organism");
        cs.addConstraint(new ContainsConstraint(r2, ConstraintOp.CONTAINS, organismQC));

        QueryExpression qf = new QueryExpression(QueryExpression.LOWER, organismNameQF);
        SimpleConstraint sc = new SimpleConstraint(qf, ConstraintOp.EQUALS,
                                                   new QueryValue(organism.toLowerCase()));
        cs.addConstraint(sc);
        cs.addConstraint(new SimpleConstraint(lengthQF, ConstraintOp.IS_NOT_NULL));
        if (bag != null) {
            QueryField qf2 = new QueryField(featureQC, "id");
            cs.addConstraint(new BagConstraint(qf2, ConstraintOp.IN, bag.getOsb()));
        }

        Query q = new Query();
        q.addFrom(featureQC);
        q.addFrom(organismQC);
        q.setConstraint(cs);
        q.setDistinct(false);
        q.addToSelect(lengthQF);
        return q;
    }

    @Override
    public List<List<Object>> getResultTable() {
        return new LinkedList<List<Object>>();
    }
}
