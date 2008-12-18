package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.flatouterjoins.MultiRow;
import org.intermine.objectstore.flatouterjoins.MultiRowFirstValue;
import org.intermine.objectstore.flatouterjoins.MultiRowValue;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.config.Type;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.results.ResultElement;
import org.intermine.web.logic.results.WebTable;
import org.intermine.web.logic.search.SearchRepository;
import org.intermine.web.logic.search.WebSearchable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.tagging.TagTypes;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.widget.config.WidgetConfig;

/**
 * @author Xavier Watkins
 */
public class BagDetailsController extends TilesAction
{

    private static final int PAGE_SIZE = 10;

    /**
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
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Map<String, List<FieldDescriptor>> classKeys = getClassKeys(servletContext);
        String bagName = request.getParameter("bagName");
        Boolean myBag = Boolean.FALSE;

        if (bagName == null) {
            bagName = request.getParameter("name");
        }

        InterMineBag imBag = null;
        String scope = request.getParameter("scope");
        if (scope == null) {
            scope = TemplateHelper.ALL_TEMPLATE;
        }

        if (scope.equals(TemplateHelper.USER_TEMPLATE)
                        || scope.equals(TemplateHelper.ALL_TEMPLATE)) {
            Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
            imBag = profile.getSavedBags().get(bagName);
            if (imBag != null) {
                myBag = Boolean.TRUE;
            }
        }

        if (scope.equals(TemplateHelper.GLOBAL_TEMPLATE)
            || scope.equals(TemplateHelper.ALL_TEMPLATE)) {
            // scope == all or global
            SearchRepository searchRepository = SearchRepository
                            .getGlobalSearchRepository(servletContext);
            Map<String, ? extends WebSearchable> publicBagMap = searchRepository
                            .getWebSearchableMap(TagTypes.BAG);
            if (publicBagMap.get(bagName) != null) {
                imBag = (InterMineBag) publicBagMap.get(bagName);
            }
        }

        if (imBag == null) {
            return null;
        }

        WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
        Model model = os.getModel();
        Type type = (Type) webConfig.getTypes().get(model.getPackageName() + "." + imBag.getType());

        LinkedList<WidgetConfig> widgets = type.getWidgets();
        Map<String, Map<String, Collection<String>>> widget2extraAttrs
        = new HashMap<String, Map<String, Collection<String>>>();
        for (WidgetConfig widget2 : widgets) {
            widget2extraAttrs.put(widget2.getId(), widget2.getExtraAttributes(
                            imBag, os));
        }
        request.setAttribute("widgets", widgets);
        request.setAttribute("widget2extraAttrs", widget2extraAttrs);

        PagedTable pagedResults = SessionMethods.getResultsTable(session, "bag." + imBag.getName());

        if (pagedResults == null || pagedResults.getExactSize() != imBag.getSize()) {
            pagedResults = SessionMethods.doQueryGetPagedTable(request, servletContext, imBag);
        }

        // TODO this needs to be removed when InterMineBag can store the initial ids of when the
        // bag was made.
        BagQueryConfig bagQueryConfig = (BagQueryConfig) servletContext
                        .getAttribute(Constants.BAG_QUERY_CONFIG);
        Map<String, String[]> additionalConverters = bagQueryConfig.getAdditionalConverters(imBag
                        .getType());
        if (additionalConverters != null) {
            for (String converterClassName : additionalConverters.keySet()) {
                String[] paramArray = additionalConverters.get(converterClassName);
                String[] urlFields = paramArray[0].split(",");
                for (int i = 0; i < urlFields.length; i++) {
                    if (request.getParameter(urlFields[i]) != null) {
                        request.setAttribute("extrafield", urlFields[i]);
                        request.setAttribute(urlFields[i], request.getParameter(urlFields[i]));
                        request.setAttribute("externalids", request.getParameter("externalids"));
                        break;
                    }
                }
            }
        }

        // Get the widget toggle state
        // TODO this needs to be re-implemented.  see #1660
//        request.setAttribute("toggledElements", SessionMethods.getWebState(session).
//                getToggledElements());

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
            && (gotoHighlightedStr.equalsIgnoreCase("t")
                || gotoHighlightedStr.equalsIgnoreCase("true"))) {
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

        request.setAttribute("firstSelectedFields",
                             pagedResults.getFirstSelectedFields(os, classKeys));


        if (page == -1) {
            // use the page from the URL
            page = (pageStr == null ? 0 : Integer.parseInt(pageStr));
        }

        pagedResults.setPageAndPageSize(page, PAGE_SIZE);

        request.setAttribute("addparameter", request.getParameter("addparameter"));
        request.setAttribute("myBag", myBag);
        request.setAttribute("bag", imBag);
        request.setAttribute("bagSize", new Integer(imBag.size()));
        request.setAttribute("pagedResults", pagedResults);
        request.setAttribute("highlightId", highlightIdStr);
        // disable using pathquery saved in session in following jsp page
        // because it caused displaying invalid column names
        request.setAttribute("notUseQuery", Boolean.TRUE);

        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<FieldDescriptor>> getClassKeys(ServletContext servletContext) {
        return (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
    }

}


