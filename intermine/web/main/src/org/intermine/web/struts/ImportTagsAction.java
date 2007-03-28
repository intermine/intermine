package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.Profile;
import org.intermine.web.logic.ProfileManager;
import org.intermine.web.logic.SessionMethods;
import org.intermine.web.logic.TagBinding;

/**
 * Import tags.
 * 
 * @author Thomas Riley
 */
public class ImportTagsAction extends InterMineAction
{

    /**
     * Import user's tags.
     * 
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        ImportTagsForm f = (ImportTagsForm) form;
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ProfileManager pm = SessionMethods.getProfileManager(servletContext);
        StringReader reader = new StringReader(f.getXml());
        int count = 0;
        if (!StringUtils.isEmpty(f.getXml())) {
            count = new TagBinding().unmarshal(pm, profile.getUsername(), reader);
        }
        recordMessage(new ActionMessage("history.importedTags", new Integer(count)), request);
        return mapping.findForward("success");
    }
    
}
