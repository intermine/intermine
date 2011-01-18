package org.intermine.bio.web.widget;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.intermine.api.profile.InterMineBag;
import org.intermine.bio.web.logic.BioUtil;
import org.intermine.metadata.Model;
import org.intermine.model.bio.Chromosome;
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
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.web.logic.widget.DataSetLdr;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 *
 *
 * @author Julie Sullivan
 */
public class ChromosomeDistributionDataSetLdr implements DataSetLdr
{
    private static final Logger LOG = Logger.getLogger(ChromosomeDistributionDataSetLdr.class);
    private DefaultCategoryDataset dataSet;
    private ObjectStore os;
    private Model model;
    private String bagType;
    private Collection<String> chromosomeList;
    private Results results;
    private int widgetTotal = 0;

    /**
     * Creates a ChromosomeDistributionDataSetLdr used to retrieve, organise
     * and structure the data to create a graph.
     *
     * @param bag the bag
     * @param os the ObjectStore
     * @param organismName the organism name
     * @throws Exception if getting the list of organisms fails
     */
    public ChromosomeDistributionDataSetLdr(InterMineBag bag, ObjectStore os, String organismName)
        throws Exception {
        super();
        this.os = os;
        model = os.getModel();
        bagType = bag.getType();

        LinkedHashMap<String, int[]> resultsTable = new LinkedHashMap<String, int[]>();

        if (organismName == null) {
            LOG.warn("can't render graph widgets without organism name");
            return;
        }

        chromosomeList = BioUtil.getChromosomes(os, Arrays.asList(organismName.toLowerCase()),
                                                false);

        if (chromosomeList.isEmpty()) {
            return;
        }

        // used for not analysed figure
        calcTotal(bag, organismName);

        // initialise results list - so all chromosomes are displayed
        for (Iterator<String> chrIter = chromosomeList.iterator(); chrIter.hasNext();) {
            String chromosomeName = chrIter.next();
            int[] count = new int[3];
            count[0] = 0;   // actual - total in bag
            count[1] = 0;   // expected
            count[2] = 0;   // total in database
            resultsTable.put(chromosomeName, count);
        }

        // calculate chromsome, gene.count for genes in list
        int totalInBagWithLocation = addActual(resultsTable, organismName, bag);

        // calculate chromsome, gene.count for genes in database
        int totalInDBWithLocation = addExpected(resultsTable, organismName);

        // calculate expected gene.count for each chromosome
        for (String chromosome : resultsTable.keySet()) {
            double expectedValue = 0;
            double proportion = 0.0000000000;
            double totalInDBWithChromosome = (resultsTable.get(chromosome))[2];

            if (totalInDBWithChromosome > 0) {
                proportion = totalInDBWithChromosome / totalInDBWithLocation;
            }
            expectedValue = totalInBagWithLocation * proportion;
            if (resultsTable.get(chromosome) != null) {
                (resultsTable.get(chromosome))[1] = (int) Math.round(expectedValue);
            }
        }

        // put all data in dataset rendered in graph
        dataSet = new DefaultCategoryDataset();
        for (Iterator<String> iterator = resultsTable.keySet().iterator(); iterator.hasNext();) {
            String chromosome = iterator.next();
            dataSet.addValue((resultsTable.get(chromosome))[0], "Actual", chromosome);
            dataSet.addValue((resultsTable.get(chromosome))[1], "Expected", chromosome);
        }
    }

    /**
     * {@inheritDoc}
     */
    public CategoryDataset getDataSet() {
        return dataSet;
    }

    @SuppressWarnings("unchecked")
    private int addExpected(HashMap<String, int[]> resultsTable, String organismName)
        throws ClassNotFoundException {

        // get counts of gene in database for gene
        Query q = getQuery(organismName, "expected", null);
        if (q == null) {
            return 0;
        }
        Results res = os.execute(q);
        Iterator iter = res.iterator();
        int grandTotal = 0;

        while (iter.hasNext()) {
            ResultsRow resRow = (ResultsRow) iter.next();

            String chromosome = (String) resRow.get(0);         // chromosome
            Long geneCount = (java.lang.Long) resRow.get(1);    // genecount

            // record total number of genes for this chromosome
            (resultsTable.get(chromosome))[2] = geneCount.intValue();
            // increase total amount of genes with chromosomes
            grandTotal += geneCount.intValue();
        }

        return grandTotal;
    }

