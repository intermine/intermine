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

import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfigUtil;

public class WidgetLdr {

    protected ObjectStore os;
    protected InterMineBag bag;
    protected String filter;
    protected QueryClass startClass;
    private static final Logger LOG = Logger.getLogger(WidgetLdr.class);

    //map containing queryclass already in the query
    protected Map<String, QueryClass> queryClassInQuery;

    /**
     * Constructor by default
     */
    public WidgetLdr() {
    }

    /**
     * Constructor initializing intermine bag, the object store and the filter
     * @param bag the intermine bag
     * @param os the object store
     * @param filter the filter
     */
    public WidgetLdr(InterMineBag bag, ObjectStore os, String filter) {
        this.bag = bag;
        this.os = os;
        this.filter = filter;
    }

    /**
     * Return a QueryValue instance using the path contraint given in input
     * @param pc the pathconstraint used to build the queryvalue
     * @return the queryvalue created
     */
    protected QueryValue buildQueryValue(PathConstraint pc) {
        String value = PathConstraint.getValue(pc);
        QueryValue queryValue = null;
        if ("true".equalsIgnoreCase(value)) {
            queryValue = new QueryValue(true);
        } else if ("false".equalsIgnoreCase(value)) {
            queryValue = new QueryValue(false);
        } else {
            queryValue = new QueryValue(value);
        }
        return queryValue;
    }

    /**
     * Create a queryField starting from the path given in input and add the contain constraints
     * between all the queryclass composing the path.
     * If addToSelect is true, add the queryFiled as select, group by and order by element.
     * @param path the path used to create the queryField
     * @param query the query to modify
     * @param addToSelect if true add the queryFiled as select, group by and order by element
     * @return the queryField created
     */
    protected QueryField createQueryFieldByPath(String path, Query query, boolean addToSelect) {
        QueryField queryField = null;
        String[] paths = path.split("\\.");
        QueryClass qc = startClass;
        for (int i = 0; i < paths.length; i++) {
            if (i == paths.length - 1) {
                queryField = new QueryField(qc, paths[i]);
                if (addToSelect) {
                    query.addToSelect(queryField);
                    query.addToGroupBy(queryField);
                    query.addToOrderBy(queryField);
                }
            } else {
                qc = addReference(query, qc, paths[i]);
            }
        }
        return queryField;
    }

    /**
     * Add a contains constraint to Query (q) from qcStart using path
     */
    protected QueryClass addReference(Query query, QueryClass qc, String path) {
        ConstraintSet cs = (ConstraintSet) query.getConstraint();
        QueryReference qr = null;
        String type = "";
        boolean useSubClass = false;
        if (WidgetConfigUtil.isPathContainingSubClass(os.getModel(), path)) {
            useSubClass = true;
            type = path.substring(path.indexOf("[") + 1, path.indexOf("]"));
            path = path.substring(0, path.indexOf("["));
        }
        QueryClass qcTmp = null;
        try {
            qr = new QueryObjectReference(qc, path);
            if (useSubClass) {
                try {
                    qcTmp = new QueryClass(Class.forName(os.getModel().getPackageName()
                                                      + "." + type));
                } catch (ClassNotFoundException cnfe) {
                    LOG.error("The type " + type + " doesn't exist in the model.");
                }
            } else {
                qcTmp = new QueryClass(qr.getType());
            }
        } catch (IllegalArgumentException e) {
            // Not a reference - try collection instead
            qr = new QueryCollectionReference(qc, path);
            if (useSubClass) {
                try {
                    qcTmp = new QueryClass(Class.forName(os.getModel().getPackageName()
                                                      + "." + type));
                } catch (ClassNotFoundException cnfe) {
                    LOG.error("The type " + type + " doesn't exist in the model.");
                }
            } else {
                qcTmp = new QueryClass(TypeUtil.getElementType(qc.getType(), path));
            }
        }
        if (addQueryClassInQuery(qcTmp, qc)) {
            String key = generateKeyForQueryClassInQuery(qcTmp, qc);
            qc = qcTmp;
            query.addFrom(qc);
            cs.addConstraint(new ContainsConstraint(qr, ConstraintOp.CONTAINS, qc));
            queryClassInQuery.put(key, qc);
        } else {
            //retrieve qc from queryClassInQuery map
            String key = generateKeyForQueryClassInQuery(qcTmp, qc);
            qc = queryClassInQuery.get(key);
        }
        return qc;
    }

    /**
     * Verify if the queryClass having as a parent (parent in a path) the queryClassParent
     * given in input is already in the queryClassInQuery map
     * @param queryClass the query class
    * @param queryClassParent the query class parent in the path
     * @return true if the queryClass is in queryClassInQuery map, otherwise false
     */
    protected boolean addQueryClassInQuery(QueryClass queryClass, QueryClass queryClassParent) {
        String key = generateKeyForQueryClassInQuery(queryClass, queryClassParent);
        if (!queryClassInQuery.containsKey(key)) {
            return true;
        }
        return false;
    }

   /**
    * Generate the key used in the queryClassInQuery map
    * key= queryclass type + '_' + queryClass parent type(for root path is an empty string)
    * @param queryClass the query class
    * @param queryClassParent the query class parent in the path
    * @return the key generated
    */
    protected String generateKeyForQueryClassInQuery(QueryClass queryClass,
        QueryClass queryClassParent) {
        String queryClassType = queryClass.getType().getSimpleName();
        if (queryClassParent == null) {
            return queryClassType + "_";
        } else {
            return queryClassType + "_" + queryClassParent.getType().getSimpleName();
        }
    }

    /**
     * Create a pathquery having a view composed by all items set in the view attribute
     * in the config file
     * @param os th eobject store
     * @param config the widget config
     * @return the path query created
     */
    protected PathQuery createPathQueryView(ObjectStore os, WidgetConfig config) {
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
        return q;
    }
}
