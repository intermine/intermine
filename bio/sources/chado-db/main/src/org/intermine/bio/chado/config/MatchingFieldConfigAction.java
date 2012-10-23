package org.intermine.bio.chado.config;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.intermine.util.CacheMap;

/**
 * A ConfigAction that succeeds only if the value matches a pattern.
 * @author Kim Rutherford
 */
public class MatchingFieldConfigAction extends ConfigAction
{
    private final Pattern pattern;
    private final CacheMap<String, Matcher> cacheMap = new CacheMap<String, Matcher>();

    /**
     * Construct a MatchingFieldConfigAction.
     */
    MatchingFieldConfigAction() {
        pattern = null;
    }

    /**
     * Construct with a pattern that values must match.  If the value from chado doesn't match
     * the pattern it isn't stored.
     * @param pattern a regular expression pattern
     */
    MatchingFieldConfigAction(Pattern pattern) {
        this.pattern = pattern;
    }

    /**
     * Validate a value for this field by matching with pattern set in the constructor.
     * @param value the value to check
     * @return true if value matches the pattern
     */
    public boolean isValidValue(String value) {
        if (pattern == null) {
            return true;
        }
        Matcher matcher;
        if (cacheMap.containsKey(value)) {
            matcher = (Matcher) cacheMap.get(value);
        } else {
            matcher = pattern.matcher(value);
            cacheMap.put(value, matcher);
        }
        return matcher.matches();
    }

    /**
     * Process the value to set and return a (possibly) altered version.  If a pattern was set
     * in the constructor and the pattern contains a capturing group, then return the contents
     * of the capturing group, otherwise return the whole value.  If there is no pattern, return
     * the whole value.
     * @param value the attribute value to process
     * @return the processed value
     */
    public String processValue(String value) {
        if (pattern == null) {
            return value;
        }
        Matcher matcher = (Matcher) cacheMap.get(value);
        if (matcher.groupCount() == 0) {
            // no capturing group in pattern so return the whole value
            return value;
        } else {
            if (matcher.groupCount() == 1) {
                if (matcher.group(1) == null) {
                    // special case - the pattern matches, but doesn't match the capturing group
                    return value;
                } else {
                    return matcher.group(1);
                }
            } else {
                throw new RuntimeException("more than one capturing group in: "
                        + pattern.toString());
            }
        }
    }
}
