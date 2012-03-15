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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.web.logic.widget.EnrichmentWidget;

/**
 * @author Julie Sullivan
 */
public class EnrichmentWidgetConfig extends WidgetConfig
{
    private String filterLabel, filters;
    private String label;
    private String externalLink, externalLinkLabel;
    private String append;
    private String enrich;
    private String startClassDisplay;
    private List<PathConstraint> pathConstraints = new ArrayList<PathConstraint>();
    private String constraints;

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
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


    /**
     * Return an XML String of this Type object
     * @return a String version of this WebConfig object
     */
    public String toString() {
        return "< title=\"" + getTitle() + "\" link=\"" + getLink() + "\" ldr=\""
               + getDataSetLoader() + "\"/>";
    }


    /**
     * {@inheritDoc}
     */
    public String getExternalLink() {
        return externalLink;
    }

    /**
     * {@inheritDoc}
     */
    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Collection<String>> getExtraAttributes(@SuppressWarnings("unused")
                                                              InterMineBag imBag,
                                                              @SuppressWarnings("unused")
                                                              ObjectStore os) {
        Map<String, Collection<String>> returnMap = new HashMap<String, Collection<String>>();
        if (getFilters() != null) {
            returnMap.put(getFilterLabel(), Arrays.asList(getFilters().split(",")));
        }
        return returnMap;
    }


    /**
     * just used for tiffin for now
     * @return the text to append to the end of the external link
     */
    public String getAppend() {
        return append;
    }

    /**
     * @param append the text to append
     */
    public void setAppend(String append) {
        this.append = append;
    }

    /**
     * @return the externalLinkLabel
     */
    public String getExternalLinkLabel() {
        return externalLinkLabel;
    }

    /**
     * @param externalLinkLabel the externalLinkLabel to set
     */
    public void setExternalLinkLabel(String externalLinkLabel) {
        this.externalLinkLabel = externalLinkLabel;
    }

    public String getEnrich() {
        return enrich;
    }

    public void setEnrich(String enrich) {
        this.enrich = enrich;
    }

    public String getStartClassDisplay() {
        return startClassDisplay;
    }

    public void setStartClassDisplay(String startClassDisplay) {
        this.startClassDisplay = startClassDisplay;
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
        String path, value;
        ConstraintOp op = null;
        //Constraint
        for (String constraint : constraintsList) {
            int opIndex = constraint.indexOf("!=");
            if (opIndex != -1) {
                op = ConstraintOp.NOT_EQUALS;
                value = constraint.substring(opIndex + 2);
            } else {
                opIndex = constraint.indexOf("=");
                value = constraint.substring(opIndex + 1);
                if (opIndex != -1) {
                    op = ConstraintOp.EQUALS;
                }
            }
            path = constraint.substring(0, opIndex);
            this.pathConstraints.add(new PathConstraintAttribute(path, op, value));
        }
    }

    public List<PathConstraint> getPathConstraints() {
        return pathConstraints;
    }

    /**
     * {@inheritDoc}
     */
    public EnrichmentWidget getWidget(InterMineBag imBag, ObjectStore os,
                                      List<String> attributes) {
        return new EnrichmentWidget(this, imBag, os, attributes.get(0), attributes
                        .get(1), attributes.get(2));
    }

}
