package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.math.BigDecimal;
import java.util.Map;

/**
 * A container for results from an Enrichment analysis.  An object of this class should contain
 * all information required to display the results of one enrichment calculation.
 *
 * @author Richard Smith
 */
public class EnrichmentResults
{

    private final Map<String, BigDecimal> pValues;
    private final Map<String, Integer> counts;
    private final Map<String, String> labels;
    private final Map<String, PopulationInfo> populationAnnotations;
    private final int analysedTotal, populationTotal;

    /**
     * Construct with pre-populated maps from enriched attributes to p-values, counts and labels.
     * @param pValues calculated p-values per attribute value for this enrichment
     * @param counts number of items in sample per attribute value
     * @param labels an additional label for each attribute value, used for display
     * @param analysedTotal the number of items in the sample that had data for the given attribute
     * @param populationAnnotations per GO term, number of genes total in database / background
     * population annotated
     * @param populationTotal total size of the population, e.g. count of genes in the database
     * (or background population)
     */
    protected EnrichmentResults(Map<String, BigDecimal> pValues, Map<String, Integer> counts,
            Map<String, String> labels, int analysedTotal,
            Map<String, PopulationInfo> populationAnnotations, int populationTotal) {
        this.pValues = pValues;
        this.counts = counts;
        this.labels = labels;
        this.analysedTotal = analysedTotal;
        this.populationAnnotations = populationAnnotations;
        this.populationTotal = populationTotal;
    }

    /**
     * Get the probability of enrichment for each attribute value found in the sample.  These may
     * have already undergone error correction.
     * @return a map from attribute value to p-value
     */
    public Map<String, BigDecimal> getPValues() {
        return pValues;
    }

    /**
     * Get the count of each attribute value found in the sample.
     * @return counts of attribute values in the sample.
     */
    public Map<String, Integer> getCounts() {
        return counts;
    }

    /**
     * Get additional display labels for attribute values in the enrichment analysis, these may be
     * names where the values analysed were identifiers.
     * @return labels for each attribute value in the sample
     */
    public Map<String, String> getLabels() {
        return labels;
    }

    /**
     * Get the count of of each attribute value in the population, e.g. number of total genes
     * annotated per GO term.
     * @return labels for each attribute value in the sample
     */
    public Map<String, PopulationInfo> getPopulationAnnotations() {
        return populationAnnotations;
    }

    // this is the number of objects in the bag that had data
    /**
     * Get the number of items in the sample that had an attribute value.  For example if performing
     * an enrichment on departments present in companies this would return the number of companies
     * in the sample that had any departments at all.
     * @return the number of items in the sample with attribute values
     */
    public int getAnalysedTotal() {
        return analysedTotal;
    }

    // this is the number of objects in the database that had data
    /**
     * Get the number of items in the database (or background population) that were annotated.
     *
     * NOTE: This is NOT the count of items in the database. We only include genes that have ANY
     * GO terms because we have a lot of garbage genes.
     *
     * @return the number of items in the population that were annotated with ANY value
     */
    public int getPopulationTotal() {
        return populationTotal;
    }

}
