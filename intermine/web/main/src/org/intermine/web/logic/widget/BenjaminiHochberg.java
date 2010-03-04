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
 *  1) The p-values of each gene are ranked from the smallest to the largest.
 *  2) The largest p-value remains as it is.
 *  3) The second largest p-value is multiplied by the total number of genes in gene
 *      list divided by its rank. If less than 0.05, it is significant.
 *      Corrected p-value = p-value*(n/n-1) < 0.05, if so, gene is significant.
 *  4) The third p-value is multiplied as in step 3:
 *      Corrected p-value = p-value*(n/n-2) < 0.05, if so, gene is significant.
 * And so on.
 *
 * @author Julie Sullivan
 */
public class BenjaminiHochberg implements ErrorCorrection
{
    private LinkedHashMap<String, BigDecimal> originalMap;
    private HashMap<String, BigDecimal> adjustedMap;
    private int numberOfTests;

    /**
    * @param originalMap HashMap of go terms and their p-value
     */
    @SuppressWarnings("unchecked")
    public BenjaminiHochberg(HashMap<String, BigDecimal> originalMap) {
        numberOfTests = originalMap.size();
        SortableMap sortedMap = new SortableMap(originalMap);
        // sort ascending
        sortedMap.sortValues(false, true);
        this.originalMap = new LinkedHashMap(sortedMap);
    }

    /**
     * Calculates the Benjamini and Hochberg correction of the false discovery rate
     * @param max maximum value we are interested in - used for display purposes only
     */
    @SuppressWarnings("unchecked")
    public void calculate(Double max) {
        adjustedMap = new HashMap();

        int index = 0;
        BigDecimal lastValue = new BigDecimal(-1);

        for (Map.Entry<String, BigDecimal> entry : originalMap.entrySet()) {

            String label = entry.getKey();
            BigDecimal p = entry.getValue();
            BigDecimal adjustedP = p;
            
            /**
             * equivalent p-values should have the same adjusted p-value.
             *
             * only increase the index if this value is different from the one we saw on the
             * previous line
             */
            if (!p.equals(lastValue)) {
                // new value, so increase index
                index++;
            }

            // largest value is not adjusted
            if (index < numberOfTests - 1) {
                // p-value * (n/ n - index)
                BigDecimal n = new BigDecimal(numberOfTests);
                BigDecimal divisor = n.subtract(new BigDecimal(index));
                BigDecimal m = n.divide(divisor, MathContext.DECIMAL128);
                adjustedP = p.multiply(m);
            }

            if (adjustedP.doubleValue() < max.doubleValue()) {
                // TODO we want to put all values in the map and let the javascript display or not
                adjustedMap.put(label, adjustedP);
            }
            lastValue = p;
        }
    }

    /**
     * @return adjusted map
     */
    public HashMap<String, BigDecimal> getAdjustedMap() {
        return adjustedMap;
    }

}


