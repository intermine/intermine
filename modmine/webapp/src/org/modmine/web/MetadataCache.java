package org.modmine.web;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.intermine.model.bio.DatabaseRecord;
import org.intermine.model.bio.Experiment;
import org.intermine.model.bio.ExpressionLevel;
import org.intermine.model.bio.LocatedSequenceFeature;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Project;
import org.intermine.model.bio.ResultFile;
import org.intermine.model.bio.Submission;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.TypeUtil;
import org.modmine.web.GBrowseParser.GBrowseTrack;


/**
 * Read modENCODE metadata into objects that simplify display code, cache results.
 * @author Richard Smith
 *
 */
public class MetadataCache
{
    private static final Logger LOG = Logger.getLogger(MetadataCache.class);

    private static final String NO_FEAT_DESCR_LOG =
        "Unable to find /WEB-INF/featureTypeDescr.properties, no feature descriptions in webapp!";

    private static Map<String, DisplayExperiment> experimentCache = null;
    private static Map<Integer, Map<String, Long>> submissionFeatureCounts = null;
    private static Map<Integer, Map<String, Long>> submissionFeatureExpressionLevelCounts = null;
    private static Map<Integer, Integer> submissionExpressionLevelCounts = null;
    private static Map<Integer, Integer> submissionIdCache = null;
    private static Map<Integer, List<GBrowseTrack>> submissionTracksCache = null;

    private static Map<Integer, Set<ResultFile>> submissionFilesCache = null;
    private static Map<Integer, Integer> filesPerSubmissionCache = null;
    private static Map<Integer, List<String>> submissionLocatedFeatureTypes = null;
    private static Map<Integer, List<String>> submissionUnlocatedFeatureTypes = null;
    private static Map<Integer, List<String[]>> submissionRepositedCache = null;
    private static Map<String, String> featDescriptionCache = null;

    private static long lastTrackCacheRefresh = 0;
    private static final long TWO_HOUR = 7200000;

    /**
     * Fetch experiment details for display.
     * @param os the production objectStore
     * @return a list of experiments
     */
    public static synchronized List<DisplayExperiment> getExperiments(ObjectStore os) {
        if (experimentCache == null) {
            readExperiments(os);
        }
        return new ArrayList<DisplayExperiment>(experimentCache.values());
    }

    /**
     * Fetch GBrowse tracks per submission fpr display.  This updates automatically from the GBrowse
     * server and refreshes periodically (according to threshold).  When refreshing another process
     * is spawned which will update tracks when finished, if GBrowse can't be accessed the current
     * list of tracks of tracks are preserved.
     * @return map from submission id to list of GBrowse tracks
     */
    public static synchronized Map<Integer, List<GBrowseTrack>> getGBrowseTracks() {
        fetchGBrowseTracks();
        while (submissionTracksCache == null) {
            try {
                MetadataCache.class.wait();
            } catch (InterruptedException e) {
            }
        }
        return submissionTracksCache;
    }


    /**
     * Fetch unlocated feature types per submission.
     * @param os the production objectStore
     * @return map of unlocated feature types
     */
    public static synchronized Map<Integer, List<String>> getLocatedFeatureTypes(ObjectStore os) {
        if (submissionLocatedFeatureTypes == null) {
            readSubmissionLocatedFeature(os);
        }
        return submissionLocatedFeatureTypes;
    }

    /**
     * Fetch unlocated feature types per submission.
     * @param os the production objectStore
     * @return map of unlocated feature types
     */
    public static synchronized Map<Integer, List<String>> getUnlocatedFeatureTypes(ObjectStore os) {
        if (submissionUnlocatedFeatureTypes == null) {
            readUnlocatedFeatureTypes(os);
        }
        return submissionUnlocatedFeatureTypes;
    }

    /**
     * Fetch unlocated feature types per submission.
     * @param os the production objectStore
     * @param dccId the dccId
     * @return map of unlocated feature types
     */
    public static synchronized
    Set<String> getUnlocatedFeatureTypesBySubId(ObjectStore os, Integer dccId) {
        if (submissionUnlocatedFeatureTypes == null) {
            readUnlocatedFeatureTypes(os);
        }
        Set<String> uf = new HashSet<String>(submissionUnlocatedFeatureTypes.get(dccId));
        return uf;
    }

    /**
     * Fetch the collection of ResultFiles per submission.
     * @param os the production objectStore
     * @return map
     */
    public static synchronized Map<Integer, Set<ResultFile>> getSubmissionFiles(ObjectStore os) {
        if (submissionFilesCache == null) {
            readSubmissionCollections(os);
        }
        return submissionFilesCache;
    }

