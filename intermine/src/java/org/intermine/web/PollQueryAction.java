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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.model.InterMineObject;
import org.intermine.web.results.PagedResults;

/**
 * repeatedly poll the status of a running query and forward client to appropriate page
 * each time.
 * 
 * @see org.intermine.web.QueryMonitor
 * @see org.intermine.web.QueryMonitorTimeout
 * @see org.intermine.web.SessionMethods#runQuery
 * @author Thomas Riley
 */
public class PollQueryAction extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(PollQueryAction.class);
    
    /**
     * Handle request from client.
     *
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
        String qid = request.getParameter("qid");
        boolean followSingleResult = "followSingleResult".equals(mapping.getParameter());
        
        if (StringUtils.isEmpty(qid)) {
            recordError(new ActionMessage("errors.pollquery.emptyqid", qid), request);
            return mapping.findForward("error");
        }
        
        QueryMonitorTimeout controller = (QueryMonitorTimeout)
                SessionMethods.getRunningQueryController(qid, session);
        if (controller == null) {
            String referer = request.getHeader("Referer");
            LOG.debug("referer = " + referer);
            if (referer.indexOf("objectDetails") >= 0 || referer.indexOf("results") >= 0) {
                LOG.debug("invalid qid " + qid + " redirect to query (from results/details)");
                return mapping.findForward("cancelled");
            } else if (session.getAttribute(Constants.QUERY_RESULTS) != null) {
                LOG.debug("invalid qid " + qid + " redirect to results");
                return mapping.findForward("cancelled");
            } else {
                LOG.debug("invalid qid, no results " + qid + " redirect to error");
                recordError(new ActionMessage("errors.pollquery.badqid", qid), request);
                return mapping.findForward("error");
            }
        }
        
        // First tickle the controller to avoid timeout
        controller.tickle();
        
        if (controller.isCancelledWithError()) {
            LOG.debug("query qid " + qid + " error");
            return mapping.findForward("failure");
        } else if (controller.isCancelled()) {
            LOG.debug("query qid " + qid + " cancelled");
            recordError(new ActionMessage("errors.pollquery.cancelled", qid), request);
            return mapping.findForward("cancelled");
        } else if (controller.isCompleted()) {
            LOG.debug("query qid " + qid + " complete");
            // Look at results, if only one result, go straight to object details page
            PagedResults pr = (PagedResults) session.getAttribute (Constants.QUERY_RESULTS);
            if (followSingleResult && pr.getSize () == 1
                    && ((List) pr.getAllRows ().get(0)).size() == 1) {
                Object o = ((List) pr.getAllRows ().get(0)).get(0);
                if (o instanceof InterMineObject) {
                    return new ActionForward("/objectDetails.do?id="
                            + ((InterMineObject) o).getId()
                            + "&trail=_" + ((InterMineObject) o).getId(), true);
                }
            }
            return mapping.findForward("results");
        } else {
            LOG.debug("query qid " + qid + " still running, making client wait");
            request.setAttribute("qid", request.getParameter("qid"));
            if (controller.getTickleCount() < 3) {
                request.setAttribute("POLL_REFRESH_SECONDS", new Integer(1));
            } else {
                request.setAttribute("POLL_REFRESH_SECONDS",
                                            new Integer(Constants.POLL_REFRESH_SECONDS));
            }
            int numdots = (controller.getTickleCount() % 3) + 1;
            request.setAttribute("dots", StringUtils.repeat(".", numdots));
            // there are different action mappings for different kinds of
            // query (portal, template, query builder) so we have to refresh
            // to the correct path
            request.setAttribute("POLL_ACTION_NAME", mapping.getPath());
            return mapping.findForward("waiting");
        }
    }
}