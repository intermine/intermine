package org.intermine.webservice.server.widget;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.path.Path;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.BagQueryResult;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.pathqueryresult.PathQueryResultHelper;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.results.WebResults;
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
 * widgets?widgetId=go_enrichment&className=Gene&extraAttributes=Bonferroni,0.1,biological_process&ids=FBgn0011648,FBgn0011655,FBgn0025800&format=html
 * get a GraphWidget
 * http://sauron.flymine.org:8080/flymine/service/widgets?widgetId=flyatlas&className=Gene&extraAttributes=&ids=FBgn0011648,FBgn0011655,FBgn0025800&format=html
 * 
 * @author "Xavier Watkins"
 */
public class WidgetsService extends WebService
{

    /**
     * Executes service specific logic. 
     * @param request request
     * @param response response
     * @throws Exception an error has occured
     */
    @Override
    protected void execute(HttpServletRequest request, HttpServletResponse response)
                    throws Exception {
        WidgetsServiceInput widgetsServiceInput = getInput();
        ServletContext servletContext = request.getSession().getServletContext();
        Profile profile = (Profile) request.getSession().getAttribute(Constants.PROFILE);
        
        String className = widgetsServiceInput.getClassName();
        List<String> ids = widgetsServiceInput.getIds();
        InterMineBag imBag = getBag(className, ids, servletContext, profile);
        WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
        WidgetConfig widgetConfig = webConfig.getWidgets().get(widgetsServiceInput.getWidgetId());
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        if (widgetConfig instanceof TableWidgetConfig) {
            ((TableWidgetConfig) widgetConfig).setClassKeys((Map) servletContext
                            .getAttribute(Constants.CLASS_KEYS));
        }
         response.getWriter().print(
                        getHtml(widgetConfig, imBag, new URLGenerator(request).getBaseURL(), os));
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
                                  ServletContext servletContext, Profile profile)
                    throws ObjectStoreException {
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
        Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
        BagQueryConfig bagQueryConfig = (BagQueryConfig) servletContext
                        .getAttribute(Constants.BAG_QUERY_CONFIG);
        Model model = os.getModel();
        try {
            className = StringUtil.capitalise(className);
            Class.forName(model.getPackageName() + "." + className);
        } catch (ClassNotFoundException clse) {
            return null;
        }
        ObjectStoreWriter uosw = profile.getProfileManager().getUserProfileObjectStore();
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

        PathQuery pathQuery = new PathQuery(model);

        List<Path> view = PathQueryResultHelper.getDefaultView(className, model, webConfig,
            null, true);

        pathQuery.setViewPaths(view);
        String label = null, id = null, code = pathQuery.getUnusedConstraintCode();
        StringBuffer sb = new StringBuffer();
        for (String ident : ids) {
            if (sb.length() > 0) {
                sb.append("\t");
            }
            sb.append(ident);
        }
        Constraint c = new Constraint(ConstraintOp.LOOKUP, sb.toString(),
                        false, label, code, id, null);
        pathQuery.addNode(className).getConstraints().add(c);
        pathQuery.setConstraintLogic("A and B and C");
        pathQuery.syncLogicExpression("and");

        Map returnBagQueryResults = new HashMap();
        WebResults webResults = PathQueryResultHelper.createPathQueryGetResults(pathQuery, profile,
                        os, classKeys,
                        bagQueryConfig, returnBagQueryResults, servletContext);
        
        // There's only one node, get the first value
        BagQueryResult bagQueryResult = (BagQueryResult) returnBagQueryResults.values().iterator()
                        .next();
        List <Integer> bagList = new ArrayList <Integer> ();
        bagList.addAll(bagQueryResult.getMatchAndIssueIds());

        InterMineBag imBag = new InterMineBag(bagName,
                        className , null , new Date() ,
                        os , profile.getUserId() , uosw);
        ObjectStoreWriter osw = new ObjectStoreWriterInterMineImpl(os);
        osw.addAllToBag(imBag.getOsb(), bagList);
        osw.close();
        profile.saveBag(imBag.getName(), imBag);
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
     * @throws Exception an error has occured
     */
    private String getHtml(WidgetConfig widgetConfig, InterMineBag bag, String prefix, ObjectStore os)
                    throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append("<html><head>");
        sb.append("<script type=\"text/javascript\" src=\"" + prefix
                  + "/dwr/interface/AjaxServices.js\"></script>");
        sb.append("<script type=\"text/javascript\" src=\"" + prefix
                  + "/dwr/engine.js\"></script>");
        sb.append("<script type=\"text/javascript\" src=\"" + prefix + "/dwr/util.js\"></script>");
        sb.append("<script type=\"text/javascript\" src=\"" + prefix + "/js/widget.js\"></script>");
        sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + prefix
                  + "/css/widget.css\"/>");
        sb.append("<script type=\"text/javascript\" src=\"" + prefix
                  + "/js/prototype.js\"></script>");
        sb.append("<script type=\"text/javascript\" src=\"" + prefix
                  + "/js/scriptaculous.js\"></script>");
        sb.append("</head><body>");
        sb.append("<form action=\"/widgetAction\" id=\"widgetaction"
                  + widgetConfig.getId() + "\">");
        sb.append("<input type=\"hidden\" name=\"link\" value=\"" + widgetConfig.getLink() + "\"/>");
        sb.append("<input type=\"hidden\" name=\"bagType\" value=\"" + bag.getType() + "\"/>");
        sb.append("<input type=\"hidden\" name=\"bagName\" value=\"" + bag.getName() + "\" />");
        sb.append("<input type=\"hidden\" name=\"widgetid\" value=\"" + widgetConfig.getId() + "\" />");
        sb.append("<input type=\"hidden\" name=\"action\" value=\"\" styleId=\"action"
                  + widgetConfig.getId() + "\"/>");
        sb.append("<input type=\"hidden\" name=\"exporttype\" value=\"\" styleId=\"export"
                  + widgetConfig.getId() + "\"/>");

