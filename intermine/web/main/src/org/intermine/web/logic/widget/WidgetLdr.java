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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.metadata.ConstraintOp;
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
import org.intermine.metadata.TypeUtil;
import org.intermine.web.logic.widget.config.EnrichmentWidgetConfig;
import org.intermine.web.logic.widget.config.GraphWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfigUtil;

/**
 * The class that performs the actual queries for widgets.
 * @author Various Artists.
 *
 */
public class WidgetLdr
{

    protected ObjectStore os;
    protected InterMineBag bag;
    protected String ids;
    protected String filter;
    protected QueryClass startClass;
    private static final Logger LOG = Logger.getLogger(WidgetLdr.class);

    //map containing queryclass already in the query
    protected Map<String, QueryClass> queryClassInQuery = new HashMap<String, QueryClass>();

    /**
     * @param bag the intermine bag
     * @param os the object store
     * @param filter the filter
     * @param config The description of the widget.
     * @param ids intermine IDs to analyse. required if bag is null
     */
    public WidgetLdr(InterMineBag bag, ObjectStore os, String filter, WidgetConfig config,
        String ids) {
        this.bag = bag;
        this.os = os;
        this.filter = filter;
        this.ids = ids;
        try {
            startClass = new QueryClass(Class.forName(os.getModel().getPackageName() + "."
                                        + config.getStartClass()));
        } catch (ClassNotFoundException e) {
            if (config instanceof EnrichmentWidgetConfig
                || config instanceof GraphWidgetConfig) {
                throw new IllegalArgumentException("Not found the class set in startClass for the"
                    + " widget " + config.getId(), e);
            }
        }
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
            if (!NumberUtils.isNumber(value)) {
                queryValue = new QueryValue(value);
            } else {
                try {
                    queryValue = new QueryValue(Integer.parseInt(value));
                } catch (NumberFormatException nfe) {
                    queryValue = new QueryValue(Double.parseDouble(value));
                }
            }
        }
        return queryValue;
    }

    /**
     * Create a queryField starting from the path given in input and add the contain constraints
     * between all the queryclass composing the path.
     * If addToSelect is true, add the queryFiled as select, group by and order by element.
     * @param path the path used to create the queryField. The path doesn't containt the startClass
     * @param query the query to modify
     * @param addToSelect if true add the queryFiled as select, group by and order by element
     * @return the queryField created
     */
    protected QueryField createQueryFieldByPath(String path, Query query, boolean addToSelect) {
        QueryField queryField = null;
        String[] splittedPath = path.split("\\.");
        QueryClass qc = startClass;

        String attribute;
        String attributePath = "";
        for (int i = 0; i < splittedPath.length; i++) {
            attribute = splittedPath[i];
            if (i == splittedPath.length - 1) {
                queryField = new QueryField(qc, attribute);
                if (addToSelect) {
                    query.addToSelect(queryField);
                    query.addToGroupBy(queryField);
                    query.addToOrderBy(queryField);
                }
            } else {
                attributePath = createAttributePath(splittedPath, i);
                qc = addReference(query, qc, attribute, attributePath);
            }
        }
        return queryField;
    }

    /**
     * Add a contains constraint to Query (q) built with the query class and attribute given
     * in input
     * @param query The query to add a reference to.
     * @param qc The class the reference belongs to.
     * @param attribute the name of the field of the class.
     * @param attributePath Another similarly named field - I wish it had been documented!
     * @return The query class of the attributePath.
     */
    protected QueryClass addReference(
            final Query query,
            final QueryClass qc,
            String attribute,
            String attributePath) {
        ConstraintSet cs = (ConstraintSet) query.getConstraint();
        QueryReference qr = null;
        String type = "";
        boolean useSubClass = false;
        if (WidgetConfigUtil.isPathContainingSubClass(os.getModel(), attribute)) {
            useSubClass = true;
            type = attribute.substring(attribute.indexOf("[") + 1, attribute.indexOf("]"));
            attribute = attribute.substring(0, attribute.indexOf("["));
        }
        QueryClass qcTmp = null;
        try {
            qr = new QueryObjectReference(qc, attribute);
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
            qr = new QueryCollectionReference(qc, attribute);
            if (useSubClass) {
                try {
                    qcTmp = new QueryClass(Class.forName(os.getModel().getPackageName()
                                                      + "." + type));
                } catch (ClassNotFoundException cnfe) {
                    LOG.error("The type " + type + " doesn't exist in the model.");
                }
            } else {
                qcTmp = new QueryClass(TypeUtil.getElementType(qc.getType(), attribute));
            }
        }
        QueryClass ret;
        if (!queryClassInQuery.containsKey(attributePath)) {
            ret = qcTmp;
            query.addFrom(ret);
            cs.addConstraint(new ContainsConstraint(qr, ConstraintOp.CONTAINS, ret));
            queryClassInQuery.put(attributePath, ret);
        } else {
            ret = queryClassInQuery.get(attributePath);
        }
        return ret;
    }

    /**
     * Return the path of the attribute at the the position index in the paths given in input
     * without reference to subclass.
     * @param paths The paths to get from.
     * @param index The index of the path to get.
     * @return the path of the attribute at the the position index
     */
    protected String createAttributePath(String[] paths, int index) {
        String partialPath = startClass.getType().getSimpleName();
        String path;
        for (int partialPathIndex = 0; partialPathIndex <= index; partialPathIndex++) {
            path = paths[partialPathIndex];
            if (path.contains("[")) {
                path = path.substring(0, path.indexOf("["));
            }
            partialPath = partialPath + "." + path;
        }
        return partialPath;
    }

}
