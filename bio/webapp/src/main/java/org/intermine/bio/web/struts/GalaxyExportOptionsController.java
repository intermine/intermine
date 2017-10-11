package org.intermine.bio.web.struts;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.results.Column;
import org.intermine.bio.web.export.BEDHttpExporter;
import org.intermine.bio.web.logic.OrganismGenomeBuildLookup;
import org.intermine.bio.web.logic.SequenceFeatureExportUtil;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.FastPathObject;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.template.SwitchOffAbility;
import org.intermine.template.TemplateQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.metadata.StringUtil;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.export.http.TableHttpExporter;
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
    private static final Logger LOG = Logger.getLogger(GalaxyExportOptionsController.class);

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

        boolean canExportAsBED = false;

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Model model = im.getModel();
        PathQuery query = new PathQuery(model);

        // org and dbkey for galaxy use
        Set<String> orgSet = new HashSet<String>();
        Set<String> genomeBuildSet = new HashSet<String>();

        // Build GenomicRegion pathquery, the request is from GenomicRegionSearch "export to Galaxy"
        if (request.getParameter("featureIds") != null) {
            String featureIds = request.getParameter("featureIds").trim();
            String orgName = request.getParameter("orgName");

            if (orgName != null && !"".equals(orgName)) {
                orgSet.add(orgName);
            }

            // Refer to GenomicRegionSearchService.getExportFeaturesQuery method
            String path = "SequenceFeature";
            query.addView(path + ".primaryIdentifier");
            query.addView(path + ".chromosomeLocation.locatedOn.primaryIdentifier");
            query.addView(path + ".chromosomeLocation.start");
            query.addView(path + ".chromosomeLocation.end");
            query.addView(path + ".organism.name");

            // use ids or pids
            String[] idsInStr = featureIds.split(",");
            Set<Integer> ids = new HashSet<Integer>();
            boolean isIds = true;
            for (String id : idsInStr) {
                id = id.trim();
                if (!Pattern.matches("^\\d+$", id)) {
                    isIds = false;
                    break;
                }
                ids.add(Integer.valueOf(id));
            }

            if (isIds) {
                query.addConstraint(Constraints.inIds(path, ids));
            } else {
                if (featureIds.contains("'")) {
                    featureIds = featureIds.replaceAll("'", "\\\\'");
                }
                query.addConstraint(Constraints.lookup(path, featureIds, null));
            }

            canExportAsBED = true;

        } else { // request from normal result table
            String tableName = request.getParameter("table");
            request.setAttribute("table", tableName);
            PagedTable pt = SessionMethods.getResultsTable(session, tableName);

            // Null check to page table, maybe session timeout?
            if (pt == null) {
                LOG.error("Page table is NULL...");
                return null;
            }

            // Check if can export as BED
            TableHttpExporter tableExporter = new BEDHttpExporter();

            try {
                canExportAsBED = tableExporter.canExport(pt);
            } catch (Exception e) {
                canExportAsBED = false;

                LOG.error("Caught an error running canExport() for: BEDHttpExporter. " + e);
                e.printStackTrace();
            }

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

            // Support export public and private lists to Galaxy
            query = pt.getWebTable().getPathQuery();
            ObjectStore os = im.getObjectStore();

            Map<PathConstraint, String> constrains = query.getConstraints();
            for (PathConstraint constraint : constrains.keySet()) {
                if (constraint instanceof PathConstraintBag) {
                    String bagName = ((PathConstraintBag) constraint).getBag();
                    InterMineBag imBag = im.getBagManager().getBag(
                            SessionMethods.getProfile(session), bagName);

                    // find the classKeys
                    Set<String> classKeySet = new LinkedHashSet<String>();
                    for (Integer id : imBag.getContentsAsIds()) {
                        String classKey =
                            pt.findClassKeyValue(im.getClassKeys(), os.getObjectById(id));
                        classKeySet.add(classKey);
                    }

                    String path = ((PathConstraintBag) constraint).getPath();
                    // replace constraint in the pathquery
                    PathConstraintLookup newConstraint = new PathConstraintLookup(
                            path, classKeySet.toString().substring(1,
                                    classKeySet.toString().length() - 1), null);
                    query.replaceConstraint(constraint, newConstraint);
                }
            }

            orgSet = SequenceFeatureExportUtil.getOrganisms(query, session);
        }

        if (query instanceof TemplateQuery) {
            TemplateQuery templateQuery = (TemplateQuery) query;
            Map<PathConstraint, SwitchOffAbility>  constraintSwitchOffAbilityMap =
                                                   templateQuery.getConstraintSwitchOffAbility();
            for (Map.Entry<PathConstraint, SwitchOffAbility> entry
                : constraintSwitchOffAbilityMap.entrySet()) {
                if (entry.getValue().compareTo(SwitchOffAbility.OFF) == 0) {
                    templateQuery.removeConstraint(entry.getKey());
                }
            }
        }

        String queryXML = PathQueryBinding.marshal(query, "", model.getName(),
                                                   PathQuery.USERPROFILE_VERSION);

//        String encodedQueryXML = URLEncoder.encode(queryXML, "UTF-8");

        String tableViewURL = new URLGenerator(request).getPermanentBaseURL()
            + "/service/query/results";

        request.setAttribute("tableURL", tableViewURL);
        request.setAttribute("queryXML", queryXML);
        request.setAttribute("size", 1000000);

        // If can export as BED
        request.setAttribute("canExportAsBED", canExportAsBED);
        if (canExportAsBED) {
            String bedURL = new URLGenerator(request).getPermanentBaseURL()
                + "/service/query/results/bed";

            request.setAttribute("bedURL", bedURL);

            genomeBuildSet = (Set<String>) OrganismGenomeBuildLookup
            .getGenomeBuildByOrgansimCollection(orgSet);

            String org = (orgSet.size() < 1)
                    ? "Organism information not available"
                    : StringUtil.join(orgSet, ",");

            // possible scenario: [null, ce3, null], should remove all null element and then join
            genomeBuildSet.removeAll(Collections.singleton(null));
            String dbkey = (genomeBuildSet.size() < 1)
                    ? "Genome Build information not available"
                    : StringUtil.join(genomeBuildSet, ",");

            request.setAttribute("org", org);
            request.setAttribute("dbkey", dbkey);
        }

        // PathMap
        Map<String, String> pathsMap = new LinkedHashMap<String, String>();
        List<String> views = query.getView();
        for (String view : views) {
            String title = query.getGeneratedPathDescription(view);
            title = WebUtil.formatColumnName(title);
            pathsMap.put(view, title);
        }

        request.setAttribute("pathsMap", pathsMap);

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
