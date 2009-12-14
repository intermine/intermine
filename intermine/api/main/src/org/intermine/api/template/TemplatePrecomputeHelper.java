package org.intermine.api.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.intermine.api.query.MainHelper;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCollectionPathExpression;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryObjectPathExpression;
import org.intermine.objectstore.query.QueryOrderable;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.PathNode;

public class TemplatePrecomputeHelper
{

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


    /**
     * Get an ObjectStore query to precompute this template - remove editable constraints
     * and add fields to select list if necessary.  Fill in indexes list with QueryNodes
     * to create additional indexes on (i.e. those added to select list).  Original
     * template is left unaltered.
     *
     * @param template to generate precompute query for
     * @param indexes any additional indexes to be created will be added to this list.
     * @param groupByNode a PathNode to group by, for summary data, or null for a precompute query
     * @return the query to precompute
     */
    public static Query getPrecomputeQuery(TemplateQuery template, List indexes,
            PathNode groupByNode) {
        // generate query with editable constraints removed
        TemplateQuery templateClone = template.cloneWithoutEditableConstraints();

        if (template.getBagNames().size() != 0) {
            throw new RuntimeException("Precomputed query can't be created "
                                       + "for a template with a list.");
        }

        List<String> indexPaths = new ArrayList<String>();
        // find nodes with editable constraints to index and possibly add to select list
        Iterator niter = template.getEditableNodes().iterator();
        while (niter.hasNext()) {
            PathNode node = (PathNode) niter.next();
            // look for editable constraints
            List ecs = template.getEditableConstraints(node);
            if (ecs != null && ecs.size() > 0) {
                // NOTE: at one point this exhibited a bug where aliases were repeated
                // in the generated query, seems to be fixed now though.
                Iterator ecsIter = ecs.iterator();
                // LOOKUP constraints already add the object (id) to the select list
                // so we don't want to add it again here.while (ecsIter.hasNext()) {
                Constraint c = (Constraint) ecsIter.next();
                String path = node.getPathString();
                if (!c.getOp().equals(ConstraintOp.LOOKUP)) {
                    if (!templateClone.viewContains(path)) {
                        templateClone.addView(path);
                    }
                    if (!indexPaths.contains(path)) {
                        indexPaths.add(path);
                    }
                }
            }
        }

        HashMap<String, QuerySelectable> pathToQueryNode = new HashMap();
        Query query = null;
        try {
            // we can get away with not passing in a BagQueryRunner and conversion templates here,
            // we know that templates cannot contain non-editable lookup constraints.
            query = MainHelper.makeQuery(templateClone, new HashMap(), pathToQueryNode, null,
                                         null, false);
        } catch (ObjectStoreException e) {
            // Not possible if last argument is null
        }
        if (groupByNode != null) {
            query.clearOrderBy();
            query.clearSelect();
            QueryNode qn = (QueryNode) pathToQueryNode.get(groupByNode.getPathString());
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
                int lastIndex = Math.max(path.lastIndexOf("."), path.lastIndexOf(":"));
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
                    lastIndex = Math.max(parentPath.lastIndexOf("."), parentPath.lastIndexOf(":"));
                }
                if (lastIndex == -1) {
                    query.addToSelect(pathToQueryNode.get(path));
                }
            }
            for (QueryOrderable qo : query.getOrderBy()) {
                if ((qo instanceof QuerySelectable) && (!query.getSelect().contains(qo))) {
                    query.addToSelect((QuerySelectable) qo);
                }
            }
            for (QuerySelectable qs : query.getSelect()) {
                if (qs instanceof QueryNode) {
                    indexes.add(qs);
                }
            }
        }
        return query;
    }
}
