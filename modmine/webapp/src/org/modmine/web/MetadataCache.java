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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
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
import org.intermine.model.bio.SequenceFeature;
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
import org.intermine.util.PropertiesUtil;
import org.intermine.util.TypeUtil;


/**
 * Read modENCODE metadata into objects that simplify display code, cache results.
 * @author Richard Smith
 *
 */
public class MetadataCache
{
    private static final Logger LOG = Logger.getLogger(MetadataCache.class);

    private static final String GBROWSE_BASE_URL = getGBrowsePrefix();
    private static final String GBROWSE_ST_URL_END = "/?action=scan";



    public static class GBrowseTrack 
    {
        private String organism; // {fly,worm}
        private String track;    // e.g. Snyder_PHA4_GFP_COMB
        private String subTrack; // e.g. PHA4_L2_GFP

        public GBrowseTrack(String organism2, String trackName) {
            this.organism  = organism2;
            this.track = trackName;
        }

        public GBrowseTrack(String organism, String track, String subTrack) {
            this.organism  = organism;
            this.track = track;
            this.subTrack = subTrack;
        }

        /**
         * @return the organism
         */
        public String getOrganism() {
            return organism;
        }

        /**
         * @return the track name
         */
        public String getTrack() {
            return track;
        }

        /**
         * @return the subTrack
         */
        public String getSubTrack() {
            return subTrack;
        }
    }


