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

import java.util.Map;

import java.io.PrintStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.util.XmlUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.query.PathQueryBinding;
import org.intermine.web.logic.query.SavedQuery;

/**
 * Action that results from a button press on the user profile page.
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
 */
public class ModifyQueryAction extends InterMineAction
{
    private static final Logger LOG = Logger.getLogger(ModifyQueryAction.class);
    
    /**
     * Forward to the correct method based on the button pressed.
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        if (request.getParameter("delete") != null) {
            return delete(mapping, form, request, response);
        } else {
            if (request.getParameter("export") != null) {
                return export(mapping, form, request, response);
            } else {
                LOG.error("Don't know what to do");
                return null;
            }
        }
    }

    /**
     * Delete some queries
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward delete(ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ModifyQueryForm mqf = (ModifyQueryForm) form;
        String type = request.getParameter("type");

        try {
            profile.disableSaving();
            for (int i = 0; i < mqf.getSelectedQueries().length; i++) {
                if ("history".equals(type)) {
                    profile.deleteHistory(mqf.getSelectedQueries()[i]);
                } else {
                    profile.deleteQuery(mqf.getSelectedQueries()[i]);
                }
            }
        } finally {
            if (profile.getUsername() != null) {
                profile.enableSaving();
            }
        }

        if ("history".equals(type)) {
            return new ForwardParameters(mapping.findForward("mymine"))
            .addParameter("subtab", "history").forward();
        } else {
            return new ForwardParameters(mapping.findForward("mymine"))
            .addParameter("subtab", "saved").forward();
        }
    }
    
    /**
     * Export the selected queries
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward export(ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ModifyQueryForm mqf = (ModifyQueryForm) form;
        String type = request.getParameter("type");

        response.setContentType("text/plain; charset=us-ascii");
        response.setHeader("Content-Disposition ", "inline; filename=saved-queries.xml");
        
        Map<String, SavedQuery> map;
        
        if ("history".equals(type)) {
            map = profile.getHistory();
        } else {
            map = profile.getSavedQueries();
        }
        
        PrintStream out = new PrintStream(response.getOutputStream());
        out.println("<queries>");
        for (int i = 0; i < mqf.getSelectedQueries().length; i++) {
            String name = mqf.getSelectedQueries()[i];
            PathQuery query = map.get(name).getPathQuery();
            String modelName = query.getModel().getName();
            String xml = PathQueryBinding.marshal(query, name, modelName);
            xml = XmlUtil.indentXmlSimple(xml);
            out.println(xml);
        }
        out.println("</queries>");
        
        return null;
    }
}