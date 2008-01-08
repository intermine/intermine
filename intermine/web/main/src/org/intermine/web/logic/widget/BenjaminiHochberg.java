package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Iterator;

import org.intermine.web.logic.SortableMap;

import java.math.BigDecimal;

/**
 * @author Julie Sullivan
 */
public class BenjaminiHochberg implements ErrorCorrection
{
    private HashMap<String, Double> originalMap = new HashMap<String, Double>();
    private HashMap<String, BigDecimal> adjustedMap = new HashMap<String, BigDecimal>();
    private double numberOfTests;
    private static final int RESULT_SCALE = 100;

    /**
     * @param numberOfTests number of tests we've run, excluding terms that only annotate one item
     * as these cannot possibly be over-represented
     * @param originalMap HashMap of go terms and their p-value
     * @param alpha the error rate user is willing to accept
     */
    public BenjaminiHochberg(HashMap<String, Double> originalMap, int numberOfTests) {
        this.numberOfTests = numberOfTests;
        SortableMap sortedMap = new SortableMap(originalMap);
        // sort descending
        sortedMap.sortValues(false, false);
        this.originalMap = new HashMap<String, Double>(sortedMap);
    }

    /**
     * Calculates the Benjamini and Hochberg correction of
     * the false discovery rate
     * adjusted p = (m/i) * p
     * 
     * @param max maximum value we are interested in.  
     */
    public void calculate(Double max) {

        adjustedMap = new HashMap<String, BigDecimal>();
        BigDecimal adjustedP;
        int i = 1;

        for (Iterator iter = originalMap.keySet().iterator(); iter.hasNext(); i++) {

            String label = (String) iter.next();
            BigDecimal k = new BigDecimal("" + i);
            BigDecimal m = new BigDecimal("" + numberOfTests);
            
            BigDecimal p = new BigDecimal("" + originalMap.get(label));
            adjustedP = k.divide(m, RESULT_SCALE, BigDecimal.ROUND_HALF_UP);
            adjustedP = p.multiply(adjustedP);

            if (adjustedP.doubleValue() < max.doubleValue()) {
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


