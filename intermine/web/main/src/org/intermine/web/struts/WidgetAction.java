package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.config.Type;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.export.http.HttpExportUtil;
import org.intermine.web.logic.export.rowformatters.CSVRowFormatter;
import org.intermine.web.logic.export.rowformatters.TabRowFormatter;
import org.intermine.web.logic.export.string.StringTableExporter;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.widget.EnrichmentWidgetLdr;
import org.intermine.web.logic.widget.Widget;
import org.intermine.web.logic.widget.WidgetURLQuery;
import org.intermine.web.logic.widget.config.WidgetConfig;
/**
 * Runs a query based on which record the user clicked on in the widget.  Used by bag table and
 * enrichment widgets.
 * @author Julie Sullivan
 */
public class WidgetAction extends InterMineAction
{
    /**
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        WidgetForm widgetForm = (WidgetForm) form;
        String key = request.getParameter("key");
        String action = widgetForm.getAction();
        if (StringUtils.isNotEmpty(key)) {
         // user clicked on a count on a widget
            return display(mapping, form, request, response);
        } else if ("displayAll".equals(widgetForm.getAction())) {
            return displayAll(mapping, form, request, response);
        } else if (action == null || "display".equals(action)) {
            // user checked some boxes and clicked 'display'
            return display(mapping, form, request, response);
        } else {
            return export(mapping, form, request, response);
        }
    }

    /**
     * Currently not used.  See #1719
     * Display selected entries in the results page
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     * @deprecated the 'not analysed' number will eventually be a link when I get time
     */
    @Deprecated
    public ActionForward notAnalysed(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        ObjectStore os = im.getObjectStore();
        WebConfig webConfig = SessionMethods.getWebConfig(request);
        Model model = im.getModel();

        WidgetForm wf = (WidgetForm) form;
        String bagName = wf.getBagName();
        String ldr = null, urlQuery = null;
        String widgetId = wf.getWidgetid();
        String selectedExtraAttribute = wf.getSelectedExtraAttribute();

        Type type = webConfig.getTypes().get(model.getPackageName() + "." + wf.getBagType());
        List<WidgetConfig> widgets = type.getWidgets();
        for (WidgetConfig widgetConfig : widgets) {
            if (widgetConfig.getId() == widgetId) {
                ldr = widgetConfig.getDataSetLoader();
                urlQuery = widgetConfig.getLink();
            }
        }

        Profile currentProfile = SessionMethods.getProfile(session);
        BagManager bagManager = im.getBagManager();
        InterMineBag bag = bagManager.getUserOrGlobalBag(currentProfile, bagName);

        Class<?> clazz = TypeUtil.instantiate(ldr);

        Constructor<?> constr = clazz.getConstructor(new Class[] {InterMineBag.class,
            ObjectStore.class, String.class});

        EnrichmentWidgetLdr enrichmentWidgetLdr = (EnrichmentWidgetLdr) constr
            .newInstance(new Object[] {bag, os, selectedExtraAttribute});

        Query q = enrichmentWidgetLdr.getQuery("analysed", null);
        Object[] o = os.executeSingleton(q).toArray();
        int n = ((java.lang.Long) o[0]).intValue();

        Collection<InterMineObject> widgetObjects = new ArrayList<InterMineObject>();
        widgetObjects.add(os.getObjectById(new Integer(n)));

        clazz = TypeUtil.instantiate(urlQuery);

        constr = clazz.getConstructor(new Class[] {ObjectStore.class, InterMineBag.class,
            String.class});

        WidgetURLQuery widgetURLQuery = (WidgetURLQuery) constr.newInstance(new Object[] {os, bag,
            null});

        // See #1719
        //PathQuery pathQuery = widgetURLQuery.generatePathQuery(widgetObjects);
        PathQuery pathQuery = widgetURLQuery.generatePathQuery(false);

        SessionMethods.loadQuery(pathQuery, session, response);
        String qid = SessionMethods.startQueryWithTimeout(request, true, pathQuery);
        Thread.sleep(200); // slight pause in the hope of avoiding holding page
        return new ForwardParameters(mapping.findForward("waiting")).addParameter("trail",
                "|bag." + bagName).addParameter("qid", qid).forward();
    }

