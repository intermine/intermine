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

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.ForwardParameters;
import org.intermine.web.logic.PathQuery;
import org.intermine.web.logic.Profile;
import org.intermine.web.logic.SavedQuery;
import org.intermine.web.logic.SessionMethods;
import org.intermine.web.logic.TemplateQuery;

/**
 * Action to display the query builder (if there is a current query) or redirect to
 * project.sitePrefix.
 * @author Tom Riley
 */
public class QueryAction extends InterMineAction
{
   /**
    * Either display the query builder or redirect to project.sitePrefix.
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
       HttpSession session = request.getSession();
       PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
       boolean showTemplate = (request.getParameter("showTemplate") != null);

       if (query == null) {
           SessionMethods.setHasQueryCookie(session, response, false);
           return new ForwardParameters(getWebProperties(request)
                   .getProperty("project.sitePrefix"), true).forward();
       }

        if (query instanceof TemplateQuery && showTemplate) {
            TemplateQuery template = (TemplateQuery) query;
            if (template.isEdited()) {
                Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
                SavedQuery sq = null;
                for (Iterator iter = profile.getHistory().values().iterator(); iter.hasNext();) {
                    sq = (SavedQuery) iter.next();
                    if (sq.getPathQuery().equals(template)) {
                        break;
                    }
                }
                if (sq != null) {
                return new ForwardParameters(mapping.findForward("template"))
                    .addParameter("loadModifiedTemplate",
                              "true")
                    .addParameter("name", sq.getName())
                              .forward();
                } else { // The template is quick search
                   return new ForwardParameters(mapping.findForward("template"))
                    .addParameter("name", template.getName()).forward();
                }
            } else {
                return new ForwardParameters(mapping.findForward("template"))
                    .addParameter("name", template.getName()).forward();
            }
        } else {
            return mapping.findForward("query");
        }
    }

}