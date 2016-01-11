package org.intermine.webservice.server.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.core.ReadWriteJSONService;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceException;

/**
 * A service that removes a saved query from profile's collection of saved queries.
 * @author Alex Kalderimis
 *
 */
public class QueryRemovalService extends ReadWriteJSONService
{

    /** @param im The InterMine state object **/
    public QueryRemovalService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() {
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
