package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

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
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.config.GraphWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfigUtil;

public class GraphWidgetLoader extends WidgetLdr implements DataSetLdr
{
    private GraphWidgetConfig config;
    private Results results;
    private int items;
    private List<List<Object>> resultTable = new LinkedList<List<Object>>();

    // TODO the parameters need to be updated
    public GraphWidgetLoader(InterMineBag bag, ObjectStore os, GraphWidgetConfig config, String filter) {
        super(bag, os, filter);
        this.config = config;

        LinkedHashMap<String, long[]> categorySeriesMap = new LinkedHashMap<String, long[]>();
        if (!config.isActualExpectedCriteria()) {
            Query q = createQuery(GraphWidgetActionType.ACTUAL);
            results = os.execute(q);
            buildCategorySeriesMap(categorySeriesMap);
        } else {
            //calculate for the bag
            int totalInBagWithLocation = addActual(categorySeriesMap);
            Map<String, Long> categoryMapInDB = new HashMap<String, Long>();
            // calculate for genes in database
            int totalInDBWithLocation = addExpected(categoryMapInDB);
            buildCategorySeriesMapForActualExpectedCriteria(categorySeriesMap, categoryMapInDB,
                                                           totalInBagWithLocation, totalInDBWithLocation);
            
        }
        //populate resultTable
        populateResultTable(categorySeriesMap);

        calcTotal();
    }

