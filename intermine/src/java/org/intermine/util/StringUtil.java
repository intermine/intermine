package org.flymine.util;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

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
     * @param str the String to count
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
     * Returns a list of tokens delimited by whitespace in String s (useful when handling XML)
     *
     * @param str the String to tokenize
     * @return the String tokens
     * @throws NullPointerException if  str is null
     */
    public static List tokenize(String str) {
         if (str == null) {
            throw new NullPointerException("Cannot pass null arguments to tokenize");
        }

        List l = new ArrayList();
        StringTokenizer st = new StringTokenizer(str);
        while (st.hasMoreTokens()) {
            l.add(st.nextToken());
        }
        return l;
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

    /**
     * Returns a decapitalised version of the given String
     *
     * @param str the String to decapitalise
     * @return the decapitalised version of str
     */
    public static String decapitalise(String str) {
        if (str == null) {
            return null;
        }
        if (str.length() <= 1) {
            return str.toLowerCase();
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    /**
     * Returns a pluralised version of the given String
     *
     * @param str the String to pluralize
     * @return the pluralised version of str
     */
    public static String pluralise(String str) {
        if (str == null) {
            return null;
        }
        return str + "s";
    }
    
    /**
     * Returns a string with the same initial letter case as the template string
     *
     * @param n the String to convert
     * @param template the String to base the conversion on
     * @return the new String, capitalised like template
     */
    public static String toSameInitialCase(String n, String template) {
        if (n == null) {
            throw new NullPointerException("String to convert cannot be null");
        }
        if (template == null) {
            return n;
        }
        Character first = new Character(template.charAt(0));
        StringBuffer sb = new StringBuffer();
        
        if (Character.isUpperCase(template.charAt(0))) {
            sb.append(Character.toUpperCase(n.charAt(0)));
        }
        if (Character.isLowerCase(template.charAt(0))) {
            sb.append(Character.toLowerCase(n.charAt(0)));
        }
        if (n.length() > 1) {
            sb.append(n.substring(1, n.length()));
        }
        
        return sb.toString();
    }

    private static long differentNumber = 0;
    
    /**
     * Returns a String that is different every time
     *
     * @return a String that is different every time
     */
    public static synchronized String uniqueString() {
        return "" + (differentNumber++);
    }

    /**
     * Sets the number that is used to generate the next uniqueString.
     * NOTE: DO NOT USE THIS METHOD, unless you are absolutely sure no other thread is going to 
     * go anywhere near StringUtil behind your back. This method is for testing purposes only.
     *
     * @param number the number to set
     */
    public static synchronized void setNextUniqueNumber(long number) {
        differentNumber = number;
    }
}
