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
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.intermine.model.bio.Experiment;
import org.intermine.model.bio.LocatedSequenceFeature;
import org.intermine.model.bio.Project;
import org.intermine.model.bio.Submission;
import org.intermine.model.bio.SubmissionData;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;


/**
 * Read modENCODE metadata into objects that simplify display code, cache results.
 * @author Richard Smith
 *
 */
public class MetadataCache
{
    // GBrowse URLs
    private static final String GBROWSE_BASE_URL
    = "http://modencode.oicr.on.ca/cgi-bin/gb2/gbrowse/";
    private static final String GBROWSE_URL_END = "/?show_tracks=1";

    // SubmissionData name for files
    private static final String FILETYPE = "%file";

    public static class GBrowseTrack
    {
        private String organism; // {fly,worm}
        private String track;    // e.g. LIEB_WIG_CHIPCHIP_POL2
        public GBrowseTrack(String organism2, String trackName) {
            this.organism  = organism2;
            this.track = trackName;
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
    }


    private static Map<String, DisplayExperiment> experimentCache = null;
    private static Map<Integer, Map<String, Long>> submissionFeatureCounts = null;
    private static Map<Integer, Integer> submissionIdCache = null;
    private static Map<Integer, List<GBrowseTrack>> submissionTracksCache = null;
    private static Map<Integer, List<String>> submissionFilesCache = null;
    private static Map<Integer, Integer> filesPerSubmissionCache = null;
    private static long lastTrackCacheRefresh = 0;
    private static final long ONE_HOUR = 3600000;

    private static final Logger LOG = Logger.getLogger(MetadataCache.class);

    /**
     * Fetch experiment details for display.
     * @param os the production objectstore
     * @return a list of experiments
     */
    public static synchronized List<DisplayExperiment> getExperiments(ObjectStore os) {
        if (experimentCache == null) {
            readExperiments(os);
        }
        return new ArrayList<DisplayExperiment>(experimentCache.values());
    }

    /**
     * Fetch GBrowse tracks for display.
     * @return map
     */
    public static synchronized Map<Integer, List<GBrowseTrack>> getGBrowseTracks() {
        long timeSinceLastRefresh = System.currentTimeMillis() - lastTrackCacheRefresh;
        if (timeSinceLastRefresh > ONE_HOUR) {
            readGBrowseTracks();
            lastTrackCacheRefresh = System.currentTimeMillis();
        }
        return submissionTracksCache;
    }

    /**
     * Fetch input/output file names per submission.
     * @param os the production objectstore
     * @return map
     */
    public static synchronized Map<Integer, List<String>> getSubmissionFiles(ObjectStore os) {
        if (submissionFilesCache == null) {
            readSubmissionFiles(os);
        }
        return submissionFilesCache;
    }

    /**
     * Fetch number of input/output file per submission.
     * @param os the production objectstore
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

    private static void readSubmissionFiles(ObjectStore os) {
        //
        long startTime = System.currentTimeMillis();
        try {
            Query q = new Query();
            QueryClass qcSubmission = new QueryClass(Submission.class);
            QueryField qfDCCid = new QueryField(qcSubmission, "DCCid");
            q.addFrom(qcSubmission);
            q.addToSelect(qfDCCid);

            QueryClass qcSubmissionData = new QueryClass(SubmissionData.class);
            QueryField qfFileName = new QueryField(qcSubmissionData, "value");
            QueryField qfDataType = new QueryField(qcSubmissionData, "type");
            q.addFrom(qcSubmissionData);
            q.addToSelect(qfFileName);
            QueryValue fileType = new QueryValue(FILETYPE);

            ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
            SimpleConstraint sc = new SimpleConstraint(new QueryExpression(QueryExpression.LOWER,
                    qfDataType), ConstraintOp.MATCHES, fileType);
            cs.addConstraint(sc);

            // join the tables
            QueryObjectReference ref1 = new QueryObjectReference(qcSubmissionData, "submission");
            ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS,
                    qcSubmission);
            cs.addConstraint(cc1);

            q.setConstraint(cs);
            q.addToOrderBy(qfDCCid);

            Results results = os.execute(q);

            submissionFilesCache = new HashMap<Integer, List<String>>();

            Integer counter = 0;

            Integer prevSub = new Integer(-1);
            List<String> subFiles = new ArrayList<String>();
            Iterator i = results.iterator();
            while (i.hasNext()) {
                ResultsRow row = (ResultsRow) i.next();

                counter++;
                Integer dccId = (Integer) row.get(0);
                String fileName = (String) row.get(1);

                if (!dccId.equals(prevSub) || counter.equals(results.size())) {
                    if (prevSub > 0) {
                        if (counter.equals(results.size())) {
                            prevSub = dccId;
                            subFiles.add(fileName);
                        }
                        List<String> subFilesIn = new ArrayList<String>();
                        subFilesIn.addAll(subFiles);
                        submissionFilesCache.put(prevSub, subFilesIn);
                        subFiles.clear();
                    }
                    prevSub = dccId;
                }
                subFiles.add(fileName);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Primed file names cache, took: " + timeTaken + "ms");
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


    /**
     * Fetch a list of file names for a given submission.
     * @param os the objectstore
     * @param dccId the modENCODE submission id
     * @return a list of file names
     */
    public static synchronized List<String> getFilesByDccId(ObjectStore os,
            Integer dccId) {
        if (submissionFilesCache == null) {
            readSubmissionFiles(os);
        }
        return new ArrayList<String>(submissionFilesCache.get(dccId));
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
        return new ArrayList<GBrowseTrack>(submissionTracksCache.get(dccId));
    }


