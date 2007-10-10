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


/**
 *
 * @author Julie Sullivan
 */
public class Bonferroni
{
    private HashMap originalMap = new HashMap();
    private HashMap<String, Double> adjustedMap = new HashMap<String, Double>();
    private double numberOfTests;

    /**
     * @param originalMap Hashmap of go terms and their pvalues.
     * @param significanceValue significance Value.
     */
    public Bonferroni(HashMap originalMap) {
        this.originalMap = originalMap;
        this.numberOfTests = originalMap.size();
    }

    /**
     * @param maxValue maximum value to display
     */
    public void calculate(Double maxValue) {

        //double bonferroni = significanceValue.doubleValue() / numberOfTests;

        for (Iterator iter = originalMap.keySet().iterator(); iter.hasNext();) {

            // get original values
            String goTerm = (String) iter.next();
            Double p = (Double) originalMap.get(goTerm);

            // calc new value
            Double adjustedP = new Double(numberOfTests * p.doubleValue());
            //Double adjustedP = new Double(bonferroni + p.doubleValue());

            // don't store values >= maxValue
            if (adjustedP.compareTo(maxValue) < 0) {
                // put new values into map
                adjustedMap.put(goTerm, adjustedP);
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
