package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.profile.Profile;
import org.intermine.web.logic.session.SessionMethods;

/**
 * A controller to edit the properties of a running application.
 * @author Alex Kalderimis
 */
public class EditPropertiesAction extends InterMineAction
{

    @Override
    public ActionForward execute(
            ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response) {
        Profile user = SessionMethods.getProfile(request.getSession());
        if (!user.isSuperuser()) {
            recordMessage(new ActionMessage("edit-properties.not-authorised"), request);
            return mapping.findForward("failure");
        }
        Properties webProperties = SessionMethods.getWebProperties(request);
        EditPropertiesForm epf = (EditPropertiesForm) form;
        if (epf.getPropertyName() != null) {
            webProperties.setProperty(epf.getPropertyName(), epf.getPropertyValue());
            Map<String, List<String>> origins
                = SessionMethods.getPropertiesOrigins(request.getSession());
            if (!origins.containsKey(epf.getPropertyName())) {
                origins.put(epf.getPropertyName(), new ArrayList<String>());
            }
            origins.get(epf.getPropertyName()).add("Runtime change: at " + new Date());
        }

        recordMessage(new ActionMessage("edit-properties.changed",
                epf.getPropertyName(), epf.getPropertyValue()), request);
        return mapping.findForward("success");
    }
}
