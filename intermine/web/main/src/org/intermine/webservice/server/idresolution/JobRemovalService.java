package org.intermine.webservice.server.idresolution;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.idresolution.IDResolver;
import org.intermine.api.idresolution.Job;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;

public class JobRemovalService extends JSONService {

    public JobRemovalService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        String uid = StringUtils.defaultString(request.getPathInfo(), "").replaceAll("^/", "");

        IDResolver idresolver = IDResolver.getInstance();

        if (idresolver.removeJob(uid) == null) {
            throw new ResourceNotFoundException("Unknown id: " + uid);
        }
    }

}
