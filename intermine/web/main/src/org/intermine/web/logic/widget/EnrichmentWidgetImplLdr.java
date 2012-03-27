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
import java.util.Arrays;
import java.util.HashMap;
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
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.widget.config.EnrichmentWidgetConfig;

public class EnrichmentWidgetImplLdr extends EnrichmentWidgetLdr
{
    private ObjectStore os;
    private InterMineBag bag;
    private EnrichmentWidgetConfig config;
    private String filter;
    private QueryClass startClass;
    private Map<PathConstraint, Boolean> pathConstraintsProcessed =
        new HashMap<PathConstraint, Boolean>();
    private String action;

    public EnrichmentWidgetImplLdr(InterMineBag bag, ObjectStore os, EnrichmentWidgetConfig config,
        String filter) {
        this.bag = bag;
        this.os = os;
        this.config = config;
        this.filter = filter;
        try {
            startClass = new QueryClass(Class.forName(os.getModel().getPackageName() + "."
                                        + config.getStartClass()));
        } catch (ClassNotFoundException e) {
            return;
        }
        String[] typeClasses = config.getTypeClass().split("\\,");
        boolean bagTypeMatch = false;
        String packageName = os.getModel().getPackageName();
        for (String typeClass : typeClasses) {
            if (typeClass.equals(packageName + "." + bag.getType())) {
                bagTypeMatch = true;
                break;
            }
        }
        if (!bagTypeMatch) {
            return;
        }
    }

