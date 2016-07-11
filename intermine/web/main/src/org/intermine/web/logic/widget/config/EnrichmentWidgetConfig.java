package org.intermine.web.logic.widget.config;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import org.intermine.web.logic.widget.EnrichmentOptions;
import org.intermine.web.logic.widget.EnrichmentWidget;
import org.intermine.web.logic.widget.WidgetOptions;

/**
 * A description of an enrichment calculation. An enrichment calculation analyses
 * a relationship between two items and provides a measure of relevance (a P-Value).
 *
 * an example enrichment widget config stanza looks like this:
 *
 * <pre>
 * &lt;enrichmentwidgetdisplayer
 *               id="colleague_enrichment"
 *               title="Colleague Enrichment"
 *               label="Colleague"
 *               description="The relationship between employees and their colleagues"
 *               startClass="Employee"
 *               startClassDisplay="name"
 *               enrich="department.employees.id"
 *               enrichIdentifier="department.employees.name"
 *               typeClass="Employee"
 *               views="name, age, department.name"/&gt;
 * </pre>
 * @author Julie Sullivan
 * @author Daniela Butano
 */
public class EnrichmentWidgetConfig extends WidgetConfig
{
    private String label;
    private String enrich;
    private String enrichIdentifier;
    private String startClassDisplay;
    private String externalLink;
    private String correctionCoefficient;
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

    @Override
    public Map<String, Collection<String>> getExtraAttributes(InterMineBag imBag,
                                                              ObjectStore os) {
        Map<String, Collection<String>> returnMap = new HashMap<String, Collection<String>>();
        if (getFilters() != null) {
            returnMap.put(getFilterLabel(), Arrays.asList(getFilters().split(",")));
        }
        return returnMap;
    }

    /**
     * The enrich property is the path to the value that should be enriched.
     * @return the value of the 'enrich' property
     **/
    public String getEnrich() {
        return enrich;
    }

    /**
     * @param enrich the value we should enrich.
     */
    public void setEnrich(String enrich) {
        this.enrich = enrich;
    }

    /**
     * @return the identifier which should be shown to users for each enriched item.
     */
    public String getEnrichIdentifier() {
        return enrichIdentifier;
    }

    /**
     * @param enrichIdentifier The new value of the enriched identifier.
     */
    public void setEnrichIdentifier(String enrichIdentifier) {
        this.enrichIdentifier = enrichIdentifier;
    }

    /**
     * @return A display string for the start class.
     */
    public String getStartClassDisplay() {
        return startClassDisplay;
    }

    /**
     * @param startClassDisplay the new value of the start class display name.
     */
    public void setStartClassDisplay(String startClassDisplay) {
        this.startClassDisplay = startClassDisplay;
    }

    /** @return the external link for this widget. **/
    public String getExternalLink() {
        return externalLink;
    }

    /** @param externalLink The value of the external link property. **/
    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
    }

    /** @param constraints The constraints for the view **/
    public void setConstraintsForView(String constraints) {
        setPathConstraints(constraints, pathConstraintsForView);
    }

    /** @return the path constraints for the view. **/
    public List<PathConstraint> getPathConstraintsForView() {
        return pathConstraintsForView;
    }

    /** @return the name of a class that can be used to correct P-Values in the results. **/
    public String getCorrectionCoefficient() {
        return correctionCoefficient;
    }

    /** @param correctionCoefficient The name of a class that implements CorrectionCoefficient **/
    public void setCorrectionCoefficient(String correctionCoefficient) {
        this.correctionCoefficient = correctionCoefficient;
    }

    @Override
    public EnrichmentWidget getWidget(InterMineBag imBag, InterMineBag populationBag,
                                      ObjectStore os, WidgetOptions options, String ids,
                                      String populationIds) {
        EnrichmentOptions eo = (EnrichmentOptions) options;
        return new EnrichmentWidget(this, imBag, populationBag, os, eo, ids, populationIds);
    }

}
