package org.intermine.util;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import java.util.Collection;
import java.util.Iterator;

/**
 * Collection of commonly used String utilities
 *
 * @author Andrew Varley
 */
public class StringUtil
{
    private static final char HEX_CHARS[]
        = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    
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
     * Returns a String formed by the delimited results of calling toString over a collection
     * @param c the collection to stringify
     * @param delimiter the character to join on
     * @return the string representation
     */
    public static String join(Collection c, String delimiter) {
        StringBuffer sb = new StringBuffer();
        for (Iterator i = c.iterator(); i.hasNext(); ) {
            sb.append(i.next().toString());
            if (i.hasNext()) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    /**
     * Returns a list of tokens delimited by whitespace in String str (useful when handling XML)
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
     * Convert a byte buffer to a hexadecimal string.
     * @param buffer byte buffer
     * @return hexadecimal string
     */
    public static String bufferToHexString(byte buffer[]) {
        StringBuffer sb = new StringBuffer(buffer.length * 2);
        for (int i = 0; i < buffer.length; i++) {
            char a = HEX_CHARS[(buffer[i] & 0xF0) >> 4];
            char b = HEX_CHARS[buffer[i] & 0x0F];
            sb.append(a);
            sb.append(b);
        }
        return sb.toString();
    }
    
    /**
     * Returns a list of tokens delimited by delim in String str.
     * eg. split("abc@#def@#", "@#") returns a 3 element array containing "abc", "def" and ""
     *
     * @param str the String to tokenize
     * @param delim the delimiter String
     * @return the String tokens
     * @throws NullPointerException if str or delim is null
     */
    public static String[] split(String str, String delim) {
        if (str == null || delim == null) {
            throw new NullPointerException("Cannot pass null arguments to tokenize");
        }

        if (delim.length() == 0) {
            throw new IllegalArgumentException("Delimiter can not be zero length");
        }

        List l = new ArrayList();

        int nextStartIndex = 0;

        while (true) {
            int delimIndex = str.indexOf(delim, nextStartIndex);
            if (delimIndex == -1) {
                break;
            }
            l.add(new Integer(delimIndex));
            nextStartIndex = delimIndex + delim.length();
        }

        // add list sentinel to avoid the special case for the last token
        l.add(new Integer(str.length()));

        String [] returnArray = new String[l.size()];

        int i = 0;
        int lastDelimStart = -delim.length();
        Iterator iter = l.iterator();
        while (iter.hasNext()) {
            int thisDelimStart = ((Integer) iter.next()).intValue();
            returnArray[i] = str.substring(lastDelimStart + delim.length(), thisDelimStart);
            lastDelimStart = thisDelimStart;
            i++;
        }

        return returnArray;
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

    /**
     * Duplicates single quotes in Strings for correct storage in postgres.
     *
     * @param s the string to format
     * @return the string with duplicated single quotes
     */
    public static String duplicateQuotes(String s) {
        if (s.indexOf('\'') == -1) {
            return s;
        } else {
            return s.replaceAll("'", "''");
        }
    }

    /**
     * Escape single backslash with single forwardslash for correct storage in postgres.
     *
     * @param s the string to format
     * @return the string with duplicated double backslash
     */
    public static String escapeBackslash(String s) {
        if (s.indexOf('\\') == -1) {
            return s;
        } else {
            return s.replaceAll("\\\\", "/");
        }
    }


    /**
     * trim left space in string
     *
     * @param s the string to format
     * @return the string with no whitespace in the left of string
     */
    public static String trimLeft(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return s.substring(i);
            }
        } 
        return s;
        
    }
    
    /**
     * Return true if all characters in a given String are digits.  Null or empty string
     * will return false.
     * @param s the string to examine
     * @return true if all characters are digits
     */
    public static boolean allDigits(String s) {
        if (s == null || s.length() == 0) {
            return false;
        }

        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i)) && !('.' == s.charAt(i))
                && !('-' == s.charAt(0))) {
                return false;
            }
        }

        return true;
    }
}
