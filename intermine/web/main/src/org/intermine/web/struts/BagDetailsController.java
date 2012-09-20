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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.results.ResultElement;
import org.intermine.api.results.WebTable;
import org.intermine.api.results.flatouterjoins.MultiRow;
import org.intermine.api.results.flatouterjoins.MultiRowFirstValue;
import org.intermine.api.results.flatouterjoins.MultiRowValue;
import org.intermine.api.search.Scope;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.Type;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.pathqueryresult.PathQueryResultHelper;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.widget.config.WidgetConfig;

/**
 * @author Xavier Watkins
 */
public class BagDetailsController extends TilesAction
{

    private static final int PAGE_SIZE = 10;
    private static final Logger LOG = Logger.getLogger(BagDetailsController.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);
        ObjectStore os = im.getObjectStore();
        Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();
        BagManager bagManager = im.getBagManager();

        String bagName = request.getParameter("bagName");
        Boolean myBag = Boolean.FALSE;

        if (bagName == null) {
            bagName = request.getParameter("name");
        }

        InterMineBag imBag = null;
        String scope = request.getParameter("scope");
        if (scope == null) {
            scope = Scope.ALL;
        }

        if (scope.equals(Scope.USER) || scope.equals(Scope.ALL)) {
            imBag = bagManager.getUserBag(profile, bagName);
            if (imBag != null) {
                myBag = Boolean.TRUE;
            }
            if (profile.getInvalidBags().containsKey(bagName)) {
                request.setAttribute("bag", profile.getInvalidBags().get(bagName));
                request.setAttribute("invalid", true);
                return null;
            }
        }

        if (scope.equals(Scope.GLOBAL) || scope.equals(Scope.ALL)) {
            if (bagManager.getGlobalBag(bagName) != null) {
                imBag = bagManager.getGlobalBag(bagName);
            } else if (imBag == null) {
                imBag = bagManager.getSharedBags(profile).get(bagName);
            }
        }

        if (imBag == null) {
            ActionMessages actionMessages = getErrors(request);
            actionMessages.add(ActionMessages.GLOBAL_MESSAGE,
                    new ActionMessage("errors.bag.missing", bagName));
            saveErrors(request, actionMessages);
            request.setAttribute("bag", imBag);
            return null;
        }

        WebConfig webConfig = SessionMethods.getWebConfig(request);
        Model model = os.getModel();
        Type type = webConfig.getTypes().get(model.getPackageName() + "." + imBag.getType());

        LinkedList<WidgetConfig> widgets = type.getWidgets();
        Map<String, Map<String, Collection<String>>> widget2extraAttrs = new HashMap<String,
                Map<String, Collection<String>>>();
        for (WidgetConfig widget2 : widgets) {
            widget2extraAttrs.put(widget2.getId(), widget2.getExtraAttributes(imBag, os));
        }
        request.setAttribute("widgets", widgets);
        request.setAttribute("widget2extraAttrs", widget2extraAttrs);

        PathQuery pathQuery = PathQueryResultHelper.makePathQueryForBag(imBag, webConfig, model);
        SessionMethods.setQuery(session, pathQuery);
        PagedTable pagedResults = SessionMethods.getResultsTable(session, "bag." + imBag.getName());

        int bagSize = imBag.getSize();
        if (pagedResults == null || pagedResults.getExactSize() != bagSize) {
            pagedResults = SessionMethods.doQueryGetPagedTable(request, imBag);
        }

        // tracks the list execution only if the list hasn't
        // just been created
        if (request.getParameter("trackExecution") == null
            || "true".equals(request.getParameter("trackExecution"))) {
            im.getTrackerDelegate().trackListExecution(imBag.getType(),
                    bagSize, profile, session.getId());
        }

        // Get the widget toggle state
        // TODO this needs to be re-implemented.  see #1660
        // request.setAttribute("toggledElements",
        //   SessionMethods.getWebState(session).
        // getToggledElements());

        // Set the size
        String pageStr = request.getParameter("page");
        int page = -1;

        String highlightIdStr = request.getParameter("highlightId");
        Integer highlightId = null;
        if (highlightIdStr != null) {
            highlightId = new Integer(Integer.parseInt(highlightIdStr));
        }
        boolean gotoHighlighted = false;
        String gotoHighlightedStr = request.getParameter("gotoHighlighted");
        if (gotoHighlightedStr != null
            && ("t".equalsIgnoreCase(gotoHighlightedStr)
                || "true".equalsIgnoreCase(gotoHighlightedStr))) {
            gotoHighlighted = true;
        }
        if (highlightId != null && gotoHighlighted) {
            // calculate the page
            WebTable webTable = pagedResults.getAllRows();

            for (int i = 0; i < webTable.size(); i++) {
                MultiRow<ResultsRow<MultiRowValue<ResultElement>>> row
                    = webTable.getResultElements(i);
                for (ResultsRow<MultiRowValue<ResultElement>> resultsRow : row) {
                    for (MultiRowValue<ResultElement> mrv : resultsRow) {
                        if (mrv instanceof MultiRowFirstValue) {
                            ResultElement resultElement = mrv.getValue();
                            if (resultElement != null) {
                                Integer id = resultElement.getId();
                                if (id.equals(highlightId)) {
                                    page = i / PAGE_SIZE;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        // which fields shall we show in preview?
        List<String> showInPreviewTable = new ArrayList<String>();
        for (Entry<String, FieldConfig> entry : type.getFieldConfigMap().entrySet()) {
            if (entry.getValue().getShowInListAnalysisPreviewTable()) {
                showInPreviewTable.add(type.getDisplayName() + "." + entry.getKey());
            }
        }
        request.setAttribute("showInPreviewTable", showInPreviewTable);

        request.setAttribute("firstSelectedFields",
                             pagedResults.getFirstSelectedFields(os, classKeys));
        if (page == -1) {
            // use the page from the URL
            page = (pageStr == null ? 0 : Integer.parseInt(pageStr));
        }

        pagedResults.setPageAndPageSize(page, PAGE_SIZE);

        // is this list public?
        Boolean isPublic = bagManager.isPublic(imBag);
        request.setAttribute("isBagPublic", isPublic);

        request.setAttribute("addparameter", request.getParameter("addparameter"));
        request.setAttribute("myBag", myBag);
        request.setAttribute("bag", imBag);
        request.setAttribute("bagSize", new Integer(imBag.size()));
        request.setAttribute("pagedResults", pagedResults);
        request.setAttribute("highlightId", highlightIdStr);
        // disable using pathquery saved in session in following jsp page
        // because it caused displaying invalid column names
        request.setAttribute("notUseQuery", Boolean.TRUE);

        // Get us token so we can show non-public widgets.
        request.setAttribute("token", profile.getDayToken());
        LOG.info("API key: " + profile.getDayToken());

        return null;
    }
}


