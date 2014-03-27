package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.DuplicateMappingException;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceException;

public class SetPreferencesService extends ReadPreferencesService {

    public SetPreferencesService(InterMineAPI im) {
        super(im);
    }

    private static final Set<String> BLACKLISTED_NAMES = new HashSet<String>(Arrays.asList("token", "format"));

    @SuppressWarnings("unchecked")
    @Override
    protected void execute() throws ServiceException {
        Map<String, String> newPrefs = new HashMap<String, String>();
        for (Object key: request.getParameterMap().keySet()) {
            String pname = String.valueOf(key);
            if (!BLACKLISTED_NAMES.contains(pname)) {
                // This is now a required value; ie. cannot be blank.
                // If you want to delete a key, delete it instead.
                newPrefs.put(pname, getRequiredParameter(pname));
            }
        } 
        Profile p = getPermission().getProfile();
        try {
            p.getPreferences().putAll(newPrefs);
        } catch (DuplicateMappingException e) {
            throw new BadRequestException(e.getMessage());
        }
        super.execute();
    }

}
