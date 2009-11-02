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

import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.bag.BagManager;
import org.intermine.api.template.TemplateManager;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Display the query builder (if there is a curernt query) or redirect to project.sitePrefix.
 *
 * @author Tom Riley
 */
public class BeginAction extends InterMineAction
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
                                @SuppressWarnings("unused") ActionForm form,
                                HttpServletRequest request,
                                @SuppressWarnings("unused") HttpServletResponse response)
       throws Exception {

       HttpSession session = request.getSession();
       ServletContext servletContext = session.getServletContext();
       
       if (request.getParameter("GALAXY_URL") != null) {
           request.getSession().setAttribute("GALAXY_URL", request.getParameter("GALAXY_URL"));
           SessionMethods.recordMessage("Welcome to FlyMine, GALAXY users. ", session);
       }

       BagManager bagManager = SessionMethods.getBagManager(servletContext);
       Integer bagCount = bagManager.getGlobalBags().size();
       
       TemplateManager templateManager = SessionMethods.getTemplateManager(servletContext);
       Integer templateCount = templateManager.getValidGlobalTemplates().size();

       /* count number of templates and bags */
       request.setAttribute("bagCount", bagCount);
       request.setAttribute("templateCount", templateCount);

       Properties properties = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
       String[] beginQueryClasses = (properties.get("begin.query.classes").toString())
                                   .split("[ ,]+");
       request.setAttribute("beginQueryClasses", beginQueryClasses);
       return mapping.findForward("begin");
   }
}
