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


/**
 * Calculates p-values for go terms using the hypergeometric distribution.
 * @author http://function.princeton.edu/GOLEM/authors.html
 */
public class Hypergeometric
{
    static double[] logFactorials;

    /**
     * Builds an array of factorials so we don't have to calculate it each time.
     * @param numGenes the number of genes in the bag.
     **/

    public Hypergeometric(int numGenes) {

        logFactorials = new double[numGenes + 1];
        logFactorials[0] = 0;
        double current = 0;
        for (int i = 1; i < numGenes + 1; i++) {
            current += Math.log(i);
            logFactorials[i] = current;
        }
    }


    /**
     * Compute the logarithm of nCk (n choose k)
     * @param n
     * @param k  
     * @return double the logarithm of nCk
     */
    private static double logChoose(int n, int k) {
        if (n == 0) {
            if (k == 0) {
                return 0;
            } else {
                return Double.NEGATIVE_INFINITY;
            }
        }
        if (k == 0) {
            return 0;
        }
        if (k == 1) {
            return Math.log(n);
        }
        if (n < k) {
            return Double.NEGATIVE_INFINITY;
        }
        return logFactorials[n] - (logFactorials[k] + logFactorials[n - k]);
    }


    /** 
     * The p-value is the sum from j=k to n of MCj*(N-M)C(n-j)/(NCn)
     *      
     * @param n Number of genes in bag (n)
     * @param k Number of genes in the bag annotated with this go term (k)
     * @param bigM Total number of genes annotated with this term (M)
     * @param bigN Total number of genes in the database (N)
     * @return p-value for this go term
     **/

    public static double calculateP(int n, int k, int bigM, int bigN) {

        double sum = 0;
        for (int j = n; j >= k; j--) {
            sum +=
                Math.exp(logChoose(bigM, j) + logChoose(bigN - bigM, n - j) - logChoose(bigN, n));
        }

        return sum;
    }
}










