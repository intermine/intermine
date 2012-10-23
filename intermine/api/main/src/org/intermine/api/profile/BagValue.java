package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringEscapeUtils.escapeXml;;

/**
 * A class representing a value stored in an InterMineBag.
 * @author dbutano
 * @author Alex Kalderimis
 *
 */
public final class BagValue
{
    final String value;
    final String extra;

    /**
     * Constructor
     * @param value The primary value of this item.
     * @param extra The extra distinguishing value of this item (eg. Organism.name for Gene).
     */
    public BagValue (String value, String extra) {
        if (isBlank(value)) {
            throw new IllegalArgumentException("The primary value of a BagValue may not be blank");
        }
        this.value = value;
        this.extra = extra;
    }

    /** @return the primary field of this item **/
    public String getValue() {
        return value;
    }

    /** @return the extra distinguishing value of this item (eg. Organism.name for Gene). **/
    public String getExtra() {
        return extra;
    }

    /**
     * Two bag values are identical if they have the same value and extra value. Blank extra values
     * are equivalent.
     * @param other The thing to compare to.
     * @return Whether or not the two things are equal.
     */
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        } else if (other instanceof BagValue) {
            BagValue obv = (BagValue) other;
            return (value.equals(obv.getValue()))
                && (isBlank(extra)) ? isBlank(obv.getExtra()) : extra.equals(obv.getExtra());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 31 + value.hashCode();
        hash = hash * 31 + (isBlank(extra) ? 0 : extra.hashCode());
        return hash;
    }

    @Override
    public String toString() {
        return "<bagValue value=\"" + escapeXml(value) + "\" extra=\"" + escapeXml(extra) + "\"/>";
    }

}
