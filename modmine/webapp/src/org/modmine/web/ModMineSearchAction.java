package org.modmine.web;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.WebResults;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.ForwardParameters;
import org.intermine.web.struts.InterMineAction;
import org.intermine.web.struts.QuickSearchAction;

/**
 * @author Richard Smith
 *
 */
public class ModMineSearchAction extends InterMineAction
{
    private static final Logger LOG = Logger.getLogger(QuickSearchAction.class);

    /**
     * Method called when user has submitted search form.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    @Override
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        ModMineSearchForm msf = (ModMineSearchForm) form;
        
        String searchTerm = msf.getSearchTerm();
        
        Map<Integer, Float> searchResults = ModMineSearch.runLuceneSearch(searchTerm);

        Set<Integer> objectIds = searchResults.keySet();
        
        // make hits available to jsp
        Map<Float, Integer> scores = new TreeMap<Float, Integer>();
        Map<Integer, Integer> subMap = ModMineSearch.getSubMap();
        for (Map.Entry<Integer, Float> entry : searchResults.entrySet()) {
            scores.put(entry.getValue(), subMap.get(entry.getKey()));
        }
        request.setAttribute("scores", scores);

        PathQuery query = new PathQuery(im.getModel());
        query.setView("Submission.DCCid, Submission.title, Submission.project.surnamePI Submission.lab.name Submission:properties.type Submission:properties.name");
        String cSub = query.addConstraint("Submission", Constraints.in(im.getObjectStore().getObjectsByIds(objectIds)));
        // TODO this should remove any endings that are equivalent - e.g. ovary/ovaries

        // TODO don't split phrases in quotes
        String[] terms = searchTerm.split(" ");
        StringBuffer logic = new StringBuffer();
        logic.append(cSub);
        logic.append(" and (");
        for (String term : terms) {
            term = term.trim();
            if (term.equalsIgnoreCase("AND")) {
                continue;
            }
            String cName = query.addConstraint("Submission:properties.name", Constraints.contains(term));
            String cType = query.addConstraint("Submission:properties.type", Constraints.contains(term));
            if (!logic.toString().endsWith("(")) {
                logic.append("or");
            }
            logic.append(" " + cName + " or " + cType + " ");
        }
        logic.append(")");
        query.setConstraintLogic(logic.toString());
        query.syncLogicExpression("and");

        WebResultsExecutor executor = im.getWebResultsExecutor(SessionMethods.getProfile(session));
        WebResults webResults = executor.execute(query);
        
        PagedTable pagedTable = new PagedTable(webResults);
        String identifier = ModMineSearch.SEARCH_KEY + searchTerm;
        SessionMethods.setResultsTable(session, identifier, pagedTable);
        
        ForwardParameters forwardParameters =
            new ForwardParameters(mapping.findForward("searchResults"));
        forwardParameters.addParameter("searchTerm", searchTerm);
        return forwardParameters.forward();
    }
}
