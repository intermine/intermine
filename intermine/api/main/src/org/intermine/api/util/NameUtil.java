package org.intermine.api.util;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * Utility methods for naming queries and lists.
 *
 * @author Julie Sullivan
 */
public final class NameUtil
{
    private NameUtil() {
        // don't
    }

    /**
     * error message for bad names.  should come from properties file instead, really
     */
    public static final String INVALID_NAME_MSG = "Names for lists and queries may only contain "
            + "A-Z, a-z, 0-9, underscores and dashes.";
    private static final String QUERY_NAME_PREFIX = "query_";
    private static final Map<String, String> SPEC_CHAR_TO_TEXT = new HashMap<String, String>();
    // A-Z, a-z, 0-9, underscores and dashes.  And spaces.  And dots.
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[^\\w\\s\\.\\-:]");
    // A-Z, a-z, 0-9, underscores and dashes.
    private static final Pattern NO_SPECIAL_CHARS_PATTERN = Pattern.compile("[^\\w\\-:]");

    /*
     * Generates a map of special characters to their name, used to swap out bad characters in
     * query/list names
     */
    static {
        SPEC_CHAR_TO_TEXT.put("!", new String("EXCLAMATION_POINT"));
        SPEC_CHAR_TO_TEXT.put("$", new String("DOLLAR_SIGN"));
        SPEC_CHAR_TO_TEXT.put("%", new String("PERCENT_SIGN"));

        SPEC_CHAR_TO_TEXT.put("^", new String("CARET"));
        SPEC_CHAR_TO_TEXT.put("&", new String("AMPERSAND"));
        SPEC_CHAR_TO_TEXT.put("(", new String("LEFT_PARENTHESIS"));
        SPEC_CHAR_TO_TEXT.put(")", new String("RIGHT_PARENTHESIS"));
        SPEC_CHAR_TO_TEXT.put("+", new String("PLUS_SIGN"));
        SPEC_CHAR_TO_TEXT.put("=", new String("EQUALS_SIGN"));
        SPEC_CHAR_TO_TEXT.put("{", new String("LEFT_BRACKET"));
        SPEC_CHAR_TO_TEXT.put("}", new String("RIGHT_BRACKET"));
        SPEC_CHAR_TO_TEXT.put("[", new String("LEFT_BRACKET"));
        SPEC_CHAR_TO_TEXT.put("]", new String("RIGHT_BRACKET"));
        SPEC_CHAR_TO_TEXT.put(":", new String("COLON"));

        SPEC_CHAR_TO_TEXT.put(";", new String("SEMICOLON"));
        SPEC_CHAR_TO_TEXT.put("@", new String("AT_SIGN"));
        SPEC_CHAR_TO_TEXT.put(",", new String("COMMA"));
        SPEC_CHAR_TO_TEXT.put("?", new String("QUESTION_MARK"));
        SPEC_CHAR_TO_TEXT.put("~", new String("TILDE"));
        SPEC_CHAR_TO_TEXT.put("#", new String("HASH"));
        SPEC_CHAR_TO_TEXT.put("<", new String("LESS_THAN"));
        SPEC_CHAR_TO_TEXT.put(">", new String("GREATER_THAN"));
        SPEC_CHAR_TO_TEXT.put("'", new String("APOSTROPHE"));
        SPEC_CHAR_TO_TEXT.put("/", new String("FORWARD_SLASH"));
        SPEC_CHAR_TO_TEXT.put("\\", new String("BACK_SLASH"));
        SPEC_CHAR_TO_TEXT.put("*", new String("ASTERISK"));
    }

    /**
     * Verifies names (bags, queries, etc) only contain A-Z, a-z, 0-9, underscores and
     * dashes.  And spaces.  And dots.
     * @param name Name of bag/query/template to be validated
     * @return isValid Returns true if this name is correct, false if this name contains a bad char
     */
    public static boolean isValidName(String name) {
        return validateName(name, true);
    }

    private static boolean validateName(String name, boolean specialChars) {
        if (StringUtils.isBlank(name)) {
            return false;
        }
        Matcher m = (specialChars ? SPECIAL_CHARS_PATTERN.matcher(name)
                : NO_SPECIAL_CHARS_PATTERN.matcher(name));
        return !m.find();
    }

