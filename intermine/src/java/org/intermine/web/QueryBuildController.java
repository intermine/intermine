package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collection;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;

import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;

/**
 * Perform initialisation steps for query editing tile prior to calling query.jsp
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class QueryBuildController extends TilesAction
{
    /**
     * @see TilesAction#execute
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();

        Map queryClasses = (Map) session.getAttribute(Constants.QUERY_CLASSES);
        String editingAlias = (String) session.getAttribute(Constants.EDITING_ALIAS);
        Map savedBagsInverse = (Map) session.getAttribute(Constants.SAVED_BAGS_INVERSE);
        Map savedQueriesInverse = (Map) session.getAttribute(Constants.SAVED_QUERIES_INVERSE);
        ServletContext servletContext = session.getServletContext();
        Model model = (Model) servletContext.getAttribute(Constants.MODEL);
        Query q = (Query) session.getAttribute(Constants.QUERY);

        //there's a query on the session but it hasn't been rendered yet
        if (q != null && queryClasses == null) {
            queryClasses = QueryBuildHelper.getQueryClasses(q, savedBagsInverse,
                                                            savedQueriesInverse);

            session.setAttribute(Constants.QUERY_CLASSES, queryClasses);
            session.setAttribute(Constants.QUERY, null);
        }

        if (queryClasses == null) {
            session.setAttribute(Constants.QUERY_CLASSES, new LinkedHashMap());
        }

        QueryBuildForm qbf = (QueryBuildForm) form;
        //someone has started editing a class - populate the form
        if (qbf.getButton().startsWith("editClass")) {
            qbf.setNewClassName(editingAlias);
            DisplayQueryClass d = (DisplayQueryClass) queryClasses.get(editingAlias);
            QueryBuildHelper.populateForm(qbf, d);
        }

        if (qbf.getButton().equals("")
            || qbf.getButton().startsWith("editIql")
            || qbf.getButton().startsWith("runQuery")
            || qbf.getButton().startsWith("updateClass")
            || qbf.getButton().startsWith("removeClass")
            || qbf.getButton().startsWith("addClass")) {

            Map savedBags = (Map) session.getAttribute(Constants.SAVED_BAGS);
            Map savedQueries = (Map) session.getAttribute(Constants.SAVED_QUERIES);

            q = QueryBuildHelper.createQuery(queryClasses, model, savedBags, savedQueries);

            session.setAttribute(Constants.QUERY, q);

            ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);

            ResultsInfo estimatedResultsInfo;

            try {
                estimatedResultsInfo = os.estimate(q);

                String newQueryName = SaveQueryHelper.findNewQueryName(savedQueries);

                if (estimatedResultsInfo.getStart() < 100000) {
                    Results results = os.execute(q);
                    request.setAttribute("results", results);

                    ResultsInfo resultsInfo;

                    resultsInfo = results.getInfo();

                    SaveQueryHelper.saveQuery(request, newQueryName, q, resultsInfo);
                } else {
                    SaveQueryHelper.saveQuery(request, newQueryName, q, estimatedResultsInfo);
                }
            } catch (ObjectStoreException e) {
                // results.getInfo() called explain and it failed or os.estimate() failed
                // - ignore
            }

        }

        //editing is continuing
        if (editingAlias != null) {
            DisplayQueryClass d = (DisplayQueryClass) queryClasses.get(editingAlias);
            ClassDescriptor cld = model.getClassDescriptorByName(d.getType());
            boolean bagsPresent = savedBagsInverse != null && savedBagsInverse.size () != 0;
            request.setAttribute("validOps", QueryBuildHelper.getValidOps(cld, bagsPresent));

            request.setAttribute("allFieldNames", QueryBuildHelper.getAllFieldNames(cld));

            Collection savedBagNames = (savedBagsInverse == null
                                        ? new HashSet()
                                        : savedBagsInverse.values());
            Collection savedQueryNames = (savedQueriesInverse == null
                                          ? new HashSet()
                                          : savedQueriesInverse.values());
            request.setAttribute("validAliases", QueryBuildHelper.getValidAliases(cld,
                                                                                  queryClasses,
                                                                                  savedBagNames,
                                                                                  savedQueryNames));
        }

        return null;
    }
}

