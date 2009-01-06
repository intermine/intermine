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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.web.logic.export.ResponseUtil;

/**
 * ResultsAction is called before results table is rendered. Can be used for setting
 * various things needed before table is rendered and that cannot be set in controller
 * as it is case of response headers. However for setting rendering related variables use 
 * TableController.
 *   
 * @author Jakub Kulaviak
 *
 */
public class ResultsAction extends InterMineAction
{
    public ActionForward execute(ActionMapping mapping,
            @SuppressWarnings("unused")
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        ResponseUtil.setNoCacheEnforced(response);
        return mapping.findForward("success");
    }

}