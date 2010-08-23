package org.intermine.bio.web.struts;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.model.bio.LocatedSequenceFeature;
import org.intermine.model.bio.Organism;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.ExportException;
import org.intermine.web.logic.export.ExportHelper;
import org.intermine.web.logic.export.http.HttpExporterBase;
import org.intermine.web.logic.pathqueryresult.PathQueryResultHelper;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.InterMineAction;
import org.intermine.web.util.URLGenerator;

/**
 * Generate feature path query.
 *
 * @author Fengyuan Hu
 */
public class GalaxyExportAction extends InterMineAction
{
//    private static final Logger LOG = Logger.getLogger(GalaxyExportAction.class);
    @Override
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Model model = im.getModel();
        WebConfig webConfig = SessionMethods.getWebConfig(request);

        String  tableName = (String) request.getParameter("tableName");
        PagedTable pt = SessionMethods.getResultsTable(session, tableName);

        PathQuery query = pt.getWebTable().getPathQuery();

        // Prepare Response
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        // Make a new view
        Integer index = Integer.parseInt(request.getParameter("index"));
        String prefix = request.getParameter("prefix");

        String type = TypeUtil.unqualifiedName(ExportHelper
                .getColumnClasses(pt).get(index).toString());
        // Type - Class name. e.g. Gene, Protein, etc.
        List<Path> view = PathQueryResultHelper.getDefaultView(type, model, webConfig,
                prefix, false);
        view = getFixedView(view);

        // Reorder the view, place chr, start and end at the first three columns
        List<Path> newView = new ArrayList<Path>();
        int chrIdx = -1;
        int startIdx = -1;
        int endIdx = -1;
        for (Path path : view) {
            if (path.toString().contains("chromosome.primaryIdentifier")) {
                chrIdx = view.indexOf(path);
            }
            if (path.toString().contains("chromosomeLocation.start")) {
                startIdx = view.indexOf(path);
            }
            if (path.toString().contains("chromosomeLocation.end")) {
                endIdx = view.indexOf(path);
            }
        }
        newView.add(view.get(chrIdx)); //Chr
        newView.add(view.get(startIdx)); //start
        newView.add(view.get(endIdx)); //end
        view.removeAll(newView);
        newView.addAll(view);
        query.setViewPaths(newView);

        // Build Webservice URL
        String queryXML = PathQueryBinding.marshal(query, "tmpName", model.getName(),
                                                   PathQuery.USERPROFILE_VERSION);
        String encodedQueryXML = URLEncoder.encode(queryXML, "UTF-8");
        StringBuffer stringUrl = new StringBuffer(
                new URLGenerator(request).getPermanentBaseURL()
                        + "/service/query/results?query=" + encodedQueryXML
                        + "&size=1000000");

        // Get extra information - genomeBuild & organism & extra info
//        ResultManipulater rm = new ResultManipulater();
//        Map<Integer, String> orgNameMap = rm.findOrganisms(pt, request, index);


        // Same function as ResultManipulater, reliable but slower
        /**
         *  Removed to GalaxyExportOptionsController
        Map<Integer, String> orgNameMap = new LinkedHashMap<Integer, String>();

        for (int i = 0; i < pt.getExactSize(); i++) {
            Organism org = ((LocatedSequenceFeature) pt.getWebTable().getResultElements(i).get(0)
                    .get(index).getValue().getObject()).getOrganism();
            orgNameMap.put(org.getTaxonId(), org.getShortName());
        }

        String genomeBuild = "";
        String organism = "";
        if (orgNameMap != null) {

            Properties props = PropertiesUtil.getProperties();
            if (orgNameMap.keySet().size() == 1) {
                if (orgNameMap.keySet().iterator().next() == 7227) {
                    genomeBuild = props.getProperty("genomeVersion.fly");
                }
                if (orgNameMap.keySet().iterator().next() == 6239) {
                    genomeBuild = props.getProperty("genomeVersion.worm");
                }
            }

            organism = Arrays.toString(orgNameMap.values().toArray());
        }
        */
        // Write to Response
        StringBuffer pathString = new StringBuffer();
        pathString.append("|");
        for (Path path : newView) {
            pathString.append(path.toStringNoConstraints());
            pathString.append("|");
        }

        StringBuffer returnString = new StringBuffer();
        // URL>>>>>info>>>organism>>>>>genomeBuild
        returnString.append(stringUrl);
        returnString.append(">>>>>");
        returnString.append(pathString);

        out.println(returnString.toString());

        out.flush();
        out.close();

        return null;
    }

    /**
     * Colon (:) is outer join and dot (.) is inner join, id replace colon with dot, it will change
     * the original query but return results which are with chr, start and end.
     *
     * @param pathQuery
     * @param joinPath
     * @throws PathException
     * */
    private List<Path> getFixedView(List<Path> view) throws PathException {
        String invalidPath = ":";
        String validPath = ".";
        List<Path> ret = new ArrayList<Path>();
        for (Path path : view) {
            if (path.toString().contains(invalidPath)) {
                String newPathString = path.toString().replace(invalidPath,
                        validPath);
                path = new Path(path.getModel(), newPathString);
            }
            ret.add(path);
        }
        return ret;
    }
}

/**
 * To find the organisms that the exported sequence features belong to.
 *
 * @author Fengyuan Hu
 *
 */
class ResultManipulater extends HttpExporterBase
{
//    private static final Logger LOG = Logger.getLogger(ResultManipulater.class);

    /**
     *
     * @param pt PagedTable
     * @param request Http Request
     * @param index index of pagedTable column for the feature to export
     * @return A Map: Key - organism's TaxonId; Value - organism's shortName
     */
    public Map<Integer, String> findOrganisms(PagedTable pt,
            HttpServletRequest request, int index) {

//        if (pt.getEstimatedSize() > 10000) { }
        ExportResultsIterator resultIt = getResultRows(pt, request);

        Map<Integer, String> orgNameMap = new LinkedHashMap<Integer, String>();
        try {
            while (resultIt.hasNext()) {
                List<ResultElement> row = resultIt.next();
                List<ResultElement> elWithObject = getResultElements(row, index);
                for (ResultElement re : elWithObject) {
                    Organism org = ((LocatedSequenceFeature) re.getObject()).getOrganism();
                    orgNameMap.put(org.getTaxonId(), org.getShortName());
                }
            }
        } catch (Exception ex) {
            throw new ExportException("Export failed", ex);
        }

        return orgNameMap;

    }

    private List<ResultElement> getResultElements(List<ResultElement> row, int index) {
        List<ResultElement> els = new ArrayList<ResultElement>();
        if (row.get(index) != null) {
            els.add(row.get(index));
        }
        return els;
    }

}
