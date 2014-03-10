package org.intermine.api.idresolution;

import java.util.Date;
import java.util.UUID;

import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.BagQueryRunner;

public class ResolutionJob implements Job
{
    private final JobInput input;
    private final BagQueryRunner runner;

    private BagQueryResult result;

    private boolean isComplete = false;
    private Exception error = null;
    private Date startedAt;
    private JobStatus status = JobStatus.PENDING;
    private final String uid;

    public ResolutionJob(UUID id, BagQueryRunner runner, JobInput in) {
        this.input = in;
        this.runner = runner;
        startedAt = null;
        uid = id.toString();
    }

    /* (non-Javadoc)
     * @see org.intermine.api.idresolution.JJob#run()
     */
    @Override
    public void run() {
        this.status = JobStatus.RUNNING;
        startedAt = new Date();
        try {
            this.result = runner.search(
                    input.getType(),
                    input.getIds(),
                    input.getExtraValue(),
                    input.getWildCards(),
                    input.getCaseSensitive());
            this.status = JobStatus.SUCCESS;
        } catch (Exception e) {
            error = e;
            this.status = JobStatus.ERROR;
        }
    }

    public String getType() {
        return input.getType();
    }

    /* (non-Javadoc)
     * @see org.intermine.api.idresolution.JJob#getResult()
     */
    @Override
    public BagQueryResult getResult() {
        return result;
    }

    public JobInput getInput() {
        return input;
    }
    
    /* (non-Javadoc)
     * @see org.intermine.api.idresolution.JJob#wasSuccessful()
     */
    @Override
    public boolean wasSuccessful() {
        return status == JobStatus.SUCCESS;
    }
    
    /* (non-Javadoc)
     * @see org.intermine.api.idresolution.JJob#getError()
     */
    @Override
    public Exception getError() {
        return error;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((input == null) ? 0 : input.hashCode());
        result = prime * result + uid.hashCode();
        result = prime * result
                + ((startedAt == null) ? 0 : startedAt.hashCode());
        
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ResolutionJob)) {
            return false;
        }
        ResolutionJob other = (ResolutionJob) obj;
        if (input == null) {
            if (other.input != null) {
                return false;
            }
        } else if (!input.equals(other.input)) {
            return false;
        }
        if (startedAt == null) {
            if (other.startedAt != null) {
                return false;
            }
        } else if (!startedAt.equals(other.startedAt)) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Job [input=" + input + ", isComplete=" + isComplete
                + ", startedAt=" + startedAt + "]";
    }

    /* (non-Javadoc)
     * @see org.intermine.api.idresolution.JJob#getUid()
     */
    @Override
    public String getUid() {
        return uid;
    }

    /* (non-Javadoc)
     * @see org.intermine.api.idresolution.JJob#getStatus()
     */
    @Override
    public JobStatus getStatus() {
        return status;
    }

    @Override
    public Date getStatedAt() {
        return startedAt;
    }

}
