package org.intermine.objectstore.intermine;

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
 * An object that holds information about extra queries run during the ResultsConverter operation.
 *
 * @author Matthew Wakeling
 */
public class ExtraQueryTime
{
    private long queryTime = 0;

    /**
     * Constructor.
     */
    public ExtraQueryTime() {
    }

    /**
     * Adds statistics for an executed query to this object.
     *
     * @param time the time taken in milliseconds for the query
     */
    public void addTime(long time) {
        queryTime += time;
    }

    /**
     * Returns the total amount of time spent in extra queries, in milliseconds.
     *
     * @return a long
     */
    public long getQueryTime() {
        return queryTime;
    }
}
