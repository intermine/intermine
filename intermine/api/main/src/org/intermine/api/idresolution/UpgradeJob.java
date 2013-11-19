package org.intermine.api.idresolution;

import java.util.UUID;

import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.BagQueryUpgrade;
import org.intermine.api.idresolution.Job.JobStatus;

public class UpgradeJob implements Job {

    private Exception error = null;
    private final BagQueryUpgrade upgrade;
    private final Long startedAt;
    private BagQueryResult result;
    private JobStatus status;
    private final String id;

    public UpgradeJob(UUID id, BagQueryUpgrade upgrade) {
        this.upgrade = upgrade;
        this.id = id.toString();
        status = JobStatus.PENDING;
        startedAt = System.currentTimeMillis();
    }

    @Override
    public void run() {
        this.status = JobStatus.RUNNING;
        try {
            this.result = upgrade.getBagQueryResult();
        } catch (Exception e) {
            error = e;
            this.status = JobStatus.ERROR;
        }
    }

    @Override
    public BagQueryResult getResult() {
        return result;
    }

    @Override
    public boolean wasSuccessful() {
        return status == JobStatus.SUCCESS;
    }

    @Override
    public Exception getError() {
        return error;
    }

    @Override
    public String getUid() {
        return id;
    }

    @Override
    public JobStatus getStatus() {
        return status;
    }

    @Override
    public String getType() {
        return upgrade.getType();
    }

}
