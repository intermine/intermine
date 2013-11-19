package org.intermine.webservice.server.idresolution;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.ConvertedObjectPair;
import org.intermine.api.util.PathUtil;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.util.DynamicUtil;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.idresolution.Job.JobStatus;
import org.intermine.webservice.server.output.JSONFormatter;
import org.json.JSONObject;

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
        Job job = Job.getJobById(jobId);
        if (job != null) {
            if (job.getStatus() != JobStatus.SUCCESS) {
                ServiceException se;
                if (job.getStatus() == JobStatus.ERROR) {
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
