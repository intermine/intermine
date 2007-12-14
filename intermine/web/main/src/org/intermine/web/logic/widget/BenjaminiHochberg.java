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

import java.math.BigDecimal;

/**
 * 
 *
 * @author Julie Sullivan
 */
public class BenjaminiHochberg  implements ErrorCorrection
{
    
    private HashMap originalMap = new HashMap();
  /**
     * the GO labels that have been tested (constructor input).
     */
    private static String [] goLabels;
    /**
     * the raw p-values that were given as input for the constructor, 
     * order corresponds to String [] goLabels.
     */
    private static String [] pvalues;
  
    private static String [] adjustedPvalues;

    /**
     * hashmap with the results (adjusted p-values) as values and the GO labels as keys.
     */
    private static HashMap adjustedMap;

    /**
     * the significance level.
     */
    private static BigDecimal alpha;
    /**
     * the number of tests.
     */
    private static int numberOfTests;
    /**
     * scale for the division in de method 'runFDR'.
     */
    private static final int RESULT_SCALE = 100;


    
    /**
     * Constructor.
     *
     * @param originalMap Hashmap of go terms and their pvalue
     */

    public BenjaminiHochberg(HashMap originalMap) {     
        this.pvalues = new String [originalMap.size()];
        this.goLabels = new String [originalMap.size()];
        int i = 0;
        for (Iterator iter = originalMap.keySet().iterator(); iter.hasNext(); i++) {
            goLabels[i] = iter.next().toString();
            pvalues[i] = originalMap.get(goLabels[i]).toString();           
        }
        this.numberOfTests = pvalues.length;
        this.adjustedPvalues = new String[numberOfTests];
        this.adjustedMap = null;
    }



    /**
     * method that calculates the Benjamini and Hochberg correction of
     * the false discovery rate
     * NOTE : convert array indexes [0..m-1] to ranks [1..m].
     * orden raw p-values low .. high
     * test p<(i/numberOfTests)*alpha from high to low (for i=numberOfTests..1)
     * i* (istar) first i such that the inequality is correct.
     * reject hypothesis for i=1..i* : labels 1..i* are overrepresented
     * <p/>
     * adjusted p-value for i-th ranked p-value p_i^adj = min(k=i..m)[min(1,numberOfTests/k p_k)]
     */

    public void calculate(Double max) {

        BigDecimal min = new BigDecimal("" + 1);
        BigDecimal adjustedP;
        for (int i = 1; i <= numberOfTests; i++) {
            
            BigDecimal newNumberOftests = new BigDecimal("" + numberOfTests);
            BigDecimal pvalue = new BigDecimal(pvalues[i]);
            BigDecimal index = new BigDecimal("" + i);            
            adjustedP = newNumberOftests.multiply(pvalue);

            adjustedP = adjustedP.divide(index, RESULT_SCALE, BigDecimal.ROUND_HALF_UP);
            if (adjustedP.compareTo(min) < 0) {
                min = adjustedP;
            }
            adjustedPvalues[i + 1] = min.toString();

        }
        adjustedMap = new HashMap();
        for (int i = 0; i < adjustedPvalues.length && i < goLabels.length; i++) {
            adjustedMap.put(goLabels[i], adjustedPvalues[i]);
        }
    }

    /**
     * @return adjusted map
     */
    public HashMap getAdjustedMap() {
        return adjustedMap;
    }

}


