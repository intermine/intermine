package org.intermine.web.struts;

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
import java.util.Map;
import java.util.Set;

/**
 * Helper class to pass to JSP from TableController.  (Was a static member of TableController but
 * Tomcat 6 failed to access the get methods).
 *
 * @author Matthew Wakeling
 */
public class DisplayLookup
{
    private int matches;
    private Set<String> unresolved, duplicates, translated, lowQuality;
    private Map<String, List> wildcards;
    private String type;
    private String extraConstraint;

    /**
     * Create a new DisplayLookup object.
     *
     * @param type a String for the type of the objects being looked up
     * @param matches the number of identifiers that matched useful objects
     * @param unresolved a Set of the identifiers that did not match anything useful
     * @param duplicates a Set of the identifiers that matched multiple useful objects
     * @param translated a Set of the identifiers that only matched objects of the wrong type
     * @param extraConstraint the String for the value of the extra constraint
     */
    public DisplayLookup(String type, int matches, Set<String> unresolved, Set<String> duplicates,
            Set<String> translated, Set<String> lowQuality, Map<String, List> wildcards,
            String extraConstraint) {
        this.matches = matches;
        this.unresolved = unresolved;
        this.duplicates = duplicates;
        this.translated = translated;
        this.lowQuality = lowQuality;
        this.wildcards = wildcards;
        this.type = type;
        this.extraConstraint = extraConstraint;
    }

    /**
     * Check if there are any identifiers that didn't match.
     * @return true if there are any issues
     */
    public boolean isIssues() {
        return (!unresolved.isEmpty() || !duplicates.isEmpty() || !translated.isEmpty() 
                        || !wildcards.isEmpty() || !lowQuality.isEmpty());
    }
    
    /**
     * Returns the number of identifiers that matched useful objects.
     *
     * @return an int
     */
    public int getMatches() {
        return matches;
    }

    /**
     * Get the type (class name) for which this lookup was performed.
     * @return the unqualified class name
     */
    public String getType() {
        return type;
    }

    /**
     * Returns a Set of the identifiers that did not match anything useful.
     *
     * @return a Set of Strings
     */
    public Set<String> getUnresolved() {
        return unresolved;
    }

    /**
     * Returns a Set of the identifiers that matched more than one useful object.
     *
     * @return a Set of Strings
     */
    public Set<String> getDuplicates() {
        return duplicates;
    }

    /**
     * Returns a Set of the identifiers that matched only objects of the wrong type.
     *
     * @return a Set of Strings
     */
    public Set<String> getTranslated() {
        return translated;
    }
    
    /**
     * Returns a Set of the identifiers that were low quality matches - e.g. matched an
     * alternate identifier.
     * @return a Set of Strings
     */
    public Set<String> getLowQuality() {
        return lowQuality;
    }
    
    /**
     * Return a map of any wildcards used and the number of identifiers they matched.
     * @return map of wildcards to the number of matches.
     */
    public Map<String, List> getWildcards() {
        return wildcards;
    }

    /**
     * Returns true if the extraConstraint is not null and non-empty.
     *
     * @return a boolean
     */
    public boolean getHasExtraConstraint() {
        return (extraConstraint != null) && (!"".equals(extraConstraint));
    }

    /**
     * Returns the extra constraint string.
     *
     * @return a String
     */
    public String getExtraConstraint() {
        return extraConstraint;
    }
}
