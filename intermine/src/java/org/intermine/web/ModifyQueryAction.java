package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.StringUtil;

/**
 * Implementation of <strong>Action</strong> that modifies a saved query
 *
 * @author Mark Woodbridge
 */
public class ModifyQueryAction extends Action
{
    protected static final String INDENT = "   ";

    /**
     * Forward to the correct method based on the button pressed
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
        if (request.getParameter("export") != null) {
            export(mapping, form, request, response);
        } if (request.getParameter("delete") != null) {
            delete(mapping, form, request, response);
        }

        return mapping.findForward("history");
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
        Map savedQueries = (Map) session.getAttribute(Constants.SAVED_QUERIES);

        ModifyQueryForm mqf = (ModifyQueryForm) form;
        
        for (int i = 0; i < mqf.getSelectedQueries().length; i++) {
            savedQueries.remove(mqf.getSelectedQueries()[i]);
        }

        return mapping.findForward("history");
    }

    /**
     * Export some queries
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
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Model model = (Model) os.getModel();
        Map savedQueries = (Map) session.getAttribute(Constants.SAVED_QUERIES);
        String[] selectedQueries = ((ModifyQueryForm) form).getSelectedQueries();
        
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition", "Attachment; Filename=\"savedQueries.xml\"");

        PrintWriter out = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));
        out.println("<query-list>");
        for (int i = 0; i < selectedQueries.length; i++) {
            String queryName = (String) selectedQueries[i];
            QueryInfo queryInfo = (QueryInfo) savedQueries.get(queryName);
            Map qNodes = queryInfo.getQuery();
            List view = queryInfo.getView();
            out.println(INDENT + "<query name='" + queryName + "' model='" + model.getName()
                        + "' view='" + StringUtil.join(view, " ") + "'>");
            for (Iterator j = qNodes.values().iterator(); j.hasNext();) {
                RightNode node = (RightNode) j.next();
                if (node.getConstraints().size() > 0) {
                    out.println(INDENT + INDENT + "<node path='" + node.getPath() + "' type='"
                                + node.getType() + "'>");
                    for (Iterator k = node.getConstraints().iterator(); k.hasNext();) {
                        Constraint c = (Constraint) k.next();
                        out.println(INDENT + INDENT + INDENT + "<constraint op='" + c.getOp()
                                    + "' value='" + c.getValue() + "'/>");
                    }
                    out.println(INDENT + INDENT + "</node>");
                }
            }
            out.println(INDENT + "</query>");
        }
        out.println("</query-list>");
        out.close();

        return null;
    }
}