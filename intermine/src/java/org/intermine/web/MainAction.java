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

import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ConstraintOp;

/**
 * Action to handle button presses on the main tile
 * @author Mark Woodbridge
 */
public class MainAction extends Action
{
    /** 
     * Method called when user has finished updating a constraint
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
        MainForm mf = (MainForm) form;
        HttpSession session = request.getSession();
        Map qNodes = (Map) session.getAttribute(Constants.QUERY);
        ServletContext servletContext = session.getServletContext();
        Model model = (Model) servletContext.getAttribute(Constants.MODEL);

        RightNode node = (RightNode) qNodes.get(mf.getPath());

        if (mf.getParsedConstraintValue() != null) {
            ConstraintOp constraintOp = ConstraintOp.
                getOpForIndex(Integer.valueOf(mf.getConstraintOp()));
            Object constraintValue = mf.getParsedConstraintValue();
            node.getConstraints().add(new Constraint(constraintOp, constraintValue));
        }

        if (mf.getSubclass() != null && !mf.getSubclass().equals("")) {
            ((Node) qNodes.get(mf.getPath())).setType(mf.getSubclass());
            session.setAttribute("path", mf.getSubclass());
        }

        mf.reset(mapping, request);

        return mapping.findForward("query");
    }
}
