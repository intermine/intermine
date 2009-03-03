package org.modmine.web;

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
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.query.WebResultsExecutor;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.results.WebResults;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for submissionProtocolsDisplayer.jsp
 * @author Richard Smith
 */
public class SubmissionProtocolsController extends TilesAction
{
    protected static final Logger LOG = Logger.getLogger(SubmissionDisplayerController.class);
    
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(@SuppressWarnings("unused")  ComponentContext context,
            @SuppressWarnings("unused") ActionMapping mapping,
            @SuppressWarnings("unused") ActionForm form,
            HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response)
    throws Exception {

        HttpSession session = request.getSession();
        ObjectStore os =
            (ObjectStore) session.getServletContext().getAttribute(Constants.OBJECTSTORE);

        // submission object
        InterMineObject o = (InterMineObject) request.getAttribute("object");
        LOG.error("SUBMISSION id: " + o.getId());

        // create the query
        PathQuery q = new PathQuery(os.getModel());
        
        q.addView("Submission.appliedProtocols.step");
        q.addView("Submission.appliedProtocols:inputs.type");
        q.addView("Submission.appliedProtocols:inputs.name");
        q.addView("Submission.appliedProtocols:inputs.value");
        q.addView("Submission.appliedProtocols.protocol.name");
        q.addView("Submission.appliedProtocols:outputs.type");
        q.addView("Submission.appliedProtocols:outputs.name");
        q.addView("Submission.appliedProtocols:outputs.value");

        q.addConstraint("Submission.id", Constraints.eq(o.getId()));
        q.addOrderBy("Submission.appliedProtocols.step");
        
        WebResultsExecutor executor = SessionMethods.getWebResultsExecutor(session);
        WebResults results = executor.execute(q);
        
        
        PagedTable pagedTable = new PagedTable(results);
        // TODO don't set a maximum?
        pagedTable.setPageSize(100);
        request.setAttribute("pagedResults", pagedTable);
        
        return null;
    }
    
}
