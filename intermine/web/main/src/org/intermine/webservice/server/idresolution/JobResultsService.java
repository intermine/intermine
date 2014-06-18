package org.intermine.webservice.server.idresolution;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.idresolution.IDResolver;
import org.intermine.api.idresolution.Job;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceException;

public class JobResultsService extends JSONService
{
    private static final Logger LOG = Logger.getLogger(JobResultsService.class);
    private final String jobId;

    public JobResultsService(InterMineAPI im, String jobId) {
        super(im);
        this.jobId = jobId;
        
    }

    @Override
    protected void execute() throws Exception {
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
                    se = new ServiceException("Job failed: " +  job.getError().getMessage());
                    this.addOutputInfo("message", job.getError().getMessage());
                } else {
                    se = new ServiceException("Job not ready");
                }
                se.setHttpErrorCode(204); // No Content.
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
