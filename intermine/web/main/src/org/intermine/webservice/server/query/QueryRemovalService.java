package org.intermine.webservice.server.query;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.core.ReadWriteJSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceException;

public class QueryRemovalService extends ReadWriteJSONService {

    public QueryRemovalService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws ServiceException {
        String name = getName();
        Profile p = getPermission().getProfile();
        if (!p.getSavedQueries().containsKey(name)) {
            throw new ResourceNotFoundException("Could not find query named " + name);
        }
        try {
            p.deleteQuery(name);
        } catch (Exception e) {
            throw new ServiceException("Could not delete query " + name);
        }
    }

    // Allow name as /user/queries/:name and /user/queries?name=:name
    private String getName() {
        String name = StringUtils.defaultString(request.getPathInfo(), "");
        name = name.replaceAll("^/", "");
        if (StringUtils.isBlank(name)) {
            name = getRequiredParameter("name");
        }
        return name; 
    }

    @Override
    protected boolean canServe(Format format) {
        switch (format) {
        case XML:
        case JSON:
            return true;
        default:
            return false;
        }
    }

}
