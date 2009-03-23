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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.MessageResources;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.query.QueryMonitorTimeout;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.ForwardParameters;
import org.intermine.web.struts.InterMineAction;

/**
 * Generate queries for overlaps of submission features and overlaps with gene flanking regions.
 * @author Richard Smith
 *
 */
public class SubmissionOverlapsAction extends InterMineAction {

    private static final Logger LOG = Logger.getLogger(SubmissionOverlapsAction.class);
    
    /**
     * Action for creating a bag of InterMineObjects or Strings from identifiers in text field.
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
                                 @SuppressWarnings("unused") HttpServletResponse response)
    throws Exception {
        HttpSession session = request.getSession();
        SubmissionOverlapsForm submissionOverlapsForm = (SubmissionOverlapsForm) form;
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);

        String submissionTitle = submissionOverlapsForm.getSubmissionTitle();
        String submissionId = submissionOverlapsForm.getSubmissionId();
        
        PathQuery q = new PathQuery(os.getModel());
        
        if (request.getParameter("overlaps") != null) {
            String featureType = submissionOverlapsForm.getOverlapFeatureType();
            String findFeatureType = submissionOverlapsForm.getOverlapFindType();
            
            q.addView(findFeatureType + ".primaryIdentifier");            
            q.addView(findFeatureType + ".overlappingFeatures.secondaryIdentifier");
            

            PathNode featureNode = q.addNode(findFeatureType + ".overlappingFeatures");
            featureNode.setType(featureType);
            q.addConstraint(findFeatureType + ".overlappingFeatures.dataSets.title",
                    Constraints.eq(submissionTitle));
            
            
        } else if (request.getParameter("flanking") != null) {
            String direction = submissionOverlapsForm.getDirection();
            String distance = submissionOverlapsForm.getDistance();
            String featureType = submissionOverlapsForm.getFlankingFeatureType();

            q.addView("GeneFlankingRegion.gene.primaryIdentifier");

            PathNode featureNode = q.addNode("GeneFlankingRegion.overlappingFeatures");
            featureNode.setType(featureType);

            q.addConstraint("GeneFlankingRegion.distance", Constraints.eq(distance));
            q.addConstraint("GeneFlankingRegion.direction", Constraints.eq(direction));
            q.addConstraint("GeneFlankingRegion.overlappingFeatures.dataSets.title", 
                    Constraints.eq(submissionTitle));

            q.setOrderBy("GeneFlankingRegion.gene.primaryIdentifier");

        }

        LOG.info("OVERLAPS: " + q.toString());
        
        //WebResultsExecutor executor = SessionMethods.getWebResultsExecutor(session);
        //WebResults webResults = executor.execute(q);
        
        QueryMonitorTimeout clientState = new QueryMonitorTimeout(
                Constants.QUERY_TIMEOUT_SECONDS * 1000);
        MessageResources messages = (MessageResources) request.getAttribute(Globals.MESSAGES_KEY);
        String qid = SessionMethods.startQuery(clientState, session, messages,
                                               false, q);
        Thread.sleep(200);
    
        String trail = "|" + submissionId;
        
        return new ForwardParameters(mapping.findForward("waiting"))
        .addParameter("qid", qid)
        .addParameter("trail", trail)
        .forward();
        
        
        
        //return mapping.findForward("results");
    }
}
