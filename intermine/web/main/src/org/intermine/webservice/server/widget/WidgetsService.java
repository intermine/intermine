package org.intermine.webservice.server.widget;

/*
 * Copyright (C) 2002-2011 FlyMine
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
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.pathqueryresult.PathQueryResultHelper;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.widget.config.EnrichmentWidgetConfig;
import org.intermine.web.logic.widget.config.GraphWidgetConfig;
import org.intermine.web.logic.widget.config.TableWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.intermine.web.util.URLGenerator;
import org.intermine.webservice.server.WebService;

/**
 * Web service that returns a widget for a given list of identifiers See
 * {@link WidgetsRequestProcessor} for parameter description
 * URL examples: get an EnrichmentWidget
 * /service/widgets?widgetId=go_enrichment&amp;className=Gene&amp;extraAttributes=Bonferroni,0.1
 * ,biological_process&amp;ids=S000000003,S000000004&amp;format=html
 * get a GraphWidget
 * /service/widgets?widgetId=flyatlas
 *   &amp;className=Gene&amp;extraAttributes=
 *   &amp;ids=FBgn0011648,FBgn0011655,FBgn0025800
 *   &amp;format=html
 *
 * @author Xavier Watkins
 */
public class WidgetsService extends WebService
{
    public WidgetsService(InterMineAPI im) {
        super(im);
    }

    /**
     * Executes service specific logic.
     *
     * @param request request
     * @param response response
     * @throws Exception an error has occured
     */
    @Override
    protected void execute(HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        WidgetsServiceInput widgetsServiceInput = getInput();
        ServletContext servletContext = request.getSession().getServletContext();
        Profile profile = SessionMethods.getProfile(request.getSession());

        String className = widgetsServiceInput.getClassName();
        List<String> ids = widgetsServiceInput.getIds();
        InterMineBag imBag = getBag(className, ids, servletContext, profile);
        WebConfig webConfig = SessionMethods.getWebConfig(servletContext);
        WidgetConfig widgetConfig = webConfig.getWidgets().get(widgetsServiceInput.getWidgetId());
        ObjectStore os = this.im.getObjectStore();
        if (widgetConfig instanceof TableWidgetConfig) {
            ((TableWidgetConfig) widgetConfig).setClassKeys(this.im.getClassKeys());
        }
        response.getWriter().print(getHtml(widgetConfig, imBag,
                    new URLGenerator(request).getBaseURL(), os));
    }

    /**
     * Make a bag and return it for a given list of ids
     * @param className the type of the bag to make
     * @param ids the object identifiers
     * @param servletContext the servletContext
     * @param profile the profile
     * @return the InterMineBag
     * @throws ObjectStoreException error when query
     */
    protected InterMineBag getBag(String className, List<String> ids,
            ServletContext servletContext, Profile profile) throws ObjectStoreException {
        Model model = this.im.getModel();
        WebConfig webConfig = SessionMethods.getWebConfig(servletContext);
        try {
            className = StringUtil.capitalise(className);
            Class.forName(model.getPackageName() + "." + className);
        } catch (ClassNotFoundException clse) {
            return null;
        }

        String bagName = null;
        Map<String, InterMineBag> profileBags = profile.getSavedBags();
        boolean bagExists = true;
        int number = 0;
        while (bagExists) {
            bagName = "webservice";
            bagName += "_" + number;
            bagExists = false;
            for (String name : profileBags.keySet()) {
                if (bagName.equals(name)) {
                    bagExists = true;
                }
            }
            number++;
        }

        StringBuffer sb = new StringBuffer();
        for (String ident : ids) {
            if (sb.length() > 0) {
                sb.append("\t");
            }
            sb.append(ident);
        }

        PathQuery pathQuery = new PathQuery(model);
        pathQuery.addViews(PathQueryResultHelper.getDefaultViewForClass(className, model, webConfig,
                null));
        pathQuery.addConstraint(Constraints.lookup(className, sb.toString(), ""));

        Map<String, BagQueryResult> returnBagQueryResults = new HashMap<String, BagQueryResult>();
        WebResultsExecutor executor = this.im.getWebResultsExecutor(profile);

        // execute query, we just need the bag query results
        executor.execute(pathQuery, returnBagQueryResults);

        // There's only one node, get the first value
        BagQueryResult bagQueryResult = returnBagQueryResults.values().iterator().next();
        List<Integer> bagList = new ArrayList<Integer>();
        bagList.addAll(bagQueryResult.getMatchAndIssueIds());

        InterMineBag imBag = profile.createBag(bagName, className, "", im.getClassKeys());
        imBag.addIdsToBag(bagList, className);
        return imBag;
    }

