package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.intermine.bio.constants.ModMineCacheKeys;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Sequence;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
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
import org.intermine.sql.Database;
import org.intermine.util.PropertiesUtil;
import org.intermine.util.TypeUtil;


/**
 * Runs queries to count features, etc per submission and experiment used for display in modMine
 * webapp.  Results are written to a property file and stored in the database to be retrieved by
 * MetaDataCache on deployment of the web application.
 * @author Richard Smith
 *
 */
public final class CreateModMineMetaDataCache
{
    private static final Logger LOG = Logger.getLogger(CreateModMineMetaDataCache.class);

    private CreateModMineMetaDataCache() {
        // don't
    }

    /**
     * Run queries to generate summary information for the modMine database and store resulting
     * properties file in the database.
     * @param os the ObjectStore to query
     * @throws IllegalAccessException if fields don't exist in data model
     * @throws SQLException if failure to write properties file to database
     * @throws IOException if failure serialising properties file
     * @throws ClassNotFoundException if failure to find named class
     */
    public static void createCache(ObjectStore os)
        throws IllegalAccessException, SQLException, IOException, ClassNotFoundException {

        Properties props = new Properties();

        readSubmissionFeatureCounts(os, props);
        readExperimentFeatureCounts(os, props);
        readSubmissionFeatureExpressionLevelCounts(os, props);
        readUniqueExperimentFeatureCounts(os, props);
        readSubmissionLocatedFeature(os, props);
        readSubmissionSequencedFeature(os, props);

        readSubmissionFileSourceCounts(os, props);

        Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
        MetadataManager.store(db, MetadataManager.MODMINE_METADATA_CACHE,
                PropertiesUtil.serialize(props));
    }

