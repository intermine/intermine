package org.flymine.web.results;

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
 * Configuration information for a column in a table
 *
 * @author Andrew Varley
 */
public class Column
{
    protected boolean visible = true;

    /**
     * Is the column visible
     *
     * @return true if the column is visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Set the visibility of the column
     *
     * @param visible true if visible, false if not
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

}
