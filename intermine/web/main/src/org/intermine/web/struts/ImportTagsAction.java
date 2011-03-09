package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.TagManager;
import org.intermine.api.search.SearchRepository;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.api.xml.TagBinding;
import org.intermine.api.xml.TagHandler;
import org.intermine.web.logic.session.SessionMethods;

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
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        ImportTagsForm f = (ImportTagsForm) form;
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);
        ProfileManager pm = im.getProfileManager();
        if (f.isOverwriting()) {
            TagManager tm = im.getTagManager();
            tm.deleteTags(null, null, null, profile.getUsername());
        }
        StringReader reader = new StringReader(f.getXml());
        int count = 0;
        if (!StringUtils.isEmpty(f.getXml())) {
            count = new TagBinding().unmarshal(pm, profile.getUsername(), reader);
        }
        recordMessage(new ActionMessage("history.importedTags", new Integer(count)), request);

        if (SessionMethods.isSuperUser(session)) {
            SearchRepository sr = SessionMethods.getGlobalSearchRepository(
                                                 session.getServletContext());
            sr.globalChange(TagTypes.TEMPLATE);
            sr.globalChange(TagTypes.BAG);
        }
        return mapping.findForward("success");
    }
}
