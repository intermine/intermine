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
        Job job = Job.getJobById(jobId);
        if (job != null) {
            if (job.getStatus() != JobStatus.SUCCESS) {
                ServiceException se;
                if (job.getStatus() == JobStatus.ERROR) {
                    se = new ServiceException("Job failed: " +  job.getError().getMessage());
                } else {
                    se = new ServiceException("Job not ready");
                }
                se.setHttpErrorCode(204); // No Content.
                throw se; 
            }
            Map<String, Object> ret = new HashMap<String, Object>();
            
            doMatches(ret, job.getResult());
            doDuplicates(ret, job.getResult(), BagQueryResult.DUPLICATE);
            doDuplicates(ret, job.getResult(), BagQueryResult.WILDCARD);
            doDuplicates(ret, job.getResult(), BagQueryResult.OTHER);
            doDuplicates(ret, job.getResult(), BagQueryResult.TYPE_CONVERTED);
            
            // also wildcards, conversions, other, unresolved.
            output.addResultItem(Arrays.asList(new JSONObject(ret).toString()));
        } else {
            throw new ResourceNotFoundException("No such job");
        }
    }
    
    @Override
    protected void cleanUp() {
        Job.JOBS.remove(jobId);
    }
    
    private void doDuplicates(Map<String, Object> ret, BagQueryResult bqr, String key) {
        
        Map<String, Map<String, List>> issues = bqr.getIssues().get(key);
        if (issues == null) {
            return;
        }
        for (Map<String, List> issueSet: issues.values()) {
            for (Entry<String, List> identToObjects: issueSet.entrySet()) {
                String ident = identToObjects.getKey();
                for (Object o: identToObjects.getValue()) {
                    InterMineObject imo;
                    Map<String, Object> resultItem;
                    if (o instanceof Integer) {
                        try {
                            imo = im.getObjectStore().getObjectById((Integer) o);
                        } catch (ObjectStoreException e) {
                            throw new IllegalStateException("Could not retrieve object reported as match", e);
                        }
                    } else if (o instanceof ConvertedObjectPair) {
                        imo = ((ConvertedObjectPair) o).getNewObject();
                    } else {
                        try {
                            imo = (InterMineObject) o;
                        } catch (ClassCastException cce) {
                            throw new InternalErrorException("When processing " + key, cce);
                        }
                    }
                    String idKey = String.valueOf(imo.getId());
                    if (ret.containsKey(idKey)) {
                        resultItem = (Map<String, Object>) ret.get(idKey);
                    } else {
                        resultItem = new HashMap<String, Object>();
                        resultItem.put("identifiers", new HashMap<String, Object>());
                    }
                    if (!resultItem.containsKey("summary")) {
                        resultItem.put("summary", getObjectDetails(imo));
                    }
                    Map<String, Object> identifiers = (Map<String, Object>) resultItem.get("identifiers");
                    
                    if (!identifiers.containsKey(ident)) {
                        identifiers.put(ident, new HashSet<String>());
                    }
                    Set<String> categories = (Set<String>) identifiers.get(ident);
                    categories.add(key);
                    String className = DynamicUtil.getSimpleClassName(imo.getClass());
                    resultItem.put("type", className.replaceAll("^.*\\.", ""));
                    ret.put(idKey, resultItem);
                }
            }
        }
    }
    
    private void doMatches(Map<String, Object> ret, BagQueryResult bqr) {
        
        for (Entry<Integer, List> pair: bqr.getMatches().entrySet()) {
            Map<String, Object> resultItem;
            InterMineObject imo;
            try {
                imo = im.getObjectStore().getObjectById(pair.getKey());
            } catch (ObjectStoreException e) {
                throw new IllegalStateException("Could not retrieve object reported as match", e);
            }
            String idKey = String.valueOf(imo.getId());
            if (ret.containsKey(idKey)) {
                resultItem = (Map<String, Object>) ret.get(idKey);
            } else {
                resultItem = new HashMap<String, Object>();
                resultItem.put("identifiers", new HashMap<String, Object>());
            }
            if (!resultItem.containsKey("summary")) {
                resultItem.put("summary", getObjectDetails(imo));
            }
            Map<String, Object> identifiers = (Map<String, Object>) resultItem.get("identifiers");
            for (Object o: pair.getValue()) {
                String ident = (String) o;
                if (!identifiers.containsKey(ident)) {
                    identifiers.put(ident, new HashSet<String>());
                }
                Set<String> categories = (Set<String>) identifiers.get(ident);
                categories.add("MATCH");
            }
            String className = DynamicUtil.getSimpleClassName(imo.getClass());
            resultItem.put("type", className.replaceAll("^.*\\.", ""));
            ret.put(idKey, resultItem);
        }
    }
    
    private Map<String, Object> getObjectDetails(InterMineObject imo) {
        WebConfig webConfig = InterMineContext.getWebConfig();
        Model m = im.getModel();
        Map<String, Object> objectDetails = new HashMap<String, Object>();
        String className = DynamicUtil.getSimpleClassName(imo.getClass());
        ClassDescriptor cd = m.getClassDescriptorByName(className);
        for (FieldConfig fc : FieldConfigHelper.getClassFieldConfigs(webConfig, cd)) {
            try {
                Path p = new Path(m, cd.getUnqualifiedName() + "." + fc.getFieldExpr());
                if (p.endIsAttribute() && fc.getShowInSummary()) {
                    objectDetails.put(
                            p.getNoConstraintsString().replaceAll("^[^.]*\\.", ""),
                            PathUtil.resolvePath(p, imo));
                }
            } catch (PathException e) {
                LOG.error(e);
            }
        }
        return objectDetails;
    }
    
    @Override
    protected Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = super.getHeaderAttributes();
        attributes.put(JSONFormatter.KEY_INTRO, "\"results\":");
        attributes.put(JSONFormatter.KEY_OUTRO, "");
        return attributes;
    }


}
