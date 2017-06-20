package org.intermine.api.query.range;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * @author Alex
 */
public interface Range
{

    /**
     * @return start coordinates
     */
    Object getStart();

    /**
     * @return end coordinates
     */
    Object getEnd();
}
