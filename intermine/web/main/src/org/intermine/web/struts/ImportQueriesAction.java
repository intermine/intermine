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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.intermine.api.profile.Profile;
import org.intermine.api.util.NameUtil;
import org.intermine.pathquery.PathQuery;
import org.intermine.metadata.StringUtil;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Imports query in XML format and forward user to the query builder.
 *
 * @author Thomas Riley
 */
public class ImportQueriesAction extends InterMineAction
{
    /**
     * {@inheritDoc}
     */
    @Override public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        ImportQueriesForm qif = (ImportQueriesForm) form;
        Profile profile = SessionMethods.getProfile(session);
        Map<String, PathQuery> queries = null;
        queries = qif.getQueryMap();

        if (queries.size() == 1
            && ((request.getParameter("query_builder") != null && "yes".equals(request
                    .getParameter("query_builder"))) || profile.getUsername() == null)) {
            // special case to redirect straight to the query builder
            PathQuery pathQuery = queries.values().iterator().next();
            if (!pathQuery.isValid()) {
                recordError(new ActionMessage("errors.importFailed",
                        StringUtil.prettyList(pathQuery.verifyQuery())), request);
            }
            try {
                SessionMethods.loadQuery(pathQuery, session, response);
            } catch (Exception e) {
                e.printStackTrace();
                return mapping.findForward("importQueries");
            }
            return mapping.findForward("query");
        }
        if (!profile.isLoggedIn()) {
            ActionMessages actionMessages = getErrors(request);
            actionMessages.add(ActionMessages.GLOBAL_MESSAGE,
                               new ActionMessage("import.queries.notloggedin"));
            saveErrors(request, actionMessages);
            return mapping.findForward("importQueries");
        }
        try {
            profile.disableSaving();
            int imported = 0;
            boolean validNameQuery = true;
            for (String queryName : queries.keySet()) {
                PathQuery query = queries.get(queryName);
                queryName = NameUtil.validateName(profile.getSavedQueries().keySet(), queryName);
                //to improve!! return a badqueryexception
                if (queryName.isEmpty()) {
                    validNameQuery = false;
                    continue;
                }
                SessionMethods.saveQuery(session, queryName, query);
                imported++;
            }
            recordMessage(new ActionMessage("query.imported", new Integer(imported)), request);
            if (!validNameQuery) {
                recordError(new ActionMessage("query.imported.invalidname"), request);
            }
            return new ForwardParameters(mapping.findForward("mymine"))
                .addParameter("subtab", "saved").forward();
        } finally {
            profile.enableSaving();
        }
    }
}