    /**
     * Verifies names (bags, queries, etc) only contain A-Z, a-z, 0-9, underscores and
     * dashes.
     * if specialChars boolean is TRUE, then dot and space are allowed.  If specialChars is FALSE,
     * it likely means the name is going to be handled by javascript, in URLS, etc and we don't
     * want to have to encode it.  eg. template name.
     *
     * @param name Name of bag/query/template to be validated
     * @param specialChars if true, then special characters DOT and SPACE are allowed in name
     * @return isValid Returns true if this name is correct, false if this name contains a bad char
     */
    public static boolean isValidName(String name, boolean specialChars) {
        return validateName(name, specialChars);
    }

    /**
     * Takes a string and replaces special characters with the text value, e.g. it would change
     * "a&amp;b" to "a_AMPERSAND_b".  This is used in the query/template imports to handle special
     * characters.
     * @param name Name of query/template
     * @return rebuiltName Name of query/template with the special characters removed
     */
    private static String replaceSpecialChars(String name) {
        String tmp = name;
        StringBuffer rebuiltName = new StringBuffer();
        for (int i = 0; i < tmp.length(); i++) {
            char c = tmp.charAt(i);
            String str = String.valueOf(c);
            if (!isValidName(str)) {
                rebuiltName.append(getSpecCharToText(str));
            } else {
                rebuiltName.append(str);
            }
        }
        return name.replaceAll("[^a-zA-Z 0-9]+", "");
    }

    /**
     * Returns the word value of special characters (ie returns _AMPERSAND_ for &amp;, etc).
     * Used for the forced renaming of queries/templates in the query/template import.
     *
     * @param specialCharacter The special character, ie &amp;
     * @return wordEquivalent The special character's name, ie AMPERSAND
     */
    private static String getSpecCharToText(String specialCharacter) {
        String wordEquivalent = SPEC_CHAR_TO_TEXT.get(specialCharacter);
        if (StringUtils.isEmpty(wordEquivalent)) {
            wordEquivalent = "SPECIAL_CHARACTER_REMOVED";
        }
        wordEquivalent = "_" + wordEquivalent + "_";
        return wordEquivalent;
    }

    /**
     * Generate a new name for a list.  Used in situations where the user has a new list without
     * creating one via the upload form, e.g. when copying or posting a list from another site
     * @param listName original name for the list
     * @param listNames a list of all lists
     * @return a unique name for the list
     */
    public static String generateNewName(Set<String> listNames, String listName) {
        int i = 1;
        while (listNames.contains(listName + "_" + i)) {
            i++;
        }
        return listName + "_" + i;
    }

    /**
     * Checks that the name doesn't already exist and returns a numbered name if it does.  Used in
     * situations where prompting the user for a good name wouldn't work, eg. query import
     * @param name the query or list name
     * @param names list of current names
     * @return a validated name for the query
     */
    public static String validateName(Collection<String> names, String name) {
        String newName = name.trim();
        if (!isValidName(name)) {
            newName = replaceSpecialChars(name);
        }
        if (names.contains(newName)) {
            int i = 1;
            while (true) {
                String testName = newName + "_" + i;
                if (!names.contains(testName)) {
                    return testName;
                }
                i++;
            }
        }
        return newName;
    }

    /**
     * Return a query name that isn't currently in use.
     *
     * @param savedQueries the Map of current saved queries
     * @return the new query name
     */
    public static String findNewQueryName(Set<String> savedQueries) {
        return findNewQueryName(savedQueries, null);
    }

    /**
     * Return a query name that isn't currently in use, returning the given name
     * if it is available.
     *
     * @param savedQueries the Map of current saved queries
     * @param name name to return if it's available
     * @return the new query name
     */
    public static String findNewQueryName(Set<String> savedQueries, String name) {
        if (StringUtils.isNotEmpty(name) && !savedQueries.contains(name)) {
            return name;
        }
        for (int i = 1;; i++) {
            String testName = QUERY_NAME_PREFIX + i;
            if (savedQueries == null || !savedQueries.contains(testName)) {
                return testName;
            }
        }
    }
}
