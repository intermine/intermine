package org.intermine.web.logic.widget.config;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.widget.Widget;


/**
 * Class representing a Widget Configuration
 * @author "Xavier Watkins"
 */
public abstract class WidgetConfig
{
    private String id;
    private String description;
    private String title;
    private String filterLabel, filters;
    private String startClass;
    private List<PathConstraint> pathConstraints = new ArrayList<PathConstraint>();
    private String typeClass;
    private String views;

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the filters as set in the config file
     */
    public String getFilters() {
        return filters;
    }

    /**
     * @return the filter values
     */
    public String getFiltersValues(ObjectStore os, InterMineBag bag) {
        if (filters == null) {
            return null;
        }
        if (!filters.contains("[list]")) {
            return filters;
        } else {
            String filterPath = filters.substring(0, filters.indexOf("=")).trim();
            Query q = new Query();
            ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
            q.setConstraint(cs);

            Model model = os.getModel();
            QueryClass startClassQueryClass;
            try {
                startClassQueryClass = new QueryClass(Class.forName(model.getPackageName()
                    + "." + startClass));
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Class not found " + bag.getType(), e);
            }
            q.addFrom(startClassQueryClass);

            QueryField qfFilter;
            QueryClass qc = startClassQueryClass;
            String[] paths = filterPath.split("\\.");
            for (int i = 0; i < paths.length; i++) {
                if (i == paths.length - 1) {
                    qfFilter = new QueryField(qc, paths[i]);
                    q.addToSelect(qfFilter);
                    q.addToOrderBy(qfFilter);
                } else {
                    try {
                        QueryObjectReference qor = new QueryObjectReference(qc, paths[i]);
                        qc = new QueryClass(qor.getType());
                        q.addFrom(qc);
                        cs.addConstraint(new ContainsConstraint(qor, ConstraintOp.CONTAINS,
                                    qc));
                    } catch (IllegalArgumentException e) {
                        // Not a reference - try collection instead
                        QueryCollectionReference qcr = new QueryCollectionReference(qc,
                                paths[i]);
                        qc = new QueryClass(TypeUtil.getElementType(qc.getType(), paths[i]));
                        q.addFrom(qc);
                        cs.addConstraint(new ContainsConstraint(qcr, ConstraintOp.CONTAINS,
                                    qc));
                    }
                }
            }
            QueryField qfGeneId = new QueryField(startClassQueryClass, "id");
            BagConstraint bc = new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb());
            cs.addConstraint(bc);

            Results r = os.execute(q);
            Iterator<ResultsRow> it = (Iterator) r.iterator();
            StringBuffer filterValuesFromDB = new StringBuffer();
            while (it.hasNext()) {
                ResultsRow rr = it.next();
                Object org =  rr.get(0);
                if (org != null) {
                    filterValuesFromDB.append(org.toString() + ",");
                }
            }
            return filterValuesFromDB.substring(0, filterValuesFromDB.length() - 1);
        }
    }


    /**
     * @param filters the filters to set
     */
    public void setFilters(String filters) {
        this.filters = filters;
    }

    /**
     * @return the label for the filters
     */
    public String getFilterLabel() {
        return filterLabel;
    }

    /**
     * @param filterLabel the label for the filters
     */
    public void setFilterLabel(String filterLabel) {
        this.filterLabel = filterLabel;
    }

    public String getStartClass() {
        return startClass;
    }

    public void setStartClass(String startClass) {
        this.startClass = startClass;
    }

    /**
     * @param imBag the InterMineBag
     * @param os the ObjectStore
     * @return the getExtraAttributes
     * @exception Exception if something goes wrong
     */
    public abstract Map<String, Collection<String>> getExtraAttributes(InterMineBag imBag,
            ObjectStore os) throws Exception;

    /**
     * @return the typeClass
     */
    public String getTypeClass() {
        return typeClass;
    }

    /**
     * @param typeClass the typeClass to set
     */
    public void setTypeClass(String typeClass) {
        this.typeClass = typeClass;
    }

    public String getViews() {
        return views;
    }

    public void setViews(String views) {
        this.views = views;
    }

    public void setConstraints(String constraints) {
        setPathConstraints(constraints, pathConstraints);
    }

    protected void setPathConstraints(String constraints, List<PathConstraint> pathConstraints) {
        String[] constraintsList = constraints.split("\\s*,\\s*");
        String path = null;
        String value = null;
        ConstraintOp op = null;
        String[] splitConstraint;
        for (String constraint : constraintsList) {
            int opIndex = constraint.indexOf("!=");
            if (opIndex != -1) {
                op = ConstraintOp.NOT_EQUALS;
                splitConstraint = constraint.split("\\s*!=\\s*");
                path = splitConstraint[0];
                value = splitConstraint[1];
            } else {
                opIndex = constraint.indexOf("=");
                if (opIndex != -1) {
                    op = ConstraintOp.EQUALS;
                    splitConstraint = constraint.split("\\s*=\\s*");
                    path = splitConstraint[0];
                    value = splitConstraint[1];
                }
            }
            pathConstraints.add(new PathConstraintAttribute(path, op, value));
        }
    }

    public List<PathConstraint> getPathConstraints() {
        return pathConstraints;
    }

    /**
     * @param imBag the bag for this widget
     * @param os objectstore
     * @param attributes extra attribute - like organism
     * @return the widget
     */
    public abstract Widget getWidget(InterMineBag imBag, ObjectStore os,
                                     List<String> attributes);


}
