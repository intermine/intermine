package org.flymine.util;


/**
 * Collection of commonly used String utilities
 *
 * @author Andrew Varley
 */
public class StringUtil
{
    private StringUtil() {
    }

    /**
     * Returns the number of occurances of str in target
     *
     * @param str the sting to count
     * @param target the String to look in
     * @return the number of occurances of str in target
     * @throws NullPointerException if either str or target are null
     */
    public static int countOccurances(String str, String target) {
        if ((str == null) || (target == null)) {
            throw new NullPointerException("Cannot pass null arguments to countOccurances");
        }

        int count = 0;
        int index = -1;

        while ((index = target.indexOf(str, index + 1)) >= 0) {
            count++;
        }
        return count;
    }


}