    /**
     * Fetch the collection of Expression Level Counts per submission.
     * @param os the production objectStore
     * @return map
     */
    public static synchronized Map<Integer, Integer>
    getSubmissionExpressionLevelCounts(ObjectStore os) {
        if (submissionExpressionLevelCounts == null) {
            readSubmissionCollections(os);
        }
        return submissionExpressionLevelCounts;
    }

    /**
     * Fetch the collection of Expression Level Counts per submission.
     * @param os the production objectStore
     * @return map
     */
    public static synchronized Map<Integer, Map<String,Long>>
    getSubmissionFeatureExpressionLevelCounts(ObjectStore os) {

        if (submissionFeatureExpressionLevelCounts == null) {
            readSubmissionFeatureExpressionLevelCounts(os);
        }
        return submissionFeatureExpressionLevelCounts;
    }

    
    /**
     * Fetch number of input/output file per submission.
     * @param os the production objectStore
     * @return map
     */
    public static synchronized Map<Integer, Integer> getFilesPerSubmission(ObjectStore os) {
        if (submissionFilesCache == null) {
            readSubmissionCollections(os);
        }
        filesPerSubmissionCache = new HashMap<Integer, Integer>();

        Iterator<Integer> dccId = submissionFilesCache.keySet().iterator();
        while (dccId.hasNext()) {
            Integer thisSub = dccId.next();
            Integer nrFiles = submissionFilesCache.get(thisSub).size();
            filesPerSubmissionCache.put(thisSub, nrFiles);
        }
        return filesPerSubmissionCache;
    }


    /**
     * Fetch a list of file names for a given submission.
     * @param os the objectStore
     * @param dccId the modENCODE submission id
     * @return a list of file names
     */
    public static synchronized List<ResultFile> getFilesByDccId(ObjectStore os,
            Integer dccId) {
        if (submissionFilesCache == null) {
            readSubmissionCollections(os);
        }
        return new ArrayList<ResultFile>(submissionFilesCache.get(dccId));
    }

    /**
     * Fetch a list of GBrowse tracks for a given submission.
     * @param dccId the modENCODE submission id
     * @return a list of file names
     */
    public static synchronized List<GBrowseTrack> getTracksByDccId(Integer dccId) {
        Map<Integer, List<GBrowseTrack>> tracks = getGBrowseTracks();
        if (tracks.get(dccId) != null) {
            return new ArrayList<GBrowseTrack>(tracks.get(dccId));
        } else {
            return new ArrayList<GBrowseTrack>();
        }
    }

    /**
     * Fetch a list of file names for a given submission.
     * @param servletContext the context
     * @return a list of file names
     */
    public static synchronized
    Map<String, String> getFeatTypeDescription(ServletContext servletContext) {
        if (featDescriptionCache == null) {
            readFeatTypeDescription(servletContext);
        }
        return featDescriptionCache;
    }


    /**
     * Fetch a map from feature type to count for a given submission.
     * @param os the objectStore
     * @param dccId the modENCODE submission id
     * @return a map from feature type to count
     */
    public static synchronized Map<String, Long> getSubmissionFeatureCounts(ObjectStore os,
            Integer dccId) {
        if (submissionFeatureCounts == null) {
            readSubmissionFeatureCounts(os);
        }
        return submissionFeatureCounts.get(dccId);
    }

    /**
     * Fetch the number of expression levels for a given submission.
     * @param os the objectStore
     * @param dccId the modENCODE submission id
     * @return a map from submission to count
     */
    public static synchronized Integer getSubmissionExpressionLevelCount(ObjectStore os,
            Integer dccId) {
        if (submissionExpressionLevelCounts == null) {
            getSubmissionExpressionLevelCounts(os);
        }
        return submissionExpressionLevelCounts.get(dccId);
    }

    /**
     * Fetch a submission by the modENCODE submission ids
     * @param os the objectStore
     * @param dccId the modENCODE submission id
     * @return the requested submission
     * @throws ObjectStoreException if error reading database
     */
    public static synchronized Submission getSubmissionByDccId(ObjectStore os, Integer dccId)
        throws ObjectStoreException {
        if (submissionIdCache == null) {
            readSubmissionFeatureCounts(os);
        }
        return (Submission) os.getObjectById(submissionIdCache.get(dccId));
    }

    /**
     * Get experiment information by name
     * @param os the objectStore
     * @param name of the experiment to fetch
     * @return details of the experiment
     * @throws ObjectStoreException if error reading database
     */
    public static synchronized DisplayExperiment getExperimentByName(ObjectStore os, String name)
        throws ObjectStoreException {
        if (experimentCache == null) {
            readExperiments(os);
        }
        return experimentCache.get(name);
    }


