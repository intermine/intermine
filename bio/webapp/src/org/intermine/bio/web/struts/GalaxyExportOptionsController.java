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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import org.intermine.api.profile.Profile;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.Column;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.LocatedSequenceFeature;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.util.DynamicUtil;
import org.intermine.util.PropertiesUtil;
import org.intermine.web.logic.export.ExportHelper;
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
//    private static final Logger LOG = Logger.getLogger(GalaxyExportOptionsController.class);
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

        // Build webservice URL
        PathQuery query = pt.getWebTable().getPathQuery();

        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Model model = im.getModel();
        String queryXML = PathQueryBinding.marshal(query, "tmpName", model.getName(),
                                                   PathQuery.USERPROFILE_VERSION);
        String encodedQueryXML = URLEncoder.encode(queryXML, "UTF-8");
        StringBuffer stringUrl = new StringBuffer(
                new URLGenerator(request).getPermanentBaseURL()
                        + "/service/query/results?query=" + encodedQueryXML
                        + "&size=1000000");

        request.setAttribute("viewURL", stringUrl.toString());
        request.setAttribute("exportAsBED", canExportAsBEDToGalaxy(pt));

        // If can be exported as BED
        Profile profile = SessionMethods.getProfile(session);
        WebResultsExecutor webResultsExecutor = im.getWebResultsExecutor(profile);

        Set<String> orgSet = new LinkedHashSet<String>();
        List<String> summaryPathList = new ArrayList<String>();
        if (canExportAsBEDToGalaxy(pt)) {
            for (Map.Entry<String, Integer> entry : pathIndexMap.entrySet()) {
                String pathToAdd = entry.getKey() + ".organism.shortName";
                query.addView(pathToAdd);
                summaryPathList.add(pathToAdd);
            }
            for (String orgPath : summaryPathList) {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                List<ResultsRow> results = webResultsExecutor.summariseQuery(query, orgPath);
                orgSet.add((String) results.get(0).get(0));
            }

            String genomeBuild = "";
            Properties props = PropertiesUtil.getProperties();
            if (orgSet.size() == 1) {
                if (orgSet.iterator().next().equals("D. melanogaster")) {
                    genomeBuild = props.getProperty("genomeVersion.fly");
                }
                if (orgSet.iterator().next().equals("C. elegans")) {
                    genomeBuild = props.getProperty("genomeVersion.worm");
                }
            }
            request.setAttribute("genomeBuild", genomeBuild);
            request.setAttribute("organism", Arrays.toString(orgSet.toArray()));
        }

        return null;
    }

    /**
     * Whether the results can be exported as BED format.
     *
     * @param pt PagedTable in the session
     * @return a boolean
     */
    private boolean canExportAsBEDToGalaxy(PagedTable pt) {
        return ExportHelper.getClassIndex(ExportHelper.getColumnClasses(pt),
                LocatedSequenceFeature.class) >= 0;
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
            @SuppressWarnings("rawtypes")
            Set<Class> prefixClasses = DynamicUtil.decomposeClass(prefixCD.getType());
            Class<?> prefixClass = prefixClasses.iterator().next();
            // Chromosome is treated as a sequence feature in the model
            if (LocatedSequenceFeature.class.isAssignableFrom(prefixClass)
                    && !Chromosome.class.isAssignableFrom(prefixClass)) {
                if (!retPaths.keySet().contains(prefix)) {
                    retPaths.put(prefix, index);
                }
            }
        }

        return retPaths;
    }

}
