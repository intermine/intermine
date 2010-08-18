package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.results.WebTable;
import org.intermine.metadata.Model;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.util.PropertiesUtil;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.util.URLGenerator;
/**
 * @deprecated
 *
 * Generates the link to send data to Galaxy
 * @author Xavier Watkins
 *
 */
public class GalaxyController extends TilesAction
{
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();

        String  tableName = (String) request.getAttribute("table");
        PagedTable pt = SessionMethods.getResultsTable(session, tableName);

        WebTable wt = pt.getWebTable();
        PathQuery query = pt.getWebTable().getPathQuery();

        // TODO the query will be NULL if the query just ran isn't on the session, eg. a quicksearch
        // This will not be true once we have a query registry.

        // The quick search is a special case - we don't want the quick search to appear in people's
        // history or replace a query that they have been editing in the QueryBuilder.

        // "Export to Galaxy" in the quick search results is grey out
        if (query == null) {
            return null;
        } else {
            SessionMethods.recordMessage(query.getView().toString(), session);

            List<String> viewList = query.getViewStrings();

            // Any clever way?
            boolean chrFlag = false;
            int chrIndex = 0;
            boolean startFlag = false;
            int startIndex = 0;
            boolean endFlag = false;
            int endIndex = 0;

            for (int i = 0; i < viewList.size(); i++) {
                if (viewList.get(i).toLowerCase().endsWith(
                        "chromosome.primaryidentifier")
                        || viewList.get(i).toLowerCase().endsWith(
                                        "chromosomelocation.object.primaryidentifier")
                        || viewList.get(i).toLowerCase()
                                .endsWith("locations.object.primaryidentifier")) {
                    chrFlag = true;
                    chrIndex = i;
                    break;
                }
            }

            for (int i = 0; i < viewList.size(); i++) {
                if (viewList.get(i).toLowerCase().endsWith("chromosomelocation.start")) {
                    startFlag = true;
                    startIndex = i;
                    break;
                }
            }

            for (int i = 0; i < viewList.size(); i++) {
                if (viewList.get(i).toLowerCase().endsWith("chromosomelocation.end")) {
                    endFlag = true;
                    endIndex = i;
                    break;
                }
            }

            if (chrFlag && startFlag && endFlag) {
                List<String> subViewList = viewList;
                List<String> tempViewList = new ArrayList<String>();

                tempViewList.add(viewList.get(chrIndex));
                tempViewList.add(viewList.get(startIndex));
                tempViewList.add(viewList.get(endIndex));

                subViewList.removeAll(tempViewList);

                List<String> newViewList = new ArrayList<String>();
                newViewList.addAll(tempViewList);
                newViewList.addAll(subViewList);

                query.setView(newViewList);

                request.setAttribute("dataType", "bed");
            }

            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            Model model = im.getModel();
            String queryXML = PathQueryBinding.marshal(query, "tmpName", model.getName(),
                                                       PathQuery.USERPROFILE_VERSION);
            String encodedQueryXML = URLEncoder.encode(queryXML, "UTF-8");
            StringBuffer stringUrl = new StringBuffer(
                    new URLGenerator(request).getPermanentBaseURL()
                            + "/service/query/results?query=" + encodedQueryXML
                            + "&size=1000000");

            request.setAttribute("urlSendBack", stringUrl.toString());

            // Get Genome build
            Properties props = PropertiesUtil.getProperties();
//            SessionMethods.recordMessage(props.getProperty("genomeVersion.fly"), session);
//            SessionMethods.recordMessage(props.getProperty("genomeVersion.worm"), session);


            request.setAttribute("genomeBuild", props.getProperty("genomeVersion.fly"));
            // Can send the view
            request.setAttribute("info", Arrays.toString(query.getViewStrings().toArray()));
            // Find in the constraint
            request.setAttribute("organism", "");
            request.setAttribute("description", "");
        }

        return null;
    }
}