    //======================

    private static void fetchGBrowseTracks() {
        long timeSinceLastRefresh = System.currentTimeMillis() - lastTrackCacheRefresh;
        if (timeSinceLastRefresh > TWO_HOUR) {
            readGBrowseTracks();
            lastTrackCacheRefresh = System.currentTimeMillis();
        }
    }
    /**
     * Set the map of GBrowse tracks.
     *
     * @param tracks map of dccId:GBrowse tracks
     */
    public static synchronized void setGBrowseTracks(Map<Integer, List<GBrowseTrack>> tracks) {
        MetadataCache.class.notifyAll();
        submissionTracksCache = tracks;
    }

    /**
     * Method to obtain the map of unlocated feature types by submission id
     *
     * @param os the objectStore
     * @return submissionUnlocatedFeatureTypes
     */
    private static Map<Integer, List<String>> readUnlocatedFeatureTypes(ObjectStore os) {
        long startTime = System.currentTimeMillis();
        try {

            if (submissionUnlocatedFeatureTypes != null) {
                return submissionUnlocatedFeatureTypes;
            }

            submissionUnlocatedFeatureTypes = new HashMap<Integer, List<String>>();

            if (submissionLocatedFeatureTypes == null) {
                readSubmissionLocatedFeature(os);
            }

            if (submissionFeatureCounts == null) {
                readSubmissionFeatureCounts(os);
            }


            for (Integer subId : submissionFeatureCounts.keySet()) {

                Set<String> allFeatures = submissionFeatureCounts.get(subId).keySet();
                Set<String> difference = new HashSet<String>(allFeatures);
                if (submissionLocatedFeatureTypes.get(subId) != null) {
                    difference.removeAll(submissionLocatedFeatureTypes.get(subId));
                }

                if (!difference.isEmpty()) {
                    List <String> thisUnlocated = new ArrayList<String>();

                    for (String fType : difference) {
                        thisUnlocated.add(fType);
                    }
                    submissionUnlocatedFeatureTypes.put(subId, thisUnlocated);
                }
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Primed unlocated feature cache, took: " + timeTaken + "ms size = "
                + submissionUnlocatedFeatureTypes.size());
        return submissionUnlocatedFeatureTypes;
    }

    /**
     *
     * @param os objectStore
     * @return map exp-tracks
     */
    public static Map<String, List<GBrowseTrack>> getExperimentGBrowseTracks(ObjectStore os) {
        Map<String, List<GBrowseTrack>> tracks = new HashMap<String, List<GBrowseTrack>>();

        Map<Integer, List<GBrowseTrack>> subTracksMap = getGBrowseTracks();

        for (DisplayExperiment exp : getExperiments(os)) {
            List<GBrowseTrack> expTracks = new ArrayList<GBrowseTrack>();
            tracks.put(exp.getName(), expTracks);
            for (Submission sub : exp.getSubmissions()) {

                if (subTracksMap.get(sub.getdCCid()) != null){

                    List<GBrowseTrack> subTracks = subTracksMap.get(sub.getdCCid());
                    if (subTracks != null) {
                        // check so it is unique
                        // expTracks.addAll(subTracks);
                        addToList(expTracks, subTracks);
                    } else {
                        continue;
                    }
                }
            }
        }
        return tracks;
    }


    /**
     * adds the elements of a list i to a list l only if they are not yet
     * there
     * @param l the receiving list
     * @param i the donating list
     */
    private static void addToList(List<GBrowseTrack> l, List<GBrowseTrack> i) {
        Iterator <GBrowseTrack> it  = i.iterator();
        while (it.hasNext()) {
            GBrowseTrack thisId = it.next();
            if (!l.contains(thisId)) {
                l.add(thisId);
            }
        }
    }

    /**
     *
     * @param os objectStore
     * @return map exp-repository entries
     */
    public static Map<String, Set<String[]>> getExperimentRepositoryEntries(ObjectStore os) {
        Map<String, Set<String[]>> reposited = new HashMap<String, Set<String[]>>();

        Map<Integer, List<String[]>> subRepositedMap = getRepositoryEntries(os);

        for (DisplayExperiment exp : getExperiments(os)) {
            Set<String[]> expReps = new HashSet<String[]>();
            for (Submission sub : exp.getSubmissions()) {
                List<String[]> subReps = subRepositedMap.get(sub.getdCCid());
                if (subReps != null) {
                    expReps.addAll(subReps);
                }
            }
            // for each experiment, we don't to count twice the same repository
            // entry produced by 2 different submissions.
            Set<String[]> expRepsCleaned = removeDuplications(expReps);
            reposited.put(exp.getName(), expRepsCleaned);
        }
        return reposited;
    }

    private static Set<String[]> removeDuplications(Set<String[]> expReps) {
        // removing the same repository entry coming from different submissions
        // in the given experiment
        Set<String> db = new HashSet<String>();
        Set<String> acc = new HashSet<String>();
        Set<String[]> dup = new HashSet<String[]>();
        for (String[] s : expReps) {
            if (db.contains(s[0]) && acc.contains(s[1])) {
                // we don't remove place holders
                if (!s[1].startsWith("To be")) {
                    dup.add(s);
                }
            }
            db.add(s[0]);
            acc.add(s[1]);
        }
        // do the difference between sets and return it
        Set<String[]> uniques = new HashSet<String[]>(expReps);
        uniques.removeAll(dup);
        return uniques;
    }


    /**
    *
    * @param os objectStore
    * @return map exp-repository entries
    */
    public static Map<String, Integer> getExperimentExpressionLevels(ObjectStore os) {
        Map<String, Integer> experimentELevel = new HashMap<String, Integer>();

        Map<Integer, Integer> subELevelMap = getSubmissionExpressionLevelCounts(os);

        for (DisplayExperiment exp : getExperiments(os)) {
            Integer expCount = 0;
            for (Submission sub : exp.getSubmissions()) {
                Integer subCount = subELevelMap.get(sub.getdCCid());
                if (subCount != null) {
                    expCount = expCount + subCount;
                }
            }
            //           if (expCount > 0) {
            experimentELevel.put(exp.getName(), expCount);
            //           }
        }
        return experimentELevel;
    }

    /**
    *
    * @param os objectStore
    * @return map exp-repository entries
    */
    public static Map<String, Map<String, Long>> 
    getExperimentFeatureExpressionLevels(ObjectStore os) {
        Map<String, Map<String, Long>> expELevels = new HashMap<String,Map<String, Long>>();

        Map<Integer, Map<String, Long>> subELevels = getSubmissionFeatureExpressionLevelCounts(os);

        for (DisplayExperiment exp : getExperiments(os)) {
//            Integer expCount = 0;
            for (Submission sub : exp.getSubmissions()) {
                Map <String, Long> subFeat= subELevels.get(sub.getdCCid());
//                Integer subCount = subELevelMap.get(sub.getdCCid());
                if (subFeat != null) {
                    Map<String, Long> expFeat =
//                        new HashMap<String, Long>();
//                    expFeat = 
                        expELevels.get(exp.getName());
                    if (expFeat == null) {
//                        if (expFeat.isEmpty()) {
                        expELevels.put(exp.getName(), subFeat);
                    } else {
//                        if (expELevels.get(exp.getName()).isEmpty()) {
//                            expELevels.put(exp.getName(), subFeat);
//                        } else {
                        for (String feat : expFeat.keySet()) {
                            Long subCount = subFeat.get(feat);
                            Long expCount = expFeat.get(feat) + subCount;
                            expFeat.put(feat, expCount);
                        }
                        expELevels.put(exp.getName(),expFeat);
                    }
                }
            }
        }
        return expELevels;
    }

    
    /**
     * Fetch a map from project name to experiment.
     * @param os the production ObjectStore
     * @return a map from project name to experiment
     */
    public static Map<String, List<DisplayExperiment>>
    getProjectExperiments(ObjectStore os) {
        long startTime = System.currentTimeMillis();
        Map<String, List<DisplayExperiment>> projectExperiments
            = new TreeMap<String, List<DisplayExperiment>>();
        for (DisplayExperiment exp : getExperiments(os)) {
            List<DisplayExperiment> exps = projectExperiments.get(exp.getProjectName());
            if (exps == null) {
                exps = new ArrayList<DisplayExperiment>();
                projectExperiments.put(exp.getProjectName(), exps);
            }
            exps.add(exp);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        LOG.info("Made project map: " + projectExperiments.size()
                + " took: " + totalTime + " ms.");
        return projectExperiments;
    }


    private static void readExperiments(ObjectStore os) {
        long startTime = System.currentTimeMillis();
        Map <String, Map<String, Long>> featureCounts = getExperimentFeatureCounts(os);

        try {
            Query q = new Query();
            QueryClass qcProject = new QueryClass(Project.class);
            QueryField qcName = new QueryField(qcProject, "name");

            q.addFrom(qcProject);
            q.addToSelect(qcProject);

            QueryClass qcExperiment = new QueryClass(Experiment.class);
            q.addFrom(qcExperiment);
            q.addToSelect(qcExperiment);

            QueryCollectionReference projExperiments = new QueryCollectionReference(qcProject,
                "experiments");
            ContainsConstraint cc = new ContainsConstraint(projExperiments, ConstraintOp.CONTAINS,
                    qcExperiment);

            q.setConstraint(cc);
            q.addToOrderBy(qcName);

            Results results = os.execute(q);

            experimentCache = new HashMap<String, DisplayExperiment>();

            Iterator i = results.iterator();
            while (i.hasNext()) {
                ResultsRow row = (ResultsRow) i.next();

                Project project = (Project) row.get(0);
                Experiment experiment = (Experiment) row.get(1);

                Map<String, Long> expFeatureCounts = featureCounts.get(experiment.getName());
                DisplayExperiment displayExp = new DisplayExperiment(experiment, project,
                        expFeatureCounts, os);
                experimentCache.put(displayExp.getName(), displayExp);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Primed experiment cache, took: " + timeTaken + "ms size = "
                + experimentCache.size());
    }

    private static Map<String, Map<String, Long>> getExperimentFeatureCounts(ObjectStore os) {
        long startTime = System.currentTimeMillis();

        // NB: example of query (with group by) enwrapping a subquery that gets rids of
        // duplications

        Query q = new Query();

        QueryClass qcExp = new QueryClass(Experiment.class);
        QueryClass qcSub = new QueryClass(Submission.class);
        QueryClass qcLsf = new QueryClass(LocatedSequenceFeature.class);

        QueryField qfName = new QueryField(qcExp, "name");
        QueryField qfClass = new QueryField(qcLsf, "class");

        q.addFrom(qcSub);
        q.addFrom(qcLsf);
        q.addFrom(qcExp);

        q.addToSelect(qcExp);
        q.addToSelect(qcLsf);
        q.addToSelect(qfName);
        q.addToSelect(qfClass);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference submissions = new QueryCollectionReference(qcExp, "submissions");
        ContainsConstraint ccSubs = new ContainsConstraint(submissions, ConstraintOp.CONTAINS,
                qcSub);
        cs.addConstraint(ccSubs);

        QueryCollectionReference features = new QueryCollectionReference(qcSub, "features");
        ContainsConstraint ccFeats = new ContainsConstraint(features, ConstraintOp.CONTAINS, qcLsf);
        cs.addConstraint(ccFeats);

        q.setConstraint(cs);

        q.setDistinct(true);

        Query superQ = new Query();
        superQ.addFrom(q);
        QueryField superQfName = new QueryField(q, qfName);
        QueryField superQfClass = new QueryField(q, qfClass);

        superQ.addToSelect(superQfName);
        superQ.addToSelect(superQfClass);
        superQ.addToOrderBy(superQfName);
        superQ.addToOrderBy(superQfClass);
        superQ.addToGroupBy(superQfName);
        superQ.addToGroupBy(superQfClass);

        superQ.addToSelect(new QueryFunction());
        superQ.setDistinct(false);

        Results results = os.execute(superQ);

        Map<String, Map<String, Long>> featureCounts =
            new LinkedHashMap<String, Map<String, Long>>();

        // for each classes set the values for jsp
        for (Iterator<ResultsRow> iter = results.iterator(); iter.hasNext(); ) {
            ResultsRow row = iter.next();
            String expName = (String) row.get(0);
            Class feat = (Class) row.get(1);
            Long count = (Long) row.get(2);

            Map<String, Long> expFeatureCounts = featureCounts.get(expName);
            if (expFeatureCounts == null) {
                expFeatureCounts = new HashMap<String, Long>();
                featureCounts.put(expName, expFeatureCounts);
            }
            expFeatureCounts.put(TypeUtil.unqualifiedName(feat.getName()), count);
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Read experiment feature counts, took: " + timeTaken + "ms");

        return featureCounts;
    }

    private static void readSubmissionFeatureCounts(ObjectStore os) {
        long startTime = System.currentTimeMillis();

        submissionFeatureCounts = new LinkedHashMap<Integer, Map<String, Long>>();
        submissionIdCache = new HashMap<Integer, Integer>();

        Query q = new Query();
        q.setDistinct(false);

        QueryClass qcSub = new QueryClass(Submission.class);
        QueryClass qcLsf = new QueryClass(LocatedSequenceFeature.class);

        QueryField qfClass = new QueryField(qcLsf, "class");

        q.addFrom(qcSub);
        q.addFrom(qcLsf);

        q.addToSelect(qcSub);
        q.addToSelect(qfClass);
        q.addToSelect(new QueryFunction());

        q.addToGroupBy(qcSub);
        q.addToGroupBy(qfClass);

        q.addToOrderBy(qcSub);
        q.addToOrderBy(qfClass);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference features = new QueryCollectionReference(qcSub, "features");
        ContainsConstraint ccFeats = new ContainsConstraint(features, ConstraintOp.CONTAINS, qcLsf);
        cs.addConstraint(ccFeats);

        q.setConstraint(cs);

        Results results = os.execute(q);

        // for each classes set the values for jsp
        for (Iterator<ResultsRow> iter = results.iterator(); iter.hasNext(); ) {
            ResultsRow row = iter.next();
            Submission sub = (Submission) row.get(0);
            Class feat = (Class) row.get(1);
            Long count = (Long) row.get(2);

            submissionIdCache.put(sub.getdCCid(), sub.getId());

            Map<String, Long> featureCounts = submissionFeatureCounts.get(sub.getdCCid());
            if (featureCounts == null) {
                featureCounts = new HashMap<String, Long>();
                submissionFeatureCounts.put(sub.getdCCid(), featureCounts);
            }
            featureCounts.put(TypeUtil.unqualifiedName(feat.getName()), count);
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Primed submissionFeatureCounts cache, took: " + timeTaken + "ms size = "
                + submissionFeatureCounts.size());
    }


    private static void readSubmissionFeatureExpressionLevelCounts(ObjectStore os) {
        long startTime = System.currentTimeMillis();

        submissionFeatureExpressionLevelCounts = new LinkedHashMap<Integer, Map<String, Long>>();
        //submissionIdCache = new HashMap<Integer, Integer>();

        Query q = new Query();
        q.setDistinct(false);

        QueryClass qcSub = new QueryClass(Submission.class);
        QueryClass qcLsf = new QueryClass(LocatedSequenceFeature.class);
        QueryClass qcEL = new QueryClass(ExpressionLevel.class);

        QueryField qfClass = new QueryField(qcLsf, "class");

        q.addFrom(qcSub);
        q.addFrom(qcLsf);
        q.addFrom(qcEL);

        q.addToSelect(qcSub);
        q.addToSelect(qfClass);
        q.addToSelect(new QueryFunction());

        q.addToGroupBy(qcSub);
        q.addToGroupBy(qfClass);

        q.addToOrderBy(qcSub);
        q.addToOrderBy(qfClass);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference features = new QueryCollectionReference(qcSub, "features");
        ContainsConstraint ccFeats = new ContainsConstraint(features, ConstraintOp.CONTAINS, qcLsf);
        cs.addConstraint(ccFeats);
        QueryCollectionReference el = new QueryCollectionReference(qcLsf, "expressionLevels");
        ContainsConstraint ccEl= new ContainsConstraint(el, ConstraintOp.CONTAINS, qcEL);
        cs.addConstraint(ccEl);

        q.setConstraint(cs);

        Results results = os.execute(q);

        // for each classes set the values for jsp
        for (Iterator<ResultsRow> iter = results.iterator(); iter.hasNext(); ) {
            ResultsRow row = iter.next();
            Submission sub = (Submission) row.get(0);
            Class feat = (Class) row.get(1);
            Long count = (Long) row.get(2);

            //submissionIdCache.put(sub.getdCCid(), sub.getId());

            Map<String, Long> featureCounts = submissionFeatureExpressionLevelCounts.get(sub.getdCCid());
            if (featureCounts == null) {
                featureCounts = new HashMap<String, Long>();
                submissionFeatureExpressionLevelCounts.put(sub.getdCCid(), featureCounts);
            }
            featureCounts.put(TypeUtil.unqualifiedName(feat.getName()), count);
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Primed submissionFeatureExpressionLevelCounts cache, took: " + timeTaken
                + "ms size = " + submissionFeatureExpressionLevelCounts.size());
//                + "<->" + submissionFeatureCounts.size());

        LOG.info("submissionFeatureELCounts " + submissionFeatureExpressionLevelCounts);

    }

    private static void readSubmissionCollections(ObjectStore os) {
        //
        long startTime = System.currentTimeMillis();
        try {
            Query q = new Query();
            QueryClass qcSubmission = new QueryClass(Submission.class);
            QueryField qfDCCid = new QueryField(qcSubmission, "DCCid");
            q.addFrom(qcSubmission);
            q.addToSelect(qcSubmission);

            q.addToOrderBy(qfDCCid);

            submissionFilesCache = new HashMap<Integer, Set<ResultFile>>();
            submissionExpressionLevelCounts = new HashMap<Integer, Integer>();
            Results results = os.executeSingleton(q);

            // for submission, get result files and expression level count
            Iterator i = results.iterator();
            while (i.hasNext()) {
                Submission sub = (Submission) i.next();
                Set<ResultFile> files = sub.getResultFiles();
                submissionFilesCache.put(sub.getdCCid(), files);
                Set<ExpressionLevel> el = sub.getExpressionLevels();
                submissionExpressionLevelCounts.put(sub.getdCCid(), el.size());
            }

        } catch (Exception err) {
            err.printStackTrace();
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Primed submission collections caches, took: " + timeTaken + "ms    size: files = "
                + submissionFilesCache.size() + ", expression levels = "
                + submissionExpressionLevelCounts.size());
    }


    private static void readSubmissionFiles(ObjectStore os) {
        //
        long startTime = System.currentTimeMillis();
        try {
            Query q = new Query();
            QueryClass qcSubmission = new QueryClass(Submission.class);
            QueryField qfDCCid = new QueryField(qcSubmission, "DCCid");
            q.addFrom(qcSubmission);
            q.addToSelect(qcSubmission);

            q.addToOrderBy(qfDCCid);

            submissionFilesCache = new HashMap<Integer, Set<ResultFile>>();
            Results results = os.executeSingleton(q);

            // for each project, get its labs
            Iterator i = results.iterator();
            while (i.hasNext()) {
                Submission sub = (Submission) i.next();
                Set<ResultFile> files = sub.getResultFiles();
                submissionFilesCache.put(sub.getdCCid(), files);
            }

        } catch (Exception err) {
            err.printStackTrace();
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Primed file names cache, took: " + timeTaken + "ms size = "
                + submissionFilesCache.size());
    }


    private static void readSubmissionLocatedFeature(ObjectStore os) {
        long startTime = System.currentTimeMillis();
        submissionLocatedFeatureTypes = new LinkedHashMap<Integer, List<String>>();

        Query q = new Query();
        q.setDistinct(true);

        QueryClass qcSub = new QueryClass(Submission.class);
        QueryClass qcLsf = new QueryClass(LocatedSequenceFeature.class);
        QueryClass qcLoc = new QueryClass(Location.class);

        QueryField qfClass = new QueryField(qcLsf, "class");

        q.addFrom(qcSub);
        q.addFrom(qcLsf);
        q.addFrom(qcLoc);

        q.addToSelect(qcSub);
        q.addToSelect(qfClass);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference features = new QueryCollectionReference(qcSub, "features");
        ContainsConstraint ccFeats = new ContainsConstraint(features, ConstraintOp.CONTAINS, qcLsf);
        cs.addConstraint(ccFeats);

        QueryObjectReference location = new QueryObjectReference(qcLsf, "chromosomeLocation");
        ContainsConstraint ccLocs = new ContainsConstraint(location, ConstraintOp.CONTAINS, qcLoc);
        cs.addConstraint(ccLocs);

        q.setConstraint(cs);

        Results results = os.execute(q);

        // for each classes set the values for jsp
        for (Iterator<ResultsRow> iter = results.iterator(); iter.hasNext(); ) {
            ResultsRow row = iter.next();
            Submission sub = (Submission) row.get(0);
            Class feat = (Class) row.get(1);

            addToMap(submissionLocatedFeatureTypes, sub.getdCCid(),
                    TypeUtil.unqualifiedName(feat.getName()));

        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Primed located features cache, took: " + timeTaken + "ms size = "
                + submissionLocatedFeatureTypes.size());
    }


    /**
     * Fetch reposited (GEO/SRA/AE..) entries per submission.
     * @param os the production objectStore
     * @return map
     */
    public static synchronized Map<Integer, List<String[]>> getRepositoryEntries(ObjectStore os) {
        if (submissionRepositedCache == null) {
            readSubmissionRepositoryEntries(os);
        }
        return submissionRepositedCache;
    }


    private static void readSubmissionRepositoryEntries(ObjectStore os) {
        //
        long startTime = System.currentTimeMillis();
        try {
            Query q = new Query();
            QueryClass qcSubmission = new QueryClass(Submission.class);
            QueryField qfDCCid = new QueryField(qcSubmission, "DCCid");
            q.addFrom(qcSubmission);
            q.addToSelect(qfDCCid);

            QueryClass qcRepositoryEntry = new QueryClass(DatabaseRecord.class);
            QueryField qfDatabase = new QueryField(qcRepositoryEntry, "database");
            QueryField qfAccession = new QueryField(qcRepositoryEntry, "accession");
            QueryField qfUrl = new QueryField(qcRepositoryEntry, "url");
            q.addFrom(qcRepositoryEntry);
            q.addToSelect(qfDatabase);
            q.addToSelect(qfAccession);
            q.addToSelect(qfUrl);

            // join the tables
            QueryCollectionReference ref1 =
                new QueryCollectionReference(qcSubmission, "databaseRecords");
            ContainsConstraint cc = new ContainsConstraint(ref1, ConstraintOp.CONTAINS,
                    qcRepositoryEntry);

            q.setConstraint(cc);
            q.addToOrderBy(qfDCCid);
            q.addToOrderBy(qfDatabase);

            Results results = os.execute(q);

            submissionRepositedCache = new HashMap<Integer, List<String[]>>();

            Integer counter = 0;

            Integer prevSub = new Integer(-1);
            List<String[]> subRep = new ArrayList<String[]>();
            Iterator i = results.iterator();
            while (i.hasNext()) {
                ResultsRow row = (ResultsRow) i.next();

                counter++;
                Integer dccId = (Integer) row.get(0);
                String db = (String) row.get(1);
                String acc = (String) row.get(2);
                String url = (String) row.get(3);
                String[] thisRecord = {db, acc, url};

                if (!dccId.equals(prevSub) || counter.equals(results.size())) {
                    if (prevSub > 0) {
                        if (counter.equals(results.size())) {
                            prevSub = dccId;
                            subRep.add(thisRecord);
                        }
                        List<String[]> subRepIn = new ArrayList<String[]>();
                        subRepIn.addAll(subRep);
                        submissionRepositedCache.put(prevSub, subRepIn);
                        subRep.clear();
                    }
                    prevSub = dccId;
                }
                subRep.add(thisRecord);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Primed Repository entries cache, took: " + timeTaken + "ms size = "
                + submissionRepositedCache.size());
    }


    /**
     * adds an element to a list which is the value of a map
     * @param m       the map (<String, List<String>>)
     * @param key     the key for the map
     * @param value   the list
     */
    private static void addToMap(Map<Integer, List<String>> m, Integer key, String value) {

        List<String> ids = new ArrayList<String>();

        if (m.containsKey(key)) {
            ids = m.get(key);
        }
        if (!ids.contains(value)) {
            ids.add(value);
            m.put(key, ids);
        }
    }

    /**
     * Method to fill the cached map of submissions (ddcId) to list of
     * GBrowse tracks
     *
     */
    private static void readGBrowseTracks() {
        Runnable r = new Runnable() {
            public void run() {
                threadedReadGBrowseTracks();
            }
        };
        Thread t = new Thread(r);
        t.start();
    }

    private static void threadedReadGBrowseTracks() {
        long startTime = System.currentTimeMillis();

        Map<Integer, List<GBrowseTrack>> tracks = new HashMap<Integer, List<GBrowseTrack>>();
        Map<Integer, List<GBrowseTrack>> flyTracks = null;
        Map<Integer, List<GBrowseTrack>> wormTracks = null;
        try {
            flyTracks = GBrowseParser.readTracks("fly");
            wormTracks = GBrowseParser.readTracks("worm");
        } catch (Exception e) {
            LOG.error(e);
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Primed GBrowse tracks cache, took: " + timeTaken + "ms  size = "
                + tracks.size());

        if (flyTracks != null && wormTracks != null) {
            tracks.putAll(flyTracks);
            tracks.putAll(wormTracks);
            setGBrowseTracks(tracks);
        }
    }



    /**
     * This method get the feature descriptions from a property file.
     *
     * @return the map feature/description
     */
    private static Map<String, String> readFeatTypeDescription(ServletContext servletContext) {
        long startTime = System.currentTimeMillis();

        featDescriptionCache = new HashMap<String, String>();

        Properties props = new Properties();

        InputStream is = servletContext.getResourceAsStream("/WEB-INF/featureTypeDescr.properties");
        if (is == null) {
            LOG.info(NO_FEAT_DESCR_LOG);
        } else {

            try {
                props.load(is);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                //throw new IllegalAccessException("Error getting featureTypeDescr.properties file",
                // e.printStackTrace());
                e.printStackTrace();
            }

            Enumeration en = props.keys();
            //                while (props.keys().hasMoreElements()) {
            while (en.hasMoreElements()) {
                String expFeat = (String) en.nextElement();
                String descr = props.getProperty(expFeat);
                featDescriptionCache.put(expFeat, descr);
            }
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Primed feature description cache, took: " + timeTaken + "ms size = "
                + featDescriptionCache.size());
        return featDescriptionCache;
    }
}
