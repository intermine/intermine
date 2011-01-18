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

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.intermine.web.logic.SortableMap;

/**
 * This correction is the less stringent than the Bonferroni, and therefore tolerates more
 * false positives.
 *
 * Corrected p-value = p-value*(n/rank)
 *  *
 *  1) The p-values of each gene are ranked from the smallest to largest.
 *  2) The p-value is multiplied by the total number of tests divided by its rank.
 *
 * @author Julie Sullivan
 */
public class BenjaminiHochberg implements ErrorCorrection
{
    private LinkedHashMap<String, BigDecimal> originalMap;
    private HashMap<String, BigDecimal> adjustedMap;
    private BigDecimal numberOfTests;
    private static final BigDecimal ONE = new BigDecimal(1);

    /**
    * @param originalMap HashMap of go terms and their p-value
    * @param testCount number of tests
     */
    public BenjaminiHochberg(HashMap<String, BigDecimal> originalMap, int testCount) {
        numberOfTests = new BigDecimal(testCount);
        SortableMap sortedMap = new SortableMap(originalMap);
        // sort ascending, smallest values first
        sortedMap.sortValues(false, true);
        this.originalMap = new LinkedHashMap(sortedMap);
    }

    /**
     * Calculates the Benjamini and Hochberg correction of the false discovery rate.
     *
     * @param max maximum value we are interested in - used for display purposes only
     */
    public void calculate(Double max) {
        adjustedMap = new HashMap();
        BigDecimal lastValue = null;
        int i = 1;
        BigDecimal index = ONE;

        // smallest to largest
        for (Map.Entry<String, BigDecimal> entry : originalMap.entrySet()) {

            BigDecimal p = entry.getValue();

            // if the p-value is not the same as previous, sync the rank
            if (lastValue == null || p.compareTo(lastValue) != 0) {
                index = new BigDecimal(i);
            }

            // n/rank
            BigDecimal m = numberOfTests.divide(index, MathContext.DECIMAL128);

            // p-value*(n/rank)
            BigDecimal adjustedP = p.multiply(m, MathContext.DECIMAL128);

            // p-value can't be over 1
            if (adjustedP.compareTo(ONE) > 0) {
                adjustedP = ONE;
            }

            // only report if value <= maximum
            if (adjustedP.doubleValue() <= max.doubleValue()) {
                adjustedMap.put(entry.getKey(), adjustedP);
            } else {
                // p-values are in ascending order, on first large number we can stop
                return;
            }

            // to compare if next value is the same
            lastValue = p;

            i++;
        }
    }

    /**
     * @return adjusted map
     */
    public HashMap<String, BigDecimal> getAdjustedMap() {
        return adjustedMap;
    }
}
