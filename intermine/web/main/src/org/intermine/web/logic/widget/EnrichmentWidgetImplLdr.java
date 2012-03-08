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
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.PathConstraint;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.widget.config.EnrichmentWidgetConfig;

public class EnrichmentWidgetImplLdr extends EnrichmentWidgetLdr
{
    private ObjectStore os;
    private InterMineBag bag;
    private EnrichmentWidgetConfig config;

    public EnrichmentWidgetImplLdr(InterMineBag bag, ObjectStore os, EnrichmentWidgetConfig config,
        String extraAttribute) {
        this.bag = bag;
        this.os = os;
        this.config = config;
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
        Query query = new Query();
        Model model = os.getModel();
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        query.setConstraint(cs);

        QueryClass startClass = null;
        try {
            startClass = new QueryClass(Class.forName(model.getPackageName() + "."
                                        + config.getStartClass()));
        } catch (ClassNotFoundException e) {
            return null;
        }
        query.addFrom(startClass);
        QueryField qfStartClassId = new QueryField(startClass, "id");

        QueryField qfEnrich = null;
        QueryField qfEnrichId = null;
        if (config.getEnrich().contains(".")) {
            QueryField[] enrichmentQueryFields =
                createEnrichmentQueryFields(model, query, startClass);
            qfEnrich = enrichmentQueryFields[0];
            qfEnrichId = enrichmentQueryFields[1];
        } else {
            qfEnrich = new QueryField(startClass, config.getEnrich());
            qfEnrichId = new QueryField(startClass, "id");
        }

        if (keys != null) {
            cs.addConstraint(new BagConstraint(qfEnrich, ConstraintOp.IN, keys));
        }

        if (!action.startsWith("population")) {
            cs.addConstraint(new BagConstraint(qfStartClassId, ConstraintOp.IN, bag.getOsb()));
        }

        query.setDistinct(true);
        Query subQ = query;
        Query mainQuery = new Query();
        mainQuery.setDistinct(false);

        QueryFunction qfCount = new QueryFunction();
        // which columns to return when the user clicks on 'export'
        if ("export".equals(action)) {
            subQ.addToSelect(qfEnrich);
            subQ.addToSelect(new QueryField(startClass, config.getStartClassDisplay()));
            subQ.addToOrderBy(qfEnrich);
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
            subQ.addToSelect(qfEnrichId);
            subQ.addToSelect(qfStartClassId);
            subQ.addToSelect(qfEnrich);

            QueryField outerQfTerm = new QueryField(subQ, qfEnrich);
            mainQuery.addFrom(subQ);
            mainQuery.addToSelect(outerQfTerm);
            mainQuery.addToGroupBy(outerQfTerm);
            mainQuery.addToSelect(qfCount);
            if ("sample".equals(action)) {
                mainQuery.addToSelect(outerQfTerm);
            }
        }
        return mainQuery;
    }

    private QueryField[] createEnrichmentQueryFields(Model model, Query query, QueryClass startQueryClass) {
        QueryField[] enrichmentQueryFields = new QueryField[2];
        String enrichPath = config.getEnrich();
        String[] paths = enrichPath.split("\\.");
        QueryClass qc = startQueryClass;
        ConstraintSet cs = (ConstraintSet) query.getConstraint();
        QueryField qf = null;
        QueryField qfConstraint = null;
        for (int i = 0; i < paths.length; i++) {
            if (i == paths.length - 1) {
                qf = new QueryField(qc, paths[i]);
                enrichmentQueryFields[0] = qf;
                enrichmentQueryFields[1] = new QueryField(qc, "id");
                /*query.addToSelect(qf);*/
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
                //check if there are any constraints starting with the same queryClass qc
                    for (PathConstraint pc : config.getPathConstraints()) {
                        String path = pc.getPath();
                        String[] pathsConstraint = path.split("\\.");
                        if (pathsConstraint[0].equals(paths[0])) {
                            String value = PathConstraint.getValue(pc);
                            QueryValue queryValue = null;
                            if ("true".equals(value)) {
                                queryValue = new QueryValue(new Boolean(true));
                            } else if ("false".equals(value)) {
                                queryValue = new QueryValue(new Boolean(true));
                            } else {
                                queryValue = new QueryValue(value);
                            }
                            QueryClass qcConstraint = null;
                            for (int index = 0; index < pathsConstraint.length - 1; index++) {
                                if (index == pathsConstraint.length - 2) {
                                    if (index == 0) {
                                        qfConstraint = new QueryField(qc,
                                            pathsConstraint[index + 1]);
                                    } else {
                                        qfConstraint = new QueryField(qcConstraint,
                                            pathsConstraint[index + 1]);
                                    }
                                    cs.addConstraint(new SimpleConstraint(qfConstraint, pc.getOp(),
                                                                          queryValue));
                                } else {
                                    try {
                                        QueryObjectReference qor = new QueryObjectReference(qc,
                                                                   pathsConstraint[index + 1]);
                                        qcConstraint = new QueryClass(qor.getType());
                                        query.addFrom(qcConstraint);
                                        cs.addConstraint(new ContainsConstraint(qor,
                                                         ConstraintOp.CONTAINS, qcConstraint));
                                    } catch (IllegalArgumentException e) {
                                        // Not a reference - try collection instead
                                        QueryCollectionReference qcr = new QueryCollectionReference(qc,
                                                                       pathsConstraint[index + 1]);
                                        qcConstraint = new QueryClass(TypeUtil.getElementType(qc.getType(),
                                                            pathsConstraint[index + 1]));
                                        query.addFrom(qcConstraint);
                                        cs.addConstraint(new ContainsConstraint(qcr, ConstraintOp.CONTAINS,
                                                                                qcConstraint));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return enrichmentQueryFields;
    }
}
