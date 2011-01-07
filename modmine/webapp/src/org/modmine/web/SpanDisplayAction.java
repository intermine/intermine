package org.modmine.web;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.web.struts.InterMineAction;

/**
 * This action forward the url params to SpanUploadResultsController
 * @author Fengyuan Hu
 */
public class SpanDisplayAction extends InterMineAction
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(SpanDisplayAction.class);

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        request.setAttribute("fromSpanDisplayAction", "true");
        request.setAttribute("dataId", request.getParameter("dataId"));
        request.setAttribute("pageSize", request.getParameter("pageSize"));
        request.setAttribute("page", request.getParameter("page"));
        request.setAttribute("method", request.getParameter("method"));

        return mapping.findForward("spanUploadResults");
    }
}
