package org.intermine.webservice.server.query;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.server.core.ReadWriteJSONService;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceException;

public class QueryRemovalService extends ReadWriteJSONService {

    public QueryRemovalService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws ServiceException {
        String name = getRequiredParameter("name");
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

}
