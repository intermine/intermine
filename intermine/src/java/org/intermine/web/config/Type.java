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

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration object for displaying a class
 *
 * @author Andrew Varley
 */
public class Type
{
    private List shortDisplayers = new ArrayList();
    private List longDisplayers = new ArrayList();

    /**
     * Add a short displayer for this Type
     *
     * @param disp the Displayer to add
     */
    public void addShortDisplayer(Displayer disp) {
        shortDisplayers.add(disp);
    }

    /**
     * Get the List of short Displayers
     *
     * @return the List of short Displayers
     */
    public List getShortDisplayers() {
        return this.shortDisplayers;
    }

   /**
     * Add a long displayer for this Type
     *
     * @param disp the Displayer to add
     */
    public void addLongDisplayer(Displayer disp) {
        longDisplayers.add(disp);
    }

    /**
     * Get the List of long Displayers
     *
     * @return the List of long Displayers
     */
    public List getLongDisplayers() {
        return this.longDisplayers;
    }

    /**
     * @see Object#equals
     *
     * @param obj the Object to compare with
     * @return true if this is equal to obj
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Type)) {
            return false;
        }

        return shortDisplayers.equals(((Type) obj).shortDisplayers)
            && longDisplayers.equals(((Type) obj).longDisplayers);
    }

    /**
     * @see Object#hashCode
     *
     * @return the hashCode for this WebConfig object
     */
    public int hashCode() {
        return shortDisplayers.hashCode() + 3 * longDisplayers.hashCode();
    }


}

