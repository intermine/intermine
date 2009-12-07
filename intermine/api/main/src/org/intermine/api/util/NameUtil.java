package org.intermine.api.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.intermine.api.profile.InterMineBag;

public class NameUtil
{
    /**
     * Verifies names (bags, queries, etc) only contain A-Z, a-z, 0-9, underscores and
     * dashes.  And spaces.  And dots.
     * @param name Name of bag/query/template to be validated
     * @return isValid Returns true if this name is correct, false if this name contains a bad char
     */
    public static boolean isValidName(String name) {
        if (name == null) {
            return false;
        }
        Pattern p = Pattern.compile("[^\\w\\s\\.\\-:]");
        Matcher m = p.matcher(name);
        return !m.find();
    }

    /**
     * Returns the word value of special characters (ie returns _AMPERSAND_ for &, etc).  Used for
     * the forced renaming of queries/templates in the query/template import.
     * @param specialCharacter The special character, ie &
     * @return wordEquivalent The special character's name, ie AMPERSAND
     */
    private static String getSpecCharToText(String specialCharacter) {

        Map<String, String> specCharToText = mapChars();
        String wordEquivalent = (String) specCharToText.get(specialCharacter);
        wordEquivalent = "_" + wordEquivalent + "_";
        return wordEquivalent;

    }

    /**
     * Takes a string and replaces special characters with the text value, e.g. it would change
     * "a&b" to "a_AMPERSAND_b".  This is used in the query/template imports to handle special
     * characters.
     * @param name Name of query/template
     * @return rebuiltName Name of query/template with the special characters removed
     */
    private static String replaceSpecialChars(String name) {
        String tmp = name;
        String rebuiltName = "";

        for (int i = 0; i < tmp.length(); i++) {
            char c = tmp.charAt(i);
            String str = String.valueOf(c);

            if (!isValidName(str)) {
                rebuiltName += getSpecCharToText(str);
            } else {
                rebuiltName += str;
            }
        }
        return rebuiltName;
    }

    private static HashMap<String, String> mapChars() {

        HashMap<String, String> specCharToText = new HashMap<String, String> ();

        specCharToText.put("‘", new String("QUOTE"));
        specCharToText.put("’", new String("QUOTE"));
        specCharToText.put("“", new String("QUOTE"));
        specCharToText.put("”", new String("QUOTE"));
        specCharToText.put("‹", new String("LESS_THAN_SIGN"));
        specCharToText.put("›", new String("GREATER_THAN_SIGN"));
        specCharToText.put("!", new String("EXCLAMATION_POINT"));
        specCharToText.put("£", new String("POUND_SIGN"));
        specCharToText.put("$", new String("DOLLAR_SIGN"));
        specCharToText.put("%", new String("PERCENT_SIGN"));

        specCharToText.put("^", new String("CARET"));
        specCharToText.put("&", new String("AMPERSAND"));
        specCharToText.put("(", new String("LEFT_PARENTHESIS"));
        specCharToText.put(")", new String("RIGHT_PARENTHESIS"));
        specCharToText.put("+", new String("PLUS_SIGN"));
        specCharToText.put("=", new String("EQUALS_SIGN"));
        specCharToText.put("{", new String("LEFT_BRACKET"));
        specCharToText.put("}", new String("RIGHT_BRACKET"));
        specCharToText.put("[", new String("LEFT_BRACKET"));
        specCharToText.put("]", new String("RIGHT_BRACKET"));
        specCharToText.put(":", new String("COLON"));

        specCharToText.put(";", new String("SEMICOLON"));
        specCharToText.put("@", new String("AT_SIGN"));
        specCharToText.put(",", new String("COMMA"));
        specCharToText.put("?", new String("QUESTION_MARK"));
        specCharToText.put("~", new String("TILDE"));
        specCharToText.put("#", new String("HASH"));
        specCharToText.put("<", new String("LESS_THAN"));
        specCharToText.put(">", new String("GREATER_THAN"));
        specCharToText.put("'", new String("APOSTROPHE"));
        specCharToText.put("/", new String("FORWARD_SLASH"));
        specCharToText.put("\\", new String("BACK_SLASH"));
        specCharToText.put("*", new String("STAR"));

        return specCharToText;
    }


    public static String generateNewName(String origName, Map<String, InterMineBag> allBags) {
        int i = 1;
        while (allBags.get(origName + "_copy" + i) != null) {
            i++;
        }
        return origName + "_copy" + i;
    }

    public static String getNewNameTextBox(String defaultName, String newBagName) {
        String newName = null;
        if (newBagName != null && newBagName.trim().length() > 0
                && !newBagName.equalsIgnoreCase(defaultName)) {
            newName = newBagName.trim();
        }
        return newName;
    }

    /**
     * Checks that the query name doesn't already exist and returns a numbered
     * name if it does.
     * @param queryName the query name
     * @param profile the user profile
     * @return a validated name for the query
     */
    public static String validateName(Collection<String> names, String name) {

        String newName = name;

        if (!isValidName(name)) {
            newName = replaceSpecialChars(name);
        }

        if (names.contains(newName)) {
            int i = 1;
            while (true) {
                String testName = newName + "_" + i;
                if (names.contains(testName)) {
                    return testName;
                }
                i++;
            }
        } else {
            return newName;
        }
    }
    
    private static final String QUERY_NAME_PREFIX = "query_";

    /**
     * Return a query name that isn't currently in use.
     *
     * @param savedQueries the Map of current saved queries
     * @return the new query name
     */
    public static String findNewQueryName(Map savedQueries) {
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
    public static String findNewQueryName(Map savedQueries, String name) {
        if (name != null && !savedQueries.containsKey(name)) {
            return name;
        }
        for (int i = 1;; i++) {
            String testName = QUERY_NAME_PREFIX + i;
            if (savedQueries == null || savedQueries.get(testName) == null) {
                return testName;
            }
        }
    }
}
