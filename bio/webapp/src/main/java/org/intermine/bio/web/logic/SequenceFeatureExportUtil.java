package org.intermine.bio.web.logic;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import org.intermine.metadata.ClassDescriptor;
import org.intermine.model.FastPathObject;
import org.intermine.model.bio.Protein;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
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

    private SequenceFeatureExportUtil() {
        super();
    }

    /**
     * From the view elements of a PathQuery, return a List of the Paths that this exporter will
     * use to find sequences to export.  The returned Paths are a subset of the prefixes of the
     * column paths.
     * eg. if the columns are ("Gene.primaryIdentifier", "Gene.secondaryIdentifier",
     * "Gene.proteins.primaryIdentifier") return ("Gene", "Gene.proteins").
     * @param pathQuery the query to look for paths in
     * @return a list of Paths that have sequence
     */
    public static List<Path> getExportClassPaths(PathQuery pathQuery) {
        List<Path> retPaths = new ArrayList<Path>();

        for (String view : pathQuery.getView()) {
            try {
                Path path = pathQuery.makePath(view);

                // path will be an attribute, prefixPath refers to parent class
                Path prefixPath = path.getPrefix();
                ClassDescriptor prefixCld = path.getLastClassDescriptor();
                Class<? extends FastPathObject> prefixClass =
                    DynamicUtil.getSimpleClass(prefixCld.getType());
                if (Protein.class.isAssignableFrom(prefixClass)
                        || SequenceFeature.class.isAssignableFrom(prefixClass)) {
                    if (!retPaths.contains(prefixPath)) {
                        retPaths.add(prefixPath);
                    }
                }
            } catch (PathException e) {
                // this should never happen with a valid query
                LOG.info("PathQuery contained an invalid path when attempting to export: " + view);
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
     */
    public static Set<String> getOrganisms(PathQuery pathQuery, HttpSession session) {

        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);
        return getOrganisms(pathQuery, im, profile);
    }


    /**
     * @param pathQuery query
     * @param im API
     * @param profile userprofile
     * @return set of organism short names
     */
    public static Set<String> getOrganisms(PathQuery pathQuery, InterMineAPI im, Profile profile) {
        WebResultsExecutor webResultsExecutor = im.getWebResultsExecutor(profile);

        Set<String> organismShortNames = new HashSet<String>();

        for (Path exportPath : getExportClassPaths(pathQuery)) {
            String summaryPath = exportPath.toStringNoConstraints() + ".organism.shortName";

            // make sure the organism name is being selected
            PathQuery cloneQuery = pathQuery.clone();
            cloneQuery.addView(summaryPath);

            try {
                List<ResultsRow> results = (List) webResultsExecutor.summariseQuery(cloneQuery,
                        summaryPath);

                for (ResultsRow row : results) {
                    organismShortNames.add((String) row.get(0));
                }
            } catch (ObjectStoreException e) {
                LOG.error("Failed to summarise path: " + summaryPath + " when retrieving organism"
                        + " short name for query: " + cloneQuery);
            }
        }
        return organismShortNames;
    }

    /**
     *
     * @author Fengyuan
     */
    public static class InvalidQueryException extends RuntimeException
    {
        /**
         * @param msg error message
         */
        InvalidQueryException(String msg) {
            super(msg);
        }

        /**
         * @param t throwable
         */
        InvalidQueryException(Throwable t) {
            super("Invalid query", t);
        }
    }

    /**
     * @param pq pathquery
     */
    public static void isValidFastaQuery(PathQuery pq) {
        if (pq.getView().size() > 1) {
            throw new InvalidQueryException("Queries to this service may only have one view.");
        }
        Path path;
        try {
            path = pq.makePath(pq.getView().get(0));
        } catch (PathException e) {
            throw new InvalidQueryException(e);
        }
        ClassDescriptor klazz = path.getLastClassDescriptor();
        ClassDescriptor sf = pq.getModel().getClassDescriptorByName("SequenceFeature");
        ClassDescriptor protein = pq.getModel().getClassDescriptorByName("Protein");
        if (sf == klazz || protein == klazz || klazz.getAllSuperDescriptors().contains(sf)
                || klazz.getAllSuperDescriptors().contains(protein)) {
            return; // OK
        } else {
            throw new InvalidQueryException("Unsuitable type for export: " + klazz);
        }
    }

    /**
     * true if valid feature
     * @param pq pathquery
     */

    public static void isValidSequenceFeatureQuery(PathQuery pq) {
        ClassDescriptor sf = pq.getModel().getClassDescriptorByName("SequenceFeature");
        for (String view: pq.getView()) {
            Path path;
            try {
                path = pq.makePath(view);
            } catch (PathException e) {
                throw new InvalidQueryException(e);
            }
            ClassDescriptor klazz = path.getLastClassDescriptor();
            if (sf == klazz || klazz.getAllSuperDescriptors().contains(sf)) {
                return; // OK
            } else {
                throw new InvalidQueryException("Unsuitable type for export: " + klazz);
            }
        }
    }

    /**
     * Get organism taxon ids info from PathQuery
     *
     * @param pathQuery PathQuery
     * @param session http session
     * @return set of organism taxon ids
     */
    public static Set<String> getTaxonIds(PathQuery pathQuery, HttpSession session) {
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);
        return getTaxonIds(pathQuery, im, profile);
    }

    /**
     * @param pathQuery query
     * @param im API
     * @param profile userprofile
     * @return set of taxon IDs
     */
    public static Set<String> getTaxonIds(PathQuery pathQuery, InterMineAPI im, Profile profile) {
        WebResultsExecutor webResultsExecutor = im.getWebResultsExecutor(profile);

        Set<String> organismTaxonIds = new HashSet<String>();

        for (Path exportPath : getExportClassPaths(pathQuery)) {
            String summaryPath = exportPath.toStringNoConstraints() + ".organism.taxonId";

            // make sure the organism name is being selected
            PathQuery cloneQuery = pathQuery.clone();
            cloneQuery.addView(summaryPath);

            try {
                List<ResultsRow> results = (List) webResultsExecutor.summariseQuery(cloneQuery,
                        summaryPath);

                for (ResultsRow row : results) {
                    organismTaxonIds.add(String.valueOf(row.get(0)));
                }
            } catch (ObjectStoreException e) {
                LOG.error("Failed to summarise path: " + summaryPath + " when retrieving organism"
                        + " taxon id for query: " + cloneQuery);
            }
        }
        return organismTaxonIds;
    }
}