    public Query getQuery(String action, List<String> keys) {
        this.action = action;
        Query query = new Query();
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        query.setConstraint(cs);
        query.addFrom(startClass);
        for (PathConstraint pathConstraint : config.getPathConstraints()) {
            pathConstraintsProcessed.put(pathConstraint, false);
        }

        QueryField qfEnrich = null;
        QueryField qfEnrichId = null;
        if (config.getEnrich().contains(".")) {
            QueryField[] enrichmentQueryFields =
                createEnrichmentQueryFields(query);
            qfEnrich = enrichmentQueryFields[0];
            qfEnrichId = enrichmentQueryFields[1];
        } else {
            qfEnrich = new QueryField(startClass, config.getEnrich());
            String enrichIdentifier = config.getEnrichIdentifier();
            if (enrichIdentifier != null && !"".equals(enrichIdentifier)
                && !enrichIdentifier.equals(config.getEnrich())) {
                qfEnrichId = new QueryField(startClass, enrichIdentifier);
            } else {
                qfEnrichId = qfEnrich;
            }
        }

        if (keys != null) {
            cs.addConstraint(new BagConstraint(qfEnrichId, ConstraintOp.IN, keys));
        }

        QueryField qfStartClassId = new QueryField(startClass, "id");
        if (!action.startsWith("population")) {
            cs.addConstraint(new BagConstraint(qfStartClassId, ConstraintOp.IN, bag.getOsb()));
        }

        //add constraints not processed before
        for (PathConstraint pc : pathConstraintsProcessed.keySet()) {
            if (!pathConstraintsProcessed.get(pc)) {
                addConstraint(pc, query, startClass);
            }
        }

        query.setDistinct(true);
        Query subQ = query;
        Query mainQuery = new Query();
        mainQuery.setDistinct(false);

        QueryFunction qfCount = new QueryFunction();
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
        // enrichment queries
        } else {
            subQ.addToSelect(qfStartClassId);
            subQ.addToSelect(qfEnrichId);
            if (qfEnrichId != qfEnrich) {
                subQ.addToSelect(qfEnrich);
            }

            QueryField outerQfEnrichId = new QueryField(subQ, qfEnrichId);
            mainQuery.addFrom(subQ);
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
            }
        }
        return mainQuery;
    }

    private QueryField[] createEnrichmentQueryFields(Query query) {
        QueryField[] enrichmentQueryFields = new QueryField[2];
        String enrichPath = config.getEnrich();
        String enrichIdentifierPath = config.getEnrichIdentifier();
        String[] paths = enrichPath.split("\\.");
        QueryClass qc = startClass;
        ConstraintSet cs = (ConstraintSet) query.getConstraint();
        QueryField qf = null;
        for (int i = 0; i < paths.length; i++) {
            if (i == paths.length - 1) {
                qf = new QueryField(qc, paths[i]);
                enrichmentQueryFields[0] = qf;
                if (enrichIdentifierPath != null && !"".equals(enrichIdentifierPath)
                    && !enrichIdentifierPath.equals(enrichPath)) {
                    String[] pathArray = enrichIdentifierPath.split("\\.");
                    enrichmentQueryFields[1] = new QueryField(qc, pathArray[pathArray.length - 1]);
                } else {
                    enrichmentQueryFields[1] = qf;
                }
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
                if (i == 0) {
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
        }
        return enrichmentQueryFields;
    }

    private void addConstraint(PathConstraint pc, Query query, QueryClass qc) {
        boolean isListConstraint = isListConstraint(pc);
        boolean isFilterConstraint = isFilterConstraint(pc);
        QueryValue queryValue = null;
        if (!isFilterConstraint && !isListConstraint) {
            queryValue = buildQueryValue(pc);
        }
        if (isFilterConstraint && !"All".equalsIgnoreCase(filter)) {
            queryValue = new QueryValue(filter);
        }

        QueryField qfConstraint = null;
        QueryClass qcConstraint = null;
        ConstraintSet cs = (ConstraintSet) query.getConstraint();
        String[] pathsConstraint = pc.getPath().split("\\.");
        int limit = (qc == startClass) ? 0 : 1;
        //subQuery used only for population
        Query subQuery = new Query();
        ConstraintSet csSubQuery = new ConstraintSet(ConstraintOp.AND);
        subQuery.setConstraint(csSubQuery);

        for (int index = 0; index < pathsConstraint.length - limit; index++) {
            if (index == pathsConstraint.length - 1 - limit) {
                if (index == 0) {
                    qfConstraint = new QueryField(qc,
                        pathsConstraint[index + limit]);
                } else {
                    qfConstraint = new QueryField(qcConstraint,
                        pathsConstraint[index + limit]);
                }
                if (isListConstraint) {
                    if (action.startsWith("population")) {
                        subQuery.addToSelect(qfConstraint);
                        subQuery.addToOrderBy(qfConstraint);
                        subQuery.addFrom(startClass);
                        subQuery.addFrom(qcConstraint);
                        QueryField qfStartClassId = new QueryField(startClass, "id");
                        csSubQuery.addConstraint(new BagConstraint(qfStartClassId, ConstraintOp.IN, bag.getOsb()));
                        QueryField outerQFConstraint = new QueryField(subQuery, qfConstraint);
                        cs.addConstraint(new SimpleConstraint(qfConstraint, ConstraintOp.EQUALS, outerQFConstraint));
                        query.addFrom(subQuery);
                    }
                } else {
                    if (queryValue != null) {
                        cs.addConstraint(new SimpleConstraint(qfConstraint, pc.getOp(),
                                                              queryValue));
                    }
                }
                pathConstraintsProcessed.put(pc, true);
            } else {
                try {
                    QueryObjectReference qor = new QueryObjectReference(qc,
                                               pathsConstraint[index + limit]);
                    qcConstraint = new QueryClass(qor.getType());
                    query.addFrom(qcConstraint);
                    cs.addConstraint(new ContainsConstraint(qor,
                                     ConstraintOp.CONTAINS, qcConstraint));
                    if (isListConstraint && action.startsWith("population")) {
                        csSubQuery.addConstraint(new ContainsConstraint(qor,
                                ConstraintOp.CONTAINS, qcConstraint));
                    }
                    pathConstraintsProcessed.put(pc, true);
                } catch (IllegalArgumentException e) {
                    // Not a reference - try collection instead
                    QueryCollectionReference qcr =
                        new QueryCollectionReference(qc,
                            pathsConstraint[index + 1]);
                    qcConstraint = new QueryClass(TypeUtil.getElementType(
                        qc.getType(), pathsConstraint[index + limit]));
                    query.addFrom(qcConstraint);
                    cs.addConstraint(new ContainsConstraint(qcr,
                        ConstraintOp.CONTAINS, qcConstraint));
                    if (isListConstraint && action.startsWith("population")) {
                        csSubQuery.addConstraint(new ContainsConstraint(qcr,
                                ConstraintOp.CONTAINS, qcConstraint));
                    }
                    pathConstraintsProcessed.put(pc, true);
                }
            }
        }
    }

    private boolean isListConstraint(PathConstraint pc) {
        String value = PathConstraint.getValue(pc);
        value = value.replace(" ", "");
        if ("[list]".equals(value)) {
            return true;
        }
        return false;
    }

    private boolean isFilterConstraint(PathConstraint pc) {
        String value = PathConstraint.getValue(pc);
        value = value.replace(" ", "");
        if (value.equals("[" + config.getFilterLabel() + "]")) {
            return true;
        }
        return false;
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
        q.addConstraint(Constraints.in(startClass + ".id", bag.getName()));
        return q;
    }
}