    private static Map<String, DisplayExperiment> experimentCache = null;
    private static Map<Integer, Map<String, Long>> submissionFeatureCounts = null;
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
        return submissionTracksCache;
    }

    private static void fetchGBrowseTracks() {
        long timeSinceLastRefresh = System.currentTimeMillis() - lastTrackCacheRefresh;
        if (timeSinceLastRefresh > TWO_HOUR) {
            readGBrowseTracks();
            lastTrackCacheRefresh = System.currentTimeMillis();
        }
    }

    public static synchronized void setGBrowseTracks(Map<Integer, List<GBrowseTrack>> tracks) {
        submissionTracksCache = tracks;
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
     * @param dccId ID from DCC
     * @return map of unlocated feature types
     */
    public static synchronized Set<String> getUnlocatedFeatureTypesBySubId(ObjectStore os,
    Integer dccId) {
        if (submissionUnlocatedFeatureTypes == null) {
            readUnlocatedFeatureTypes(os);
        }
        Set<String> uf = new HashSet<String>(submissionUnlocatedFeatureTypes.get(dccId));
        return uf;
    }

    /**
     * Fetch input/output file names per submission.
     * @param os the production objectStore
     * @return map
     */
    public static synchronized Map<Integer, Set<ResultFile>> getSubmissionFiles(ObjectStore os) {
        if (submissionFilesCache == null) {
            readSubmissionFiles(os);
        }
        return submissionFilesCache;
    }

    /**
     * Fetch number of input/output file per submission.
     * @param os the production objectStore
     * @return map
     */
    public static synchronized Map<Integer, Integer> getFilesPerSubmission(ObjectStore os) {
        if (submissionFilesCache == null) {
            readSubmissionFiles(os);
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
            readSubmissionFiles(os);
        }
        return new ArrayList<ResultFile>(submissionFilesCache.get(dccId));
    }

    /**
     * Fetch a list of file names for a given submission.
     * @param dccId the modENCODE submission id
     * @return a list of file names
     */
    public static synchronized List<GBrowseTrack> getTracksByDccId(Integer dccId) {
        if (submissionTracksCache == null) {
            readGBrowseTracks();
        }
        if(submissionTracksCache.get(dccId) != null)
        	return new ArrayList<GBrowseTrack>(submissionTracksCache.get(dccId));
        else
        	return new ArrayList<GBrowseTrack>();
    }

    /**
     * Fetch a list of file names for a given submission.
     * @param servletContext servletContext
     * @return a list of file names
     */
    public static synchronized Map<String, String> getFeatTypeDescription(ServletContext
    servletContext) {
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


    /**
     * Method to obtain the map of unlocated feature types by submission id
     *
     * @param os the objectStore
     * @return submissionUnlocatedFeatureTypes
     */
    private static Map<Integer, List<String>> readUnlocatedFeatureTypes(ObjectStore os) {
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
        return submissionUnlocatedFeatureTypes;
    }


    public static Map<String, List<GBrowseTrack>> getExperimentGBrowseTracks(ObjectStore os) {
        Map<String, List<GBrowseTrack>> tracks = new HashMap<String, List<GBrowseTrack>>();

        Map<Integer, List<GBrowseTrack>> subTracksMap = getGBrowseTracks();

        for (DisplayExperiment exp : getExperiments(os)) {
            List<GBrowseTrack> expTracks = new ArrayList<GBrowseTrack>();
            tracks.put(exp.getName(), expTracks);
            for (Submission sub : exp.getSubmissions()) {
                List<GBrowseTrack> subTracks = subTracksMap.get(sub.getdCCid());
                if (subTracks != null) {
                    // check so it is unique
                    // expTracks.addAll(subTracks);
                    addToList(expTracks, subTracks);
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

            Iterator<ResultsRow<?>> i = results.iterator();
            while (i.hasNext()) {
                ResultsRow<?> row = (ResultsRow<?>) i.next();

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
        LOG.info("Primed experiment cache, took: " + timeTaken + "ms");
    }


    private static Map<String, Map<String, Long>> getExperimentFeatureCounts(ObjectStore os) {
        long startTime = System.currentTimeMillis();
        Query q = new Query();
        q.setDistinct(false);

        QueryClass qcExp = new QueryClass(Experiment.class);
        QueryClass qcSub = new QueryClass(Submission.class);
        QueryClass qcLsf = new QueryClass(SequenceFeature.class);

        QueryField qfName = new QueryField(qcExp, "name");
        QueryField qfClass = new QueryField(qcLsf, "class");

        q.addFrom(qcSub);
        q.addFrom(qcLsf);
        q.addFrom(qcExp);

        q.addToSelect(qfName);
        q.addToSelect(qfClass);
        q.addToSelect(new QueryFunction());

        q.addToGroupBy(qfName);
        q.addToGroupBy(qfClass);

        q.addToOrderBy(qfName);
        q.addToOrderBy(qfClass);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference submissions = new QueryCollectionReference(qcExp, "submissions");
        ContainsConstraint ccSubs = new ContainsConstraint(submissions, ConstraintOp.CONTAINS,
                qcSub);
        cs.addConstraint(ccSubs);

        QueryCollectionReference features = new QueryCollectionReference(qcSub, "features");
        ContainsConstraint ccFeats = new ContainsConstraint(features, ConstraintOp.CONTAINS, qcLsf);
        cs.addConstraint(ccFeats);

        q.setConstraint(cs);

        Results results = os.execute(q);

        Map<String, Map<String, Long>> featureCounts =
            new LinkedHashMap<String, Map<String, Long>>();

        // for each classes set the values for jsp
        for (Iterator<ResultsRow> iter = results.iterator(); iter.hasNext(); ) {
            ResultsRow row = iter.next();
            String expName = (String) row.get(0);
            Class<?> feat = (Class<?>) row.get(1);
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
        QueryClass qcLsf = new QueryClass(SequenceFeature.class);

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
        LOG.info("Primed submission cache, took: " + timeTaken + "ms");
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
            Iterator<?> i = results.iterator();
            while (i.hasNext()) {
                Submission sub = (Submission) i.next();
                Set<ResultFile> files = sub.getResultFiles();
                submissionFilesCache.put(sub.getdCCid(), files);
            }

        } catch (Exception err) {
            err.printStackTrace();
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Primed file names cache, took: " + timeTaken + "ms");
    }




    private static void readSubmissionLocatedFeature(ObjectStore os) {
        long startTime = System.currentTimeMillis();
        submissionLocatedFeatureTypes = new LinkedHashMap<Integer, List<String>>();

        Query q = new Query();
        q.setDistinct(true);

        QueryClass qcSub = new QueryClass(Submission.class);
        QueryClass qcLsf = new QueryClass(SequenceFeature.class);
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
        LOG.info("Primed located features cache, took: " + timeTaken + "ms");
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
            QueryCollectionReference ref1 = new QueryCollectionReference(qcSubmission,
            "databaseRecords");
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
            Iterator<?> i = results.iterator();
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
        LOG.info("Primed Repository entries cache, took: " + timeTaken + "ms");
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
            
            flyTracks = readTracks("fly");
            wormTracks = readTracks("worm");
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
     * Method to read the list of GBrowse tracks for a given organism
     *
     * @param organism (i.e. fly or worm)
     * @return submissionTracksCache
     */
    private static Map<Integer, List<GBrowseTrack>> readTracks(String organism) {
        Map<Integer, List<GBrowseTrack>> submissionsToTracks = 
            new HashMap<Integer, List<GBrowseTrack>>();
        try {
            URL url = new URL(GBROWSE_BASE_URL + organism + GBROWSE_ST_URL_END);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;

            
            // examples of lines:
            //
            // [Henikoff_Salt_H3_WIG]
            // key      = H3.3 Chromatin fractions extracted with NaCl
            // select   = 80mM fraction#2534 350mM fraction#2535 600mM fraction#2536
            // citation = <h1> H3.3 NaCl Salt Extracted Chromatin ....
            //
            // [LIEB_WIG_CHROMNUC_ENV]
            // key      = Chromosome-Nuclear Envelope Interaction
            // select   = SDQ3891_LEM2_N2_MXEMB_1#2729 SDQ3897_NPP13_N2_MXEMB_1#2738
            // citation = <h1> Chromosome-Nuclear Envelope Interaction proteins...
            //
            // note: subtracks have also names with spaces

            StringBuffer trackName = new StringBuffer();
            StringBuffer toAppend = new StringBuffer();

            while ((line = reader.readLine()) != null) {
                LOG.debug("SUBTRACK LINE: " + line);
                if (line.startsWith("[")) {
                    // this is a track
                    trackName.setLength(0);
                    trackName.append(line.substring(1, line.indexOf(']')));
                }
                if (line.startsWith("select")) {
                    // here subtracks are listed
                    String data = line.replace("select   = ", "");
                    String[] result = data.split("\\s");
                    for (String token : result) {
                        if (token.indexOf('#') < 0) {
                            // we are dealing with a bit of name
                            toAppend.append(token + " ");
                        } else {
                            // this is a token with subId
                            String subTrack = toAppend.toString()
                            + token.substring(0, token.indexOf('#'));
                            Integer dccId = Integer.parseInt(token.substring(token.indexOf('#') + 1,
                            token.length()));
                            LOG.debug("SUBTRACK: " + subTrack);
                            toAppend.setLength(0); // empty buffer
                            GBrowseTrack newTrack = new GBrowseTrack(organism, trackName.toString(),
                            subTrack);
                            addToGBMap(submissionsToTracks, dccId, newTrack);
                        }
                    }
                }
            }
            reader.close();
        } catch (Exception err) {
            err.printStackTrace();
        }
        return submissionsToTracks;
    }


    /**
     * This method adds a GBrowse track to a map with
     * key = dccId
     * value = list of associated GBrowse tracks
     */
    private static void addToGBMap(
            Map<Integer, List<GBrowseTrack>> m,
            Integer key, GBrowseTrack value) {
        //
        List<GBrowseTrack> gbs = new ArrayList<GBrowseTrack>();

        if (m.containsKey(key)) {
            gbs = m.get(key);
        }
        if (!gbs.contains(value)) {
            gbs.add(value);
            m.put(key, gbs);
        }
    }


    /**
     * This method get the GBrowse base URL from the properties
     * or default to one
     * @return the base URL
     */
    private static String getGBrowsePrefix() {
        String gbrowseDefaultUrl = "http://modencode.oicr.on.ca/cgi-bin/gb2/gbrowse/";
        Properties props = PropertiesUtil.getProperties();
        String gbURL = props.getProperty("gbrowse.prefix") + "/";
        if (gbURL == null || gbURL.length() < 5) {
            return gbrowseDefaultUrl;
        }
       return gbURL;
    }


    /**
     * This method get the GBrowse base URL from the properties
     * or default to one
     * @return the base URL
     */
    private static Map<String, String> readFeatTypeDescription(ServletContext servletContext) {

        featDescriptionCache = new HashMap<String, String>();

        Properties props = new Properties();

            InputStream is
            = servletContext.getResourceAsStream("/WEB-INF/featureTypeDescr.properties");
            if (is == null) {
                LOG.info("Unable to find /WEB-INF/featureTypeDescr.properties, "
                + "there will be no feature type descriptions");
            } else {
                try {
                    props.load(is);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Enumeration<?> en = props.keys();
                while (en.hasMoreElements()) {
                    String expFeat = (String) en.nextElement();
                    String descr = props.getProperty(expFeat);
                    featDescriptionCache.put(expFeat, descr);
                }
            }
        return featDescriptionCache;
    }


}
