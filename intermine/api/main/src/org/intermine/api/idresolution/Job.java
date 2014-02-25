package org.intermine.api.idresolution;

import java.util.Date;

import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.idresolution.Job.JobStatus;

public interface Job extends Runnable {

    public enum JobStatus {PENDING, RUNNING, SUCCESS, ERROR};

    public void run();

    public BagQueryResult getResult();

    public boolean wasSuccessful();

    public Exception getError();

    public Date getStatedAt();

    /**
     * @return the uid
     */
    public String getUid();

    /**
     * @return the status
     */
    public JobStatus getStatus();

    public String getType();

}