package org.intermine.api.tracker;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;

import org.intermine.api.tracker.track.Track;
/**
 * Interface to represent a Tracker, an object tracking the users activities in the webapp
 * into the database
 * @author dbutano
 */
public interface Tracker
{
    /**
     * Create the table where the tracker saves data
     * @throws Exception when a database error access is verified
     */
    void createTrackerTable(Connection con) throws Exception;

    /**
     * Save into the table a Track
     * @param track the object saved into the database representing the user activity
     */
    void storeTrack(Track track);

    /**
     * Return the tracker's name
     * @return String representing the tracker's name
     */
    String getName();
}
