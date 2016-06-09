package org.intermine.api.tracker.track;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.sql.Connection;
/**
 * Class representing the track
 * @author dbutano
 */
public interface Track
{
    /**
     * Validate the track before saving into the database
     * @return true if the track is valid
     */
    boolean validate();

    /**
     * Return the track formatted into an array of String to be saved in the database
     * @return Object[] an array of Objects
     */
    Object[] getFormattedTrack();

    /**
     * Save into the table the track object representing the user activity
     * @param con database connection
     */
    void store(Connection con);

    /**
     * Return the table name where the track has to be saved
     * @return String the table name
     */
    String getTableName();
}
