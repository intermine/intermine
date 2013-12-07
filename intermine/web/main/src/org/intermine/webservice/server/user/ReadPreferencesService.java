package org.intermine.webservice.server.user;

import java.util.HashMap;

import org.intermine.api.InterMineAPI;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.core.ReadWriteJSONService;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

public class ReadPreferencesService extends ReadWriteJSONService {

    public static final String DENIAL_MSG = "All requests to read and manage preferences must be authenticated";

    public ReadPreferencesService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected String getResultsKey() {
        return "preferences";
    }

    @Override
    protected void execute() throws ServiceException {
        addResultItem(new HashMap<String, Object>(getPermission().getProfile().getPreferences()), false);
    }

}
