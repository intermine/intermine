package org.intermine.api.idresolution;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.Date;

import org.intermine.api.bag.BagQueryResult;

/**
 *
 * @author Alex
 *
 */
public interface Job extends Runnable
{

    /**
     * @author Alex
     *
     */
    public enum JobStatus {
        /**
         * job is pending
         */
        PENDING,
        /**
         * job is currently running
         */
        RUNNING,
        /**
         * job has completed successfully
         */
        SUCCESS,
        /**
         * job failed
         */
        ERROR
    }


    /**
     * run job
     */
    @Override
    void run();

    /**
     * @return query result
     */
    BagQueryResult getResult();

    /**
     * @return true if successful
     */
    boolean wasSuccessful();

    /**
     * @return error
     */
    Exception getError();

    /**
     * @return date started at
     */
    Date getStartedAt();

    /**
     * @return the uid
     */
    String getUid();

    /**
     * @return the status
     */
    JobStatus getStatus();

    /**
     * @return type
     */
    String getType();

}
