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

import org.intermine.web.results.PagedTable;
import org.intermine.web.results.ResultElement;
import org.intermine.web.results.WebResults;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

/**
 * Repeatedly poll the status of a running query and forward client to appropriate page
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
            LOG.debug("invalid qid " + qid + " redirecting as if cancelled");
            return mapping.findForward("cancelled");
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
            PagedTable pr = SessionMethods.getResultsTable(session, "results." + qid);
            if (followSingleResult) {
                List allRows = pr.getAllRows ();
                if ((allRows instanceof WebResults)) {
                    WebResults webResults = (WebResults) allRows;
                    // Query can have more than one column, forward from the first
                    Object cell = null;
                    Integer forwardId = null;
                    if (webResults.size() > 0) {
                        cell = webResults.getResultElements(0).get(0);
                        if (cell instanceof ResultElement) {
                            forwardId = ((ResultElement) cell).getId();
                        }
                        if (forwardId != null && webResults.size() > 1 && webResults.size() < 100) {
                            // special case hack - if every element of the first column is the same,
                            // use that as the object to forward to
                            for (int i = 1; i < webResults.size() && forwardId != null; i++) {
                                cell = webResults.getResultElements(i).get(0);
                                if (cell instanceof ResultElement) {
                                    if (!forwardId.equals(((ResultElement) cell).getId())) {
                                        forwardId = null;
                                    }
                                }
                            }
                        }
                    }

                    if (forwardId != null) {
                        String url = "/objectDetails.do?id=" + forwardId + "&trail=_" + forwardId;
                        return new ActionForward(url, true);
                    }
                }
            }
            return new ForwardParameters(mapping.findForward("results"))
                .addParameter("table", "results." + qid).forward();
        } else {
            LOG.debug("query qid " + qid + " still running, making client wait");
            request.setAttribute("qid", request.getParameter("qid"));
            if (controller.getTickleCount() < 4) {
                request.setAttribute("POLL_REFRESH_SECONDS", new Integer(1));
            } else {
                request.setAttribute("POLL_REFRESH_SECONDS",
                                            new Integer(Constants.POLL_REFRESH_SECONDS));
            }
            int imgnum = ((controller.getTickleCount() + 1) % 4) + 1;
            if (controller.getTickleCount() < 4) {
                request.setAttribute("imgnum", new Integer(1));
            } else {
                request.setAttribute("imgnum", new Integer(imgnum));
            }

            // there are different action mappings for different kinds of
            // query (portal, template, query builder) so we have to refresh
            // to the correct path
            request.setAttribute("POLL_ACTION_NAME", mapping.getPath());
            return mapping.findForward("waiting");
        }
    }
}
