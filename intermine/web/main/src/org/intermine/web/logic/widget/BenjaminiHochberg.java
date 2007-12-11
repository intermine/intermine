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
public class BenjaminiHochberg  
{

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
    private static HashMap correctionMap;

    /**
     * the significance level.
     */
    private static BigDecimal alpha;
    /**
     * the number of tests.
     */
    private static int m;
    /**
     * scale for the division in de method 'runFDR'.
     */
    private static final int RESULT_SCALE = 100;


    private final static double MAX = 0.10;


    
    /**
     * Constructor.
     *
     * @param originalMap Hashmap of go terms and their pvalue
     */

    public BenjaminiHochberg(HashMap originalMap) {
        //Get all the go labels and their corresponding pvalues from the map

        Iterator iteratorGoLabelsSet = originalMap.keySet().iterator();
        this.hash = new HashEntry [originalMap.size()];
        this.pvalues = new String [originalMap.size()];
        this.goLabels = new String [originalMap.size()];
        for (int i = 0; iteratorGoLabelsSet.hasNext(); i++) {
            goLabels[i] = iteratorGoLabelsSet.next().toString();
            pvalues[i] = originalMap.get(new Integer(goLabels[i])).toString();
            hash[i] = new HashEntry(goLabels[i], pvalues[i]);
        }
        this.m = pvalues.length;
        this.adjustedPvalues = new String[m];
        this.correctionMap = null;
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
     * test p<(i/m)*alpha from high to low (for i=m..1)
     * i* (istar) first i such that the inequality is correct.
     * reject hypothesis for i=1..i* : labels 1..i* are overrepresented
     * <p/>
     * adjusted p-value for i-th ranked p-value p_i^adj = min(k=i..m)[min(1,m/k p_k)]
     */

    public void calculate() {

        // ordening the pvalues.

        java.util.Arrays.sort(hash, new HashComparator()); 
        this.ordenedPvalues = parse(hash);
        // calculating adjusted p-values.
        BigDecimal min = new BigDecimal("" + 1);
        BigDecimal mkprk;
        for (int i = m; i > 0; i--) {
            mkprk = (new BigDecimal("" 
                                    + m).multiply(
                                    new BigDecimal(ordenedPvalues[i 
                                     - 1]))).divide(new BigDecimal("" + i), 
                                      RESULT_SCALE, BigDecimal.ROUND_HALF_UP);
            if (mkprk.compareTo(min) < 0) {
                min = mkprk;
            }
            adjustedPvalues[i - 1] = min.toString();

        }
        correctionMap = new HashMap();
        for (int i = 0; i < adjustedPvalues.length && i < ordenedGOLabels.length; i++) {
            correctionMap.put(ordenedGOLabels[i], adjustedPvalues[i]);
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
     * getter for the map of corrected p-values.
     *
     * @return HashMap correctionMap.
     */
    public HashMap getCorrectionMap() {
        return correctionMap;
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
     * Run the Task.
     */
    public void run() {
        calculate();
    }

    /**
     * Gets the Task Title.
     *
     * @return human readable task title.
     */
    public String getTitle() {
        return new String("Calculating FDR correction");
    }


}


