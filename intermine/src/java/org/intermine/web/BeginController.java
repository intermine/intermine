package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.tiles.actions.TilesAction;

import org.intermine.objectstore.query.ConstraintOp;

/**
 * Controller Action for begin.jsp. Loads class categories and provides a category to template
 * queries map to the JSP page.
 *
 * @author Thomas Riley
 */

public class BeginController extends TilesAction
{
    protected static final Logger LOG = Logger.getLogger(BeginController.class);
    
    /**
     * Populate request with browse box parameters etc.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if an error occurs
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Properties properties = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        
        if (properties != null) {
            request.setAttribute("browseTemplateName",
                                                properties.getProperty("begin.browse.template"));
            // might want to make the operator a model web.properties property
            request.setAttribute("browseOperator", ConstraintOp.MATCHES.getIndex());
        }
        
        return null;
    }
}
