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

    /**
     * Returns a capitalised version of the given String
     *
     * @param str the String to capitalise
     * @return the capitalised version of str
     */
    public static String capitalise(String str) {
        if (str == null) {
            return null;
        }
        if (str.length() <= 1) {
            return str.toUpperCase();
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1, str.length());
    }

}
