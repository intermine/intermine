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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.Model;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.web.logic.widget.DataSetLdr;

/**
 *
 * @author Julie Sullivan
  */
public class OverlapsDataSetLdr implements DataSetLdr
{

    private static final Logger LOG = Logger.getLogger(OverlapsDataSetLdr.class);
    private Model model;
    private String bagType;
    private Results results;
    private int widgetTotal = 0;
    private static final String DISTANCE_10_KB = "10.0kb";

    /**
     * Creates a FeatureLengthDataSetLdr used to retrieve, organise
     * and structure the data to create a graph
     * @param bag the bag
     * @param os the ObjectStore
     * @param organismName the organism name
     * @throws Exception if getting the list of organisms fails
     *
     */
    public OverlapsDataSetLdr(InterMineBag bag, ObjectStore os, String organismName)
        throws Exception {
        super();

        model = os.getModel();
        bagType = bag.getType();

        if (organismName == null) {
            LOG.warn("can't render graph widgets without organism name");
            return;
        }

/*        XYSeries upstream = getSeries(bag, os, organismName, "upstream");
        XYSeries downstream = getSeries(bag, os, organismName, "downstream");
        if (upstream == null && downstream == null) {
            return;
        }
        dataSet = new XYSeriesCollection();
        if (upstream != null) {
            ((XYSeriesCollection) dataSet).addSeries(upstream);
        }
        if (downstream != null) {
            ((XYSeriesCollection) dataSet).addSeries(downstream);
        }*/
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
/*    private XYSeries getSeries(InterMineBag bag, ObjectStore os,  String organismName,
                               String seriesName)
        throws ClassNotFoundException {

        Map<String, Integer> distanceToNearestGene = new HashMap<String, Integer>();

        Query q = getQuery(organismName, bag, seriesName);
        results = os.execute(q, 50000, true, true, true);

        Iterator<?> iter = results.iterator();
        while (iter.hasNext()) {
            ResultsRow<?> resRow = (ResultsRow<?>) iter.next();
            String featureIdentifier = (java.lang.String) resRow.get(0);
            Integer featureStart = (java.lang.Integer) resRow.get(1);
            Integer featureEnd = (java.lang.Integer) resRow.get(2);
            Integer geneStart = (java.lang.Integer) resRow.get(3);
            Integer geneEnd = (java.lang.Integer) resRow.get(4);

            Integer distance = null;
            if ("downstream".equals(seriesName)) {
                distance = featureStart - geneEnd;
            } else if ("upstream".equals(seriesName)) {
                distance = geneStart - featureEnd;
            }
            if (distance.compareTo(new Integer(0)) < 0) {
                distance = 0;
            }

            Integer currentDistance = distanceToNearestGene.get(featureIdentifier);
            if (currentDistance == null) {
                distanceToNearestGene.put(featureIdentifier, distance);
            } else {
                // see if our new distance is lower
                if (distance.compareTo(currentDistance) < 0) {
                    distanceToNearestGene.put(featureIdentifier, distance);
                }
            }
        }
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (Map.Entry<String, Integer> entry : distanceToNearestGene.entrySet()) {
            stats.addValue(entry.getValue());
        }
        Double std = stats.getStandardDeviation();
        if (std.compareTo(0.0) <= 0) {
            widgetTotal = 0;
            return null;
        }
        Function2D actual = new NormalDistributionFunction2D(stats.getMean(), std);
        int total = (int) stats.getN();
        widgetTotal = total;
        return DatasetUtilities.sampleFunction2DToSeries(actual, 0.0, stats.getMax(), total,
                                                         seriesName);
    }*/

    private Query getQuery(String organism, InterMineBag bag, String seriesName)
        throws ClassNotFoundException {

        QueryClass organismQC = new QueryClass(Organism.class);
        Class<?> bagCls = Class.forName(model.getPackageName() + "." + bagType);

        QueryClass featureQC = new QueryClass(bagCls);
        QueryClass geneFlankingRegionQC = null;
        QueryClass geneQC = new QueryClass(Gene.class);
        QueryClass geneLocationQC = new QueryClass(Location.class);
        QueryClass featureLocationQC = new QueryClass(Location.class);

        try {
            geneFlankingRegionQC = new QueryClass(Class.forName(model.getPackageName()
                                                          + ".GeneFlankingRegion"));
        } catch (ClassNotFoundException e) {
            LOG.error("Error rendering overlaps widget", e);
            // don't throw an exception, return NULL instead.  The widget will display 'no
            // results'. the javascript that renders widgets assumes a valid widget and thus
            // can't handle an exception thrown here.
            return null;
        }

        QueryField geneStart = new QueryField(geneLocationQC, "start");
        QueryField geneEnd = new QueryField(geneLocationQC, "end");
        QueryField organismNameQF = new QueryField(organismQC, "name");

        QueryField featureIdentifier = new QueryField(featureQC, "primaryIdentifier");
        QueryField featureStart = new QueryField(featureLocationQC, "start");
        QueryField featureEnd = new QueryField(featureLocationQC, "end");

        QueryField distance = new QueryField(geneFlankingRegionQC, "distance");
        QueryField direction = new QueryField(geneFlankingRegionQC, "direction");

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryObjectReference r1 = new QueryObjectReference(featureQC, "organism");
        cs.addConstraint(new ContainsConstraint(r1, ConstraintOp.CONTAINS, organismQC));

        QueryExpression qf1 = new QueryExpression(QueryExpression.LOWER, organismNameQF);
        SimpleConstraint sc1 = new SimpleConstraint(qf1, ConstraintOp.EQUALS,
                                                   new QueryValue(organism.toLowerCase()));
        cs.addConstraint(sc1);

        QueryField qf2 = new QueryField(featureQC, "id");
        cs.addConstraint(new BagConstraint(qf2, ConstraintOp.IN, bag.getOsb()));

        // binding site to gene flanking region
        QueryCollectionReference c1
            = new QueryCollectionReference(featureQC, "overlappingFeatures");
        cs.addConstraint(new ContainsConstraint(c1, ConstraintOp.CONTAINS, geneFlankingRegionQC));

        // binding site.location
        QueryObjectReference r2 = new QueryObjectReference(featureQC, "chromosomeLocation");
        cs.addConstraint(new ContainsConstraint(r2, ConstraintOp.CONTAINS, featureLocationQC));

        // overlappingfeatures.gene
        QueryObjectReference r3 = new QueryObjectReference(geneFlankingRegionQC, "gene");
        cs.addConstraint(new ContainsConstraint(r3, ConstraintOp.CONTAINS, geneQC));

        // gene.location
        QueryObjectReference r4 = new QueryObjectReference(geneQC, "chromosomeLocation");
        cs.addConstraint(new ContainsConstraint(r4, ConstraintOp.CONTAINS, geneLocationQC));

        // overlappingfeatures.distance = '10.0kb'
        QueryExpression qf3 = new QueryExpression(QueryExpression.LOWER, distance);
        SimpleConstraint sc2 = new SimpleConstraint(qf3, ConstraintOp.EQUALS,
                                                   new QueryValue(DISTANCE_10_KB));
        cs.addConstraint(sc2);

        // direction = upstream | downstream
        QueryExpression qf4 = new QueryExpression(QueryExpression.LOWER, direction);
        SimpleConstraint sc3 = new SimpleConstraint(qf4, ConstraintOp.EQUALS,
                                                   new QueryValue(seriesName));

        cs.addConstraint(sc3);

        Query q = new Query();
        q.addFrom(featureQC);
        q.addFrom(organismQC);
        q.addFrom(geneFlankingRegionQC);
        q.addFrom(geneQC);
        q.addFrom(geneLocationQC);
        q.addFrom(featureLocationQC);

        q.setConstraint(cs);
        q.setDistinct(false);

        q.addToSelect(featureIdentifier);
        q.addToSelect(featureStart);
        q.addToSelect(featureEnd);
        q.addToSelect(geneStart);
        q.addToSelect(geneEnd);
        return q;
    }

    @Override
    public List<List<Object>> getResultTable() {
        return new LinkedList<List<Object>>();
    }
}