    @SuppressWarnings("unchecked")
    private int addActual(HashMap<String, int[]> resultsTable, String organismName,
                          InterMineBag bag)
        throws ClassNotFoundException {
        // query for chromosome, gene.count for genes in list
        Query q = getQuery(organismName, "actual", bag);
        results = os.execute(q, 50000, true, true, true);

        // find out how many genes in the bag have a chromosome location, use this
        // to work out the expected number for each chromosome. This is a hack to
        // deal with the proportion of genes not assigned to a chromosome, it would
        // be easier of they were located on an 'unknown' chromosome.
        int totalInBagWithLocation = 0;

        Iterator iter = results.iterator();
        while (iter.hasNext()) {
            ResultsRow resRow = (ResultsRow) iter.next();
            String chromosome = (String) resRow.get(0);
            Long geneCount = (java.lang.Long) resRow.get(1);
            // set the gene.count for genes in this bag with this chromosome
            (resultsTable.get(chromosome))[0] = geneCount.intValue();
            // increase total
            totalInBagWithLocation += geneCount.intValue();
        }
        return totalInBagWithLocation;
    }

    private Query getQuery(String organism, String resultsType, InterMineBag bag)
        throws ClassNotFoundException {

        QueryClass organismQC = new QueryClass(Organism.class);
        QueryClass chromosomeQC = new QueryClass(Chromosome.class);
        Class<?> bagCls = Class.forName(model.getPackageName() + "." + bagType);
        QueryClass featureQC = new QueryClass(bagCls);

        /* TODO we need to figure out another way to do this, this returns the wrong data */
        // query LocatedSequenceFeature if possible for better chance of using precompute
//        if (LocatedSequenceFeature.class.isAssignableFrom(bagCls)) {
//            featureQC = new QueryClass(LocatedSequenceFeature.class);
//        } else {
//            featureQC = new QueryClass(bagCls);
//        }

        QueryField chromoQF = new QueryField(chromosomeQC, "primaryIdentifier");
        QueryFunction countQF = new QueryFunction();
        QueryField organismNameQF = new QueryField(organismQC, "name");

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryObjectReference r = new QueryObjectReference(featureQC, "chromosome");
        cs.addConstraint(new ContainsConstraint(r, ConstraintOp.CONTAINS, chromosomeQC));

        Collection<String> chrs = new ArrayList<String>();
        // make chromos lowercase for query
        for (Iterator<String> iter = chromosomeList.iterator(); iter.hasNext();) {
            String chromosomeName = (iter.next()).toLowerCase();
            chrs.add(chromosomeName);
        }

        if (!chrs.isEmpty()) {
            QueryField qfChrId = new QueryField(chromosomeQC, "primaryIdentifier");
            QueryExpression qf = new QueryExpression(QueryExpression.LOWER, qfChrId);
            cs.addConstraint(new BagConstraint(qf, ConstraintOp.IN, chrs));
        } else {
            return null;
        }

        QueryObjectReference r2 = new QueryObjectReference(featureQC, "organism");
        cs.addConstraint(new ContainsConstraint(r2, ConstraintOp.CONTAINS, organismQC));

        QueryExpression qf = new QueryExpression(QueryExpression.LOWER, organismNameQF);
        SimpleConstraint sc = new SimpleConstraint(qf, ConstraintOp.EQUALS,
                                                   new QueryValue(organism.toLowerCase()));
        cs.addConstraint(sc);

        if (bag != null) {
            QueryField qf2 = new QueryField(featureQC, "id");
            cs.addConstraint(new BagConstraint(qf2, ConstraintOp.IN, bag.getOsb()));
        }

        Query q = new Query();
        q.addFrom(chromosomeQC);
        q.addFrom(featureQC);
        q.addFrom(organismQC);
        q.setConstraint(cs);

        if (!"total".equals(resultsType)) {
            q.setDistinct(false);
            q.addToSelect(chromoQF);
            q.addToSelect(countQF);
            q.addToGroupBy(chromoQF);
            q.addToOrderBy(chromoQF);
            return q;
        }
        q.setDistinct(true);
        q.addToSelect(new QueryField(featureQC, "id"));
        Query superQ = new Query();
        superQ.addFrom(q);
        superQ.addToSelect(countQF);
        return superQ;
    }

    private void calcTotal(InterMineBag bag, String organismName) throws ClassNotFoundException {
        Query q = getQuery(organismName, "total", bag);
        if (q == null) {
            return;
        }
        Object[] o = os.executeSingleton(q).toArray();
        int n = ((java.lang.Long) o[0]).intValue();
        if (n > 0) {
            widgetTotal = n;
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
