package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Calculates p-values for go terms using the hypergeometric distribution.
 * See online documentation for detailed information about what this class is and what it does.
 * @author Julie Sullivan
 */
public final class Hypergeometric
{
    private Hypergeometric() {
        // don't instantiate
    }

    static double[] factorials;

    private static void getFactorials(int n) {
        if (factorials == null || factorials.length < n) {
            factorials = new double[n + 1];
            factorials[0] = 0;
            double current = 0;
            for (int i = 1; i < n + 1; i++) {
                current += Math.log(i);
                factorials[i] = current;
            }
        }
    }

    /**
     * Compute the log of nCr (n Choose r)
     *           n!
     * nCr =  ---------
     *        r! (n-r)!
     * @param n
     * @param r
     * @return double the log of nCr
     */
    private static double logChoose(int n, int r) {
        if (n == 0) {
            if (r == 0) {
                return 0;
            }
            return Double.NEGATIVE_INFINITY;
        }
        if (r == 0) {
            return 0;
        }
        if (r == 1) {
            return Math.log(n);
        }
        if (n < r) {
            return Double.NEGATIVE_INFINITY;
        }
        return factorials[n] - (factorials[r] + factorials[n - r]);
    }

    /**
     * The value is calculated as:
     *
     *      (M choose x) (N-M choose n-x)
     * P =   -----------------------------
     *               N choose n
     *
     * @param k number of objects in our list annotated with this term
     * @param n number of objects in our list annotated with any term
     * @param bigM Total number of objects in the database annotated with this term
     * @param bigN Total number of objects in the database annotated with any term
     * @return p-value for this term
     **/
    public static double calculateP(int k, int n, int bigM, int bigN) {
        double p = 0;
        // TODO maybe we don't have to call this each time?
        getFactorials(bigN);
        for (int i = n; i >= k; i--) {
            p += Math.exp(logChoose(bigM, i) + logChoose(bigN - bigM, n - i) - logChoose(bigN, n));
        }
        return p;
    }
}
