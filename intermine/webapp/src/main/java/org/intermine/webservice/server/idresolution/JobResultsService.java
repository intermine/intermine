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
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.NoContentException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceException;

/** @author Alex Kalderimis **/
public class JobResultsService extends JSONService
{
    private final String jobId;

    /**
     * Construct a jobs service handler.
     * @param im The InterMine state object.
     * @param jobId The job we are interested in.
     */
    public JobResultsService(InterMineAPI im, String jobId) {
        super(im);
        this.jobId = jobId;
    }

    @Override
    protected void execute() {
        BagResultFormatter formatter;
        if ("true".equals(getOptionalParameter("idkeys", "false"))) {
            formatter = new BagResultOutputKeyFormatter(im);
        } else {
            formatter = new BagResultCategoryKeyFormatter(im);
        }
        Job job = IDResolver.getInstance().getJobById(jobId);
        if (job != null) {
            if (job.getStatus() != Job.JobStatus.SUCCESS) {
                ServiceException se;
                if (job.getStatus() == Job.JobStatus.ERROR) {
                    se = new NoContentException("Job failed: " +  job.getError().getMessage());
                    this.addOutputInfo("message", job.getError().getMessage());
                } else {
                    se = new NoContentException("Job not ready");
                }
                throw se;
            }

            addResultItem(formatter.format(job), false);
        } else {
            throw new ResourceNotFoundException("No such job");
        }
    }

    @Override
    protected String getResultsKey() {
        return "results";
    }

}
