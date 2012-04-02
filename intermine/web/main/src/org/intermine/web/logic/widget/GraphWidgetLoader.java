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
import org.intermine.pathquery.PathQuery;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.widget.config.GraphWidgetConfig;

public class GraphWidgetLoader implements DataSetLdr
{
    private ObjectStore os;
    private InterMineBag bag;
    private GraphWidgetConfig config;
    private QueryClass startClassQueryClass;
    private Results results;
    private int items;
    private List<List<Object>> resultTable = new LinkedList<List<Object>>();
    private Map<PathConstraint, Boolean> pathConstraintsProcessed =
        new HashMap<PathConstraint, Boolean>();

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
        for (PathConstraint pathConstraint : config.getPathConstraints()) {
            pathConstraintsProcessed.put(pathConstraint, false);
        }
        try {
            startClassQueryClass = new QueryClass(Class.forName(model.getPackageName()
                    + "." + config.getStartClass()));
            query.addFrom(startClassQueryClass);

            QueryField qfCategoryPath = null;
            QueryField qfSeriesPath = null;
            if (!calcTotal) {
                QueryField[] categoryAndSeriesQueryFields =
                    createCategoryAndSeriesQueryFields(model, query, startClassQueryClass);
                qfCategoryPath = categoryAndSeriesQueryFields[0];
                qfSeriesPath = categoryAndSeriesQueryFields[1];
            }

            QueryField idQueryField = null;
            if (config.getTypeClass().equals(model.getPackageName() + "."
                                            + config.getStartClass())) {
                idQueryField = new QueryField(startClassQueryClass, "id");
                cs.addConstraint(new BagConstraint(idQueryField, ConstraintOp.IN, bag.getOsb()));
            } else {
                //update query adding the bag
                QueryClass bagTypeQueryClass = new QueryClass(Class.forName(model.getPackageName()
                                                              + "." + bag.getType()));
                query.addFrom(bagTypeQueryClass);
                idQueryField = new QueryField(bagTypeQueryClass, "id");
                cs.addConstraint(new BagConstraint(idQueryField, ConstraintOp.IN, bag.getOsb()));

                QueryClass qc = null;
                //we use bag path only if we don't start from Gene...
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
            }

            //add constraints not processed before
            for (PathConstraint pc : pathConstraintsProcessed.keySet()) {
                if (!pathConstraintsProcessed.get(pc)) {
                    addConstraint(pc, query, startClassQueryClass);
                }
            }

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
        if (qfSeriesPath == null) {
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
        //QueryClass qcConstraint = null;
        ConstraintSet cs = (ConstraintSet) query.getConstraint();
        String[] pathsConstraint = pc.getPath().split("\\.");
        int limit = (qc == startClassQueryClass) ? 0 : 1;
        QueryValue queryValue = buildQueryValue(pc);
        for (int index = 0; index < pathsConstraint.length - limit; index++) {
            if (index == pathsConstraint.length - 1 - limit) {
                qfConstraint = new QueryField(qc, pathsConstraint[index + limit]);
                if (queryValue != null) {
                    cs.addConstraint(new SimpleConstraint(qfConstraint, pc.getOp(),
                                                          queryValue));
                }
                pathConstraintsProcessed.put(pc, true);
            } else {
                try {
                    QueryObjectReference qor = new QueryObjectReference(qc,
                                               pathsConstraint[index + limit]);
                    qc = new QueryClass(qor.getType());
                    query.addFrom(qc);
                    cs.addConstraint(new ContainsConstraint(qor,
                                     ConstraintOp.CONTAINS, qc));
                    pathConstraintsProcessed.put(pc, true);
                } catch (IllegalArgumentException e) {
                    // Not a reference - try collection instead
                    QueryCollectionReference qcr =
                        new QueryCollectionReference(qc,
                            pathsConstraint[index + limit]);
                    qc = new QueryClass(TypeUtil.getElementType(
                        qc.getType(), pathsConstraint[index + limit]));
                    query.addFrom(qc);
                    cs.addConstraint(new ContainsConstraint(qcr,
                        ConstraintOp.CONTAINS, qc));
                    pathConstraintsProcessed.put(pc, true);
                }
            }
        }
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
/*    private void addConstraint(PathConstraint pc, Query query, QueryClass qc) {
        QueryValue queryValue = null;
        queryValue = buildQueryValue(pc);

        QueryField qfConstraint = null;
        ConstraintSet cs = (ConstraintSet) query.getConstraint();
        String[] pathsConstraint = pc.getPath().split("\\.");

        for (int index = 0; index < pathsConstraint.length; index++) {
            if (index == pathsConstraint.length - 1) {
                if (index == 0) {
                    qfConstraint = new QueryField(qc,
                        pathsConstraint[index]);
                    if (queryValue != null) {
                       cs.addConstraint(new SimpleConstraint(qfConstraint, pc.getOp(),
                                                          queryValue));
                    }
                }
            } else {
                try {
                    QueryObjectReference qor = new QueryObjectReference(qc,
                                               pathsConstraint[index]);
                    qc = new QueryClass(qor.getType());
                    query.addFrom(qc);
                    cs.addConstraint(new ContainsConstraint(qor,
                                     ConstraintOp.CONTAINS, qc));
                } catch (IllegalArgumentException e) {
                    // Not a reference - try collection instead
                    QueryCollectionReference qcr =
                        new QueryCollectionReference(qc,
                            pathsConstraint[index]);
                    qc = new QueryClass(TypeUtil.getElementType(
                        qc.getType(), pathsConstraint[index]));
                    query.addFrom(qc);
                    cs.addConstraint(new ContainsConstraint(qcr,
                        ConstraintOp.CONTAINS, qc));
                }
            }
        }
    }*/

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
        if (config.isBagPathSet()) {
            q.addConstraint(Constraints.in(config.getBagPath(), bag.getName()));
        } else {
            q.addConstraint(Constraints.in(config.getStartClass(), bag.getName()));
        }

        //category constraint
        q.addConstraint(Constraints.eq(prefix + config.getCategoryPath(), "%category"));
        //series constraint
        q.addConstraint(Constraints.eq(prefix + config.getSeriesPath(),"%series"));

        return q;
    }

}
