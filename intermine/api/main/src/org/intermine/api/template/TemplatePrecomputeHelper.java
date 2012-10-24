package org.intermine.api.template;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.query.MainHelper;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCollectionPathExpression;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryObjectPathExpression;
import org.intermine.objectstore.query.QueryOrderable;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathException;
import org.intermine.template.TemplateQuery;

/**
 * Helper class providing methods for precomputing and summarising TemplateQuery objects.
 *
 * @author Richard Smith
 */
public final class TemplatePrecomputeHelper
{
    private TemplatePrecomputeHelper() {
    }

    private static final Logger LOG = Logger.getLogger(TemplatePrecomputeHelper.class);

    /**
     * Get an ObjectStore query to precompute this template - remove editable constraints
     * and add fields to select list if necessary.  Fill in indexes list with QueryNodes
     * to create additional indexes on (i.e. those added to select list).  Original
     * template is left unaltered.
     *
     * @param template to generate precompute query for
     * @param indexes any additional indexes to be created will be added to this list.
     * @return the query to precompute
     */
    public static Query getPrecomputeQuery(TemplateQuery template, List indexes) {
        return TemplatePrecomputeHelper.getPrecomputeQuery(template, indexes, null);
    }

    public static Query getPrecomputeQuery(TemplateQuery template,
            List<? super QueryNode> indexes, String groupBy) {
        return getPrecomputeQuery(template, indexes, groupBy, null);
    }

    /**
     * Get an ObjectStore query to precompute this template - remove editable constraints
     * and add fields to select list if necessary.  Fill in indexes list with QueryNodes
     * to create additional indexes on (i.e. those added to select list).  Original
     * template is left unaltered.
     *
     * @param template to generate precompute query for
     * @param indexes any additional indexes to be created will be added to this list.
     * @param groupBy a path to group by, for summary data, or null for a precompute query
     * @return the query to precompute
     */
    public static Query getPrecomputeQuery(TemplateQuery template,
            List<? super QueryNode> indexes, String groupBy, InterMineAPI im) {
        // generate query with editable constraints removed
        TemplateQuery templateClone = template.cloneWithoutEditableConstraints();

        List<String> problems = templateClone.verifyQuery();
        if (!problems.isEmpty()) {
            throw new RuntimeException("Template query " + template + " does not validate: "
                    + problems);
        }
        for (PathConstraint constraint : templateClone.getConstraints().keySet()) {
            if (constraint instanceof PathConstraintBag) {
                throw new RuntimeException("Precomputed query can't be created "
                        + "for a template with a list.");
            }
        }

        List<String> indexPaths = new ArrayList<String>();
        // find nodes with editable constraints to index and possibly add to select list
        try {
            for (PathConstraint con : template.getEditableConstraints()) {
                Path conPath = templateClone.makePath(con.getPath());
                String conPathString;
                if (conPath.endIsAttribute()) {
                    conPathString = con.getPath();
                } else {
                    conPathString = con.getPath() + ".id";
                }
                if (!templateClone.getView().contains(conPathString)) {
                    templateClone.addView(conPathString);
                }
                indexPaths.add(conPathString);
            }
        } catch (PathException e) {
            // Should not happen if the query is valid
            throw new Error(e);
        }

        HashMap<String, QuerySelectable> pathToQueryNode = new HashMap<String, QuerySelectable>();
        Query query = null;
        try {
            // we can get away with not passing in a BagQueryRunner and conversion templates here,
            // we know that templates cannot contain non-editable lookup constraints.
            BagQueryRunner bagQueryRunner = (im != null) ? im.getBagQueryRunner() : null;
            query = MainHelper.makeQuery(templateClone, new HashMap<String, InterMineBag>(),
                    pathToQueryNode, bagQueryRunner, null);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Error getting precompute query for template "
                    + template.getName(), e);
        } catch (ObjectStoreException e) {
            // Not possible if last argument is null
            throw new Error("Error with template " + template, e);
        }
        if (groupBy != null) {
            query.clearOrderBy();
            query.clearSelect();
            QueryNode qn = (QueryNode) pathToQueryNode.get(groupBy);
            query.addToSelect(qn);
            // We don't actually need GROUP BY - just use DISTINCT instead.
            //query.addToGroupBy(qn);
            query.setDistinct(true);
        } else {
            // Queries only select objects, need to add editable constraints to select so they can
            // be indexed in precomputed table.  Create additional indexes for fields.
            Iterator<String> indexIter = indexPaths.iterator();
            while (indexIter.hasNext()) {
                String path = indexIter.next();
                int lastIndex = path.lastIndexOf(".");
                String parentPath = path;
                while (lastIndex != -1) {
                    parentPath = parentPath.substring(0, lastIndex);
                    QuerySelectable parentNode = pathToQueryNode.get(parentPath);
                    if (parentNode instanceof QueryObjectPathExpression) {
                        QueryObjectPathExpression qope = (QueryObjectPathExpression) parentNode;
                        if (qope.getSelect().isEmpty()) {
                            qope.addToSelect(qope.getDefaultClass());
                        }
                        qope.addToSelect(pathToQueryNode.get(path));
                        break;
                    } else if (parentNode instanceof QueryCollectionPathExpression) {
                        QueryCollectionPathExpression qcpe =
                            (QueryCollectionPathExpression) parentNode;
                        if (qcpe.getSelect().isEmpty()) {
                            qcpe.addToSelect(qcpe.getDefaultClass());
                        }
                        qcpe.addToSelect(pathToQueryNode.get(path));
                        break;
                    }
                    lastIndex = parentPath.lastIndexOf(".");
                }
                if (lastIndex == -1) {
                    if (!query.getSelect().contains(pathToQueryNode.get(path))) {
                        query.addToSelect(pathToQueryNode.get(path));
                    }
                }
            }
            for (QueryOrderable qo : query.getOrderBy()) {
                if ((qo instanceof QuerySelectable) && (!query.getSelect().contains(qo))) {
                    query.addToSelect((QuerySelectable) qo);
                }
            }
            for (QuerySelectable qs : query.getSelect()) {
                if (qs instanceof QueryNode) {
                    indexes.add((QueryNode) qs);
                }
            }
        }
        return query;
    }
}
