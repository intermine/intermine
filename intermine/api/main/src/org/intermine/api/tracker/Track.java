package org.intermine.api.tracker;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Class to represent the track
 * @author dbutano
 */
public interface Track
{
    /**
     * Validate the track before saving into the database
     * @return true if the track is valid
     */
    boolean validate();
}
