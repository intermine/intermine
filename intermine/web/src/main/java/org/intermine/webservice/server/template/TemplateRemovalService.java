package org.intermine.webservice.server.template;

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
import org.intermine.api.profile.ProfileManager.ApiPermission.Level;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.core.ReadWriteJSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

/**
 * A service that deletes templates.
 * @author Alex Kalderimis
 *
 */
public class TemplateRemovalService extends ReadWriteJSONService
{

    /** @param im The InterMine state object **/
    public TemplateRemovalService(InterMineAPI im) {
        super(im);
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

    @Override
    protected void validateState() {
        if (getPermission().getLevel() == Level.RO) {
            throw new ServiceForbiddenException("Access denied.");
        }
    }

    @Override
    protected void execute() throws Exception {
        String name = StringUtils.defaultString(request.getPathInfo(), "");
        name = name.replaceAll("^/", "");
        if (StringUtils.isBlank(name)) {
            throw new BadRequestException("No name provided");
        }
        Profile p = getPermission().getProfile();
        if (!p.getSavedTemplates().containsKey(name)) {
            throw new ResourceNotFoundException("No template called " + name);
        }
        p.deleteTemplate(name, im.getTrackerDelegate(), true);
    }

}
