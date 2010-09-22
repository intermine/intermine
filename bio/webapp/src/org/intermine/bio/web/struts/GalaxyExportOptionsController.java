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

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.results.Column;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.FastPathObject;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.util.URLGenerator;

/**
 * Controller for galaxyExportOptions.tile.
 *
 * @author Fengyuan Hu
 */

public class GalaxyExportOptionsController extends TilesAction
{
    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        String tableName = request.getParameter("table");
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        PagedTable pt = SessionMethods.getResultsTable(session, tableName);

        LinkedHashMap<Path, Integer> exportClassPathsMap = getExportClassPaths(pt);
        List<Path> exportClassPaths = new ArrayList<Path>(exportClassPathsMap.keySet());

        Map<String, String> pathMap = new LinkedHashMap<String, String>();
        for (Path path: exportClassPaths) {
            String pathString = path.toStringNoConstraints();
            String displayPath = pathString.replace(".", " &gt; ");
            pathMap.put(pathString, displayPath);
        }

        Map<String, Integer> pathIndexMap = new LinkedHashMap<String, Integer>();
        for (int index = 0; index < exportClassPaths.size(); index++) {
            String pathString = exportClassPaths.get(index).toStringNoConstraints();
            Integer idx = exportClassPathsMap.get(exportClassPaths.get(index));
            pathIndexMap.put(pathString, idx);
        }

        request.setAttribute("exportClassPaths", pathMap);
        request.setAttribute("pathIndexMap", pathIndexMap);

        // If can export feature
        if (request.getParameter("exportAsBED") != null) {
            request.setAttribute("exportAsBED", request.getParameter("exportAsBED"));
        } else {
            request.setAttribute("exportAsBED", false);
        }

        // Build webservice URL
        PathQuery query = pt.getWebTable().getPathQuery();

        // Test if it has bag constraints
        // TODO Move the method to PathQuery class in the next round refactoring
        /*
        Set<String> bagNames = query.getBagNames();
        if (!bagNames.isEmpty()) {
            Map<PathConstraint, String> constrains = query.getConstraints();
            for (PathConstraint constraint : constrains.keySet()) {
                if (constraint instanceof PathConstraintBag) {
                    String bagName = ((PathConstraintBag) constraint).getBag();
                    String path = ((PathConstraintBag) constraint).getPath();
                    InterMineBag imBag = im.getBagManager().getUserOrGlobalBag(
                            SessionMethods.getProfile(session), bagName);
                    PathConstraintIds newConstraint = new PathConstraintIds(
                            path, ConstraintOp.IN, imBag.getContentsAsIds());
                    query.replaceConstraint(constraint, newConstraint);
                }
            }
        }*/

        // TODO Bag is not support so far, Galaxy can not fetch data by the query, fix me
        String isBag = "false";
        Set<String> bagNames = query.getBagNames();
        if (!bagNames.isEmpty()) {
            isBag = "true";
            request.setAttribute("isBag", isBag);
        }

        Model model = im.getModel();
        String queryXML = PathQueryBinding.marshal(query, "tmpName", model.getName(),
                                                   PathQuery.USERPROFILE_VERSION);
        String encodedQueryXML = URLEncoder.encode(queryXML, "UTF-8");
        StringBuffer stringUrl = new StringBuffer(
                new URLGenerator(request).getPermanentBaseURL()
                        + "/service/query/results?query=" + encodedQueryXML
                        + "&size=1000000");

        request.setAttribute("viewURL", stringUrl.toString());

        return null;
    }


    /**
     * From the columns of the PagedTable, return a List of the Paths that this exporter will
     * use to find sequences to export.  The returned Paths are a subset of the prefixes of the
     * column paths.
     * eg. if the columns are ("Gene.primaryIdentifier", "Gene.secondaryIdentifier",
     * "Gene.proteins.primaryIdentifier") return ("Gene", "Gene.proteins").
     * @param pt the PagedTable
     * @return a list of Paths that have sequence
     */
    public static LinkedHashMap<Path, Integer> getExportClassPaths(PagedTable pt) {
        LinkedHashMap<Path, Integer> retPaths = new LinkedHashMap<Path, Integer>();

        List<Column> columns = pt.getColumns();

        for (int index = 0; index < columns.size(); index++) {
            Path prefix = columns.get(index).getPath().getPrefix();
            ClassDescriptor prefixCD = prefix.getLastClassDescriptor();
            Class<? extends FastPathObject> prefixClass = DynamicUtil.getSimpleClass(prefixCD
                    .getType());
            // Chromosome is treated as a sequence feature in the model
            if (SequenceFeature.class.isAssignableFrom(prefixClass)
                    && !Chromosome.class.isAssignableFrom(prefixClass)) {
                if (!retPaths.keySet().contains(prefix)) {
                    retPaths.put(prefix, index);
                }
            }
        }

        return retPaths;
    }

}