    private WidgetsServiceInput getInput() {
        return new WidgetsRequestParser(request).getInput();
    }

    /**
     * Returns the HTML used for displaying widgets in the service
     * @param widgetConfig the WidgetConfig
     * @param bag the list
     * @param os the ObjectStore
     * @return a String representing the generated HTML
     * @throws Exception an error has occurred
     */
    private String getHtml(WidgetConfig widgetConfig, InterMineBag bag, String prefix,
            ObjectStore os) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append("<!-- WidgetsService.java -->\n");
        sb.append("<html><head>\n");
        sb.append("<script type=\"text/javascript\" src=\"" + prefix
                  + "/dwr/interface/AjaxServices.js\"></script>\n");
        sb.append("<script type=\"text/javascript\" src=\"" + prefix
                  + "/dwr/engine.js\"></script>\n");
        sb.append("<script type=\"text/javascript\" src=\"" + prefix
                  + "/dwr/util.js\"></script>\n");
        sb.append("<script type=\"text/javascript\" src=\"" + prefix
                  + "/js/widget.js\"></script>\n");
        sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + prefix
                  + "/css/widget.css\"/>\n");
        sb.append("<script type=\"text/javascript\" src=\"" + prefix
                  + "/js/prototype.js\"></script>\n");
        sb.append("<script type=\"text/javascript\" src=\"" + prefix
                  + "/js/scriptaculous.js\"></script>\n");
        sb.append("</head>\n<body>\n");
        sb.append("<form action=\"/widgetAction\" id=\"widgetaction"
                  + widgetConfig.getId() + "\">\n");
        sb.append("<input type=\"hidden\" name=\"link\" value=\""
                  + widgetConfig.getLink() + "\"/>\n");
        sb.append("<input type=\"hidden\" name=\"bagType\" value=\"" + bag.getType() + "\"/>\n");
        sb.append("<input type=\"hidden\" name=\"bagName\" value=\"" + bag.getName() + "\" />\n");
        sb.append("<input type=\"hidden\" name=\"widgetid\" value=\""
                  + widgetConfig.getId() + "\" />\n");
        sb.append("<input type=\"hidden\" name=\"action\" value=\"\" styleId=\"action"
                  + widgetConfig.getId() + "\"/>\n");
        sb.append("<input type=\"hidden\" name=\"exporttype\" value=\"\" styleId=\"export"
                  + widgetConfig.getId() + "\"/>\n");

        sb.append("<div id=\"widgetcontainer" + widgetConfig.getId()
                  + "\" class=\"widgetcontainer\">\n");
        sb.append("<h3>" + widgetConfig.getTitle() + "</h3>\n");
        sb.append("<p>" + widgetConfig.getDescription() + "<br/>");
        sb.append("<span style=\"margin-top:5px\">Number of " + bag.getType()
                  + "s in this list not analysed in this widget:");
        sb.append("<span id=\"widgetnotanalysed" + widgetConfig.getId() + "\"></span>");
        sb.append("</span>\n");
        sb.append("</p>\n");
        // <c:set var="extraAttrMap" value="${widget2extraAttrs[widget.id]}" />
        if (widgetConfig instanceof EnrichmentWidgetConfig) {
            sb.append("<fieldset>\n");
            sb.append("<legend>Options</legend>\n");
            sb.append("<ol>\n");
            sb.append("<input type=\"hidden\" name=\"externalLink" + widgetConfig.getId()
                      + "\" styleId=\"externalLink" + widgetConfig.getId() + "\" value=\""
                      + widgetConfig.getExternalLink() + "\"/>\n");
            sb.append("<input type=\"hidden\" name=\"externalLinkLabel" + widgetConfig.getId()
                      + "\" styleId=\"externalLinkLabel" + widgetConfig.getId() + "\" value=\""
                      + widgetConfig.getExternalLinkLabel() + "\"/>\n");
            sb.append("<li>\n");
            sb.append("<label>Multiple Hypothesis Test Correction</label>\n");
            sb.append("<select name=\"errorCorrection\" id=\"errorCorrection"
                      + widgetConfig.getId() + "\" onchange=\"getProcessEnrichmentWidgetConfig('"
                      + widgetConfig.getId() + "','" + bag.getName() + "');\">\n");
            sb.append("<option value=\"Benjamini and Hochberg\">Benjamini and Hochberg</option>\n");
            sb.append("<option value=\"Bonferroni\">Bonferroni</option>\n");
            sb.append("<option value=\"None\">None</option>\n");
            sb.append("</select>\n");
            sb.append("</li>\n");
            sb.append("<li style=\"float:right\">\n");
            sb.append("<label>Maximum value to display</label>\n");
            sb.append("<select name=\"max\" id=\"max" + widgetConfig.getId()
                      + "\" onchange=\"getProcessEnrichmentWidgetConfig('" + widgetConfig.getId()
                      + "','" + bag.getName() + "')\">\n");
            sb.append("<option value=\"0.01\">0.01</option>\n");
            sb.append("<option value=\"0.05\">0.05</option>\n");
            sb.append("<option value=\"0.10\">0.10</option>\n");
            sb.append("<option value=\"0.50\">0.50</option>\n");
            sb.append("<option value=\"1.00\">1.00</option>\n");
            sb.append("</select>\n");
            sb.append("</li>\n");
        }
        Map<String, Collection<String>> extraAttrsMap = widgetConfig.getExtraAttributes(bag, os);
        if (extraAttrsMap != null) {
            for (String label : extraAttrsMap.keySet()) {
                sb.append("<li>\n");
                sb.append("<label>" + label + "</label>\n");
                sb.append("<select name=\"selectedExtraAttribute\" id=\"widgetselect"
                          + widgetConfig.getId() + "\" onchange=\"getProcess"
                          + TypeUtil.unqualifiedName(widgetConfig.getClass().getName()) + "('"
                          + widgetConfig.getId() + "','" + bag.getName() + "');\">\n");
                for (String option : extraAttrsMap.get(label)) {
                    sb.append("<option value=\"" + option + "\">" + option + "</option>\n");
                }
                sb.append("</select>\n");
                sb.append("</li>\n");
            }
        }
        sb.append("</ol>\n");
        sb.append("</fieldset>\n");

        if (widgetConfig instanceof EnrichmentWidgetConfig
                        || widgetConfig instanceof TableWidgetConfig) {
            sb.append("<div id=\"widget_tool_bar_div_" + widgetConfig.getId()
                      + "\" class=\"widget_tool_bar_div\" >\n");
            sb.append("<ul id=\"widget_button_bar_"
                      + widgetConfig.getId()
                      + "\" onclick=\"toggleToolBarMenu(event,'widget');\""
                      + " class=\"widget_button_bar\" >\n");
            sb.append("<li id=\"tool_bar_li_display_" + widgetConfig.getId()
                      + "\"><span id=\"tool_bar_button_display_" + widgetConfig.getId()
                      + "\" class=\"widget_tool_bar_button\">Display</span></li>\n");
            sb.append("<li id=\"tool_bar_li_export_" + widgetConfig.getId()
                      + "\"><span id=\"tool_bar_button_export_" + widgetConfig.getId()
                      + "\" class=\"widget_tool_bar_buttons\">Export</span></li>\n");
            sb.append("</ul>\n");
            sb.append("</div>\n");

            sb.append("<div id=\"tool_bar_item_display_" + widgetConfig.getId()
                      + "\" style=\"visibility:hidden;width:200px\" class=\"tool_bar_item\">\n");
            sb.append("<a href=\"javascript:submitWidgetForm('" + widgetConfig.getId()
                      + "','display',null)\">Display checked items in results table</a>\n");
            sb.append("<hr/>\n");
            sb.append("<a href=\"javascript:hideMenu('tool_bar_item_display_"
                      + widgetConfig.getId() + "','widget')\" >Cancel</a>\n");
            sb.append("</div>\n");

            sb.append("<div id=\"tool_bar_item_export_" + widgetConfig.getId()
                      + "\" style=\"visibility:hidden;width:230px\" class=\"tool_bar_item\">\n");
            sb.append("<a href=\"javascript:submitWidgetForm('" + widgetConfig.getId()
                      + "','export','csv')\">Export selected as comma separated values</a><br/>\n");
            sb.append("<a href=\"javascript:submitWidgetForm('" + widgetConfig.getId()
                      + "','export','tab')\">Export selected as tab separated values</a>\n");
            sb.append("<hr/>");
            sb.append("<a href=\"javascript:hideMenu('tool_bar_item_export_" + widgetConfig.getId()
                      + "','widget')\" >Cancel</a>\n");
            sb.append("</div>\n");
        }
        sb.append("<div id=\"widgetdata" + widgetConfig.getId() + "\" class=\"widgetdata\">\n");
        if (widgetConfig instanceof TableWidgetConfig
                        || widgetConfig instanceof EnrichmentWidgetConfig) {
            sb.append("<table id=\"tablewidget" + widgetConfig.getId() + "\" border=\"0\" >\n");
            sb.append("<thead id=\"tablewidget" + widgetConfig.getId() + "head\"></thead>\n");
            sb.append("<tbody id=\"tablewidget" + widgetConfig.getId() + "body\"></tbody>\n");
            sb.append("</table>");
        }
        sb.append("</div>\n");
        sb.append("<div id=\"widgetdatawait" + widgetConfig.getId()
                  + "\" class=\"widgetdatawait\"><img src=\"" + prefix
                  + "/images/wait30.gif\" title=\"Searching...\"/></div>\n");
        sb.append("<div id=\"widgetdatanoresults"
                  + widgetConfig.getId()
                  + "\" class=\"widgetdatawait\" style=\"display:none;\">"
                  + "<i>no results found</i></div>\n");
        sb.append("<script language=\"javascript\">");
        if (widgetConfig instanceof GraphWidgetConfig) {
            sb.append("<!--//<![CDATA[\n");
            sb.append("getProcessGraphWidgetConfig('" + widgetConfig.getId() + "','"
                      + bag.getName() + "');\n");
            sb.append("//]]>-->\n");
        } else
            if (widgetConfig instanceof TableWidgetConfig) {
                sb.append("<!--//<![CDATA[\n");
                sb.append("getProcessTableWidgetConfig('" + widgetConfig.getId() + "','"
                          + bag.getName() + "');\n");
                sb.append("//]]>-->\n");
            } else if (widgetConfig instanceof EnrichmentWidgetConfig) {
                sb.append("<!--//<![CDATA[\n");
                sb.append("getProcessEnrichmentWidgetConfig('" + widgetConfig.getId() + "','"
                          + bag.getName() + "');\n");
                sb.append("//]]>-->\n");
            }
        sb.append("</script>\n");
        sb.append("</div>\n");
        sb.append("</form>\n");
        sb.append("</body></html>\n");
        return sb.toString();
    }
}
