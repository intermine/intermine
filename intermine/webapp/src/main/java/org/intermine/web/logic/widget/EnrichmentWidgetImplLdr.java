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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.TypeUtil;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.BagConstraint;
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
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.PathConstraint;
import org.intermine.web.logic.widget.config.EnrichmentWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfigUtil;

/**
 * Implement methods to access data an enrichment calculation needs to be provided with.
 * @author Daniela Butano
 *
 */
public class EnrichmentWidgetImplLdr extends WidgetLdr
{
    private EnrichmentWidgetConfig config;
    private String action;
    private InterMineBag populationBag;
    private boolean extraCorrectionCoefficient;
    private CorrectionCoefficient correctionCoefficient;
    private String populationIds;

    /**
     * Construct an Enrichment widget loader, which performs the queries needed for
     * enrichment statistics.
     *
     * @param bag The bag containing the items we are interested in examining.
     * @param populationBag The bag containing the background population for
     * this test (MAY BE NULL).
     * @param os The connection to the Object Store database.
     * @param config The configuration detailing the kind of enrichment to do.
     * @param filter An optional filter value.
     * @param extraCorrectionCoefficient if true correction coefficient has been selected
     * @param correctionCoefficient a instance of correction coefficient
     * @param applyCorrectionCoefficient
     * @param ids list of IDs to analyse, use instead of intermine bag if bag is NULL
     * @param populationIds use instead of populationBag
     */
    public EnrichmentWidgetImplLdr(InterMineBag bag, InterMineBag populationBag,
                                   ObjectStore os, EnrichmentWidgetConfig config,
                                   String filter, boolean extraCorrectionCoefficient,
                                   CorrectionCoefficient correctionCoefficient,
                                   String ids, String populationIds) {
        super(bag, os, filter, config, ids);
        this.populationBag = populationBag;
        this.extraCorrectionCoefficient = extraCorrectionCoefficient;
        this.correctionCoefficient = correctionCoefficient;
        this.config = config;
        this.populationIds = populationIds;
    }

