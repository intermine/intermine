package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.results.DisplayObject;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.template.TemplateQuery;

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
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Map displayObjects = SessionMethods.getDisplayObjects(session);

        String pageName = (String) context.getAttribute("pageName");
        String bagName = (String) context.getAttribute("bagName");
        String objectId = (String) context.getAttribute("objectId");
        String name = (String) context.getAttribute("name");
        String htmlPageTitle = (String) context.getAttribute("pageNameTitle");
        String scope = (String) context.getAttribute("scope");

        /* aspect */
        if (name != null && pageName.equals("aspect")) {

            htmlPageTitle = htmlPageTitle + ":  " + name;

        /* bag */
        } else if (pageName.equals("bagDetails")) {

            if (bagName != null && !bagName.equals("")) {
                htmlPageTitle = htmlPageTitle + ":  " + bagName;
            } else {
                htmlPageTitle = htmlPageTitle + ":  " + name;
            }

        /* template */
        } else if (pageName.equals("template")) {

            String templateTitle = "";
            String username = "";
            if (scope != null && scope.equals("user")) {
                username = ((Profile) session
                                .getAttribute(Constants.PROFILE)).getUsername();
            } else {
                ProfileManager pm = SessionMethods.getProfileManager(servletContext);
                username = pm.getSuperuser();
            }
            TemplateQuery template = TemplateHelper.findTemplate(servletContext, session,
                                      username, name, TemplateHelper.ALL_TEMPLATE);
            if (template != null) {
                templateTitle = ":  " + template.getTitle();
            }

            htmlPageTitle = htmlPageTitle + templateTitle;

        /* object */
        } else if (pageName.equals("objectDetails") && objectId != null) {

            Integer id = new Integer(Integer.parseInt(objectId));
            InterMineObject object = os.getObjectById(id);
            if (object == null) {
                return null;
            }
            DisplayObject dobj = (DisplayObject) displayObjects.get(id);
            if (dobj == null) {
                dobj = ObjectDetailsController.makeDisplayObject(session, object);
            }

            // TODO use the class keys instead
            String idForPageTitle = "";
            if (dobj.getAttributes().get("primaryIdentifier") != null) {
                idForPageTitle = dobj.getAttributes().get("primaryIdentifier").toString();
            }
            if (idForPageTitle == null
                            && !idForPageTitle.equals("")
                            && dobj.getAttributes().get("secondaryIdentifier") != null) {
                idForPageTitle = dobj.getAttributes().get("secondaryIdentifier").toString();
            }
            if (idForPageTitle == null
                            && !idForPageTitle.equals("")
                            && dobj.getAttributes().get("identifier") != null) {
                idForPageTitle = dobj.getAttributes().get("identifier").toString();
            }

            if (idForPageTitle != null && !idForPageTitle.equals("")) {
                htmlPageTitle = htmlPageTitle + ":  " + idForPageTitle;
            }

        }
        request.setAttribute("htmlPageTitle", htmlPageTitle);
        return null;
    }


}
