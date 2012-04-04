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
import java.util.List;
import java.util.Map;

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
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
    private String constraints;
    private List<PathConstraint> pathConstraints = new ArrayList<PathConstraint>();
    private String typeClass;
    private String views;

    /**
     * The Constructor
     */
    public WidgetConfig() {
        super();
    }

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
     * @return the filters
     */
    public String getFilters() {
        return filters;
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

    public String getConstraints() {
        return constraints;
    }

    public void setConstraints(String constraints) {
        this.constraints = constraints;
        setPathConstraints();
    }

    public void setPathConstraints() {
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
            this.pathConstraints.add(new PathConstraintAttribute(path, op, value));
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
