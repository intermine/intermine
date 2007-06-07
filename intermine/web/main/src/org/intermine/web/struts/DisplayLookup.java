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

import java.util.Set;

/**
 * Helper class to pass to JSP from TableController.  (Was a static member of TableController but
 * Tomcat 6 failed to access the get methods).
 *
 * @author Matthew Wakeling
 */
class DisplayLookup
{
    private int matches;
    private Set<String> unresolved, duplicates, translated;

    /**
     * Create a new DisplayLookup object.
     *
     * @param matches the number of identifiers that matched useful objects
     * @param unresolved a Set of the identifiers that did not match anything useful
     * @param duplicates a Set of the identifiers that matched multiple useful objects
     * @param translated a Set of the identifiers that only matched objects of the wrong type
     */
    public DisplayLookup(int matches, Set<String> unresolved, Set<String> duplicates,
                         Set<String> translated) {
        this.matches = matches;
        this.unresolved = unresolved;
        this.duplicates = duplicates;
        this.translated = translated;
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
}