    private PathQuery generatePathQuery(HttpSession session, ActionForm form,
            HttpServletRequest request, String bagName, boolean showAll)
        throws NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException, PathException {
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        ObjectStore os = im.getObjectStore();
        String key = request.getParameter("key");
        String link = request.getParameter("link");
        String allOrSelected = "All";
        WidgetForm wf = (WidgetForm) form;
        String widgetTitle = wf.getWidgetTitle();
        if (StringUtils.isEmpty(key)) {
            // the key (the specific object we are querying on) can come from the form or the URL
            key = wf.getSelectedAsString();
        }
        if (!StringUtils.isEmpty(key)) {
            allOrSelected = "Selected";
        }
        Profile currentProfile = SessionMethods.getProfile(session);
        BagManager bagManager = im.getBagManager();
        InterMineBag bag = bagManager.getUserOrGlobalBag(currentProfile, bagName);

        Class<?> clazz = TypeUtil.instantiate(link);
        Constructor<?> constr = clazz.getConstructor(new Class[] {ObjectStore.class,
            InterMineBag.class, String.class});
        WidgetURLQuery urlQuery = (WidgetURLQuery) constr.newInstance(new Object[] {os, bag, key});

        String bagType = bag.getType();

        PathQuery q = urlQuery.generatePathQuery(showAll);
        String description = allOrSelected + " " + bagType + "s from the list '" + bagName + "'";
        if (!StringUtils.isEmpty(widgetTitle)) {
            // widget title will be null if we don't have a widget form, eg. the user clicked on
            // a link instead of submitting the form.  FIXME!
            description += " for the widget '" + widgetTitle + "'";
        }
        q.setDescription(description);
        return q;
    }

    /**
     * Display selected entries in the results page
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward display(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        String bagName = request.getParameter("bagName");
        PathQuery pathQuery = generatePathQuery(session, form, request, bagName, false);
        SessionMethods.loadQuery(pathQuery, session, response);
        String qid = SessionMethods.startQueryWithTimeout(request, true, pathQuery);
        Thread.sleep(200); // slight pause in the hope of avoiding holding page
        return new ForwardParameters(mapping.findForward("waiting")).addParameter("trail",
                        "|bag." + bagName).addParameter("qid", qid).forward();
    }

    /**
     * Display all entries in the results page
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward displayAll(ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        String bagName = request.getParameter("bagName");
        PathQuery pathQuery = generatePathQuery(session, form, request, bagName, true);
        SessionMethods.loadQuery(pathQuery, session, response);
        String qid = SessionMethods.startQueryWithTimeout(request, true, pathQuery);
        Thread.sleep(200); // slight pause in the hope of avoiding holding page
        return new ForwardParameters(mapping.findForward("waiting")).addParameter("trail",
                        "|bag." + bagName).addParameter("qid", qid).forward();
    }

    /**
     * Export selected entries.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward export(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        WidgetForm widgetForm = (WidgetForm) form;
        WebConfig webConfig = SessionMethods.getWebConfig(request);
        ObjectStore os = im.getObjectStore();
        Model model = im.getModel();

        String widgetId = widgetForm.getWidgetid();
        Type type = webConfig.getTypes().get(
                        model.getPackageName() + "." + widgetForm.getBagType());
        List<WidgetConfig> widgets = type.getWidgets();
        for (WidgetConfig widgetConfig : widgets) {
            if (widgetConfig.getId().equals(widgetId)) {
                StringTableExporter stringExporter;
                PrintWriter writer = HttpExportUtil.
                    getPrintWriterForClient(request, response.getOutputStream());
                if ("csv".equals(widgetForm.getExporttype())) {
                    stringExporter = new StringTableExporter(writer, new CSVRowFormatter());
                    ResponseUtil.setCSVHeader(response, "widget" + widgetForm.getWidgetid()
                                                        + ".csv");
                } else {
                    stringExporter = new StringTableExporter(writer, new TabRowFormatter());
                    ResponseUtil.setTabHeader(response, "widget" + widgetForm.getWidgetid()
                                                        + ".tsv");
                }
                List<String> attributes = new ArrayList<String>();
                attributes.add(widgetForm.getSelectedExtraAttribute());
                attributes.add(widgetForm.getMax());
                attributes.add(widgetForm.getErrorCorrection());
                attributes.add(widgetForm.getHighlight());
                attributes.add(widgetForm.getPValue());
                attributes.add(widgetForm.getNumberOpt());

                Profile currentProfile = SessionMethods.getProfile(session);
                BagManager bagManager = im.getBagManager();
                InterMineBag bag = bagManager.getUserOrGlobalBag(currentProfile,
                        widgetForm.getBagName());
                Widget widget = widgetConfig.getWidget(bag, os, attributes);
                stringExporter.export(widget.getExportResults(widgetForm.getSelected()));
            }
        }
        return null;
    }
}
