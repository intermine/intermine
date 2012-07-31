package org.intermine.web.logic.widget.config;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.PathConstraint;
import org.intermine.web.logic.widget.EnrichmentWidget;

/**
 * @author Julie Sullivan
 * @author dbutano
 */
public class EnrichmentWidgetConfig extends WidgetConfig
{
    private String label;
    private String enrich;
    private String enrichIdentifier;
    private String startClassDisplay;
    private String externalLink;
    private List<PathConstraint> pathConstraintsForView = new ArrayList<PathConstraint>();

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
     * Return an XML String of this Type object
     * @return a String version of this WebConfig object
     */
    public String toString() {
        return "< title=\"" + getTitle() + "/>";
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

    public String getEnrich() {
        return enrich;
    }

    public void setEnrich(String enrich) {
        this.enrich = enrich;
    }

    public String getEnrichIdentifier() {
        return enrichIdentifier;
    }

    public void setEnrichIdentifier(String enrichIdentifier) {
        this.enrichIdentifier = enrichIdentifier;
    }

    public String getStartClassDisplay() {
        return startClassDisplay;
    }

    public void setStartClassDisplay(String startClassDisplay) {
        this.startClassDisplay = startClassDisplay;
    }

    public String getExternalLink() {
        return externalLink;
    }

    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
    }

    public void setConstraintsForView(String constraints) {
        setPathConstraints(constraints, pathConstraintsForView);
    }

    public List<PathConstraint> getPathConstraintsForView() {
        return pathConstraintsForView;
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
