package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.HashMap;

/**
 * Describe references/attributes that define a restricted subclass.
 * Contains a map from path expression to value.
 *
 * @author Richard Smith
 */
public class SubclassRestriction
{
    private Map restrictions = new HashMap();

    /**
     * Add a path expression/value pair representing a restriction.
     * @param fieldPath path expression represtentig restriction
     * @param value value of attribute (may be null)
     */
    public void addRestriction(String fieldPath, Object value) {
        restrictions.put(fieldPath, value);
    }

    /**
    * Return true if there currently any restrictions.
    * @return true if there are currently and restrictions
    */
    public boolean hasRestrictions() {
        return !restrictions.isEmpty();
    }

    /**
     * Return restrictions map.
     * @return map of path expression/value representing restrictions
     */
    public Map getRestrictions() {
        return restrictions;
    }

    /**
     * @see Object#equals
     */
    public boolean equals(Object obj) {
        if (obj instanceof SubclassRestriction) {
            SubclassRestriction sub = (SubclassRestriction) obj;
            return this.restrictions.equals(sub.restrictions);
        }
        return false;
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return restrictions.hashCode();
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        return restrictions.toString();
    }
}
