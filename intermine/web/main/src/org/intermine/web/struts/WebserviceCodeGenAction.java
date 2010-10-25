package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2010 FlyMine
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

/**
 * Action to handle the web service code generation
 *
 * @author Fengyuan Hu
 */
public class WebserviceCodeGenAction extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(WebserviceCodeGenAction.class);

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        String method = request.getParameter("method");
        String source = request.getParameter("source");

        LOG.info("method >>>>> " + method);
        LOG.info("source >>>>> " + source);

        return null;
    }
}
