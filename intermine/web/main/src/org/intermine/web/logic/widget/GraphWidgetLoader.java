package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryEvaluable;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.PathConstraint;
import org.intermine.web.logic.widget.config.GraphWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfigUtil;

/**
 * The class that does the actual querying for results.
 * @author Alex Kalderimis
 *
 */
public class GraphWidgetLoader extends WidgetLdr implements DataSetLdr
{
    private GraphWidgetConfig config;
    private Results results;
    private int items;
    private List<List<Object>> resultTable = new LinkedList<List<Object>>();

    /**
     * Constructor.
     * @param bag The list we are running over.
     * @param os The data-store.
     * @param config The description of the list tool.
     * @param filter A filter value.
     * @param ids intermine IDs, required if bag is NULL
     */
    public GraphWidgetLoader(InterMineBag bag,
                              ObjectStore os,
                              GraphWidgetConfig config,
                              String filter, String ids) {
        super(bag, os, filter, config, ids);
        this.config = config;
        LinkedHashMap<String, long[]> categorySeriesMap = new LinkedHashMap<String, long[]>();
        if (!config.comparesActualToExpected()) {
            Query q = createQuery(GraphWidgetActionType.ACTUAL);
            results = os.execute(q);
            buildCategorySeriesMap(categorySeriesMap);
        } else {
            //calculate for the bag
            int totalInBagWithLocation = addActual(categorySeriesMap);
            LinkedHashMap<String, Long> categoryMapInDB = new LinkedHashMap<String, Long>();
            // calculate for genes in database
            int totalInDBWithLocation = addExpected(categoryMapInDB);
            buildCategorySeriesMapForActualExpectedCriteria(categorySeriesMap, categoryMapInDB,
                totalInBagWithLocation, totalInDBWithLocation);
        }
        //populate resultTable
        populateResultTable(categorySeriesMap);

        calcTotal();
    }

    private Query createQuery(GraphWidgetActionType action) {
        Model model = os.getModel();
        Query query = new Query();
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        query.setConstraint(cs);
        query.addFrom(startClass);

        queryClassInQuery = new HashMap<String, QueryClass>();
        String key = startClass.getType().getSimpleName();
        queryClassInQuery.put(key, startClass);

        QueryField qfCategoryPath = null;
        QueryField qfSeriesPath = null;
        if (!GraphWidgetActionType.TOTAL.equals(action)) {
            qfCategoryPath = createQueryFieldByPath(config.getCategoryPath(), query, true);
            if (!config.comparesActualToExpected() && config.hasSeries()) {
                qfSeriesPath = createQueryFieldByPath(config.getSeriesPath(), query, true);
            }
        }

        QueryField idQueryField = null;
        if (config.getTypeClass().equals(config.getStartClass())) {
            idQueryField = new QueryField(startClass, "id");
            if (!GraphWidgetActionType.EXPECTED.equals(action)) {
                cs.addConstraint(new BagConstraint(idQueryField, ConstraintOp.IN,
                                                   bag.getOsb()));
            }
        } else {
            //update query adding the bag
            QueryClass bagTypeQueryClass;
            try {
                bagTypeQueryClass = new QueryClass(Class.forName(model.getPackageName()
                                                          + "." + bag.getType()));
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Not found the class typebag for the bag "
                                                  + bag.getName(), e);
            }
            query.addFrom(bagTypeQueryClass);
            idQueryField = new QueryField(bagTypeQueryClass, "id");
            if (!GraphWidgetActionType.EXPECTED.equals(action)) {
                cs.addConstraint(new BagConstraint(idQueryField, ConstraintOp.IN,
                                                   bag.getOsb()));
            }

            QueryClass qc = null;
            //we use bag path only if we don't start from Gene...
            String bagPath = config.getListPath();
            String path = bagPath.split("\\.")[1];
            try {
                QueryObjectReference qor = null;
                if (bagPath.startsWith(config.getStartClass())) {
                    qor = new QueryObjectReference(startClass, path);
                    qc = bagTypeQueryClass;
                } else {
                    qor = new QueryObjectReference(bagTypeQueryClass, path);
                    qc = startClass;
                }
                cs.addConstraint(new ContainsConstraint(qor, ConstraintOp.CONTAINS, qc));
            } catch (IllegalArgumentException e) {
                // Not a reference - try collection instead
                QueryCollectionReference qcr = null;
                if (bagPath.startsWith(config.getStartClass())) {
                    qcr = new QueryCollectionReference(startClass, path);
                    qc = bagTypeQueryClass;
                } else {
                    qcr = new QueryCollectionReference(bagTypeQueryClass, path);
                    qc = startClass;
                }
                cs.addConstraint(new ContainsConstraint(qcr, ConstraintOp.CONTAINS, qc));
            }
        }

        for (PathConstraint pathConstraint : config.getPathConstraints()) {
            addConstraint(pathConstraint, query);
        }