    /**
     * Returns the relevant query.  this method is used for 6 widget queries.
     *
     * export query:
     *
     *      select identifier and term where key = what the user selected on the widget
     *
     * analysed query:
     *
     *      select object.id where object is used in query
     *
     *  the results of this query are used as a NOT_IN constraint in a pathquery.  the pathquery
     *  is run when the user clicks on the 'not analysed' number on the widget.
     *
     * population query:
     *
     *     M = total annotated with this term in reference population
     *
     * annotated population query:
     *
     *     N = total annotated with any term in reference population
     *
     * sample query:
     *
     *     k = total annotated with this term in bag
     *
     * annotated sample query:
     *
     *     n = total annotated with any term in bag (used to be bag.count)
     *
     * @param keys the keys of the records to be exported
     * @param action which query to be built.
     * @return query to return the correct result set for this widget
     */
    public Query getQuery(String action, List<String> keys) {
        this.action = action;
        queryClassInQuery = new HashMap<String, QueryClass>();
        String key = startClass.getType().getSimpleName();
        queryClassInQuery.put(key, startClass);

        Query query = new Query();
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        query.setConstraint(cs);
        query.addFrom(startClass);

        QueryField qfEnrich = null;
        QueryField qfEnrichId = null;
        qfEnrich = createQueryFieldByPath(config.getEnrich(), query, false);
        if (config.getEnrichIdentifier() != null) {
            qfEnrichId = createQueryFieldByPath(config.getEnrichIdentifier(), query, false);
        } else {
            qfEnrichId = qfEnrich;
        }

        if (keys != null) {
            cs.addConstraint(new BagConstraint(qfEnrichId, ConstraintOp.IN, keys));
        }

        QueryField qfStartClassId = new QueryField(startClass, "id");
        if (!action.startsWith("population")) {
            if (bag != null) {
                cs.addConstraint(new BagConstraint(qfStartClassId, ConstraintOp.IN, bag.getOsb()));
            } else if (ids != null) {
                // use list of IDs instead of bag
                String[] idArray = ids.split(",");
                Collection<Integer> idsCollection = new LinkedHashSet<Integer>();
                for (String id : idArray) {
                    try {
                        idsCollection.add(Integer.valueOf(id.trim()));
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("List of IDs contains invalid integer: " + id,
                                e);
                    }
                }
                cs.addConstraint(new BagConstraint(qfStartClassId, ConstraintOp.IN, idsCollection));
            }
        } else if (populationBag != null || populationIds != null) {
            if (populationBag != null) {
                cs.addConstraint(new BagConstraint(qfStartClassId,
                        ConstraintOp.IN, populationBag.getOsb()));
            } else if (populationIds != null) {
                // use list of IDs instead of bag
                String[] idArray = populationIds.split(",");
                List<String> idCollection = Arrays.asList(idArray);
                cs.addConstraint(new BagConstraint(qfStartClassId, ConstraintOp.IN, idCollection));
            }
        }

        for (PathConstraint pathConstraint : config.getPathConstraints()) {
            addConstraint(pathConstraint, query);
        }

        query.setDistinct(true);
        Query subQ = query;
        Query mainQuery = new Query();
        mainQuery.setDistinct(false);

        QueryFunction qfCount = new QueryFunction();
        QueryField qfCorrection = null;
        if (extraCorrectionCoefficient
            && correctionCoefficient.isApplicable()) {
            qfCorrection =
                    correctionCoefficient.updateQueryWithCorrectionCoefficient(subQ, startClass);
        }
        // which columns to return when the user clicks on 'export'
        if ("export".equals(action)) {
            subQ.addToSelect(qfEnrichId);
            subQ.addToSelect(new QueryField(startClass, config.getStartClassDisplay()));
            subQ.addToSelect(qfStartClassId);
            subQ.addToOrderBy(qfEnrichId);
            return subQ;
        // analysed query:  return the gene only
        } else if ("analysed".equals(action)) {
            subQ.addToSelect(qfStartClassId);
            return subQ;
        // total query:  only return the count of unique genes
        } else if (action.endsWith("Total")) {
            subQ.addToSelect(qfStartClassId);
            mainQuery.addFrom(subQ);
            mainQuery.addToSelect(qfCount);
            // and for the whole population the average length
            if (action.startsWith("population") && qfCorrection != null) {
                correctionCoefficient.updatePopulationQuery(mainQuery, subQ, qfCorrection);
            }
        // enrichment queries
        } else {
            subQ.addToSelect(qfStartClassId);
            subQ.addToSelect(qfEnrichId);
            if (qfEnrichId != qfEnrich) {
                subQ.addToSelect(qfEnrich);
            }
            mainQuery.addFrom(subQ);
            QueryField outerQfEnrichId = new QueryField(subQ, qfEnrichId);
            mainQuery.addToSelect(outerQfEnrichId);
            mainQuery.addToGroupBy(outerQfEnrichId);
            mainQuery.addToSelect(qfCount);
            if ("sample".equals(action)) {
                if (qfEnrichId != qfEnrich) {
                    QueryField outerQfEnrich = new QueryField(subQ, qfEnrich);
                    mainQuery.addToSelect(outerQfEnrich);
                    mainQuery.addToGroupBy(outerQfEnrich);
                } else {
                    mainQuery.addToSelect(outerQfEnrichId);
                }
            } else if ("population".equals(action) && qfCorrection != null) {
                correctionCoefficient.updatePopulationQuery(mainQuery, subQ, qfCorrection);
            }
        }
        return mainQuery;
    }

