package org.modmine.web;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Experiment;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.model.bio.Submission;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.OverlapConstraint;
import org.intermine.objectstore.query.OverlapRange;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;

/**
 * SpanOverlapQuery is a class with query logic. It has two methods: runSpanValidationQuery queries
 * the information of all the organisms and their chromosomes' names and length for span validation
 * use; runSpanOverlapQuery queries LocatedSequenceFeatures that overlap users' spans.
 *
 * @author Fengyuan Hu
 *
 */
public final class SpanOverlapQueryRunner
{

    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(SpanOverlapQueryRunner.class);

    private SpanOverlapQueryRunner() {
    }

    /**
     * Query the information of all the organisms and their chromosomes' names and length. The
     * results is stored in a Map. The result data will be used to validate users' span data.
     * For each span, its chromosome must match the chrPID and range must not go beyond the length.
     *
     * @param im - the InterMineAPI
     * @return chrInfoMap - a HashMap with rgName as key and its chrInfo accordingly as value
     */
    public static Map<String, List<ChromosomeInfo>> runSpanValidationQuery(InterMineAPI im) {

        // a Map contains orgName and its chrInfo accordingly
        // e.g. <D.Melanogaster, (D.Melanogaster, X, 5000)...>
        Map<String, List<ChromosomeInfo>> chrInfoMap =
            new HashMap<String, List<ChromosomeInfo>>();

        try {
            Query q = new Query();

            QueryClass qcOrg = new QueryClass(Organism.class);
            QueryClass qcChr = new QueryClass(Chromosome.class);

            // Result columns
            QueryField qfOrgName = new QueryField(qcOrg, "shortName");
            QueryField qfChrPID = new QueryField(qcChr, "primaryIdentifier");
            QueryField qfChrLength = new QueryField(qcChr, "length");

            // As in SQL SELECT ?,?,?
            q.addToSelect(qfOrgName);
            q.addToSelect(qfChrPID);
            q.addToSelect(qfChrLength);

            // As in SQL FROM ?,?
            q.addFrom(qcChr);
            q.addFrom(qcOrg);

            // As in SQL WHERE ?
            QueryObjectReference organism = new QueryObjectReference(qcChr,
                    "organism");
            ContainsConstraint ccOrg = new ContainsConstraint(organism,
                    ConstraintOp.CONTAINS, qcOrg);
            q.setConstraint(ccOrg);

            Results results = im.getObjectStore().execute(q);

            // a List contains all the chrInfo (organism, chrPID, length)
            List<ChromosomeInfo> chrInfoList = new ArrayList<ChromosomeInfo>();
            // a Set contains all the orgName
            Set<String> orgSet = new HashSet<String>();

            // Handle results
            for (Iterator<?> iter = results.iterator(); iter.hasNext();) {
                ResultsRow<?> row = (ResultsRow<?>) iter.next();

                String orgName = (String) row.get(0);
                String chrPID = (String) row.get(1);
                Integer chrLength = (Integer) row.get(2);

                // Add orgName to HashSet to filter out duplication
                orgSet.add(orgName);

                if (chrLength != null) {
                    ChromosomeInfo chrInfo = new ChromosomeInfo();
                    chrInfo.setOrgName(orgName);
                    chrInfo.setChrPID(chrPID);
                    chrInfo.setChrLength(chrLength);

                    // Add ChromosomeInfo to Arraylist
                    chrInfoList.add(chrInfo);
                }
            }

            // Iterate orgSet and chrInfoList to put data in chrInfoMap which has the key as the
            // orgName and value as a ArrayList containing a list of chrInfo which has the same
            // orgName
            for (String orgName : orgSet) {

                // a List to store chrInfo for the same organism
                List<ChromosomeInfo> chrInfoSubList = new ArrayList<ChromosomeInfo>();

                for (ChromosomeInfo chrInfo : chrInfoList) {
                    if (orgName.equals(chrInfo.getOrgName())) {
                        chrInfoSubList.add(chrInfo);
                        chrInfoMap.put(orgName, chrInfoSubList);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return chrInfoMap;
    }

    /**
     * Query the relevant overlap region and additional information according to user's uploaded
     * spans.
     *
     * @param form A SpanUploadForm object
     * @param spanList Spans in an ArrayList
     * @param im A InterMineAPI object
     * @return spanOverlapResultMap A HashMap contains all the spans and their query results
     * @throws ClassNotFoundException Model.getQualifiedTypeName() throws
     */
    @SuppressWarnings("rawtypes")
    public static Map<Span, List<SpanQueryResultRow>> runSpanOverlapQuery(SpanUploadForm form,
            List<Span> spanList, InterMineAPI im) throws ClassNotFoundException {

        String orgName = form.getOrgName();

        // featureTypes in this case are (the last bit of ) class instead of featuretype in the db
        // table; gain the full name by Model.getQualifiedTypeName(className)
        List<Class> ftKeys = new ArrayList<Class>();
        String modelPackName = im.getModel().getPackageName();
        for (String aClass : form.getFeatureTypes()) {
            ftKeys.add(Class.forName(modelPackName + "." + aClass));
        }

        List<Integer> subKeys = getSubmissionsByExperimentNames(form.getExperiments(), im);


        Map<Span, List<SpanQueryResultRow>> spanOverlapResultDisplayMap =
            new LinkedHashMap<Span, List<SpanQueryResultRow>>();

        try {
            Query q;
            for (Span aSpan: spanList) {

                LOG.info("Span >>>>> " + aSpan.getChr() + "  " + aSpan.getStart() + "  " + aSpan.getEnd());

                q = new Query();
                q.setDistinct(true);

                String chrPID = aSpan.getChr();
                Integer start = aSpan.getStart();
                Integer end = aSpan.getEnd();

                //>>>>> TEST CODE <<<<<
//                LOG.info("OrgName: " + orgName);
//                LOG.info("chrPID: " + chrPID);
//                LOG.info("start: " + start);
//                LOG.info("end: " + end);
//                LOG.info("FeatureTypes: " + ftKeys);
//                LOG.info("Submissions: " + subKeys);
                //>>>>> TEST CODE <<<<<

                // DB tables
                QueryClass qcOrg = new QueryClass(Organism.class);
                QueryClass qcChr = new QueryClass(Chromosome.class);
                QueryClass qcFeature = new QueryClass(SequenceFeature.class);
                QueryClass qcLoc = new QueryClass(Location.class);
                QueryClass qcSubmission = new QueryClass(Submission.class);

                QueryField qfOrgName = new QueryField(qcOrg, "shortName");
                QueryField qfChrPID = new QueryField(qcChr, "primaryIdentifier");
                QueryField qfFeaturePID = new QueryField(qcFeature, "primaryIdentifier");
                QueryField qfFeatureClass = new QueryField(qcFeature, "class");
                QueryField qfSubmissionTitle = new QueryField(qcSubmission, "title");
                QueryField qfSubmissionDCCid = new QueryField(qcSubmission, "DCCid");
                QueryField qfChr = new QueryField(qcChr, "primaryIdentifier");
                QueryField qfLocStart = new QueryField(qcLoc, "start");
                QueryField qfLocEnd = new QueryField(qcLoc, "end");

                q.addToSelect(qfFeaturePID);
                q.addToSelect(qfFeatureClass);
                q.addToSelect(qfChr);
                q.addToSelect(qfLocStart);
                q.addToSelect(qfLocEnd);
                q.addToSelect(qfSubmissionDCCid);
                q.addToSelect(qfSubmissionTitle);

                q.addFrom(qcChr);
                q.addFrom(qcOrg);
                q.addFrom(qcFeature);
                q.addFrom(qcLoc);
                q.addFrom(qcSubmission);

                q.addToOrderBy(qfLocStart, "ascending");

                ConstraintSet constraints = new ConstraintSet(ConstraintOp.AND);

                q.setConstraint(constraints);

                // SequenceFeature.organism = Organism
                QueryObjectReference organism = new QueryObjectReference(qcFeature,
                        "organism");
                ContainsConstraint ccOrg = new ContainsConstraint(organism,
                        ConstraintOp.CONTAINS, qcOrg);
                constraints.addConstraint(ccOrg);

                // Organism.name = orgName
                SimpleConstraint scOrg = new SimpleConstraint(qfOrgName,
                        ConstraintOp.EQUALS, new QueryValue(orgName));
                constraints.addConstraint(scOrg);

                // Location.feature = SequenceFeature
                QueryObjectReference locSubject = new QueryObjectReference(qcLoc,
                        "feature");
                ContainsConstraint ccLocSubject = new ContainsConstraint(locSubject,
                        ConstraintOp.CONTAINS, qcFeature);
                constraints.addConstraint(ccLocSubject);

                // Location.locatedOn = Chromosome
                QueryObjectReference locObject = new QueryObjectReference(qcLoc,
                        "locatedOn");
                ContainsConstraint ccLocObject = new ContainsConstraint(locObject,
                        ConstraintOp.CONTAINS, qcChr);
                constraints.addConstraint(ccLocObject);

                // Chromosome.primaryIdentifier = chrPID
                SimpleConstraint scChr = new SimpleConstraint(qfChrPID,
                        ConstraintOp.EQUALS, new QueryValue(chrPID));
                constraints.addConstraint(scChr);

                // SequenceFeature.submissions = Submission
                QueryCollectionReference submission = new QueryCollectionReference(qcFeature,
                         "submissions");
                ContainsConstraint ccSubmission = new ContainsConstraint(submission,
                        ConstraintOp.CONTAINS, qcSubmission);
                constraints.addConstraint(ccSubmission);

                // SequenceFeature.class in a list
                constraints.addConstraint(new BagConstraint(qfFeatureClass,
                        ConstraintOp.IN, ftKeys));
                // Submission.CCDid in a list
                constraints.addConstraint(new BagConstraint(qfSubmissionDCCid,
                        ConstraintOp.IN, subKeys));


                OverlapRange overlapInput = new OverlapRange(new QueryValue(start),
                        new QueryValue(end), locObject);
                OverlapRange overlapFeature = new OverlapRange(new QueryField(qcLoc,
                        "start"), new QueryField(qcLoc, "end"), locObject);
                OverlapConstraint oc = new OverlapConstraint(overlapInput,
                        ConstraintOp.OVERLAPS, overlapFeature);
                constraints.addConstraint(oc);


                long queryStartTime = System.currentTimeMillis();

                Results results = im.getObjectStore().execute(q);

                long queryEndTime = System.currentTimeMillis();
                long queryRunTime = queryEndTime - queryStartTime;
                LOG.info("Query Run Time >>>>> " + queryRunTime);

                //>>>>> TEST CODE <<<<<
//                LOG.info("Query: " + q.toString());
//                LOG.info("Result Size: " + results.size());
//                LOG.info("Result >>>>> " + results);
                //>>>>> TEST CODE <<<<<

                List<SpanQueryResultRow> spanResults = new ArrayList<SpanQueryResultRow>();
                if (results == null || results.isEmpty()) {
                    LOG.info("Overlapped feature size >>>>> Null");
                    spanOverlapResultDisplayMap.put(aSpan, null);
                }
                else {
                    for (Iterator<?> iter = results.iterator(); iter.hasNext();) {
                        ResultsRow<?> row = (ResultsRow<?>) iter.next();

                        SpanQueryResultRow aRow = new SpanQueryResultRow();
                        aRow.setFeaturePID((String) row.get(0));
                        aRow.setFeatureClass((Class) row.get(1));
                        aRow.setChr((String) row.get(2));
                        aRow.setStart((Integer) row.get(3));
                        aRow.setEnd((Integer) row.get(4));
                        aRow.setSubDCCid((String) row.get(5));
                        aRow.setSubTitle((String) row.get(6));

                        spanResults.add(aRow);
                    }

                    LOG.info("Overlapped feature size >>>>> " + spanResults.size());
                    spanOverlapResultDisplayMap.put(aSpan, spanResults);
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

//        return spanOverlapResultMap;
        return spanOverlapResultDisplayMap;
    }

    /**
     * A method that compensates for previous design defect. From the webpage, the experiments are
     * passed to the db query logic, but the query works on submissions instead of experiments. Find
     * all the submissions under the experiments.
     *
     * @param exps experiments that user select
     * @return an array of submission DCCid
     */
    private static List<Integer> getSubmissionsByExperimentNames(String[] exps, InterMineAPI im) {

        Set<Integer> subSet = new HashSet<Integer>();

        try {
            Query q = new Query();
            q.setDistinct(true);

            QueryClass qcExperiment = new QueryClass(Experiment.class);
            QueryClass qcSubmission = new QueryClass(Submission.class);

            q.addFrom(qcExperiment);
            q.addFrom(qcSubmission);

            QueryField qfDCCid = new QueryField(qcSubmission, "DCCid");
            QueryField qfExpName = new QueryField(qcExperiment, "name");
            q.addToSelect(qfDCCid);

            ConstraintSet constraints = new ConstraintSet(ConstraintOp.AND);

            q.setConstraint(constraints);

            // Experiment.id = Submission.experimentid
            QueryCollectionReference subs = new QueryCollectionReference(
                    qcExperiment, "submissions");
            ContainsConstraint ccSubs = new ContainsConstraint(subs, ConstraintOp.CONTAINS,
                    qcSubmission);
            constraints.addConstraint(ccSubs);

            // Experiment.name in a list
            constraints.addConstraint(new BagConstraint(qfExpName,
                    ConstraintOp.IN, Arrays.asList(exps)));

            Results results = im.getObjectStore().execute(q);

            // for submission, get DCCid
            Iterator<?> i = results.iterator();
            while (i.hasNext()) {
                ResultsRow<?> row = (ResultsRow<?>) i.next();
                Integer subDCCid = (Integer) row.get(0);
                subSet.add(subDCCid);
            }

        } catch (Exception err) {
            err.printStackTrace();
        }

        List<Integer> subKeys = new ArrayList<Integer>(subSet);

        return subKeys;
    }
}
