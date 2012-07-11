package org.modmine.web.logic;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.bio.web.model.CytoscapeNetworkEdgeData;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.metadata.Model;
import org.intermine.model.bio.Chromosome;
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
import org.modmine.web.ChromosomeInfo;
import org.modmine.web.GBrowseParser.GBrowseTrack;
import org.modmine.web.GBrowseTrackInfo;
import org.modmine.web.MetadataCache;
import org.modmine.web.model.SpanQueryResultRow;
import org.modmine.web.model.SpanUploadConstraint;

/**
 * SpanOverlapQuery is a class with query logic. It has two methods: runSpanValidationQuery queries
 * the information of all the organisms and their chromosomes' names and length for span validation
 * use; runSpanOverlapQuery queries LocatedSequenceFeatures that overlap users' spans.
 *
 * @author Fengyuan Hu
 *
 */
public class SpanOverlapQueryRunner implements Runnable
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(SpanOverlapQueryRunner.class);

    private String spanUUIDString = null;
    private List<GenomicRegion> spanList = null;
    @SuppressWarnings("rawtypes")
    private List<Class> ftKeys = null;
    private List<String> subKeys = null;
    private String orgName = null;
    private InterMineAPI im = null;
    private HttpServletRequest request = null;

    private static Map<String, List<ChromosomeInfo>> chrInfoMap = null;

    /**
     * Constructor
     * @param spanUUIDString UUID for one spanUpload
     * @param spanList list of uploaded spans
     * @param ftKeys features
     * @param subKeys submissions
     * @param orgName organism name
     * @param im intermineAPI
     * @param request http request
     */
    public SpanOverlapQueryRunner(String spanUUIDString, List<GenomicRegion> spanList,
            @SuppressWarnings("rawtypes") List<Class> ftKeys, List<String> subKeys, String orgName,
            InterMineAPI im, HttpServletRequest request) {
        this.spanUUIDString = spanUUIDString;
        this.spanList = spanList;
        this.ftKeys = ftKeys;
        this.subKeys = subKeys;
        this.orgName = orgName;
        this.im = im;
        this.request = request;
    }

    /**
     * Query the relevant overlap region and additional information according to user's uploaded
     * spans.
     *
     * @throws InterruptedException e
     */
    public void runSpanOverlapQuery() throws InterruptedException {

        // Use spanConstraintMap to check whether the spanUpload is duplicated, the map is saved in
        // the session
        @SuppressWarnings("unchecked")
        Map<SpanUploadConstraint, String> spanConstraintMap =
            (HashMap<SpanUploadConstraint, String>)  request
            .getSession().getAttribute("spanConstraintMap");

        if (spanConstraintMap == null) {
            spanConstraintMap = new HashMap<SpanUploadConstraint, String>();
        }

        SpanUploadConstraint c = new SpanUploadConstraint();
        c.setFtKeys(ftKeys);
        c.setSubKeys(subKeys);
        c.setSpanList(spanList);
        c.setSpanOrgName(orgName);

        if (spanConstraintMap.size() == 0) {
            spanConstraintMap.put(c, spanUUIDString);
        } else {
            if (spanConstraintMap.containsKey(c)) {
                spanUUIDString = spanConstraintMap.get(c);
                request.setAttribute("spanUUIDString", spanUUIDString);
            } else {
                spanConstraintMap.put(c, spanUUIDString);
            }
        }

        request.getSession().setAttribute("spanConstraintMap", spanConstraintMap);
        request.setAttribute("spanQueryTotalCount", spanList.size());

        (new Thread(this)).start();
    }

    @Override
    public void run() {
        queryExecutor();
    }

    /**
     * The method to run all the queries.
     */
    @SuppressWarnings("rawtypes")
    private void queryExecutor() {

        // Use spanOverlapFullResultMap to store the data in the session
        @SuppressWarnings("unchecked")
        Map<String, Map<GenomicRegion, List<SpanQueryResultRow>>> spanOverlapFullResultMap =
             (Map<String, Map<GenomicRegion, List<SpanQueryResultRow>>>) request
                            .getSession().getAttribute("spanOverlapFullResultMap");

        if (spanOverlapFullResultMap == null) {
            spanOverlapFullResultMap =
                new HashMap<String, Map<GenomicRegion, List<SpanQueryResultRow>>>();
        }

        Map<GenomicRegion, List<SpanQueryResultRow>> spanOverlapResultDisplayMap = Collections
                .synchronizedMap(new LinkedHashMap<GenomicRegion, List<SpanQueryResultRow>>());

        // GBrowse track
        @SuppressWarnings("unchecked")
        Map<String, Map<GenomicRegion, LinkedHashMap<String, LinkedHashSet<GBrowseTrackInfo>>>>
        gbrowseFullTrackMap = (HashMap<String, Map<GenomicRegion, LinkedHashMap<String,
                LinkedHashSet<GBrowseTrackInfo>>>>) request.getSession()
                    .getAttribute("gbrowseFullTrackMap");

        if (gbrowseFullTrackMap == null) {
            gbrowseFullTrackMap = new HashMap<String, Map<GenomicRegion, LinkedHashMap<String,
            LinkedHashSet<GBrowseTrackInfo>>>>();
        }

        Map<GenomicRegion, LinkedHashMap<String, LinkedHashSet<GBrowseTrackInfo>>> gbrowseTrackMap =
            Collections.synchronizedMap(new LinkedHashMap<GenomicRegion, LinkedHashMap<String,
                        LinkedHashSet<GBrowseTrackInfo>>>());

        if (!spanOverlapFullResultMap.containsKey(spanUUIDString)) {
            spanOverlapFullResultMap.put(spanUUIDString, spanOverlapResultDisplayMap);
            request.getSession().setAttribute("spanOverlapFullResultMap", spanOverlapFullResultMap);

            gbrowseFullTrackMap.put(spanUUIDString, gbrowseTrackMap);
            request.getSession().setAttribute("gbrowseFullTrackMap", gbrowseFullTrackMap);

            try {
                Query q;
                for (GenomicRegion aSpan: spanList) {
                    q = new Query();
                    q.setDistinct(true);

                    String chrPID = aSpan.getChr();
                    Integer start = aSpan.getStart();
                    Integer end = aSpan.getEnd();

                    /*
                    >>>>> TEST CODE <<<<<
                    LOG.info("OrgName: " + orgName);
                    LOG.info("chrPID: " + chrPID);
                    LOG.info("start: " + start);
                    LOG.info("end: " + end);
                    LOG.info("FeatureTypes: " + ftKeys);
                    LOG.info("Submissions: " + subKeys);
                    >>>>> TEST CODE <<<<<
                    */

                    // DB tables
                    QueryClass qcOrg = new QueryClass(Organism.class);
                    QueryClass qcChr = new QueryClass(Chromosome.class);
                    QueryClass qcFeature = new QueryClass(SequenceFeature.class);
                    QueryClass qcLoc = new QueryClass(Location.class);
                    QueryClass qcSubmission = new QueryClass(Submission.class);

                    QueryField qfOrgName = new QueryField(qcOrg, "shortName");
                    QueryField qfChrPID = new QueryField(qcChr, "primaryIdentifier");
                    QueryField qfFeaturePID = new QueryField(qcFeature, "primaryIdentifier");
                    QueryField qfFeatureId = new QueryField(qcFeature, "id");
                    QueryField qfFeatureClass = new QueryField(qcFeature, "class");
                    QueryField qfSubmissionTitle = new QueryField(qcSubmission, "title");
                    QueryField qfSubmissionDCCid = new QueryField(qcSubmission, "DCCid");
                    QueryField qfChr = new QueryField(qcChr, "primaryIdentifier");
                    QueryField qfLocStart = new QueryField(qcLoc, "start");
                    QueryField qfLocEnd = new QueryField(qcLoc, "end");

                    q.addToSelect(qfFeatureId);
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

                    Results results = im.getObjectStore().execute(q);

                    /*
                    >>>>> TEST CODE <<<<<
                    LOG.info("Query: " + q.toString());
                    LOG.info("Result Size: " + results.size());
                    LOG.info("Result >>>>> " + results);
                    >>>>> TEST CODE <<<<<
                    */

                    List<SpanQueryResultRow> spanResults = new ArrayList<SpanQueryResultRow>();
                    if (results == null || results.isEmpty()) {
                        spanOverlapResultDisplayMap.put(aSpan, null);
                        gbrowseTrackMap.put(aSpan, null);
                    }
                    else {
                        for (Iterator<?> iter = results.iterator(); iter.hasNext();) {
                            ResultsRow<?> row = (ResultsRow<?>) iter.next();

                            SpanQueryResultRow aRow = new SpanQueryResultRow();
                            aRow.setFeatureId((Integer) row.get(0));
                            aRow.setFeaturePID((String) row.get(1));
                            aRow.setFeatureClass(((Class) row.get(2)).getSimpleName());
                            aRow.setChr((String) row.get(3));
                            aRow.setStart((Integer) row.get(4));
                            aRow.setEnd((Integer) row.get(5));
                            aRow.setSubDCCid((String) row.get(6));
                            aRow.setSubTitle((String) row.get(7));

                            spanResults.add(aRow);
                        }
                        spanOverlapResultDisplayMap.put(aSpan, spanResults);
                        gbrowseTrackMap.put(aSpan, getSubGbrowseTrack(spanResults)); // Gbrowse
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Query the information of all the organisms and their chromosomes' names and length. The
     * results is stored in a Map. The result data will be used to validate users' span data.
     * For each span, its chromosome must match the chrPID and range must not go beyond the length.
     *
     * @param im - the InterMineAPI
     * @return chrInfoMap - a HashMap with orgName as key and its chrInfo accordingly as value
     */
    public static synchronized Map<String, List<ChromosomeInfo>> getChrInfo(InterMineAPI im) {
        if (chrInfoMap == null) {
            runSpanValidationQuery(im);
        }
        return chrInfoMap;
    }

    private static void runSpanValidationQuery(InterMineAPI im) {

        // a Map contains orgName and its chrInfo accordingly
        // e.g. <D.Melanogaster, (D.Melanogaster, X, 5000)...>
        chrInfoMap = new HashMap<String, List<ChromosomeInfo>>();

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

                String org = (String) row.get(0);
                String chrPID = (String) row.get(1);
                Integer chrLength = (Integer) row.get(2);

                // Add orgName to HashSet to filter out duplication
                orgSet.add(org);

                if (chrLength != null) {
                    ChromosomeInfo chrInfo = new ChromosomeInfo();
                    chrInfo.setOrgName(org);
                    chrInfo.setChrPID(chrPID);
                    chrInfo.setChrLength(chrLength);

                    // Add ChromosomeInfo to Arraylist
                    chrInfoList.add(chrInfo);
                }
            }

            // Iterate orgSet and chrInfoList to put data in chrInfoMap which has the key as the
            // orgName and value as a ArrayList containing a list of chrInfo which has the same
            // orgName
            for (String o : orgSet) {

                // a List to store chrInfo for the same organism
                List<ChromosomeInfo> chrInfoSubList = new ArrayList<ChromosomeInfo>();

                for (ChromosomeInfo chrInfo : chrInfoList) {
                    if (o.equals(chrInfo.getOrgName())) {
                        chrInfoSubList.add(chrInfo);
                        chrInfoMap.put(o, chrInfoSubList);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private LinkedHashMap<String, LinkedHashSet<GBrowseTrackInfo>> getSubGbrowseTrack(
            List<SpanQueryResultRow> spanResults) {

        LinkedHashMap<String, LinkedHashSet<GBrowseTrackInfo>> subGTrack =
            new LinkedHashMap<String, LinkedHashSet<GBrowseTrackInfo>>();
        for (SpanQueryResultRow aRow : spanResults) {
            if (MetadataCache.getTracksByDccId(aRow.getSubDCCid()).size() > 0) {
                List<GBrowseTrack> trackList =
                    MetadataCache.getTracksByDccId(aRow.getSubDCCid());
                LinkedHashSet<GBrowseTrackInfo> trackInfoList =
                    new LinkedHashSet<GBrowseTrackInfo>();
                for (GBrowseTrack aTrack : trackList) {
                    GBrowseTrackInfo aTrackInfo = new GBrowseTrackInfo(
                            aTrack.getOrganism(), aTrack.getTrack(),
                            aTrack.getSubTrack(), aTrack.getDCCid());
                    trackInfoList.add(aTrackInfo);
                }
                subGTrack.put(aRow.getSubDCCid(), trackInfoList);
            }
        }

        return subGTrack;
    }
}
