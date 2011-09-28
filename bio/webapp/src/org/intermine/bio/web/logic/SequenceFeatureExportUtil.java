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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.intermine.model.bio.SequenceFeature;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.util.StringUtil;
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
        + " for SequenceFeature export.";

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
                // Sequence not supported by BioSequence when exporting
//                || Sequence.class.isAssignableFrom(prefixClass)
                ) {
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

        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);
        WebResultsExecutor webResultsExecutor = im.getWebResultsExecutor(profile);
        Model model = pathQuery.getModel();

        try {
            Entry<String, Class<?>> prefixEntry = getPathPrefix(pathQuery);

            String summaryPath = prefixEntry.getKey() + ".organism.shortName";

            // BioEntity has organism as reference, SequenceFeature is subclass of BioEntity
            Set<String> superClassNames = model.getClassDescriptorByName(
                    prefixEntry.getValue().getName()).getAllSuperclassNames();

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

    /**
     * @param path the path to parse
     * @return a path with out any [] constraints
     */
    private static String pathWithNoConstraints(String path) {
        StringBuffer sb = new StringBuffer(path.length());
        String[] queryBits = path.split("\\.");
        for (int i = 0; i < queryBits.length; i++) {
            String refName = queryBits[i];
            if (refName.indexOf('[') > 0) {
                refName = refName.substring(0, refName.indexOf('['));
            }
            if (sb.length() > 0) {
                sb.append(".");
            }
            sb.append(refName);
        }
        return sb.toString();
    }

    private static Entry<String, Class<?>> getPathPrefix(PathQuery pathQuery)
        throws PathException, ClassNotFoundException {

        Model model = pathQuery.getModel();
        String rootClsName = pathQuery.getRootClass();
        ClassDescriptor rootCld = model.getClassDescriptorByName(rootClsName);
        String rooClsFullName = rootCld.getName();
        Class<?> rootCls = Class.forName(rooClsFullName);

        if (SequenceFeature.class.isAssignableFrom(rootCls)
                || Protein.class.isAssignableFrom(rootCls)) {
            return new AbstractMap.SimpleEntry<String, Class<?>>(rootClsName, rootCls);
        } else {
            List<String> views = pathQuery.getView();
            Map<String, Class<?>> pathClsMap = new HashMap<String, Class<?>>();
            for (String view : views) {
                view = pathWithNoConstraints(view);
                String[] queryBits = view.split("\\.");
                for (int i = 1; i < queryBits.length; i++) {
                    // e.g. Pathway.genes.name > genes.name
                    if (rootCld.getCollectionDescriptorByName(queryBits[i]) != null
                            || rootCld.getReferenceDescriptorByName(queryBits[i]) != null) {
                        String eleClsName = rootCld
                                .getCollectionDescriptorByName(queryBits[i]) != null ? rootCld
                                .getCollectionDescriptorByName(queryBits[i])
                                    .getReferencedClassName() : rootCld
                                    .getReferenceDescriptorByName(queryBits[i])
                                        .getReferencedClassName();
                        Class<?> eleCls = Class.forName(eleClsName);

                        String trace = StringUtil.join(Arrays.asList(Arrays
                                .copyOfRange(queryBits, 0, i + 1)), ".");

                        pathClsMap.put(trace, eleCls);
                    }
                }
            }

            for (Entry<String, Class<?>> e: pathClsMap.entrySet()) {
                if (SequenceFeature.class.isAssignableFrom(e.getValue())
                        || Protein.class.isAssignableFrom(e.getValue())) {
                    return e;
                }
            }
        }

        return null;
    }

    private static void processException(Exception e) {
        LOG.error(ERROR_MSG, e);
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        throw new RuntimeException(e);
    }
}
