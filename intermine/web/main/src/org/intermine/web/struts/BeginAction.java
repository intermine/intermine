package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

import org.intermine.web.logic.Constants;
import org.intermine.web.logic.search.SearchRepository;
import org.intermine.web.logic.search.WebSearchable;
import org.intermine.web.logic.session.SessionMethods;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

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
       SearchRepository searchRepository = (SearchRepository)
                               servletContext.getAttribute(Constants.GLOBAL_SEARCH_REPOSITORY);
       if (request.getParameter("GALAXY_URL") != null) {
           request.getSession().setAttribute("GALAXY_URL", request.getParameter("GALAXY_URL"));
           SessionMethods.recordMessage("Welcome to FlyMine, GALAXY users. ", session);
       }

       Map<String, ? extends WebSearchable> webSearchables =
                                                       searchRepository.getWebSearchableMap("bag");
       int bagCount = webSearchables.size();
       webSearchables = searchRepository.getWebSearchableMap("template");
       int templateCount = webSearchables.size();

       /* count number of templates and bags */
       request.setAttribute("bagCount", new Integer(bagCount));
       request.setAttribute("templateCount", new Integer(templateCount));

       return mapping.findForward("begin");
   }
}