        sb.append("<div id=\"widgetcontainer" + widgetConfig.getId()
                  + "\" class=\"widgetcontainer\">");
        sb.append("<h3>" + widgetConfig.getTitle() + "</h3>");
        sb.append("<p>" + widgetConfig.getDescription() + "<br/>");
         sb
                        .append("<span style=\"margin-top:5px\">Number of " + bag.getType()
                  + "s in this list not analysed in this widget:");
         sb.append("<span id=\"widgetnotanalysed"+widgetConfig.getId()+"\"></span>");
         sb.append("</span>");
        sb.append("</p>");
        // <c:set var="extraAttrMap" value="${widget2extraAttrs[widget.id]}" />
        if (widgetConfig instanceof EnrichmentWidgetConfig) {
            sb.append("<fieldset>");
            sb.append("<legend>Options</legend>");
            sb.append("<ol>");
            sb.append("<input type=\"hidden\" name=\"externalLink" + widgetConfig.getId()
                      + "\" styleId=\"externalLink" + widgetConfig.getId() + "\" value=\""
                      + widgetConfig.getExternalLink() + "\"/>");
            sb.append("<input type=\"hidden\" name=\"externalLinkLabel" + widgetConfig.getId()
                      + "\" styleId=\"externalLinkLabel" + widgetConfig.getId() + "\" value=\""
                      + widgetConfig.getExternalLinkLabel() + "\"/>");
            sb.append("<li>");
            sb.append("<label>Multiple Hypothesis Test Correction</label>");
            sb.append("<select name=\"errorCorrection\" id=\"errorCorrection"
                      + widgetConfig.getId() + "\" onchange=\"getProcessEnrichmentWidgetConfig('"
                      + widgetConfig.getId() + "','" + bag.getName() + "}');\">");
            sb
                            .append("<html:option value=\"Benjamini and Hochberg\">Benjamini and Hochberg</html:option>");
            sb.append("<option value=\"Bonferroni\">Bonferroni</option>");
            sb.append("<option value=\"None\">None</option>");
            sb.append("</select>");
            sb.append("</li>");
            sb.append("<li style=\"float:right\">");
            sb.append("<label>Maximum value to display</label>");
            sb.append("<select name=\"max\" id=\"max" + widgetConfig.getId()
                      + "\" onchange=\"getProcessEnrichmentWidgetConfig('" + widgetConfig.getId()
                      + "','" + bag.getName() + "')\">");
            sb.append("<option value=\"0.01\">0.01</option>");
            sb.append("<option value=\"0.05\">0.05</option>");
            sb.append("<option value=\"0.10\">0.10</option>");
            sb.append("<option value=\"1.00\">1.00</option>");
            sb.append("<option value=\"0.50\">0.50</option>");
            sb.append("</select>");
            sb.append("</li>");
        }
        Map<String, Collection<String>> extraAttrsMap = widgetConfig.getExtraAttributes(bag, os);
        if (extraAttrsMap != null) {
            for (String label : extraAttrsMap.keySet()) {
                sb.append("<li>");
                sb.append("<label>" + label + "</label>");
                sb.append("<select name=\"selectedExtraAttribute\" id=\"widgetselect"
                          + widgetConfig.getId() + "\" onchange=\"getProcess"
                          + TypeUtil.unqualifiedName(widgetConfig.getClass().getName()) + "('"
                          + widgetConfig.getId() + "','" + bag.getName() + "');\">");
                for (String option : extraAttrsMap.get(label)) {
                    sb.append("option value=\"" + option + "\">" + option + "</option>");
                }
                sb.append("</select>");
                sb.append("</li>");
            }
        }
        sb.append("</ol>");
        sb.append("</fieldset>");

