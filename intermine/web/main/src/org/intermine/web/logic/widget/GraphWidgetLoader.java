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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

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
import org.intermine.pathquery.PathQuery;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.widget.config.GraphWidgetConfig;

public class GraphWidgetLoader implements DataSetLdr
{
    private ObjectStore os;
    private InterMineBag bag;
    private GraphWidgetConfig config;
    private Results results;
    private int items;
    private List<List<Object>> resultTable = new LinkedList<List<Object>>();

    // TODO the parameters need to be updated
    public GraphWidgetLoader(InterMineBag bag, ObjectStore os, GraphWidgetConfig config) {
        this.bag = bag;
        this.os = os;
        this.config = config;

        Query q = createQuery(false);
        results = os.execute(q);

        LinkedHashMap<String, long[]> categorySeriesMap = buildCategorySeriesMap();

        //populate resultTable
        populateResultTable(categorySeriesMap);

        calcTotal();
    }

    public Query createQuery(boolean calcTotal) {
        Model model = os.getModel();
        Query query = new Query();
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        query.setConstraint(cs);

        try {
            QueryClass startClassQueryClass = new QueryClass(Class.forName(model.getPackageName()
                    + "." + config.getStartClass()));
            query.addFrom(startClassQueryClass);

            QueryField qfCategoryPath = null;
            QueryField qfSeriesPath = null;
            if (!calcTotal) {
                String categoryPath = config.getCategoryPath();
                String seriesPath = config.getSeriesPath();
                if (categoryPath.contains(".")) {
                    qfCategoryPath = updateQuery(model, query, startClassQueryClass, categoryPath);
                } else {
                    qfCategoryPath = new QueryField(startClassQueryClass, categoryPath);
                    query.addToSelect(qfCategoryPath);
                    query.addToGroupBy(qfCategoryPath);
                    query.addToOrderBy(qfCategoryPath);
                }
                if (seriesPath.contains(".")) {
                    qfSeriesPath = updateQuery(model, query, startClassQueryClass, seriesPath);
                } else {
                    qfSeriesPath = new QueryField(startClassQueryClass, seriesPath);
                    query.addToSelect(qfSeriesPath);
                    query.addToGroupBy(qfSeriesPath);
                }
            }

            //update query adding the bag
            //TODO if bagtype != Gene
            QueryClass bagTypeQueryClass = new QueryClass(Class.forName(model.getPackageName()
                                                          + "." + bag.getType()));

            query.addFrom(bagTypeQueryClass);
            QueryField idQueryField = new QueryField(bagTypeQueryClass, "id");
            cs.addConstraint(new BagConstraint(idQueryField, ConstraintOp.IN, bag.getOsb()));

            QueryClass qc = null;
            String bagPath = config.getBagPath();
            String path = bagPath.split("\\.")[1];
            try {
                QueryObjectReference qor = null;
                if (bagPath.startsWith(config.getStartClass())) {
                    qor = new QueryObjectReference(startClassQueryClass, path);
                    qc = bagTypeQueryClass;
                } else {
                    qor = new QueryObjectReference(bagTypeQueryClass, path);
                    qc = startClassQueryClass;
                }
                cs.addConstraint(new ContainsConstraint(qor, ConstraintOp.CONTAINS, qc));
            } catch (IllegalArgumentException e) {
                // Not a reference - try collection instead
                QueryCollectionReference qcr = null;
                if (bagPath.startsWith(config.getStartClass())) {
                    qcr = new QueryCollectionReference(startClassQueryClass, path);
                    qc = bagTypeQueryClass;
                } else {
                    qcr = new QueryCollectionReference(bagTypeQueryClass, path);
                    qc = startClassQueryClass;
                }
                cs.addConstraint(new ContainsConstraint(qcr, ConstraintOp.CONTAINS, qc));
            }

            //add dataset constraint
            for (PathConstraint pathConstraint : config.getPathConstraints()) {
                addConstraint(pathConstraint, query, startClassQueryClass);
            }

/*            if (config.getDataSetPath() != "") {
                QueryClass dataSetQueryClass = new QueryClass(Class.forName(model.getPackageName()
                                                                            + ".DataSet"));
                query.addFrom(dataSetQueryClass);
                QueryObjectReference qor = new QueryObjectReference(startClassQueryClass,
                                                                    config.getDataSetPath());
                cs.addConstraint(new ContainsConstraint(qor, ConstraintOp.CONTAINS,
                                                        dataSetQueryClass));
                QueryExpression qf2 = new QueryExpression(QueryExpression.LOWER,
                        new QueryField(dataSetQueryClass, "name"));
                cs.addConstraint(new SimpleConstraint(qf2, ConstraintOp.EQUALS,
                    new QueryValue(config.getDataSetValue().toLowerCase())));
            }*/

            QueryFunction qfCount = new QueryFunction();
            if (!calcTotal) {
                query.setDistinct(false);
                query.addToSelect(idQueryField);
                query.addToGroupBy(idQueryField);
                Query subQ = query;
                Query mainQuery = new Query();
                mainQuery.setDistinct(false);
                mainQuery.addFrom(subQ);
                QueryField qfSubCategoryPath = new QueryField(subQ, qfCategoryPath);
                mainQuery.addToSelect(qfSubCategoryPath);
                QueryField qfSubSeriesPath = new QueryField(subQ, qfSeriesPath);
                mainQuery.addToSelect(qfSubSeriesPath);
                mainQuery.addToSelect(qfCount);
                mainQuery.addToGroupBy(qfSubCategoryPath);
                mainQuery.addToGroupBy(qfSubSeriesPath);
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

    /*
     *
     */
    private QueryField updateQuery(Model model, Query query, QueryClass startQueryClass, String path) {
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
            } else {
                try {
                    QueryObjectReference qor = new QueryObjectReference(qc, paths[i]);
                    qc = new QueryClass(qor.getType());
                    query.addFrom(qc);
                    cs.addConstraint(new ContainsConstraint(qor, ConstraintOp.CONTAINS,
                                qc));
                } catch (IllegalArgumentException e) {
                    // Not a reference - try collection instead
                    QueryCollectionReference qcr = new QueryCollectionReference(qc,
                            paths[i]);
                    qc = new QueryClass(TypeUtil.getElementType(qc.getType(), paths[i]));
                    query.addFrom(qc);
                    cs.addConstraint(new ContainsConstraint(qcr, ConstraintOp.CONTAINS,
                                qc));
                }
            }
        }
        return qf;
    }

    private QueryValue buildQueryValue(PathConstraint pc) {
        String value = PathConstraint.getValue(pc);
        QueryValue queryValue = null;
        if ("true".equals(value)) {
            queryValue = new QueryValue(true);
        } else if ("false".equals(value)) {
            queryValue = new QueryValue(false);
        } else {
            queryValue = new QueryValue(value);
        }
        return queryValue;
    }

    private void addConstraint(PathConstraint pc, Query query, QueryClass qc) {
        QueryValue queryValue = null;
        queryValue = buildQueryValue(pc);

        QueryField qfConstraint = null;
        QueryClass qcConstraint = null;
        ConstraintSet cs = (ConstraintSet) query.getConstraint();
        String[] pathsConstraint = pc.getPath().split("\\.");

        for (int index = 0; index < pathsConstraint.length; index++) {
            if (index == pathsConstraint.length - 1) {
                if (index == 0) {
                    qfConstraint = new QueryField(qc,
                        pathsConstraint[index]);
                } else {
                    qfConstraint = new QueryField(qcConstraint,
                        pathsConstraint[index]);
                }

                if (queryValue != null) {
                    cs.addConstraint(new SimpleConstraint(qfConstraint, pc.getOp(),
                                                          queryValue));
                }
            } else {
                try {
                    QueryObjectReference qor = new QueryObjectReference(qc,
                                               pathsConstraint[index]);
                    qcConstraint = new QueryClass(qor.getType());
                    query.addFrom(qcConstraint);
                    cs.addConstraint(new ContainsConstraint(qor,
                                     ConstraintOp.CONTAINS, qcConstraint));
                } catch (IllegalArgumentException e) {
                    // Not a reference - try collection instead
                    QueryCollectionReference qcr =
                        new QueryCollectionReference(qc,
                            pathsConstraint[index + 1]);
                    qcConstraint = new QueryClass(TypeUtil.getElementType(
                        qc.getType(), pathsConstraint[index]));
                    query.addFrom(qcConstraint);
                    cs.addConstraint(new ContainsConstraint(qcr,
                        ConstraintOp.CONTAINS, qcConstraint));
                }
            }
        }
    }

    private LinkedHashMap<String, long[]> buildCategorySeriesMap() {
        LinkedHashMap<String, long[]> categorySeriesMap = new LinkedHashMap<String, long[]>();
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
        return categorySeriesMap;
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
        Results res = os.execute(createQuery(true));
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

    public PathQuery createPathQuery() {
        Model model = os.getModel();
        PathQuery q = new PathQuery(model);
        String[] views = config.getViews().split("\\s*,\\s*");
        String prefix = config.getStartClass() + ".";
        for (String view : views) {
            if (!view.startsWith(prefix)) {
                view = prefix + view;
            }
            q.addView(view);
        }

        // bag constraint
        q.addConstraint(Constraints.in(config.getBagPath(), bag.getName()));

        //category constraint
        q.addConstraint(Constraints.eq(prefix + config.getCategoryPath(), "%category"));
        //series constraint
        q.addConstraint(Constraints.eq(prefix + config.getSeriesPath(),"%series"));

        return q;
    }

}
