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
import java.math.MathContext;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.intermine.web.logic.SortableMap;



/**
 * See online help docs for detailed description of what error correction is and why we need it.
 * Briefly, in all experiments certain things happen that look interesting but really just
 * happened by chance.  We need to account for this phenomenon to ensure our numbers are
 * interesting behaviour and not just random happenstance.
 *
 * To do this we take all of our p-values and adjust them.  Here we are using on of our two
 * methods available - which one we use is determined by the user.
 * @author Julie Sullivan
 */
public final class ErrorCorrection
{
    protected static final BigDecimal ZERO = new BigDecimal(0);
    protected static final BigDecimal ONE = new BigDecimal(1);

    private ErrorCorrection() {
        // don't instatianiate
    }

    /**
     * @param testCount number of tests, eg. total number of go terms in database
     * @param results map of term to p-value
     * @param max maximum value to display, selected by user
     * @param errorCorrection which error correction to use
     * @return map containing adjusted p-values
     */
    public static Map<String, BigDecimal> adjustPValues(String errorCorrection,
            Map<String, BigDecimal> results, Double max, int testCount) {
        Map<String, BigDecimal> adjustedResults;

        if ("Bonferroni".equals(errorCorrection)) {
            adjustedResults = calculateBonferroni(results, testCount, max);
        } else if ("Benjamini Hochberg".equals(errorCorrection)) {
            adjustedResults = calculateBenjaminiHochberg(results, testCount, max);
        } else if ("Holm-Bonferroni".equals(errorCorrection)) {
            adjustedResults = calculateBonferroniHolm(results, testCount, max);
        } else {
            adjustedResults = calculate(results, max);
        }
        return sortMap(adjustedResults);
    }

    private static Map<String, BigDecimal> sortMap(Map<String, BigDecimal> originalMap) {
        SortableMap sortedMap = new SortableMap(originalMap);
        // sort ascending, smallest values first
        sortedMap.sortValues(false, true);
        return new LinkedHashMap(sortedMap);
    }

    /**
     * No test correction selected by user so only remove raw p-values if they are greater than
     * the maximum value allowed.
     *
     * @param maxValue maximum value to display
     */
    private static Map<String, BigDecimal> calculate(Map<String, BigDecimal> results,
            Double max) {
        Map<String, BigDecimal> adjustedMap = new HashMap<String, BigDecimal>();
        for (String id : results.keySet()) {
            BigDecimal pvalue = results.get(id);
            if (pvalue.doubleValue() <= max.doubleValue()) {
                adjustedMap.put(id, pvalue);
            }
        }
        return adjustedMap;
    }

    /**
     * See online documentation for an in depth description of error correction and bonferroni.
     * Briefly, the p-values are adjusted (multiple hypothesis test correction) by
     *
     *  adjusted p = p * number of tests
     *
     * For example, given 100 tests and an alpha value of .05, we would expect 5 false positives.
     *
     * @param numberOfTests maximum value to display
     */
    private static Map<String, BigDecimal> calculateBonferroni(Map<String, BigDecimal> results,
            int numberOfTests, Double max) {
        Map<String, BigDecimal> adjustedMap = new HashMap<String, BigDecimal>();
        for (Entry<String, BigDecimal> entry : results.entrySet()) {

            // get original values
            BigDecimal p = entry.getValue();

            // calc new value - p * N
            BigDecimal adjustedP = p.multiply(new BigDecimal(numberOfTests),
                    MathContext.DECIMAL128);

            // p is never over 1
            if (adjustedP.compareTo(ONE) >= 0) {
                adjustedP = ONE;
            }

            // don't store values > maxValue
            if (adjustedP.doubleValue() <= max.doubleValue()) {
                adjustedMap.put(entry.getKey(), adjustedP);
            }
        }
        return adjustedMap;
    }

    /**
     * This correction is the less stringent than the Bonferroni, and therefore tolerates more
     * false positives.
     *
     * Corrected p-value = p-value*(n/rank)
     *  *
     *  1) The p-values of each gene are ranked from the smallest to largest.
     *  2) The p-value is multiplied by the total number of tests divided by its rank.
     *
     * @param max maximum value to display
     */
    private static Map<String, BigDecimal> calculateBenjaminiHochberg(
            Map<String, BigDecimal> results, int numberOfTests, Double max) {

        Map<String, BigDecimal> adjustedResults = new HashMap<String, BigDecimal>();
        Map<String, BigDecimal> sortedResults = sortMap(results);

        BigDecimal lastValue = null;
        int i = 1;
        BigDecimal index = ONE;

        for (Entry<String, BigDecimal> entry : sortedResults.entrySet()) {

            BigDecimal p = entry.getValue();

            // if the p-value is not the same as previous, sync the rank
            if (lastValue == null || p.compareTo(lastValue) != 0) {
                index = new BigDecimal(i);
            }

            // n/rank
            BigDecimal m = new BigDecimal(numberOfTests).divide(index, MathContext.DECIMAL128);

            // p-value*(n/rank)
            BigDecimal adjustedP = p.multiply(m, MathContext.DECIMAL128);

            // p-value can't be over 1
            if (adjustedP.compareTo(ONE) > 0) {
                adjustedP = ONE;
            }

            // only report if value <= maximum
            if (adjustedP.doubleValue() <= max.doubleValue()) {
                adjustedResults.put(entry.getKey(), adjustedP);
            } else {
                // p-values are in ascending order, on first large number we can stop
                return adjustedResults;
            }

            // to compare if next value is the same
            lastValue = p;

            i++;
        }
        return adjustedResults;
    }

    /**
     * Calculates the Bonferroni and Holm correction of the false discovery rate.
     *
     * Adjusted p-value = p-value x (number of tests - rank)
     *
     * @param max maximum value we are interested in - used for display purposes only
     */
    private static Map<String, BigDecimal> calculateBonferroniHolm(Map<String, BigDecimal> results,
            int numberOfTests, Double max) {

        Map<String, BigDecimal> adjustedResults = new HashMap<String, BigDecimal>();
        Map<String, BigDecimal> sortedResults = sortMap(results);

        BigDecimal lastValue = null;
        // array index, always increments
        int i = 0;
        // rank, only increments if pvalue is unique
        BigDecimal rank = ZERO;

        // smallest to largest
        for (Entry<String, BigDecimal> entry : sortedResults.entrySet()) {

            BigDecimal p = entry.getValue();
            BigDecimal m = null;

            if (lastValue != null && p.compareTo(lastValue) != 0) {
                // if the p-value is not the same as previous, increment the rank
                rank = new BigDecimal(i);
            }

            // n - rank
            m = new BigDecimal(numberOfTests).subtract(rank, MathContext.DECIMAL128);

            // p-value*(n-rank)
            BigDecimal adjustedP = p.multiply(m, MathContext.DECIMAL128);

            // only report if value > maximum
            if (adjustedP.doubleValue() <= max.doubleValue()) {
                adjustedResults.put(entry.getKey(), adjustedP);
            } else {
                // p-values are ordered, so stop when we've gone too far
                return adjustedResults;
            }

            // to compare if next value is the same
            lastValue = p;

            i++;
        }
        return adjustedResults;
    }
}
