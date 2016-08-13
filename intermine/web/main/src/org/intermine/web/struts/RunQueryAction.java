package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Run a query as passed in directly as XML.
 * @author Alex Kalderimis
 *
 */
public class RunQueryAction extends InterMineAction
{

    private static final Logger LOG = Logger.getLogger(RunQueryAction.class);

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        QueryForm qf = (QueryForm) form;
        try {
            PathQuery pq
                = PathQueryBinding.unmarshalPathQuery(
                        new StringReader(qf.getQuery()), PathQuery.USERPROFILE_VERSION);
            HttpSession session = request.getSession();
            SessionMethods.setQuery(session, pq);
        } catch (Exception e) {
            ActionMessage msg = new ActionMessage("struts.runquery.failed", e.getMessage());
            recordError(msg, request, e, LOG);
            return mapping.findForward("failure");
        }
        return mapping.findForward("success");
    }
}