    private void addConstraint(PathConstraint pc, Query query) {
        boolean isListConstraint = WidgetConfigUtil.isListConstraint(pc);
        boolean isFilterConstraint = WidgetConfigUtil.isFilterConstraint(config, pc);
        QueryValue queryValue = null;
        if (!isFilterConstraint && !isListConstraint) {
            queryValue = buildQueryValue(pc);
        }
        if (isFilterConstraint && !"All".equalsIgnoreCase(filter)) {
            queryValue = new QueryValue(filter);
        }

        QueryClass qc = startClass;
        QueryField qfConstraint = null;
        QueryClass qcConstraint = null;
        ConstraintSet cs = (ConstraintSet) query.getConstraint();
        String[] pathsConstraint = pc.getPath().split("\\.");
        //subQuery used only for population
        Query subQuery = new Query();
        ConstraintSet csSubQuery = new ConstraintSet(ConstraintOp.AND);
        subQuery.setConstraint(csSubQuery);

        for (int index = 0; index < pathsConstraint.length; index++) {
            if (index == pathsConstraint.length - 1) {
                qfConstraint = new QueryField(qc, pathsConstraint[index]);
                if (isListConstraint) {
                    if (action.startsWith("population")) {
                        subQuery.addToSelect(qfConstraint);
                        subQuery.addToOrderBy(qfConstraint);
                        subQuery.addFrom(startClass);
                        subQuery.addFrom(qcConstraint);
                        QueryField qfStartClassId = new QueryField(startClass, "id");
                        if (bag != null) {
                            csSubQuery.addConstraint(new BagConstraint(qfStartClassId,
                                                 ConstraintOp.IN, bag.getOsb()));
                        } else if (ids != null) {
                            // use list of IDs instead of bag
                            String[] idArray = ids.split(",");
                            Collection<Integer> idsCollection = new LinkedHashSet<Integer>();
                            for (String intermineId : idArray) {
                                try {
                                    idsCollection.add(Integer.valueOf(intermineId.trim()));
                                } catch (NumberFormatException e) {
                                    throw new RuntimeException("List of IDs contains invalid "
                                            + "integer: " + intermineId, e);
                                }
                            }
                            csSubQuery.addConstraint(new BagConstraint(qfStartClassId,
                                    ConstraintOp.IN, idsCollection));
                        }
                        QueryField outerQFConstraint = new QueryField(subQuery, qfConstraint);
                        cs.addConstraint(new SimpleConstraint(qfConstraint, ConstraintOp.EQUALS,
                                                              outerQFConstraint));
                        query.addFrom(subQuery);
                    }
                } else {
                    if (queryValue != null) {
                        if (!"null".equalsIgnoreCase(queryValue.getValue().toString())) {
                            QueryEvaluable qe = null;
                            if (isFilterConstraint || isListConstraint
                                || queryValue.getValue() instanceof Boolean) {
                                qe = qfConstraint;
                            } else {
                                qe = new QueryExpression(QueryExpression.LOWER, qfConstraint);
                            }
                            cs.addConstraint(new SimpleConstraint(qe, pc.getOp(), queryValue));
                        } else {
                            ConstraintOp op = (pc.getOp().equals(ConstraintOp.EQUALS))
                                              ? ConstraintOp.IS_NULL
                                              : ConstraintOp.IS_NOT_NULL;
                            cs.addConstraint(new SimpleConstraint(qfConstraint, op));
                        }
                    }
                }
            } else {
                QueryReference qr;
                try {
                    qr = new QueryObjectReference(qc,
                                               pathsConstraint[index]);
                    qcConstraint = new QueryClass(qr.getType());
                } catch (IllegalArgumentException e) {
                    // Not a reference - try collection instead
                    qr = new QueryCollectionReference(qc, pathsConstraint[index]);
                    qcConstraint = new QueryClass(TypeUtil.getElementType(
                        qc.getType(), pathsConstraint[index]));
                }
                String partialPath = createAttributePath(pathsConstraint, index);
                if (!queryClassInQuery.containsKey(partialPath)) {
                    qc = qcConstraint;
                    query.addFrom(qc);
                    cs.addConstraint(new ContainsConstraint(qr,
                            ConstraintOp.CONTAINS, qc));
                    if (isListConstraint && action.startsWith("population")) {
                        csSubQuery.addConstraint(new ContainsConstraint(qr,
                               ConstraintOp.CONTAINS, qc));
                    }
                    queryClassInQuery.put(partialPath, qc);
                } else {
                    qc = queryClassInQuery.get(partialPath);
                }
            }
        }
    }

    /**
     * @param calcTotal whether or not to calculate the total number of annotated objects in the
     * sample
     * @return the query representing the sample population (the list)
     */
    public Query getSampleQuery(boolean calcTotal) {
        String actionLocal = calcTotal ? "sampleTotal" : "sample";
        return getQuery(actionLocal, null);
    }

    /**
     * @param calcTotal whether or not to calculate the total number of annotated objects in the
     * database
     * @return the query representing the entire population (all the items in the database)
     */
    public Query getPopulationQuery(boolean calcTotal) {
        String actionLocal = calcTotal ? "populationTotal" : "population";
        return getQuery(actionLocal, null);
    }

    /**
     * @param keys the keys to the records to be exported
     * @return the query representing the records to be exported
     */
    public Query getExportQuery(List<String> keys) {
        return getQuery("export", keys);
    }
}