        QueryFunction qfCount = new QueryFunction();
        if (!GraphWidgetActionType.TOTAL.equals(action)) {
            query.setDistinct(false);
            query.addToSelect(idQueryField);
            query.addToOrderBy(idQueryField);
            query.addToGroupBy(idQueryField);
            Query subQ = query;
            Query mainQuery = new Query();
            mainQuery.setDistinct(false);
            mainQuery.addFrom(subQ);
            QueryField qfSubCategoryPath = new QueryField(subQ, qfCategoryPath);
            mainQuery.addToSelect(qfSubCategoryPath);
            QueryField qfSubSeriesPath = null;
            if (qfSeriesPath != null) {
                qfSubSeriesPath = new QueryField(subQ, qfSeriesPath);
                mainQuery.addToSelect(qfSubSeriesPath);
            }
            mainQuery.addToSelect(qfCount);
            mainQuery.addToGroupBy(qfSubCategoryPath);
            if (qfSeriesPath != null) {
                mainQuery.addToGroupBy(qfSubSeriesPath);
            }
            return mainQuery;
        } else {
            Query subQ = query;
            subQ.setDistinct(true);
            subQ.addToSelect(idQueryField);

            Query mainQuery = new Query();
            mainQuery.setDistinct(false);
            mainQuery.addFrom(subQ);
            mainQuery.addToSelect(qfCount);
            return mainQuery;
        }
    }

    /**
     * Add a pathContraint to a query.
     * The pathConstraint contains a value as a String or
     * as a reference to a filter in the format [filter label]
     * @param pc the pathConstraind added to the query
     * @param query the query
     */
    private void addConstraint(PathConstraint pc, Query query) {
        QueryClass qc = startClass;
        QueryField qfConstraint = null;
        ConstraintSet cs = (ConstraintSet) query.getConstraint();
        String[] pathConstraintSplitted = pc.getPath().split("\\.");
        boolean isFilterConstraint = WidgetConfigUtil.isFilterConstraint(config, pc);
        QueryValue queryValue = null;
        if (!isFilterConstraint) {
            queryValue = buildQueryValue(pc);
        }
        if (isFilterConstraint && !"All".equalsIgnoreCase(filter)) {
            queryValue = new QueryValue(filter);
        }
        for (int index = 0; index < pathConstraintSplitted.length; index++) {
            if (index == pathConstraintSplitted.length - 1) {
                qfConstraint = new QueryField(qc, pathConstraintSplitted[index]);
                if (queryValue != null) {
                    if (!"null".equalsIgnoreCase(queryValue.getValue().toString())) {
                        QueryEvaluable qe = null;
                        if ( queryValue.getValue() instanceof String && !isFilterConstraint) {
                            qe = new QueryExpression(QueryExpression.LOWER, qfConstraint);
                        } else {
                            qe = qfConstraint;
                        }
                        cs.addConstraint(new SimpleConstraint(qe, pc.getOp(), queryValue));
                    } else {
                        ConstraintOp op = (pc.getOp().equals(ConstraintOp.EQUALS))
                                          ? ConstraintOp.IS_NULL
                                          : ConstraintOp.IS_NOT_NULL;
                        cs.addConstraint(new SimpleConstraint(qfConstraint, op));
                    }
                }
            } else {
                String partialPath = createAttributePath(pathConstraintSplitted, index);
                qc = addReference(query, qc, pathConstraintSplitted[index], partialPath);
            }
        }
    }

    private void buildCategorySeriesMap(
        HashMap<String, long[]> categorySeriesMap) {
        if (config.hasSeries()) {
            String[] seriesValue = config.getSeriesValues().split("\\,");
            for (Iterator<?> it = results.iterator(); it.hasNext();) {
                ResultsRow<?> row = (ResultsRow<?>) it.next();
                String category = (String) row.get(0);
                Object series = row.get(1);
                long count = (Long) row.get(2);
                if (series != null) {
                    if (categorySeriesMap.get(category) != null) {
                        for (int indexSeries = 0; indexSeries < seriesValue.length; indexSeries++) {
                            if (isSeriesValue(seriesValue[indexSeries], series)) {
                                (categorySeriesMap.get(category))[indexSeries] = count;
                                break;
                            }
                        }
                    } else {
                        long[] counts = new long[seriesValue.length];
                        for (int indexSeries = 0; indexSeries < seriesValue.length; indexSeries++) {
                            if (isSeriesValue(seriesValue[indexSeries], series)) {
                                counts[indexSeries] = count;
                                break;
                            }
                        }
                        categorySeriesMap.put(category, counts);
                    }
                }
            }
        } else {
            for (Iterator<?> it = results.iterator(); it.hasNext();) {
                ResultsRow<?> row = (ResultsRow<?>) it.next();
                String category;
                try {
                    category = (String) row.get(0);
                } catch (ClassCastException cce) {
                    category = Integer.toString((Integer) row.get(0));
                }
                long count = (Long) row.get(1);
                long[] counts = {count};
                categorySeriesMap.put(category, counts);
            }
        }
    }

    private boolean isSeriesValue(String seriesValue, Object series) {
        if ("true".equalsIgnoreCase(seriesValue) || "false".equalsIgnoreCase(seriesValue)) {
            return (Boolean.parseBoolean(seriesValue) == (Boolean) series);
        } else {
            return seriesValue.equals(series);
        }
    }

    private void populateResultTable(LinkedHashMap<String, long[]> categorySeriesMap) {
        List<Object> headerRow = new LinkedList<Object>();
        List<Object> dataRow = null;
        headerRow.add(config.getRangeLabel());
        if (config.hasSeries()) {
            String[] seriesLabels = config.getSeriesLabels().split(",");
            for (String seriesLabel : seriesLabels) {
                headerRow.add(seriesLabel);
            }
        }
        resultTable.add(headerRow);
        ArrayList<String> categories = new ArrayList<String>(categorySeriesMap.keySet());
        Collections.sort(categories);
        for (String category : categories) {
            dataRow = new LinkedList<Object>();
            dataRow.add(category);
            long[] seriesCounts = categorySeriesMap.get(category);
            for (long seriesCount : seriesCounts) {
                dataRow.add(seriesCount);
            }
            resultTable.add(dataRow);
        }

    }

    private void calcTotal() {
        Results res = os.execute(createQuery(GraphWidgetActionType.TOTAL));
        Iterator<?> iter = res.iterator();
        while (iter.hasNext()) {
            ResultsRow<?> resRow = (ResultsRow<?>) iter.next();
            items = ((java.lang.Long) resRow.get(0)).intValue();
        }
    }

    @Override
    public Results getResults() {
        return results;
    }

    @Override
    public int getWidgetTotal() {
        return items;
    }

    @Override
    public List<List<Object>> getResultTable() {
        return resultTable;
    }



    private int addExpected(Map<String, Long> resultsTable) {
        // get counts of gene in database for gene
        Query q = createQuery(GraphWidgetActionType.EXPECTED);
        if (q == null) {
            return 0;
        }
        Results res = os.execute(q);
        Iterator<?> iter = res.iterator();
        int grandTotal = 0;

        while (iter.hasNext()) {
            @SuppressWarnings("rawtypes")
            ResultsRow resRow = (ResultsRow) iter.next();

            String chromosome = (String) resRow.get(0);         // chromosome
            long geneCount = (java.lang.Long) resRow.get(1);    // genecount
            resultsTable.put(chromosome, geneCount);

            // increase total amount of genes with chromosomes
            grandTotal += geneCount;
        }
        return grandTotal;
    }

    private int addActual(HashMap<String, long[]> resultsTable) {
        // query for chromosome, gene.count for genes in list
        Query q = createQuery(GraphWidgetActionType.ACTUAL);
        if (q == null) {
            return 0;
        }
        results = os.execute(q, 50000, true, true, true);

        // find out how many genes in the bag have a chromosome location, use this
        // to work out the expected number for each chromosome. This is a hack to
        // deal with the proportion of genes not assigned to a chromosome, it would
        // be easier of they were located on an 'unknown' chromosome.
        int totalInBagWithLocation = 0;

        Iterator<?> iter = results.iterator();
        while (iter.hasNext()) {
            @SuppressWarnings("rawtypes")
            ResultsRow resRow = (ResultsRow) iter.next();
            String chromosome = (String) resRow.get(0);
            int geneCount = ((java.lang.Long) resRow.get(1)).intValue();
            // set the gene.count for genes in this bag with this chromosome
            if (resultsTable.get(chromosome) == null) {
                long[] counts = new long[2];
                counts[0] = geneCount;
                resultsTable.put(chromosome, counts);
            } else {
                long[] counts = resultsTable.get(chromosome);
                counts[0] = geneCount;
            }

            // increase total
            totalInBagWithLocation += geneCount;
        }
        return totalInBagWithLocation;
    }

    private void buildCategorySeriesMapForActualExpectedCriteria(
        HashMap<String, long[]> categorySeriesMap, Map<String, Long> categoryMapInDB,
        int totalInBagWithLocation, int totalInDBWithLocation) {

        for (String category : categoryMapInDB.keySet()) {
            double expectedValue = 0;
            double proportion = 0.0000000000;
            double totalInDBWithChromosome = (categoryMapInDB.get(category));

            if (totalInDBWithChromosome > 0) {
                proportion = totalInDBWithChromosome / totalInDBWithLocation;
            }
            expectedValue = totalInBagWithLocation * proportion;
            if (categorySeriesMap.get(category) != null) {
                (categorySeriesMap.get(category))[1] = (int) Math.round(expectedValue);
            } else {
                long[] counts = new long[2];
                counts[0] = 0;
                counts[1] = (int) Math.round(expectedValue);
                categorySeriesMap.put(category, counts);
            }
        }
    }
}
