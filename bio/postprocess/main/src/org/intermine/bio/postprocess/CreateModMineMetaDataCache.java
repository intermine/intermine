package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2011 FlyMine
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
     */
    public static void createCache(ObjectStore os)
        throws IllegalAccessException, SQLException, IOException {

        Properties props = new Properties();

        readSubmissionFeatureCounts(os, props);
        readExperimentFeatureCounts(os, props);
        readSubmissionFeatureExpressionLevelCounts(os, props);
        readUniqueExperimentFeatureCounts(os, props);
        readSubmissionLocatedFeature(os, props);

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

        @SuppressWarnings("unchecked") Iterator<ResultsRow> iter =
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

        @SuppressWarnings("unchecked") Iterator<ResultsRow> iter =
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

        @SuppressWarnings("unchecked") Iterator<ResultsRow> iter =
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

        @SuppressWarnings("unchecked") Iterator<ResultsRow> iter = (Iterator) results.iterator();
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
        LOG.info("Read experiment feature counts, took: " + timeTaken + "ms");
    }

    // TODO MOVE THIS QUERY TO CreateModMineMetaDataCache and add value to ModMineCacheKeys
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
        @SuppressWarnings("unchecked") Iterator<ResultsRow> iter =
            (Iterator) results.iterator();
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

}
