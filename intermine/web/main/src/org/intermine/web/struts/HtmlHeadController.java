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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Cookie;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.search.Scope;
import org.intermine.api.template.TemplateManager;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.template.TemplateQuery;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.results.ReportObjectFactory;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for the html head tile.  Determines what is shown on the title of the webpage
 *
 * @author Julie Sullivan
 */
public class HtmlHeadController extends TilesAction
{
    protected static final Logger LOG = Logger.getLogger(HtmlHeadController.class);

    /**
     *
     * @param context The Tiles ComponentContext
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if an error occurs
     */
    @Override
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        ObjectStore os = im.getObjectStore();
        ReportObjectFactory reportObjects = SessionMethods.getReportObjects(session);

        String pageName = (String) context.getAttribute("pageName");
        String bagName = (String) context.getAttribute("bagName");
        String objectId = (String) context.getAttribute("objectId");
        String name = (String) context.getAttribute("name");
        String htmlPageTitle = (String) context.getAttribute("pageNameTitle");
        String scope = (String) context.getAttribute("scope");

        /* aspect */
        if (name != null && "aspect".equals(pageName)) {

            htmlPageTitle = htmlPageTitle + ":  " + name;

        /* bag */
        } else if ("bagDetails".equals(pageName)) {

            if (bagName != null && !"".equals(bagName)) {
                htmlPageTitle = htmlPageTitle + ":  " + bagName;
            } else {
                htmlPageTitle = htmlPageTitle + ":  " + name;
            }

        /* template */
        } else if ("template".equals(pageName)) {

            String templateTitle = "";
            TemplateQuery template = null;
            Profile profile = null;

            TemplateManager templateManager = im.getTemplateManager();
            if (scope != null && scope.equals(Scope.USER)) {
                profile = SessionMethods.getProfile(session);
                template = templateManager.getUserOrGlobalTemplate(profile, name);
            } else {
                template = templateManager.getGlobalTemplate(name);
            }

            if (template != null) {
                templateTitle = ":  " + template.getTitle();
            }

            htmlPageTitle = htmlPageTitle + templateTitle;

        /* report page */
        } else if ("report".equals(pageName) && objectId != null) {
            Integer id = null;
            try {
                id = new Integer(Integer.parseInt(objectId));

                InterMineObject object = os.getObjectById(id);
                if (object == null) {
                    request.setAttribute("htmlPageTitle", htmlPageTitle);
                    return null;
                }

                ReportObject reportObject = reportObjects.get(object);
                htmlPageTitle = reportObject.getHtmlHeadTitle();

            } catch (Exception e) {
                LOG.warn("Could not correctly set the page title for object ID - " + objectId);
            }
        }
        request.setAttribute("htmlPageTitle", htmlPageTitle);

        // Can we employ user tracking?
        String userTrackingMessage = (String) SessionMethods.getWebProperties(
                request.getSession().getServletContext()).get(
                "google.analytics.message");
        request.setAttribute("userTrackingMessage", userTrackingMessage);
        request.setAttribute("userTracking", canWeUserTrack(request));

        return null;
    }

    /**
     * Determone if we can employ user tracking
     * @param request HTTP Servlet Request
     * @return Integer 0/1/2 - "[no]/[yes]/[not yet]"
     */
    private String canWeUserTrack(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return "2";
        }
        for (int i = 0; i < cookies.length; i++) {
            Cookie cookie = cookies[i];
            if ("userTracking".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return "2";
    }

}
