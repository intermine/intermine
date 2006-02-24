/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

package org.intermine.web;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * 
 * @author tom riley
 */
public class BeginAction extends InterMineAction
{
   /**
    * Either display the begin.page or redirect to project.sitePrefix.
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
       Boolean archived = (Boolean) request.getSession().getServletContext()
           .getAttribute(Constants.ARCHIVED);
       if (archived == Boolean.TRUE) {
           return mapping.findForward("begin");
       } else {
           return new ForwardParameters(getWebProperties(request)
                   .getProperty("project.sitePrefix"), true).forward();
       }
   }

}