        if (widgetConfig instanceof EnrichmentWidgetConfig
            || widgetConfig instanceof TableWidgetConfig) {
            sb.append("<div id=\"widget_tool_bar_div_" + widgetConfig.getId()
                      + "\" class=\"widget_tool_bar_div\" >");
            sb
                            .append("<ul id=\"widget_button_bar_"
                                    + widgetConfig.getId()
                                    + "\" onclick=\"toggleToolBarMenu(event,'widget');\" class=\"widget_button_bar\" >");
            sb.append("<li id=\"tool_bar_li_display_" + widgetConfig.getId()
                      + "\"><span id=\"tool_bar_button_display_" + widgetConfig.getId()
                      + "\" class=\"widget_tool_bar_button\">Display</span></li>");
            sb.append("<li id=\"tool_bar_li_export_" + widgetConfig.getId()
                      + "\"><span id=\"tool_bar_button_export_" + widgetConfig.getId()
                      + "\" class=\"widget_tool_bar_buttons\">Export</span></li>");
            sb.append("</ul>");
            sb.append("</div>");

            sb.append("<div id=\"tool_bar_item_display_" + widgetConfig.getId()
                      + "\" style=\"visibility:hidden;width:200px\" class=\"tool_bar_item\">");
            sb.append("<a href=\"javascript:submitWidgetForm('" + widgetConfig.getId()
                      + "','display',null)\">Display checked items in results table</a>");
            sb.append("<hr/>");
            sb.append("<a href=\"javascript:hideMenu('tool_bar_item_display_"
                      + widgetConfig.getId() + "','widget')\" >Cancel</a>");
            sb.append("</div>");

            sb.append("<div id=\"tool_bar_item_export_" + widgetConfig.getId()
                      + "\" style=\"visibility:hidden;width:230px\" class=\"tool_bar_item\">");
            sb.append("<a href=\"javascript:submitWidgetForm('" + widgetConfig.getId()
                      + "','export','csv')\">Export selected as comma separated values</a><br/>");
            sb.append("<a href=\"javascript:submitWidgetForm('" + widgetConfig.getId()
                      + "','export','tab')\">Export selected as tab separated values</a>");
            sb.append("<hr/>");
            sb.append("<a href=\"javascript:hideMenu('tool_bar_item_export_" + widgetConfig.getId()
                      + "','widget')\" >Cancel</a>");
            sb.append("</div>");
        }
        sb.append("<div id=\"widgetdata" + widgetConfig.getId() + "\" class=\"widgetdata\">");
        if (widgetConfig instanceof TableWidgetConfig
            || widgetConfig instanceof EnrichmentWidgetConfig) {
            sb.append("<table id=\"tablewidget" + widgetConfig.getId() + "\" border=\"0\" >");
            sb.append("<thead id=\"tablewidget" + widgetConfig.getId() + "head\"></thead>");
            sb.append("<tbody id=\"tablewidget" + widgetConfig.getId() + "body\"></tbody>");
            sb.append("</table>");
        }
        sb.append("</div>");
        sb.append("<div id=\"widgetdatawait" + widgetConfig.getId()
                  + "\" class=\"widgetdatawait\"><img src=\"" + prefix
                  + "/images/wait30.gif\" title=\"Searching...\"/></div>");
        sb
                        .append("<div id=\"widgetdatanoresults"
                                + widgetConfig.getId()
                                + "\" class=\"widgetdatawait\" style=\"display:none;\"><i>no results found</i></div>");
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
            } else
                if (widgetConfig instanceof EnrichmentWidgetConfig) {
                    sb.append("<!--//<![CDATA[\n");
                    sb.append("getProcessEnrichmentWidgetConfig('" + widgetConfig.getId() + "','"
                              + bag.getName() + "');\n");
                    sb.append("//]]>-->\n");
                }
        sb.append("</script>");
        sb.append("</div>");
        sb.append("</form>");
        sb.append("</body></html>");
        return sb.toString();
    }
}
