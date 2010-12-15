package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2010 FlyMine
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
 * false positives. There will be also less false negative genes. Here is how it works:
 *  1) The p-values of each gene are ranked from the largest to the smallest.
 *  2) The largest p-value remains as it is.
 *  3) The second largest p-value is multiplied by the total number of genes in gene
 *      list divided by its rank. If less than 0.05, it is significant.
 *      Corrected p-value = p-value*(n/n-1)
 *  4) The third p-value is multiplied as in step 3:
 *      Corrected p-value = p-value*(n/n-2)
 * And so on.
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
     */
    @SuppressWarnings("unchecked")
    public BenjaminiHochberg(HashMap<String, BigDecimal> originalMap) {
        numberOfTests = new BigDecimal(originalMap.size());
        SortableMap sortedMap = new SortableMap(originalMap);
        // sort descending, largest values first
        sortedMap.sortValues(false, false);
        this.originalMap = new LinkedHashMap(sortedMap);
    }

    /**
     * Calculates the Benjamini and Hochberg correction of the false discovery rate
     * @param max maximum value we are interested in - used for display purposes only
     */
    @SuppressWarnings("unchecked")
    public void calculate(Double max) {
        adjustedMap = new HashMap();
        BigDecimal index = numberOfTests;
        BigDecimal lastValue = null;

        // largest to smallest
        for (Map.Entry<String, BigDecimal> entry : originalMap.entrySet()) {

            String label = entry.getKey();
            BigDecimal p = entry.getValue();
            BigDecimal adjustedP = p;

            if (index.equals(numberOfTests)) {
                // p is never over 1
                if (adjustedP.compareTo(ONE) >= 0) {
                    adjustedP = ONE;
                }

                // only report if value > maximum
                if (adjustedP.doubleValue() <= max.doubleValue()) {
                    adjustedMap.put(label, adjustedP);
                }

                // decrease index
                index = index.subtract(ONE);

                // largest item is not updated
                continue;
            }

            // n - 1
            BigDecimal divisor = null;
            if (lastValue != null && p.compareTo(lastValue) == 0) {
                // last p-value was the same as this one, use the previous index value
                divisor = numberOfTests.subtract(index.add(ONE));
            } else {
                divisor = numberOfTests.subtract(index);
            }

            // n/(n-1)
            BigDecimal m = numberOfTests.divide(divisor, MathContext.DECIMAL128);

            // n/(n-1) * p
            adjustedP = p.multiply(m, MathContext.DECIMAL128);

            // p is never over 1
            if (adjustedP.compareTo(ONE) >= 0) {
                adjustedP = ONE;
            }

            // only report if value > maximum
            if (adjustedP.doubleValue() <= max.doubleValue()) {
                adjustedMap.put(label, adjustedP);
            }

            // to compare if next value is the same
            lastValue = p;

            // decrease i
            index = index.subtract(ONE);
        }
    }

    /**
     * @return adjusted map
     */
    public HashMap<String, BigDecimal> getAdjustedMap() {
        return adjustedMap;
    }

}


