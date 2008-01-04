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
 * @author Julie Sullivan
 */
public class Bonferroni implements ErrorCorrection
{
    private HashMap originalMap = new HashMap();
    private HashMap<String, BigDecimal> adjustedMap = new HashMap<String, BigDecimal>();
    private double numberOfTests;
    
    /**
     * @param originalMap map of terms and their p-values.
     * @param numberOfTests number of tests we've run, excluding terms that only annotate one item
     * as these cannot possibly be overrepresented
     */
    public Bonferroni(HashMap originalMap, int numberOfTests) {
        this.originalMap = originalMap;
        this.numberOfTests = numberOfTests;
    }

    /**
     * @param max maximum value to display
     */
    public void calculate(Double max) {

        for (Iterator iter = originalMap.keySet().iterator(); iter.hasNext();) {

            // get original values
            String label = (String) iter.next();
            BigDecimal p = new BigDecimal("" + originalMap.get(label));

            // calc new value
            BigDecimal adjustedP = p.multiply(new BigDecimal(numberOfTests));

            // don't store values >= maxValue
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
