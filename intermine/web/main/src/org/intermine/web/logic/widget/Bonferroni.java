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
import java.util.Map;


/**
 * See online documentation for an in depth description of error correction and bonferroni.
 * Briefly, the p-values are adjusted (multiple hypothesis test correction) by adding the
 * alpha divided by the total number of tests to the original number.
 *
 * For example, given 100 tests and an alpha value of .05, we would expect 5 false positives.
 *
 * @author Julie Sullivan
 */
public class Bonferroni implements ErrorCorrection
{
    private HashMap<String, BigDecimal> originalMap = new HashMap<String, BigDecimal>();
    private HashMap<String, BigDecimal> adjustedMap = new HashMap<String, BigDecimal>();
    private BigDecimal numberOfTests, alphaPerTest;
    private static final BigDecimal ALPHA = new BigDecimal(0.05);
    private static final BigDecimal ONE = new BigDecimal(1);

    /**
     * @param originalMap HashMap of go terms and their p-value
     */
    public Bonferroni(HashMap<String, BigDecimal> originalMap) {
        this.originalMap = originalMap;
        numberOfTests = new BigDecimal(originalMap.size());
        if (numberOfTests.intValue() == 0) {
            // the results should never be empty, this shouldn't happen.  if the results for the
            // widget are empty, then there should be no error correction done
            alphaPerTest = numberOfTests;
        } else {
            alphaPerTest = ALPHA.divide(numberOfTests, MathContext.DECIMAL32);
        }
    }

    /**
     * @param max maximum value to display
     */
    @SuppressWarnings("unchecked")
    public void calculate(Double max) {

        for (Map.Entry<String, BigDecimal> entry : originalMap.entrySet()) {

            // get original values
            String label = entry.getKey();
            BigDecimal p = entry.getValue();

            // calc new value - p * n
            BigDecimal adjustedP = p.add(alphaPerTest);

            // p is never over 1
            if (adjustedP.compareTo(ONE) >= 0) {
                adjustedP = ONE;
            }

            // don't store values >= maxValue
            if (adjustedP.compareTo(new BigDecimal(max.doubleValue())) <= 0) {
                adjustedMap.put(label, adjustedP);
            }
        }
    }

    /**
     * @return adjusted map
     */
    public HashMap<String, BigDecimal> getAdjustedMap() {
        return adjustedMap;
    }
}
