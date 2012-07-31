package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math.distribution.HypergeometricDistributionImpl;

/**
 * Calculate enrichment of an attribute applied to members of a sample that is a subset of a larger
 * population.  The result is a p-value per attribute that represents the probability that the
 * number of occurrences of the attribute in the sample happened by chance based on the number of
 * occurrences in the population as a whole.  Thus a low p-value indicates that the attribute may
 * be characteristic of the items in the sample.
 *
 * Enrichment is implemented using a Hypergeometric test.
 *
 * @author Julie Sullivan
 * @author Richard Smith
 */
public final class EnrichmentCalculation
{
    private EnrichmentCalculation() {
    }

    /**
     * Perform an enrichment calculation based on input from some source and return results that
     * include a p-value and count for each attribute that was observed in the sample.  Also perform
     * the type multiple hypothesis error correction specified before returning the results.
     * @param input details of the sample and population
     * @param maxValue the maximum p-value to return, for display purposes
     * @param errorCorrection the type of error correction to perform or None
     * @return results of the enrichment calculation
     */
    public static EnrichmentResults calculate(EnrichmentInput input, Double maxValue,
            String errorCorrection) {

        int sampleSize = input.getSampleSize();
        int populationSize = input.getPopulationSize();

        Map<String, Integer> sampleCounts = input.getAnnotatedCountsInSample();
        Map<String, Integer> populationCounts = input.getAnnotatedCountsInPopulation();

        Map<String, BigDecimal> rawResults = new HashMap<String, BigDecimal>();
        for (Map.Entry<String, Integer> entry : sampleCounts.entrySet()) {
            String attribute = entry.getKey();

            Integer sampleCount = entry.getValue();
            Integer populationCount = populationCounts.get(attribute);

            HypergeometricDistributionImpl h =
                new HypergeometricDistributionImpl(populationSize, populationCount, sampleSize);
            Double pValue = h.upperCumulativeProbability(sampleCount);
            rawResults.put(attribute, new BigDecimal(pValue));
        }

        Map<String, BigDecimal> correctedResults = ErrorCorrection.adjustPValues(errorCorrection,
                rawResults, maxValue, input.getTestCount());

        // record the number of items in the sample that had any values for the attribute
        int widgetTotal = rawResults.isEmpty() ? 0 : sampleSize;

        EnrichmentResults results = new EnrichmentResults(correctedResults,
                input.getAnnotatedCountsInSample(), input.getLabels(), widgetTotal);

        return results;
    }
}

