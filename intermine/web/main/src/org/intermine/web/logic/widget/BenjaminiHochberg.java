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
    private HashMap<String, BigDecimal> adjustedMap = new HashMap<String, BigDecimal>();
    private double numberOfTests;
    private static final int RESULT_SCALE = 100;
    
    /**
     * Constructor.
     *
     * @param originalMap Hashmap of go terms and their pvalue
     */
    public BenjaminiHochberg(HashMap originalMap) {     
        this.numberOfTests = originalMap.size();
        this.originalMap = originalMap;
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

        adjustedMap = new HashMap<String, BigDecimal>();
        BigDecimal adjustedP;
        int i = 0;
        
        for (Iterator iter = originalMap.keySet().iterator(); iter.hasNext(); i++) {
            
            String label = (String) iter.next();  
            BigDecimal index = new BigDecimal("" + i + 1); // don't divide by zero
            
            BigDecimal p = new BigDecimal("" + originalMap.get(label));
            adjustedP = p.multiply(new BigDecimal("" + numberOfTests));
            adjustedP = adjustedP.divide(index, RESULT_SCALE, BigDecimal.ROUND_HALF_UP);

            if (adjustedP.doubleValue() < max.doubleValue()) {
                adjustedMap.put(label, adjustedP);
            }
        }
    }

    /**
     * @return adjusted map
     */
    public HashMap getAdjustedMap() {
        return adjustedMap;
    }

}