    /**
     * Fetch a map from feature type to count for a given submission.
     * @param os the objectstore
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
     * @param os the objectstore
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
     * @param os the objectstore
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
        LOG.info("Primed experiment cache, took: " + timeTaken + "ms");
    }


    private static Map<String, Map<String, Long>> getExperimentFeatureCounts(ObjectStore os) {
        long startTime = System.currentTimeMillis();
        Query q = new Query();
        q.setDistinct(false);

        QueryClass qcExp = new QueryClass(Experiment.class);
        QueryClass qcSub = new QueryClass(Submission.class);
        QueryClass qcLsf = new QueryClass(LocatedSequenceFeature.class);

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
        LOG.info("Primed submission cache, took: " + timeTaken + "ms");
    }


    /**
     * Method to fill the cached map of submissions (ddcId) to list of
     * GBrowse tracks
     *
     */
    private static void readGBrowseTracks() {
        long startTime = System.currentTimeMillis();

        submissionTracksCache = new HashMap<Integer, List<GBrowseTrack>>();
        try {
            readTracks("fly");
            readTracks("worm");
        } catch (Exception err) {
            err.printStackTrace();
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Primed GBrowse tracks cache, took: " + timeTaken + "ms  size = "
                + submissionTracksCache.size());
    }


    /**
     * Method to read the list of GBrowse tracks for a given organism
     *
     * @param organism (i.e. fly or worm)
     * @return submissionTracksCache
     */
    private static Map<Integer, List<GBrowseTrack>> readTracks(String organism) {
        try {
            URL url = new URL(GBROWSE_BASE_URL + organism + GBROWSE_URL_END);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            // apparently the rules for parsing are:
            // fields tab separated, dccId space separated
            // if track/dccId are in the name, the first is the track, the second is the dccId
            // if there is only one number in the name, this is a dccId
            // examples of lines:
            // Dm_adult_wh_read_pair_2458_712    712 574 2458    Whole Adult Fly
            // Dm_cell_line_reads_342  Kc167 (C-tailed polyA RNA)
            // Karpen_HISMODENZ 284 332 923 926 927 928 945 947 948 952 2330 2208 2216 2227 2782
                // 2786 2278 2326 2788 2299   ChIP signal for Histone Modifying Enzymes

            while ((line = reader.readLine()) != null) {
                String[] result = line.split("\\t");
                String trackName = result[0];
                GBrowseTrack newTrack = new GBrowseTrack(organism, trackName);
                // look for dccId in the line
                String list = result[1];
                String[] dccIds = list.split("\\s");
                parseTokens(dccIds, newTrack, false);

                if (list.length() < 2) {
                    // look for dccId in the track name
                    // (only if there are no dccid in the proper field)
                    String[] nameSplit = trackName.split("_");
                    parseTokens(nameSplit, newTrack, true);
                }
                //
                //
                //                // look for dccId in the track name
                //                String[] nameSplit = trackName.split("_");
                //                parseTokens(nameSplit, newTrack, true);
            }
            reader.close();
        } catch (Exception err) {
            err.printStackTrace();
        }
        return submissionTracksCache;
    }

    /**
     * This method looks for dccId in the tokenised line
     * or the tokenised track name
     * In the first case it uses an offset: the last token is always part of
     * the description even if it is a number (e.g. Green Lab ESTs 2008-12-02 set 1)
     *
     * @param tokes the array of tokens
     * @param track the GBrowse track
     * @param isName: needed to include only last token in name parsing (see above)
     */
    private static void parseTokens(String[] tokens,
            GBrowseTrack track, Boolean isName) {
        // starting from the end, because when checking track names only
        // the last number is ok if there are 2
        for (int x = (tokens.length - 1); x > -1; x--) {
            if (StringUtil.allDigits(tokens[x])) {
                // this is a submission Id
                Integer dccId = Integer.parseInt(tokens[x]);
                // add to map sub trackname
                addToGBMap(submissionTracksCache, dccId, track);
                if (isName) { //track name
                    break;
                }
            }
        }
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
}
