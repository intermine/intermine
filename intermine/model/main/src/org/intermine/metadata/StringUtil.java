package org.intermine.metadata;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;

/**
 * Collection of commonly used String utilities
 *
 * @author Andrew Varley
 */
public final class StringUtil
{
    private static final char[] HEX_CHARS
        = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    private StringUtil() {
        // do nothing
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
     * Returns a String formed by the delimited results of calling toString over a collection.
     *
     * @param c the collection to stringify
     * @param delimiter the character to join on
     * @return the string representation
     */
    public static String join(Collection<?> c, String delimiter) {
        StringBuffer sb = new StringBuffer();
        boolean needComma = false;
        for (Object o : c) {
            if (needComma) {
                sb.append(delimiter);
            }
            needComma = true;
            sb.append(o.toString());
        }
        return sb.toString();
    }

    /**
     * @param str the String to tokenize
     * @param delimiter the delimiter
     * @return the String tokens
     * @throws NullPointerException if  str is null
     */
    public static List<String> tokenize(String str, String delimiter) {
        if (str == null) {
            throw new NullPointerException("Cannot pass null arguments to tokenize");
        }
        List<String> l = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(str, delimiter);
        while (st.hasMoreTokens()) {
            l.add(st.nextToken());
        }
        return l;
    }


    /**
     * Returns a list of tokens delimited by whitespace in String str (useful when handling XML)
     *
     * @param str the String to tokenize
     * @return the String tokens
     * @throws NullPointerException if  str is null
     */
    public static List<String> tokenize(String str) {
        if (str == null) {
            throw new NullPointerException("Cannot pass null arguments to tokenize");
        }

        List<String> l = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(str);
        while (st.hasMoreTokens()) {
            l.add(st.nextToken());
        }
        return l;
    }

    /**
     * Returns a list of tokens delimited by comma.  Useful for queries.
     *
     * @param strings the String to tokenize
     * @param lowercase if true, set all strings to be lowercase
     * @return the String tokens
     */
    public static Collection<String> tokenize(String strings, boolean lowercase) {
        if (strings == null) {
            return null;
        }
        Collection<String> coll = new ArrayList<String>();
        for (String s : strings.split(",")) {
            if (lowercase) {
                coll.add(s.toLowerCase());
            } else {
                coll.add(s);
            }
        }
        return coll;
    }

    /**
     * Convert a byte buffer to a hexadecimal string.
     * @param buffer byte buffer
     * @return hexadecimal string
     */
    public static String bufferToHexString(byte[] buffer) {
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

        List<Integer> l = new ArrayList<Integer>();

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
        for (Integer thisDelimStartInteger : l) {
            int thisDelimStart = thisDelimStartInteger.intValue();
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
     * Returns a decapitalised version of the given String unless string is an acronym.
     *
     * Gene    --> gene
     * Protein --> protein
     * MRNA    --> MRNA
     * CDS     --> CDS
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
        // second character is uppercase, so we probably have an acronym.  leave as upper
        if (Character.isUpperCase(str.charAt(1))) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    /**
     * Reverses the capitalisation of the first character of the given String.
     *
     * @param str a String
     * @return another String
     */
    public static String reverseCapitalisation(String str) {
        if (str == null) {
            return null;
        }
        if ("".equals(str)) {
            return str;
        }
        char first = str.charAt(0);
        if (Character.isLowerCase(first)) {
            return Character.toUpperCase(first) + str.substring(1);
        }
        return Character.toLowerCase(first) + str.substring(1);
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
        }
        return s.replaceAll("'", "''");
    }

    /**
     * Escapes single quotes and backslashes, with backslashes.
     *
     * @param str the string to format
     * @return the modified string
     */
    public static String escapeWithBackslashes(String str) {
        String s = str;
        if (s.indexOf('\\') != -1) {
            s = s.replace("\\", "\\\\");
        }
        if (s.indexOf('\'') != -1) {
            s = s.replace("\'", "\\\'");
        }
        return s;
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
        }
        return s.replaceAll("\\\\", "/");
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
     * will return false.  Ignores negative sign and decimal point.
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
    /**
     * Take a collection of Strings and return a combined string as a comma separated list
     * with 'and' between the final pair.  For example: [a, b, c] -> "a, b and c".
     *
     * @param elements a collection of strings to put in the list.
     * @return a string with all the elements suitable for inclusion in a sentence.
     */
    public static String prettyList(Collection<String> elements) {
        return StringUtil.prettyList(elements, false);
    }

    /**
     * Take a collection of Strings and return a combined string as a comma separated list
     * with 'and' between the final pair.  For example: [a, b, c] -> "a, b and c".
     *
     * @param elements a collection of strings to put in the list.
     * @param sort if true then order the strings alphabetically
     * @return a string with all the elements suitable for inclusion in a sentence.
     */
    public static String prettyList(Collection<String> elements, boolean sort) {
        Collection<String> col;
        if (sort) {
            col = new TreeSet<String>(elements);
        } else {
            col = elements;
        }
        StringBuffer sb = new StringBuffer();
        int pos = 1;
        for (String str : col) {
            sb.append(str);
            if (pos == (col.size() - 1)) {
                sb.append(" and ");
            } else if (pos < col.size()) {
                sb.append(", ");
            }
            pos++;
        }
        return sb.toString();
    }


    /**
     * Return 'a' or 'an' according to first letter of the given article.  If article starts with
     * a vowel or appears to be an acronym return 'an'.
     *
     * @param s the subject of the article
     * @return the appropriate indefinite article
     */
    @SuppressWarnings("boxing")
    public static String indefiniteArticle(String s) {
        List<Character> vowels =
            new ArrayList<Character>(Arrays.asList(new Character[] {'a', 'e', 'i', 'o', 'u'}));
        String noun = s.trim();
        if (vowels.contains(noun.charAt(0))) {
            return "an";
        }

        List<Character> vowelPronounced = new ArrayList<Character>(Arrays.asList(
                    new Character[] {'A', 'E', 'F', 'H', 'I', 'L', 'M', 'N', 'O', 'R', 'S', 'X'}));
        if (Character.isUpperCase(noun.charAt(0))) {
            if ((noun.length() == 1 || Character.isUpperCase(noun.charAt(1)))
                    && vowelPronounced.contains(noun.charAt(0))) {
                return "an";
            }
        }
        return "a";
    }

    /**
     * Make a Map from the serialized String returned by jQuery.sortable("serialize").
     *
     * @param str the String
     * @return a Map
     */
    public static Map<String, String> serializedSortOrderToMap(String str) {
        Map<String, String> returnMap = new LinkedHashMap<String, String>();
        String[] strArray = str.split("&");
        for (String path: strArray) {
            returnMap.put(StringUtils.split(path, "[]=")[0], StringUtils.split(
                        path, "[]=")[1]);
        }
        return returnMap;
    }

    /**
     * Converts all the colons in a String into dots.
     *
     * @param in an input String
     * @return a new String
     */
    public static String colonsToDots(String in) {
        char[] array = in.toCharArray();
        for (int i = 0; i < array.length; i++) {
            if (array[i] == ':') {
                array[i] = '.';
            }
        }
        return new String(array);
    }

    /**
     * Trim starting and trailing '/' characters from a string if present.
     *
     * @param s the string to trim slashes from
     * @return a string with no starting or trailing slashes, or null if input string was null
     */
    public static String trimSlashes(String s) {
        if (s == null) {
            return null;
        }
        String formattedString = s;
        if (formattedString.startsWith("/")) {
            formattedString = formattedString.substring(1);
        }
        if (formattedString.endsWith("/")) {
            formattedString = formattedString.substring(0, formattedString.length() - 1);
        }
        if (formattedString.startsWith("/") || formattedString.endsWith("/")) {
            formattedString = trimSlashes(formattedString);
        }
        return formattedString;
    }

    /**
     * Wraps the given String into several lines and ultimately truncates it with an ellipsis.
     *
     * @param input the String to shorten
     * @param lineLength the maximum line length
     * @param lineCount the maximum number of lines
     * @return a formatted String
     */
    public static LineWrappedString wrapLines(String input, int lineLength, int lineCount) {
        return wrapLines(input, lineLength, lineCount, 0);
    }

    /**
     * Wraps the given String into several lines and ultimately truncates it with an ellipsis.
     *
     * @param input the String to shorten
     * @param lineLength the maximum line length
     * @param lineCount the maximum number of lines
     * @param lastLineShorter the number of characters by which the last line is shorter
     * @return a formatted String
     */
    public static LineWrappedString wrapLines(String input, int lineLength, int lineCount,
            int lastLineShorter) {
        String formattedString = input.trim();
        String trimmed = "";
        boolean truncated = false;
        for (int i = 1; i <= lineCount; i++) {
            if (i == lineCount) {
                if (formattedString.length() > lineLength - lastLineShorter) {
                    int breakPoint = formattedString.lastIndexOf(" ",
                            lineLength - 3 - lastLineShorter);
                    if (breakPoint > lineLength / 2) {
                        trimmed += formattedString.substring(0, breakPoint) + "...";
                    } else {
                        trimmed += formattedString.substring(0,
                                lineLength - 3 - lastLineShorter) + "...";
                    }
                    truncated = true;
                } else {
                    trimmed += formattedString;
                    break;
                }
            } else {
                if (formattedString.length() > lineLength) {
                    int breakPoint = formattedString.lastIndexOf(" ", lineLength);
                    if (breakPoint > lineLength / 2) {
                        trimmed += formattedString.substring(0, breakPoint) + "\n";
                        formattedString = formattedString.substring(breakPoint + 1);
                    } else {
                        trimmed += formattedString.substring(0, lineLength - 1)
                            + "-\n";
                        formattedString = formattedString.substring(lineLength - 1);
                    }
                } else {
                    trimmed += formattedString;
                    break;
                }
            }
        }
        return new LineWrappedString(trimmed, truncated);
    }

    /**
     * Class for returning multiple values from the wrapLines method.
     *
     * @author Matthew Wakeling
     */
    public static class LineWrappedString
    {
        private String wrapped;
        private boolean truncated;

        /**
         * Constructor.
         *
         * @param wrapped the String in converted form
         * @param truncated true if the String had to be truncated to make it fit
         */
        public LineWrappedString(String wrapped, boolean truncated) {
            this.wrapped = wrapped;
            this.truncated = truncated;
        }

        /**
         * Returns the wrapped String.
         *
         * @return a String
         */
        public String getString() {
            return wrapped;
        }

        /**
         * Returns whether the String had to be truncated.
         *
         * @return a boolean
         */
        public boolean isTruncated() {
            return truncated;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return wrapped.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof LineWrappedString) {
                LineWrappedString lws = (LineWrappedString) o;
                return wrapped.equals(lws.wrapped) && (truncated == lws.truncated);
            }
            return false;
        }
    }
}
