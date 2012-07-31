package org.modmine.web;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.WebResults;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Protocol;
import org.intermine.model.bio.ResultFile;
import org.intermine.model.bio.Submission;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for submissionProtocolsDisplayer.jsp
 * @author Richard Smith
 */
public class SubmissionProtocolsController extends TilesAction
{
    protected static final Logger LOG = Logger.getLogger(SubmissionProtocolsController.class);

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
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        ObjectStore os = im.getObjectStore();

        // submission object
        InterMineObject o = (InterMineObject) request.getAttribute("object");
        LOG.info("SUBMISSION id: " + o.getId());

        // create the query
        PathQuery q = new PathQuery(os.getModel());
        q.addView("Submission.appliedProtocols.step");
        q.addView("Submission.appliedProtocols.inputs.type");
        q.addView("Submission.appliedProtocols.inputs.name");
        q.addView("Submission.appliedProtocols.inputs.value");
        q.addView("Submission.appliedProtocols.protocol.name");
        q.addView("Submission.appliedProtocols.outputs.type");
        q.addView("Submission.appliedProtocols.outputs.name");
        q.addView("Submission.appliedProtocols.outputs.value");

        q.addConstraint(Constraints.eq("Submission.id", o.getId().toString()));
        // rm the outer join for i/o: check if ok. if not add
        q.setOuterJoinStatus("Submission.appliedProtocols.inputs", OuterJoinStatus.OUTER);
        q.setOuterJoinStatus("Submission.appliedProtocols.outputs", OuterJoinStatus.OUTER);
        
        q.addOrderBy("Submission.appliedProtocols.step", OrderDirection.ASC);

        Profile profile = SessionMethods.getProfile(session);
        WebResultsExecutor executor = im.getWebResultsExecutor(profile);
        WebResults results = executor.execute(q);

        if (results.size() > 2000) {
            request.setAttribute("subId", o.getId());
            return null;
        }

        PagedTable pagedTable = new PagedTable(results);
        // NB: you need to set a maximum, default is 10!
        pagedTable.setPageSize(2000);
        request.setAttribute("pagedResults", pagedTable);

        // let's get also the dccid (needed for external link)
        // maybe it can be gained in a simpler way
        Query q1 = new Query();
        QueryClass qc = new QueryClass(Submission.class);
        QueryField qcId = new QueryField(qc, "id");

        q1.addFrom(qc);
        q1.addToSelect(qc);
        SimpleConstraint sc = new SimpleConstraint(qcId, ConstraintOp.EQUALS,
                new QueryValue(o.getId()));
        q1.setConstraint(sc);
        Results result = os.executeSingleton(q1);

         // and dccId, protocols
        String dccId = null;
        Set<ResultFile> rf = new HashSet<ResultFile>();
        Set<Protocol> pt = new HashSet<Protocol>();

        Iterator i = result.iterator();
        while (i.hasNext()) {
            Submission sub = (Submission) i.next();
            dccId = sub.getdCCid();
            pt = sub.getProtocols();
            rf = sub.getResultFiles();
        }
        
        request.setAttribute("DCCid", dccId);
        request.setAttribute("protocols", pt);
        request.setAttribute("files", rf);

        return null;
    }
}
