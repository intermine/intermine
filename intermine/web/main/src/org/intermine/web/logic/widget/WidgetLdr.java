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
import java.util.Map;

import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.widget.config.GraphWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfigUtil;

public class WidgetLdr {

    protected ObjectStore os;
    protected InterMineBag bag;
    protected String filter;
    protected QueryClass startClass;
    protected Map<PathConstraint, Boolean> pathConstraintsProcessed =
        new HashMap<PathConstraint, Boolean>();

    public WidgetLdr() {
    }

    public WidgetLdr(InterMineBag bag, ObjectStore os, String filter) {
        this.bag = bag;
        this.os = os;
        this.filter = filter;
    }
    protected QueryValue buildQueryValue(PathConstraint pc) {
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
        try {
            qr = new QueryObjectReference(qc, path);
            if (useSubClass) {
                try {
                    qc = new QueryClass(Class.forName(os.getModel().getPackageName()
                                                      + "." + type));
                } catch (ClassNotFoundException cnfe) {
                    qc = new QueryClass(qr.getType());
                }
            } else {
                qc = new QueryClass(qr.getType());
            }
        } catch (IllegalArgumentException e) {
            // Not a reference - try collection instead
            qr = new QueryCollectionReference(qc, path);
            if (useSubClass) {
                try {
                    qc = new QueryClass(Class.forName(os.getModel().getPackageName()
                                                      + "." + type));
                } catch (ClassNotFoundException cnfe) {
                    qc = new QueryClass(TypeUtil.getElementType(qc.getType(), path));
                }
            } else {
                qc = new QueryClass(TypeUtil.getElementType(qc.getType(), path));
            }
        }
        query.addFrom(qc);
        cs.addConstraint(new ContainsConstraint(qr, ConstraintOp.CONTAINS, qc));
        return qc;
    }
}
