package org.intermine.webservice.server.idresolution;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.InterMineAPI;
import org.intermine.api.idresolution.IDResolver;
import org.intermine.api.idresolution.Job;
import org.intermine.api.idresolution.Job.JobStatus;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;

/** @author Alex Kalderimis **/
public class JobStatusService extends JSONService
{

    private final String jobId;

    /**
     * Construct a handler for this request.
     * @param im The InterMine state object.
     * @param jobId The id of the job.
     */
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
