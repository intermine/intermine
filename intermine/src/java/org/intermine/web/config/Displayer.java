package org.flymine.web.config;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Configuration object describing details of a displayer
 *
 * @author Andrew Varley
 */
public class Displayer
{
    private String src;

    /**
     * Set the source of this displayer
     *
     * @param src the source
     */
    public void setSrc(String src) {
        this.src = src;
    }

    /**
     * Get the source of this displayer
     *
     * @return the source
     */
    public String getSrc() {
        return this.src;
    }

    /**
     * @see Object#equals
     *
     * @param obj the Object to compare with
     * @return true if this is equal to obj
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Displayer)) {
            return false;
        }

        return src.equals(((Displayer) obj).src);
    }

    /**
     * @see Object#hashCode
     *
     * @return the hashCode for this Displayer object
     */
    public int hashCode() {
        return src.hashCode();
    }

    /**
     * Return an XML String of this Type object
     *
     * @return a String version of this WebConfig object
     */
    public String toString() {
        return "<displayer src=\"" + src + "\"/>";
    }


}