    public Query createQuery(GraphWidgetActionType action) {
        Model model = os.getModel();
        Query query = new Query();
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        query.setConstraint(cs);
        for (PathConstraint pathConstraint : config.getPathConstraints()) {
            pathConstraintsProcessed.put(pathConstraint, false);
        }
        try {
            startClass = new QueryClass(Class.forName(model.getPackageName()
                    + "." + config.getStartClass()));
            query.addFrom(startClass);

            QueryField qfCategoryPath = null;
            QueryField qfSeriesPath = null;
            if (!GraphWidgetActionType.TOTAL.equals(action)) {
                QueryField[] categoryAndSeriesQueryFields =
                    createCategoryAndSeriesQueryFields(model, query, startClass);
                qfCategoryPath = categoryAndSeriesQueryFields[0];
                qfSeriesPath = categoryAndSeriesQueryFields[1];
            }

            QueryField idQueryField = null;
            if (config.getTypeClass().equals(model.getPackageName() + "."
                                            + config.getStartClass())) {
                idQueryField = new QueryField(startClass, "id");
                if (!GraphWidgetActionType.EXPECTED.equals(action)) {
                    cs.addConstraint(new BagConstraint(idQueryField, ConstraintOp.IN,
                                                       bag.getOsb()));
                }
            } else {
                //update query adding the bag
                QueryClass bagTypeQueryClass = new QueryClass(Class.forName(model.getPackageName()
                                                              + "." + bag.getType()));
                query.addFrom(bagTypeQueryClass);
                idQueryField = new QueryField(bagTypeQueryClass, "id");
                if (!GraphWidgetActionType.EXPECTED.equals(action)) {
                    cs.addConstraint(new BagConstraint(idQueryField, ConstraintOp.IN,
                                                       bag.getOsb()));
                }

                QueryClass qc = null;
                //we use bag path only if we don't start from Gene...
                String bagPath = config.getBagPath();
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

            //add constraints not processed before
            for (PathConstraint pc : pathConstraintsProcessed.keySet()) {
                if (!pathConstraintsProcessed.get(pc)) {
                    addConstraint(pc, query, startClass);
                }
            }

            QueryFunction qfCount = new QueryFunction();
            if (!GraphWidgetActionType.TOTAL.equals(action)) {
                query.setDistinct(false);
                query.addToSelect(idQueryField);
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
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Class not found while processing bag with type"
                                               + bag.getType(), e);
        }
    }

    private QueryField[] createCategoryAndSeriesQueryFields(Model model, Query query,
        QueryClass startQueryClass) {
        QueryField qfCategoryPath = null;
        QueryField qfSeriesPath = null;
        QueryField[] categoryAndSeriesQueryFields = new QueryField[2];
        String categoryPath = config.getCategoryPath();
        String seriesPath = config.getSeriesPath();
        if (categoryPath.contains(".")) {
            QueryField[] queryFields = updateQuery(model, query, startQueryClass, categoryPath);
            qfCategoryPath = queryFields[0];
            if (queryFields[1] != null) {
                qfSeriesPath = queryFields[1];
            }
        } else {
            qfCategoryPath = new QueryField(startQueryClass, categoryPath);
            query.addToSelect(qfCategoryPath);
            query.addToGroupBy(qfCategoryPath);
            query.addToOrderBy(qfCategoryPath);
        }

        if (qfSeriesPath == null && !config.isActualExpectedCriteria()) {
            if (seriesPath.contains(".")) {
                qfSeriesPath = updateQuery(model, query, startQueryClass, seriesPath)[0];
            } else {
                qfSeriesPath = new QueryField(startQueryClass, seriesPath);
                query.addToSelect(qfSeriesPath);
                query.addToGroupBy(qfSeriesPath);
            }
        }
        categoryAndSeriesQueryFields[0] = qfCategoryPath;
        categoryAndSeriesQueryFields[1] = qfSeriesPath;
        return categoryAndSeriesQueryFields;
    }

    /*
     *
     */
    private QueryField[] updateQuery(Model model, Query query, QueryClass startQueryClass,
                                     String path) {
        QueryField[] queryFields = new QueryField[2];
        String[] paths = path.split("\\.");
        QueryClass qc = startQueryClass;
        ConstraintSet cs = (ConstraintSet) query.getConstraint();
        QueryField qf = null;
        for (int i = 0; i < paths.length; i++) {
            if (i == paths.length - 1) {
                qf = new QueryField(qc, paths[i]);
                query.addToSelect(qf);
                query.addToGroupBy(qf);
                query.addToOrderBy(qf);
                queryFields[0] = qf;
            } else {
                qc = addReference(query, qc, paths[i]);
            }
            if (i == 0) {
                //check if there is series path
                //starting with the same queryClass qc
                if (!config.getSeriesPath().contains(path)) {
                    String[] pathsConstraint = config.getSeriesPath().split("\\.");
                    if (pathsConstraint.length > 1) {
                        if (pathsConstraint[0].equals(paths[0])) {
                            String seriesPath = config.getSeriesPath()
                                .replace(pathsConstraint[0] + ".", "");
                            queryFields[1] = updateQuery(model, query, qc, seriesPath)[0];
                        }
                    }
                }
                //check if there are any constraints, not yet processed,
                //starting with the same queryClass qc
                for (PathConstraint pc : config.getPathConstraints()) {
                    if (!pathConstraintsProcessed.get(pc)) {
                        String[] pathsConstraint = pc.getPath().split("\\.");
                        if (pathsConstraint[0].equals(paths[0])) {
                            addConstraint(pc, query, qc);
                        }
                    }
                }
            }
        }
        return queryFields;
    }

    private void addConstraint(PathConstraint pc, Query query, QueryClass qc) {
        QueryField qfConstraint = null;
        ConstraintSet cs = (ConstraintSet) query.getConstraint();
        String[] pathsConstraint = pc.getPath().split("\\.");
        int limit = (qc == startClass) ? 0 : 1;
        boolean isFilterConstraint = WidgetConfigUtil.isFilterConstraint(config, pc);
        QueryValue queryValue = null;
        if (!isFilterConstraint) {
            queryValue = buildQueryValue(pc);
        }
        if (isFilterConstraint && !"All".equalsIgnoreCase(filter)) {
            queryValue = new QueryValue(filter);
        }

        for (int index = 0; index < pathsConstraint.length - limit; index++) {
            if (index == pathsConstraint.length - 1 - limit) {
                qfConstraint = new QueryField(qc, pathsConstraint[index + limit]);
                if (queryValue != null) {
                    cs.addConstraint(new SimpleConstraint(qfConstraint, pc.getOp(),
                                                          queryValue));
                }
                pathConstraintsProcessed.put(pc, true);
            } else {
                qc = addReference(query, qc, pathsConstraint[index + limit]);
                pathConstraintsProcessed.put(pc, true);
            }
        }
    }

    private void buildCategorySeriesMap(
        HashMap<String, long[]> categorySeriesMap) {
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
        String[] seriesLabels = config.getSeriesLabels().split(",");
        for (String seriesLabel : seriesLabels) {
            headerRow.add(seriesLabel);
        }
        resultTable.add(headerRow);

        for (String category : categorySeriesMap.keySet()) {
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

    /**
     * Returns the pathquery based on the views set in config file and the bag constraint.
     * Executed when the user selects any columns in the in the graph widget.
     * @return the query generated
     */
    public PathQuery createPathQuery() {
        PathQuery q = createPathQueryView(os, config);

        // bag constraint
        if (config.isBagPathSet()) {
            q.addConstraint(Constraints.in(config.getBagPath(), bag.getName()));
        } else {
            q.addConstraint(Constraints.in(config.getStartClass(), bag.getName()));
        }

        String prefix = config.getStartClass() + ".";
        //category constraint
        q.addConstraint(Constraints.eq(prefix + config.getCategoryPath(), "%category"));
        //series constraint
        q.addConstraint(Constraints.eq(prefix + config.getSeriesPath(),"%series"));

        return q;
    }

    private int addExpected(Map<String, Long> resultsTable) {
        // get counts of gene in database for gene
        Query q = createQuery(GraphWidgetActionType.EXPECTED);
        if (q == null) {
            return 0;
        }
        Results res = os.execute(q);
        Iterator iter = res.iterator();
        int grandTotal = 0;

        while (iter.hasNext()) {
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

        Iterator iter = results.iterator();
        while (iter.hasNext()) {
            ResultsRow resRow = (ResultsRow) iter.next();
            String chromosome = (String) resRow.get(0);
            int geneCount = ((java.lang.Long) resRow.get(1)).intValue();
            // set the gene.count for genes in this bag with this chromosome
            if (resultsTable.get(chromosome) == null) {
                long[] counts = new long[2];
                counts[0] = geneCount;
                resultsTable.put(chromosome, counts);
            } else {
                resultsTable.get(chromosome)[0] = geneCount;
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
