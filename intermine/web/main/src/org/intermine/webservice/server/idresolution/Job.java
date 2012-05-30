package org.intermine.webservice.server.idresolution;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.webservice.server.idresolution.IdResolutionService.Input;

public class Job implements Runnable
{
    public enum JobStatus {PENDING, RUNNING, SUCCESS, ERROR};
    @SuppressWarnings("unchecked")
    public static final Map<String, Job> JOBS = MapUtils.synchronizedMap(
            new HashMap<String, Job>());

    private final Input input;
    private final BagQueryRunner runner;

    private BagQueryResult result;

    private boolean isComplete = false;
    private Exception error = null;
    private final Long startedAt;
    private JobStatus status = JobStatus.PENDING;
    private final String uid = generateUID();

    public Job(BagQueryRunner runner, Input in) {
        this.input = in;
        this.runner = runner;
        startedAt = System.currentTimeMillis();
        JOBS.put(this.getUid(), this);
    }
    
    private static String generateUID() {
        String uid = getPossibleUID();
        while (!uidIsUnique(uid)) {
            uid = getPossibleUID();
        }

        return uid;
    }
    
    private static boolean uidIsUnique(String uid) {
        if (uid == null) {
            throw new IllegalArgumentException("uid may not be null");
        }
        if (JOBS.isEmpty()) {
            return true;
        }
        if (JOBS.containsKey(uid)) {
            return false;
        }
        return true;
    }
    
    private static String getPossibleUID() {
        return String.format("%s-%s-%s-%s",
            RandomStringUtils.randomAlphanumeric(4),
            RandomStringUtils.randomAlphanumeric(4),
            RandomStringUtils.randomAlphanumeric(4),
            RandomStringUtils.randomAlphanumeric(4)).toLowerCase(); 
    }
    
    public static Job getJobById(String uid) {
        if (JOBS.containsKey(uid)) {
            return JOBS.get(uid);
        }
        else {
            return null;
        }
    }

    @Override
    public void run() {
        this.status = JobStatus.RUNNING;
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
    
    public BagQueryResult getResult() {
        return result;
    }
    
    public boolean wasSuccessful() {
        return error == null;
    }
    
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
        if (!(obj instanceof Job)) {
            return false;
        }
        Job other = (Job) obj;
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

    /**
     * @return the uid
     */
    public String getUid() {
        return uid;
    }

    /**
     * @return the status
     */
    public JobStatus getStatus() {
        return status;
    }

}
