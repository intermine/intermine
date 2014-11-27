package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.results.ResultElement;
import org.intermine.api.results.WebTable;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.query.QueryMonitorTimeout;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Repeatedly poll the status of a running query and forward client to appropriate page
 * each time.
 *
 * @see org.intermine.web.logic.query.QueryMonitor
 * @see org.intermine.web.logic.query.QueryMonitorTimeout
 * @see org.intermine.web.logic.session.SessionMethods#runQuery
 * @author Thomas Riley
 */
@SuppressWarnings("deprecation")
public class PollQueryAction extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(PollQueryAction.class);

    /**
     * Handle request from client.
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
        String trail = request.getParameter("trail");
        String queryBuilder = request.getParameter("queryBuilder");
        boolean followSingleResult = "followSingleResult".equals(mapping.getParameter());
        request.setAttribute("trail", trail);
        request.setAttribute("queryBuilder", queryBuilder);
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
            // Look at results, if only one result, go straight to object details page
            PagedTable pr = SessionMethods.getResultsTable(session, "results." + qid);
            if (followSingleResult) {
                WebTable webResults = pr.getAllRows();

                // Query can have more than one column, forward from the first
                Object cell = null;
                Integer forwardId = null;
                if (webResults.size() == 1) {
                    cell = webResults.getResultElements(0).get(0).get(0).getValue();
                    if (cell instanceof ResultElement) {
                        forwardId = ((ResultElement) cell).getId();
                    }
                }

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

                if (forwardId != null) {
                    if (trail != null) {
                        trail += "|" + forwardId;
                    } else {
                        trail = "|" + forwardId;
                    }
                    String url = "/report.do?id=" + forwardId + "&trail=" + trail;
                    return new ActionForward(url, true);
                }
            }

            // Send us off to see the results in a table.
            if (trail != null) {
                trail += "|results." + qid;
            } else {
                trail = "|results." + qid;
            }
            PathQuery pq = null;
            if (pr != null && pr.getPathQuery() != null) {
                pq = pr.getPathQuery();
            } else {
                pq = controller.getPathQuery();
            }
            if (pq != null) {
                request.setAttribute("query", pq);
            }

            ForwardParameters fp = new ForwardParameters(mapping.findForward("results"))
                                    .addParameter("trail", trail)
                                    .addParameter("table", "results." + qid);
            if (queryBuilder != null) {
                fp.addParameter("queryBuilder", queryBuilder);
            }
            return fp.forward();
        } else {
            request.setAttribute("qid", qid);
            request.setAttribute("trail", trail);
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
