package org.intermine.sql.writebatch;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.SQLException;

/**
 * An interface representing a job to be performed when actually flushing data to a database.
 *
 * @author Matthew Wakeling
 */
public interface FlushJob
{
    /**
     * Performs the flush of all data represented by this object.
     *
     * @throws SQLException if there is a problem performing the flush
     */
    public void flush() throws SQLException;
}
