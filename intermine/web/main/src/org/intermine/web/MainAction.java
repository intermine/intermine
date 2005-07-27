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

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.intermine.objectstore.query.ConstraintOp;

/**
 * Action to handle button presses on the main tile
 * @author Mark Woodbridge
 */
public class MainAction extends InterMineAction
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
        HttpSession session = request.getSession();
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        MainForm mf = (MainForm) form;

        PathNode node = (PathNode) query.getNodes().get(mf.getPath());

        Integer cindex = (request.getParameter("cindex") != null) ?
                new Integer(request.getParameter("cindex")) : null;
        if (cindex != null) {
            // We're updating an existing constraint, just remove the old one
            node.removeConstraint((Constraint) node.getConstraints().get(cindex.intValue()));
            session.removeAttribute("editingConstraintIndex");
            session.removeAttribute("editingConstraintValue");
            session.removeAttribute("editingConstraintOperand");
        }
        
        if (request.getParameter("attribute") != null) {
            ConstraintOp constraintOp = ConstraintOp.
                getOpForIndex(Integer.valueOf(mf.getAttributeOp()));
            Object constraintValue = mf.getParsedAttributeValue();
            node.getConstraints().add(new Constraint(constraintOp, constraintValue));
        }

        if (request.getParameter("bag") != null) {
            ConstraintOp constraintOp = ConstraintOp.
                getOpForIndex(Integer.valueOf(mf.getBagOp()));
            Object constraintValue = mf.getBagValue();
            node.getConstraints().add(new Constraint(constraintOp, constraintValue));
        }

        if (request.getParameter ("loop") != null) {
            ConstraintOp constraintOp = ConstraintOp.
                getOpForIndex(Integer.valueOf(mf.getLoopQueryOp()));
            Object constraintValue = mf.getLoopQueryValue();
            node.getConstraints().add(new Constraint(constraintOp, constraintValue));
        }
        
        if (request.getParameter("subclass") != null) {
            node.setType(mf.getSubclassValue());
            session.setAttribute("path", mf.getSubclassValue());
        }
        
        if (request.getParameter("nullnotnull") != null) {
            if (mf.getNullConstraint().equals("NotNULL")) {
                node.getConstraints().add(new Constraint(ConstraintOp.IS_NOT_NULL, null));
            } else {
                node.getConstraints().add(new Constraint(ConstraintOp.IS_NULL, null));
            }
        }

        mf.reset(mapping, request);

        return mapping.findForward("query");
    }
}
