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

    

    private HashEntry[] hash;
    /**
     * the GO labels that have been tested (constructor input).
     */
    private static String [] goLabels;
    /**
     * the raw p-values that were given as input for the constructor, 
     * order corresponds to String [] goLabels.
     */
    private static String [] pvalues;
    /**
     * the goLabels ordened according to the ordened pvalues.
     */
    private static String [] ordenedGOLabels;
    /**
     * the raw p-values ordened in ascending order.
     */
    private static String [] ordenedPvalues;
    /**
     * the adjusted p-values ordened in ascending order.
     */
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
        this.hash = new HashEntry [originalMap.size()];
        this.pvalues = new String [originalMap.size()];
        this.goLabels = new String [originalMap.size()];
        int i = 0;
        for (Iterator iter = originalMap.keySet().iterator(); iter.hasNext(); i++) {
            goLabels[i] = iter.next().toString();
            pvalues[i] = originalMap.get(goLabels[i]).toString();
            hash[i] = new HashEntry(goLabels[i], pvalues[i]);
        }
        this.numberOfTests = pvalues.length;
        this.adjustedPvalues = new String[numberOfTests];
        this.adjustedMap = null;
    }

/**
 * 
 *
 * @author Kim Rutherford
 */
    class HashEntry 
    {
        public String key;
        public String value;

        /**
         * 
         * @param k
         * @param v
         */
        public HashEntry(String k, String v) {
            this.key = k;
            this.value = v;
        }
    } 

    /**
     * 
     *
     * @author Kim Rutherford
     */
    class HashComparator implements java.util.Comparator 
    {

        public int compare(Object o1, Object o2) {
            return (new BigDecimal(((HashEntry) 
                            o1).value)).compareTo(new BigDecimal(((HashEntry) o2).value));
        }
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

        // ordening the pvalues.

        java.util.Arrays.sort(hash, new HashComparator()); 
        this.ordenedPvalues = parse(hash);
        // calculating adjusted p-values.
        BigDecimal min = new BigDecimal("" + 1);
        BigDecimal mkprk;
        for (int i = numberOfTests; i > 0; i--) {
            
            BigDecimal newNumberOftests = new BigDecimal("" + numberOfTests);
            BigDecimal pvalue = new BigDecimal(ordenedPvalues[i - 1]);
            BigDecimal index = new BigDecimal("" + i);            
            mkprk = newNumberOftests.multiply(pvalue);
            mkprk = mkprk.divide(index, RESULT_SCALE, BigDecimal.ROUND_HALF_UP);
            if (mkprk.compareTo(min) < 0) {
                min = mkprk;
            }
            adjustedPvalues[i - 1] = min.toString();

        }
        adjustedMap = new HashMap();
        for (int i = 0; i < adjustedPvalues.length && i < ordenedGOLabels.length; i++) {
            adjustedMap.put(ordenedGOLabels[i], adjustedPvalues[i]);
        }
    }


    public String [] parse(HashEntry [] data) {
        String[] keys = new String[data.length];
        String[] values = new String[data.length];
        for (int i = 0; i < data.length; i++) {
            keys[i] = data[i].key;
            values[i] = data[i].value;
        }
        ordenedGOLabels = keys;
        return values;
    }


    /**
     * getter for the ordened p-values.
     *
     * @return String[] with the ordened p-values.
     */
    public String[] getOrdenedPvalues() {
        return ordenedPvalues;
    }

    /**
     * getter for the adjusted p-values.
     *
     * @return String[] with the adjusted p-values.
     */
    public String[] getAdjustedPvalues() {
        return adjustedPvalues;
    }

    /**
     * getter for the ordened GOLabels.
     *
     * @return String[] with the ordened GOLabels.
     */
    public String[] getOrdenedGOLabels() {
        return ordenedGOLabels;
    }


    /**
     * @return adjusted map
     */
    public HashMap getAdjustedMap() {
        return adjustedMap;
    }

}


