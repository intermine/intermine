package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.UserPreferences;
import org.intermine.api.tag.TagTypes;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.template.TemplatePrecomputeHelper;
import org.intermine.api.template.TemplateSummariser;
import org.intermine.api.tracker.TrackerDelegate;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.template.TemplateQuery;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.results.WebState;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Tiles controller for history tile (page).
 *
 * @author Thomas Riley
 */
public class MyMineController extends TilesAction
{
    /**
     * Set up attributes for the myMine page.
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        TagManager tagManager = im.getTagManager();
        String page = request.getParameter("page");

        Profile profile = SessionMethods.getProfile(session);

        if (SessionMethods.isSuperUser(session)) {
            TrackerDelegate td = im.getTrackerDelegate();
            Map<String, Integer> templateCounter = td.getAccessCounter();
            if (templateCounter != null) {
                request.setAttribute("templateCounter", templateCounter);
            }
            Map<String, Integer> templateRank = td.getRank(im.getTemplateManager());
            if (templateRank != null) {
                request.setAttribute("templateRank", templateRank);
            }
        }

        /* if the user is on a restricted page and they are not logged in, send them to the bags
         * page.  query history is not a restricted page.
         */
        if (page != null && !"history".equals(page) && !profile.isLoggedIn()) {
            page = "lists";
        }

        if (!StringUtils.isEmpty(page)) {
            session.setAttribute(Constants.MYMINE_PAGE, page);
        }

        if (page != null) {
            if ("templates".equals(page)) {
                // prime the tags cache so that the templates tags will be quick to access
                String userName = profile.getUsername();
                if (userName != null) {
                    // discard result - we just want the query to be run.
                    tagManager.getTags(null, null, TagTypes.TEMPLATE, userName);
                }
            }
        }

        // get the precomputed and summarised info
        if (onSubTab(request, "templates")) {
            session.removeAttribute(Constants.NEW_TEMPLATE);
            getPrecomputedSummarisedInfo(profile, session, request);
        }
        if (onSubTab(request, "lists") || onSubTab(request, null)) {
            ActionMessages actionErrors = getErrors(request);
            if (im.getBagManager().isAnyBagToUpgrade(profile)) {
                actionErrors.add(ActionMessages.GLOBAL_MESSAGE,
                        new ActionMessage("login.upgradeListManually"));
            }
            if (!profile.getInvalidBags().isEmpty()) {
                actionErrors.add(ActionMessages.GLOBAL_MESSAGE,
                        new ActionMessage("bags.invalid.notice"));
            }
            saveErrors(request, actionErrors);
        }

        if (onSubTab(request, "account")) {
            session.setAttribute("SPECIAL_PREFERENCES", UserPreferences.COMMON_KEYS);
            session.setAttribute("BOOLEAN_PREFERENCES", UserPreferences.BOOLEAN_KEYS);
        }
        return null;
    }

    private static boolean onSubTab(HttpServletRequest request, String inQuestion) {
        WebState webState = SessionMethods.getWebState(request.getSession());
        String subTab = request.getParameter("subtab");
        String wsst = webState.getSubtab("subtabmymine");
        if (inQuestion == null) {
            return (subTab == null && wsst == null);
        }
        return (subTab != null && inQuestion.equals(subTab)) || (wsst != null && inQuestion.equals(wsst));
    }

    /**
     * Retrieve the information about precomputed and summarised templates for the
     * given profile, and store it into the request
     *
     * @param profile the user Profile
     * @param session the HttpSession
     * @param request the Servlet Request
     * @throws ObjectStoreException when something goes wrong...
     */
    public static void getPrecomputedSummarisedInfo(Profile profile, HttpSession session,
            HttpServletRequest request) throws ObjectStoreException {
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Map<String, ApiTemplate> templates = profile.getSavedTemplates();
        ObjectStoreInterMineImpl os = (ObjectStoreInterMineImpl) im.getObjectStore();

        Map<String, String> precomputedTemplateMap = new HashMap<String, String>();
        Map<String, String> summarisedTemplateMap = new HashMap<String, String>();

        TemplateSummariser summariser = im.getTemplateSummariser();
        for (ApiTemplate template : templates.values()) {
            if (template.isValid()) {
                if ((session.getAttribute("precomputing_" + template.getName()) != null)
                        && "true".equals(session.getAttribute("precomputing_" + template
                                .getName()))) {
                    precomputedTemplateMap.put(template.getName(), "precomputing");
                } else {
                    Query query = TemplatePrecomputeHelper
                        .getPrecomputeQuery(template, new ArrayList<QuerySelectable>(), null);
                    precomputedTemplateMap.put(template.getName(), Boolean.toString(os
                                .isPrecomputed(query, "template")));
                }
                if ((session.getAttribute("summarising_" + template.getName()) != null)
                        && "true".equals(session.getAttribute("summarising_" + template
                                .getName()))) {
                    summarisedTemplateMap.put(template.getName(), "summarising");
                } else {
                    summarisedTemplateMap.put(template.getName(), Boolean.toString(summariser
                                .isSummarised(template)));
                }
            }
        }

        request.setAttribute("precomputedTemplateMap", precomputedTemplateMap);
        request.setAttribute("summarisedTemplateMap", summarisedTemplateMap);
    }
}
