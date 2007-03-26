package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.sql.Database;

/**
 * DataTracker for use with IntergrationWriterFirstSourceImpl.  When loading the first
 * source we wish to write ti the data tracker as normal but know that and existing
 * object in the database can only be from the current source.  We can assume that id
 * the gfetSource() method is called the source should always be a skeleton of the
 * current Source.
 *
 * @author Wenyan Ji
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public class DataTrackerFirstSource extends DataTracker
{
    Source skelSource = null;

    /**
     * @see DataTracker#DataTracker
     */
    public DataTrackerFirstSource(Database db, int maxSize, int commitSize) {
        super(db, maxSize, commitSize);
    }

    /**
     * Set a skeleton Source which will be returned when calling getSource.
     * @param skelSource the skeleton source
     */
    public void setSkelSource(Source skelSource) {
        if (!skelSource.getSkeleton()) {
            throw new IllegalArgumentException("skelSource was not a skeleton: " + skelSource);
        }
        this.skelSource = skelSource;
    }


    /**
     * IntegrationWriterFirstSourceImpl will only call this method if storing a real
     * object and a skeleton from that source exists in the database.  We can therefore
     * always return a skeleton from that source without reading tracker table.
     *
     * @param id the ID of the object
     * @param field the name of the field
     * @return the Source
     */
    public synchronized Source getSource(Integer id, String field) {
        if (id == null) {
            throw new NullPointerException("id cannot be null");
        }
        if (this.skelSource == null) {
            throw new IllegalStateException("skelSource has not been set!");
        }
        if (broken != null) {
            IllegalArgumentException e = new IllegalArgumentException();
            e.initCause(broken);
            throw e;
        }
        return skelSource;
    }
}
