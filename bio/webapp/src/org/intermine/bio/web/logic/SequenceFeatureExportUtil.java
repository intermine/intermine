package org.intermine.bio.web.logic;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.Column;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.FastPathObject;
import org.intermine.model.bio.Protein;
import org.intermine.model.bio.Sequence;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Utility methods for LocatedSequenceFeature exporting.
 *
 * @author Fengyuan Hu
 *
 */
public final class SequenceFeatureExportUtil
{
    private static final Logger LOG = Logger.getLogger(SequenceFeatureExportUtil.class);

    private static final Object ERROR_MSG = "Error happened during fetching organism information"
        + " for LocatedSequenceFeature exporting.";

    private SequenceFeatureExportUtil() {
        super();
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
    public static List<Path> getExportClassPaths(PagedTable pt) {
        List<Path> retPaths = new ArrayList<Path>();

        List<Column> columns = pt.getColumns();

        for (Column column: columns) {
            Path prefix = column.getPath().getPrefix();
            ClassDescriptor prefixCD = prefix.getLastClassDescriptor();
            Class<? extends FastPathObject> prefixClass = DynamicUtil.getSimpleClass(prefixCD
                    .getType());
            if (Protein.class.isAssignableFrom(prefixClass)
                || SequenceFeature.class.isAssignableFrom(prefixClass)
                || Sequence.class.isAssignableFrom(prefixClass)) {
                if (!retPaths.contains(prefix)) {
                    retPaths.add(prefix);
                }
            }
        }

        return retPaths;
    }

    /**
     * Get organism info from PathQuery
     *
     * @param pathQuery PathQuery
     * @param session http session
     * @return set of organisms
     * @throws Exception Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Set<String> getOrganisms(PathQuery pathQuery, HttpSession session)
        throws Exception {

        try {
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            Profile profile = SessionMethods.getProfile(session);
            WebResultsExecutor webResultsExecutor = im.getWebResultsExecutor(profile);

            String summaryPath = pathQuery.getRootClass() + ".organism.shortName";

            // BioEntity has organism as reference, SequenceFeature is subclass of BioEntity
            Model model = pathQuery.getModel();
            Set<String> superClassNames = model.getClassDescriptorByName(
                    pathQuery.getRootClass()).getAllSuperclassNames();

            int i = 0;
            for (String n : superClassNames) {
                i++;
                if (model.getClassDescriptorByName(n)
                        .getReferenceDescriptorByName("organism") != null) {
                    pathQuery.addView(summaryPath);
                    break;
                }
            }

            if (i == superClassNames.size()) {
                throw new Exception("Sequence feature type "
                        + pathQuery.getRootClass()
                        + " does not have organism as reference.");
            }

            List<ResultsRow> results = (List) webResultsExecutor.summariseQuery(pathQuery,
                    summaryPath);

            Set<String> orgSet = new HashSet<String>();

            for (ResultsRow row : results) {
                orgSet.add((String) row.get(0));
            }

            pathQuery.removeView(summaryPath);
            return orgSet;

        } catch (RuntimeException e) {
            processException(e);
            return null;
        }
    }

    private static void processException(Exception e) {
        LOG.error(ERROR_MSG, e);
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        throw new RuntimeException(e);
    }
}
