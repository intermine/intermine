package org.intermine.webservice.server.user;

import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.webservice.server.exceptions.ServiceException;

public class DeletePreferencesService extends ReadPreferencesService {

    public DeletePreferencesService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws ServiceException {
        String key = getOptionalParameter("key");
        Map<String, String> preferences = getPermission().getProfile().getPreferences();
        if (key == null) {
            preferences.clear();
        } else {
            preferences.remove(key);
        }
        super.execute();
    }

}