    private static void readSubmissionFeatureCounts(ObjectStore os, Properties props) {
        long startTime = System.currentTimeMillis();

        Model model = os.getModel();

        try {
            String errorMessage = "Not performing readSubmissionFeatureCounts ";
            PostProcessUtil.checkFieldExists(model, "Submission", "features", errorMessage);
            PostProcessUtil.checkFieldExists(model, "Submission", "DCCid", errorMessage);
        } catch (MetaDataException e) {
            return;
        }

        Query q = new Query();
        q.setDistinct(false);

        QueryClass qcSub = new QueryClass(model.getClassDescriptorByName("Submission").getType());
        QueryClass qcLsf = new QueryClass(SequenceFeature.class);

        QueryField qfDccId = new QueryField(qcSub, "DCCid");
        QueryField qfClass = new QueryField(qcLsf, "class");

        q.addFrom(qcSub);
        q.addFrom(qcLsf);

        q.addToSelect(qfDccId);
        q.addToSelect(qfClass);
        q.addToSelect(new QueryFunction());

        q.addToGroupBy(qfDccId);
        q.addToGroupBy(qfClass);

        q.addToOrderBy(qfDccId);
        q.addToOrderBy(qfClass);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference features = new QueryCollectionReference(qcSub, "features");
        ContainsConstraint ccFeats = new ContainsConstraint(features, ConstraintOp.CONTAINS, qcLsf);
        cs.addConstraint(ccFeats);

        q.setConstraint(cs);

        Results results = os.execute(q);

        @SuppressWarnings({ "unchecked", "rawtypes" }) Iterator<ResultsRow> iter =
            (Iterator) results.iterator();
        while (iter.hasNext()) {
            ResultsRow<?> row = iter.next();
            String dccId = (String) row.get(0);
            Class<?> feat = (Class<?>) row.get(1);
            Long count = (Long) row.get(2);

            String key = ModMineCacheKeys.SUB_FEATURE_COUNT + "."
                + dccId + "." + TypeUtil.unqualifiedName(feat.getName());
            props.put(key, "" + count);
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Created submissionFeatureCounts cache, took: " + timeTaken + "ms");
    }

    private static void readExperimentFeatureCounts(ObjectStore os, Properties props) {
        long startTime = System.currentTimeMillis();

        Model model = os.getModel();
        try {
            String errorMessage = "Not performing readSubmissionFeatureCounts ";
            PostProcessUtil.checkFieldExists(model, "Experiment", "name", errorMessage);
            PostProcessUtil.checkFieldExists(model, "Experiment", "submissions", errorMessage);
            PostProcessUtil.checkFieldExists(model, "Submission", "features", errorMessage);
        } catch (MetaDataException e) {
            return;
        }

        // NB: example of query (with group by) enwrapping a subquery that gets rids of
        // duplications

        Query q = new Query();

        QueryClass qcExp = new QueryClass(model.getClassDescriptorByName("Experiment").getType());
        QueryClass qcSub = new QueryClass(model.getClassDescriptorByName("Submission").getType());
        QueryClass qcFeat = new QueryClass(SequenceFeature.class);

        QueryField qfName = new QueryField(qcExp, "name");
        QueryField qfClass = new QueryField(qcFeat, "class");

        q.addFrom(qcSub);
        q.addFrom(qcFeat);
        q.addFrom(qcExp);

        q.addToSelect(qcExp);
        q.addToSelect(qcFeat);
        q.addToSelect(qfName);
        q.addToSelect(qfClass);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference submissions = new QueryCollectionReference(qcExp, "submissions");
        ContainsConstraint ccSubs = new ContainsConstraint(submissions, ConstraintOp.CONTAINS,
                qcSub);
        cs.addConstraint(ccSubs);

        QueryCollectionReference features = new QueryCollectionReference(qcSub, "features");
        ContainsConstraint ccFeats =
            new ContainsConstraint(features, ConstraintOp.CONTAINS, qcFeat);
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

        @SuppressWarnings({ "unchecked", "rawtypes" }) Iterator<ResultsRow> iter =
            (Iterator) results.iterator();
        while (iter.hasNext()) {
            ResultsRow<?> row = iter.next();
            String expName = fixSpaces((String) row.get(0));
            Class<?> feat = (Class<?>) row.get(1);
            Long count = (Long) row.get(2);

            String key = ModMineCacheKeys.EXP_FEATURE_COUNT + "."
                + expName + "." + TypeUtil.unqualifiedName(feat.getName());
            props.put(key, "" + count);
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Read experiment feature counts, took: " + timeTaken + "ms");
    }

    /**
     * to escape spaces in the key of a property file
     * @param expName
     * @return exp name with spaces escaped
     */
    private static String fixSpaces(String expName) {
        String expNameFixed = expName.replace(" " , "\\ ");
        return expNameFixed;
    }

    private static void readSubmissionFeatureExpressionLevelCounts(ObjectStore os,
            Properties props) {
        long startTime = System.currentTimeMillis();

        Model model = os.getModel();
        Query q = new Query();
        q.setDistinct(false);

        QueryClass qcSub = new QueryClass(model.getClassDescriptorByName("Submission").getType());
        QueryClass qcLsf = new QueryClass(SequenceFeature.class);
        QueryClass qcEL =
            new QueryClass(model.getClassDescriptorByName("ExpressionLevel").getType());

        QueryField qfClass = new QueryField(qcLsf, "class");
        QueryField qfDccId = new QueryField(qcSub, "DCCid");

        q.addFrom(qcSub);
        q.addFrom(qcLsf);
        q.addFrom(qcEL);

        q.addToSelect(qfDccId);
        q.addToSelect(qfClass);
        q.addToSelect(new QueryFunction());

        q.addToGroupBy(qfDccId);
        q.addToGroupBy(qfClass);

        q.addToOrderBy(qfDccId);
        q.addToOrderBy(qfClass);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference features = new QueryCollectionReference(qcSub, "features");
        ContainsConstraint ccFeats = new ContainsConstraint(features, ConstraintOp.CONTAINS, qcLsf);
        cs.addConstraint(ccFeats);
        QueryCollectionReference el = new QueryCollectionReference(qcLsf, "expressionLevels");
        ContainsConstraint ccEl = new ContainsConstraint(el, ConstraintOp.CONTAINS, qcEL);
        cs.addConstraint(ccEl);

        q.setConstraint(cs);

        Results results = os.execute(q);

        @SuppressWarnings({ "unchecked", "rawtypes" }) Iterator<ResultsRow> iter =
            (Iterator) results.iterator();
        while (iter.hasNext()) {
            ResultsRow<?> row = iter.next();
            String dccId = (String) row.get(0);
            Class<?> feat = (Class<?>) row.get(1);
            Long count = (Long) row.get(2);

            String key = ModMineCacheKeys.SUB_FEATURE_EXPRESSION_LEVEL_COUNT + "."
                + dccId + "." + TypeUtil.unqualifiedName(feat.getName());
            props.put(key, "" + count);
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Read submissionFeatureExpressionLevelCounts cache, took: " + timeTaken + "ms");
    }


    private static void readUniqueExperimentFeatureCounts(ObjectStore os, Properties props) {
        long startTime = System.currentTimeMillis();

        Model model = os.getModel();

        Query q = new Query();
        QueryClass qcSub = new QueryClass(model.getClassDescriptorByName("Submission").getType());
        QueryClass qcExp = new QueryClass(model.getClassDescriptorByName("Experiment").getType());
        QueryClass qcFeat = new QueryClass(SequenceFeature.class);
        QueryClass qcChr = new QueryClass(Chromosome.class);
        QueryClass qcLoc = new QueryClass(Location.class);

        QueryField qfExpName = new QueryField(qcExp, "name");
        QueryField qfFeatureType = new QueryField(qcFeat, "class");
        QueryField qfChrID = new QueryField(qcChr, "primaryIdentifier");
        QueryField qfStart = new QueryField(qcLoc, "start");
        QueryField qfEnd = new QueryField(qcLoc, "end");

        q.addFrom(qcSub);
        q.addFrom(qcFeat);
        q.addFrom(qcExp);
        q.addFrom(qcChr);
        q.addFrom(qcLoc);

        q.addToSelect(qfExpName);
        q.addToSelect(qfFeatureType);
        q.addToSelect(qfChrID);
        q.addToSelect(qfStart);
        q.addToSelect(qfEnd);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference submissions = new QueryCollectionReference(qcExp, "submissions");
        ContainsConstraint ccSubs = new ContainsConstraint(submissions, ConstraintOp.CONTAINS,
                qcSub);
        cs.addConstraint(ccSubs);

        QueryCollectionReference features = new QueryCollectionReference(qcSub, "features");
        ContainsConstraint ccFeat = new ContainsConstraint(features, ConstraintOp.CONTAINS, qcFeat);
        cs.addConstraint(ccFeat);

        QueryObjectReference chromosome = new QueryObjectReference(qcFeat, "chromosome");
        ContainsConstraint ccChr = new ContainsConstraint(chromosome,
                ConstraintOp.CONTAINS, qcChr);
        cs.addConstraint(ccChr);

        QueryObjectReference chromosomeLocation = new QueryObjectReference(qcFeat,
            "chromosomeLocation");
        ContainsConstraint ccChrLoc = new ContainsConstraint(chromosomeLocation,
                ConstraintOp.CONTAINS, qcLoc);
        cs.addConstraint(ccChrLoc);

        q.setConstraint(cs);
        q.setDistinct(true);

        Query superQ = new Query();
        superQ.addFrom(q);
        QueryField superQfName = new QueryField(q, qfExpName);
        QueryField superQfFT = new QueryField(q, qfFeatureType);

        superQ.addToSelect(superQfName);
        superQ.addToSelect(superQfFT);

        superQ.addToOrderBy(superQfName);
        superQ.addToOrderBy(superQfFT);
        superQ.addToGroupBy(superQfName);
        superQ.addToGroupBy(superQfFT);

        superQ.addToSelect(new QueryFunction());
        superQ.setDistinct(false);

        Results results = os.execute(superQ);

        @SuppressWarnings({ "unchecked", "rawtypes" })
        Iterator<ResultsRow> iter = (Iterator) results.iterator();
        while (iter.hasNext()) {
            ResultsRow<?> row = iter.next();
            String expName = fixSpaces((String) row.get(0));
            Class<?> feat = (Class<?>) row.get(1);
            Long count = (Long) row.get(2);

            String key = ModMineCacheKeys.UNIQUE_EXP_FEATURE_COUNT + "."
                + expName + "." + TypeUtil.unqualifiedName(feat.getName());
            props.put(key, "" + count);
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Read experiment unique feature counts, took: " + timeTaken + "ms");
    }


    private static void readSubmissionLocatedFeature(ObjectStore os, Properties props) {

        long startTime = System.currentTimeMillis();

        Model model = os.getModel();

        Query q = new Query();
        q.setDistinct(true);

        QueryClass qcSub = new QueryClass(model.getClassDescriptorByName("Submission").getType());
        QueryClass qcLsf = new QueryClass(SequenceFeature.class);
        QueryClass qcLoc = new QueryClass(Location.class);

        QueryField qfDccId = new QueryField(qcSub, "DCCid");
        QueryField qfClass = new QueryField(qcLsf, "class");

        q.addFrom(qcSub);
        q.addFrom(qcLsf);
        q.addFrom(qcLoc);

        q.addToSelect(qfDccId);
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
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Iterator<ResultsRow> iter = (Iterator) results.iterator();
        while (iter.hasNext()) {
            ResultsRow<?> row = iter.next();
            String dccId = (String) row.get(0);
            Class<?> feat = (Class<?>) row.get(1);

            String key = ModMineCacheKeys.SUB_LOCATED_FEATURE_TYPE
                + "." + dccId + "." + TypeUtil.unqualifiedName(feat.getName());
            props.put(key, "" + TypeUtil.unqualifiedName(feat.getName()));

        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Read located features types, took: " + timeTaken + " ms.");
    }

    private static void readSubmissionSequencedFeature(ObjectStore os, Properties props) {

        long startTime = System.currentTimeMillis();

        Model model = os.getModel();

        Query q = new Query();
        q.setDistinct(true);

        QueryClass qcSub = new QueryClass(model.getClassDescriptorByName("Submission").getType());
        QueryClass qcLsf = new QueryClass(SequenceFeature.class);
        QueryClass qcSeq = new QueryClass(Sequence.class);

        QueryField qfDccId = new QueryField(qcSub, "DCCid");
        QueryField qfClass = new QueryField(qcLsf, "class");

        q.addFrom(qcSub);
        q.addFrom(qcLsf);
        q.addFrom(qcSeq);

        q.addToSelect(qfDccId);
        q.addToSelect(qfClass);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference features = new QueryCollectionReference(qcSub, "features");
        ContainsConstraint ccFeats = new ContainsConstraint(features, ConstraintOp.CONTAINS, qcLsf);
        cs.addConstraint(ccFeats);

        QueryObjectReference location = new QueryObjectReference(qcLsf, "sequence");
        ContainsConstraint ccLocs = new ContainsConstraint(location, ConstraintOp.CONTAINS, qcSeq);
        cs.addConstraint(ccLocs);

        q.setConstraint(cs);

        Results results = os.execute(q);

        // for each classes set the values for jsp
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Iterator<ResultsRow> iter = (Iterator) results.iterator();
        while (iter.hasNext()) {
            ResultsRow<?> row = iter.next();
            String dccId = (String) row.get(0);
            Class<?> feat = (Class<?>) row.get(1);

            String key = ModMineCacheKeys.SUB_SEQUENCED_FEATURE_TYPE
                + "." + dccId + "." + TypeUtil.unqualifiedName(feat.getName());
            props.put(key, "" + TypeUtil.unqualifiedName(feat.getName()));

        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Read sequenced features types, took: " + timeTaken + " ms.");
    }

 //======


    private static void readSubmissionFileSourceCounts(ObjectStore os, Properties props)
        throws ClassNotFoundException {
        long startTime = System.currentTimeMillis();

        Model model = os.getModel();
        Query q = new Query();
        q.setDistinct(false);

        QueryClass qcSub = new QueryClass(model.getClassDescriptorByName(
                "Submission").getType());
        QueryClass qcLsf = new QueryClass(Class.forName(model.getPackageName() + ".BindingSite"));
//        QueryClass qcLsf = new QueryClass(BindingSite.class);
        // QueryClass qcLsf = new QueryClass(SequenceFeature.class);
        // QueryClass qcEL =
        // new
        // QueryClass(model.getClassDescriptorByName("BindingSite").getType());

        QueryField qfDccId = new QueryField(qcSub, "DCCid");
        QueryField qfClass = new QueryField(qcLsf, "class");
        QueryField qfFile = new QueryField(qcLsf, "sourceFile");

        q.addFrom(qcSub);
        q.addFrom(qcLsf);
        // q.addFrom(qcEL);

        q.addToSelect(qfDccId);
        q.addToSelect(qfClass);
        q.addToSelect(qfFile);
        q.addToSelect(new QueryFunction());

        q.addToGroupBy(qfDccId);
        q.addToGroupBy(qfClass);
        q.addToGroupBy(qfFile);

        q.addToOrderBy(qfDccId);
        q.addToOrderBy(qfClass);
        q.addToOrderBy(qfFile);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference features = new QueryCollectionReference(qcSub,
                "features");
        ContainsConstraint ccFeats = new ContainsConstraint(features,
                ConstraintOp.CONTAINS, qcLsf);
        cs.addConstraint(ccFeats);
        // QueryCollectionReference el = new QueryCollectionReference(qcLsf,
        // "expressionLevels");
        // ContainsConstraint ccEl = new ContainsConstraint(el,
        // ConstraintOp.CONTAINS, qcEL);
        // cs.addConstraint(ccEl);

        q.setConstraint(cs);

        Results results = os.execute(q);

        @SuppressWarnings({ "unchecked", "rawtypes" })
        Iterator<ResultsRow> iter = (Iterator) results.iterator();
        while (iter.hasNext()) {
            ResultsRow<?> row = iter.next();
            String dccId = (String) row.get(0);
            Class<?> feat = (Class<?>) row.get(1);
            String fileName = (String) row.get(2);
            Long count = (Long) row.get(3);

            String key = ModMineCacheKeys.SUB_FILE_SOURCE_COUNT + "."
                    + dccId + "." + TypeUtil.unqualifiedName(feat.getName()) + "." + fileName;
            props.put(key, "" + count);
        }

        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Read submissionFileSourceCounts cache, took: " + timeTaken
                + "ms");
    }


//    private static void readSubmissionRepositoryEntries(ObjectStore os) {
//        //
//        long startTime = System.currentTimeMillis();
//        try {
//            Query q = new Query();
//            QueryClass qcSubmission = new QueryClass((Class.forName(os.getModel().getPackageName()
//                    + ".Submission")));
//            QueryField qfDCCid = new QueryField(qcSubmission, "DCCid");
//            q.addFrom(qcSubmission);
//            q.addToSelect(qfDCCid);
//
//            QueryClass qcRepositoryEntry =
//          new QueryClass((Class.forName(os.getModel().getPackageName() + ".DatabaseRecord")));
//
//            QueryField qfDatabase = new QueryField(qcRepositoryEntry,
//                    "database");
//            QueryField qfAccession = new QueryField(qcRepositoryEntry,
//                    "accession");
//            QueryField qfUrl = new QueryField(qcRepositoryEntry, "url");
//            q.addFrom(qcRepositoryEntry);
//            q.addToSelect(qfDatabase);
//            q.addToSelect(qfAccession);
//            q.addToSelect(qfUrl);
//
//            // join the tables
//            QueryCollectionReference ref1 = new QueryCollectionReference(
//                    qcSubmission, "databaseRecords");
//            ContainsConstraint cc = new ContainsConstraint(ref1,
//                    ConstraintOp.CONTAINS, qcRepositoryEntry);
//
//            q.setConstraint(cc);
//            q.addToOrderBy(qfDCCid);
//            q.addToOrderBy(qfDatabase);
//
//            Results results = os.execute(q);
//
//            submissionRepositedCache = new HashMap<String, List<String[]>>();
//
//            Integer counter = 0;
//
//            // Integer prevSub = new Integer(-1);
//            String prevSub = null;
//            List<String[]> subRep = new ArrayList<String[]>();
//            Iterator<?> i = results.iterator();
//            while (i.hasNext()) {
//                ResultsRow<?> row = (ResultsRow<?>) i.next();
//
//                counter++;
//                String dccId = (String) row.get(0);
//                String db = (String) row.get(1);
//                String acc = (String) row.get(2);
//                String url = (String) row.get(3);
//                String[] thisRecord = {db, acc, url};
//
//                if (!dccId.equals(prevSub) || counter.equals(results.size())) {
//                    if (prevSub != null) {
//                        if (counter.equals(results.size())) {
//                            prevSub = dccId;
//                            subRep.add(thisRecord);
//                        }
//                        List<String[]> subRepIn = new ArrayList<String[]>();
//                        subRepIn.addAll(subRep);
//                        submissionRepositedCache.put(prevSub, subRepIn);
//                        subRep.clear();
//                    }
//                    prevSub = dccId;
//                }
//                subRep.add(thisRecord);
//            }
//        } catch (Exception err) {
//            err.printStackTrace();
//        }
//        long timeTaken = System.currentTimeMillis() - startTime;
//        LOG.info("Primed Repository entries cache, took: " + timeTaken
//                + "ms size = " + submissionRepositedCache.size());
//    }




}
