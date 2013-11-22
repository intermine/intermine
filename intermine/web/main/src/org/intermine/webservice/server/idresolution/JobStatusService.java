package org.intermine.webservice.server.idresolution;

import org.intermine.api.InterMineAPI;
import org.intermine.api.idresolution.IDResolver;
import org.intermine.api.idresolution.Job;
import org.intermine.api.idresolution.Job.JobStatus;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;

public class JobStatusService extends JSONService
{

    private final String jobId;

    public JobStatusService(InterMineAPI im, String jobId) {
        super(im);
        this.jobId = jobId;
    }

    @Override
    protected void execute() throws Exception {
        Job job = IDResolver.getInstance().getJobById(jobId);
        if (job != null) {
            if (job.getStatus() == JobStatus.ERROR) {
                this.addOutputInfo("message", job.getError().getMessage());
            }
            addResultValue(job.getStatus().name(), false);
        } else {
            throw new ResourceNotFoundException("No such job: " + jobId);
        }
    }

    @Override
    protected String getResultsKey() {
        return "status";
    }

}
