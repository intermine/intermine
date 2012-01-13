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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Submission;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.OverlapConstraint;
import org.intermine.objectstore.query.OverlapRange;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryForeignKey;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.ForwardParameters;
import org.intermine.web.struts.InterMineAction;

/**
 * Generate queries for overlaps of submission features and overlaps with gene
 * flanking regions.
 *
 * @author Richard Smith
 *
 */
public class SubmissionOverlapsAction extends InterMineAction
{
    private static final Logger LOG = Logger.getLogger(SubmissionOverlapsAction.class);

    /**
     * Action for creating a bag of InterMineObjects or Strings from identifiers
     * in text field.
     *
     * @param mapping        The ActionMapping used to select this instance
     * @param form           The optional ActionForm bean for this request (if any)
     * @param request        The HTTP request we are processing
     * @param response       The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception  if the application business logic throws an exception
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
                    throws Exception {
        final InterMineAPI im = SessionMethods.getInterMineAPI(request
                .getSession());
        ObjectStore os = im.getObjectStore();

        long bT = System.currentTimeMillis();     // to monitor time spent in the process
        SubmissionOverlapsForm submissionOverlapsForm = (SubmissionOverlapsForm) form;
        String submissionId = submissionOverlapsForm.getSubmissionId();

        PathQuery q = new PathQuery(os.getModel());

        if (request.getParameter("overlaps") != null) {
            getOverlappingFeatures(submissionOverlapsForm, q, im);
        } else if (request.getParameter("flanking") != null) {
            getOverlappingFeatures(submissionOverlapsForm, q, im);
        } else if (request.getParameter("flanking2") != null) {
            getOverlappingFeatures(submissionOverlapsForm, q, im);
        }

        String qid = SessionMethods.startQueryWithTimeout(request, false, q);
        Thread.sleep(200);

        String trail = "|" + submissionId;

        LOG.info("OVERLAP:TOTALTIME: execute: " + (System.currentTimeMillis() - bT) + " ms");

        return new ForwardParameters(mapping.findForward("waiting")).addParameter("qid", qid)
                .addParameter("trail", trail).forward();
    }

    /**
     * @param submissionOverlapsForm
     * @param submissionTitle
     * @param q
     */
    private void getOverlappingFeatures(
            SubmissionOverlapsForm submissionOverlapsForm, PathQuery q, InterMineAPI im)
                    throws ObjectStoreException, ClassNotFoundException {

        String submissionTitle = submissionOverlapsForm.getSubmissionTitle();
        String givenFeatureType = submissionOverlapsForm.getOverlapFeatureType();
        String overlapFeatureType = submissionOverlapsForm.getOverlapFindType();
        String description = "Results of searching for " + overlapFeatureType
                + "s that overlap" + givenFeatureType + "s generated by submission "
                + submissionTitle +".";
        q.setDescription(description);

        q.addView(overlapFeatureType + ".primaryIdentifier");
        q.addView(overlapFeatureType + ".chromosomeLocation.start");
        q.addView(overlapFeatureType + ".chromosomeLocation.end");
        q.addView(overlapFeatureType + ".chromosomeLocation.strand");

        if ("Exon".equals(overlapFeatureType)) {
            q.addView(overlapFeatureType + ".gene.primaryIdentifier");
        }

        q.addConstraint(Constraints.inIds(overlapFeatureType,
        getOverlappingFeaturesId(submissionOverlapsForm, im)));
    }



    private Set<Integer> getOverlappingFeaturesId(
            SubmissionOverlapsForm submissionOverlapsForm, InterMineAPI im)
                    throws ObjectStoreException, ClassNotFoundException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        String direction = submissionOverlapsForm.getDirection();
        String distance = submissionOverlapsForm.getDistance();

        String featureType = submissionOverlapsForm.getOverlapFeatureType();  //GF
        String findFeatureType = submissionOverlapsForm.getOverlapFindType(); //OF--gene
        String submissionId = submissionOverlapsForm.getSubmissionId();

        LOG.info("OVERLAP FORM distance: " + distance);
        LOG.info("OVERLAP FORM direction: " + direction);
        LOG.info("OVERLAP FORM overlap feat: " + findFeatureType);
        LOG.info("OVERLAP FORM given feat: " + featureType);


        Submission submission = (Submission) im.getObjectStore().getObjectById(
                Integer.valueOf(submissionId));
        int organismId = submission.getOrganism().getId();

        int beforeStartOfOF = getLeftMargin(direction, distance);
        int afterEndOfOF = getRightMargin(direction, distance);

        String modelPackName = im.getModel().getPackageName();
        Class<?> featureCls = Class.forName(modelPackName + "." + featureType);
        Class<?> overlapFeatureCls = Class.forName(modelPackName + "." + findFeatureType);

        Query query = new Query();
        query.setDistinct(false);

        QueryClass qcGivenFeature = new QueryClass(featureCls);
        QueryClass qcOverlapFeature = new QueryClass(overlapFeatureCls);
        QueryClass qcGivenFeatureLoc = new QueryClass(Location.class);
        QueryClass qcOverlapFeatureLoc = new QueryClass(Location.class);
        //        QueryField qfSequenceFeatureId = new QueryField(qcSequenceFeature, "id");
        QueryField qfOFId = new QueryField(qcOverlapFeature, "id");
        query.addToSelect(qfOFId);
        //        query.addToSelect(qfSequenceFeatureId);

        query.addFrom(qcGivenFeature);
        query.addFrom(qcOverlapFeature);
        query.addFrom(qcGivenFeatureLoc);
        query.addFrom(qcOverlapFeatureLoc);

        ConstraintSet constraints = new ConstraintSet(ConstraintOp.AND);
        query.setConstraint(constraints);

        QueryObjectReference locOFSubject = new QueryObjectReference(
                qcOverlapFeatureLoc, "feature");
        ContainsConstraint ccLocOFSubject = new ContainsConstraint(
                locOFSubject, ConstraintOp.CONTAINS, qcOverlapFeature);
        constraints.addConstraint(ccLocOFSubject);

        QueryObjectReference locGFSubject = new QueryObjectReference(
                qcGivenFeatureLoc, "feature");
        ContainsConstraint ccLocGFSubject = new ContainsConstraint(
                locGFSubject, ConstraintOp.CONTAINS,
                qcGivenFeature);
        constraints.addConstraint(ccLocGFSubject);

        // SequenceFeaure.chromosome = Gene.chromosome
        constraints.addConstraint(new SimpleConstraint(new QueryForeignKey(
                qcOverlapFeature, "chromosome"), ConstraintOp.EQUALS,
                new QueryForeignKey(qcGivenFeature, "chromosome")));

        // SequenceFeaure.organism = Gene.organism
        constraints.addConstraint(new SimpleConstraint(new QueryForeignKey(
                qcOverlapFeature, "organism"), ConstraintOp.EQUALS, new QueryForeignKey(
                        qcGivenFeature, "organism")));

        SimpleConstraint scOrg = new SimpleConstraint(new QueryForeignKey(
                qcOverlapFeature, "organism"), ConstraintOp.EQUALS, new QueryValue(
                        organismId));
        constraints.addConstraint(scOrg);

        QueryCollectionReference qcrSubmission = new QueryCollectionReference(
                submission, "features");
        constraints.addConstraint(new ContainsConstraint(qcrSubmission,
                ConstraintOp.CONTAINS, qcGivenFeature));

        // Sequence feature location chromosome reference
        QueryObjectReference givenFeatureLocatedOnRef = new QueryObjectReference(
                qcGivenFeatureLoc, "locatedOn");

        // Gene location chromosome reference
        QueryObjectReference overlapFeatureLocatedOnRef = new QueryObjectReference(
                qcOverlapFeatureLoc, "locatedOn");

        OverlapRange givenFeatureRange = new OverlapRange(new QueryField(
                qcGivenFeatureLoc, "start"), new QueryField(
                        qcGivenFeatureLoc, "end"), givenFeatureLocatedOnRef);

        OverlapRange overlapFeatureRange = new OverlapRange(new QueryExpression(
                new QueryField(qcOverlapFeatureLoc, "start"), QueryExpression.SUBTRACT,
                new QueryValue(beforeStartOfOF)), new QueryExpression(
                        new QueryField(qcOverlapFeatureLoc, "end"), QueryExpression.ADD,
                        new QueryValue(afterEndOfOF)), overlapFeatureLocatedOnRef);

        OverlapConstraint oc = new OverlapConstraint(givenFeatureRange,
                ConstraintOp.OVERLAPS, overlapFeatureRange);

        constraints.addConstraint(oc);

        ObjectStoreInterMineImpl ob = (ObjectStoreInterMineImpl) im.getObjectStore();

        LOG.info("OVERLAP " + ob.generateSql(query));

//        Results results = im.getObjectStore().execute(query, 100000,true, false, true);
        SingletonResults results = im.getObjectStore()
                .executeSingleton(query, 100000,true, false, true);
        if (results == null || results.isEmpty()) {
            return null;
        }
        Set<Integer> overlapFeatureIdSet = new HashSet<Integer>();
        for (Iterator<?> iter = results.iterator(); iter.hasNext();) {
            overlapFeatureIdSet.add((Integer)iter.next());
//            ResultsRow<?> row = (ResultsRow<?>) iter.next();
//            geneIdSet.add((Integer) row.get(0));
        }

        LOG.info("OVERLAP TIME: " + (System.currentTimeMillis() - bT) + " ms");
        return overlapFeatureIdSet;
    }


    /**
     * @param distance
     * @return
     */
    private int getFlankingSize(String distance) {
        if ("0.5kb".equals(distance)) {
            return 500;
        }
        if ("1.0kb".equals(distance)) {
            return 1000;
        }
        if ("2.0kb".equals(distance)) {
            return 2000;
        }
        if ("5.0kb".equals(distance)) {
            return 5000;
        }
        if ("10.0kb".equals(distance)) {
            return 10000;
        }
        return 0;
    }
    /**
     * @param distance
     * @param direction
     * @return
     */
    private int getLeftMargin(String direction, String distance) {

        if ("bothways".equalsIgnoreCase(direction)) {
            int beforeStartOfGene = getFlankingSize(distance);
            return beforeStartOfGene;
        }
        if ("upstream".equalsIgnoreCase(direction)) {
            int beforeStartOfGene = getFlankingSize(distance);
            return beforeStartOfGene;
        }
        return 0;
    }

    /**
     * @param distance
     * @param direction
     * @return
     */
    private int getRightMargin(String direction, String distance) {

        if ("bothways".equalsIgnoreCase(direction)) {
            int afterEndOfGene = getFlankingSize(distance);
            return afterEndOfGene;
        }
        if ("downstream".equalsIgnoreCase(direction)) {
            int afterEndOfGene = getFlankingSize(distance);
            return afterEndOfGene;
        }
        return 0;
    }


/* ---------------------------------------------------------- */

    /**
     * TO SAVE: it is about as fast as the single set one
    *  if we can improve the pathquery speed...
    *  */

    private Map<String, Set<Integer>> getOverlappingGenes(
            SubmissionOverlapsForm submissionOverlapsForm, InterMineAPI im)
                    throws ObjectStoreException, ClassNotFoundException {

        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        String direction = submissionOverlapsForm.getDirection();
        String distance = submissionOverlapsForm.getDistance();
        String featureType = submissionOverlapsForm.getFlankingFeatureType();
        String submissionId = submissionOverlapsForm.getSubmissionId();

        Submission submission = (Submission) im.getObjectStore().getObjectById(
                Integer.valueOf(submissionId));
        int organismId = submission.getOrganism().getId();

        int beforeStartOfGene = getLeftMargin(direction, distance);
        int afterEndOfGene = getRightMargin(direction, distance);

        String modelPackName = im.getModel().getPackageName();
        Class<?> featureCls = Class.forName(modelPackName + "." + featureType);

        Query query = new Query();
        query.setDistinct(true);

        QueryClass qcSequenceFeature = new QueryClass(featureCls);
        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcSequenceFeatureLoc = new QueryClass(Location.class);
        QueryClass qcGeneLoc = new QueryClass(Location.class);
        // QueryClass qcSubmission = new QueryClass(Submission.class);
        // QueryClass qcOrg = new QueryClass(Organism.class);
        // QueryClass qcChr = new QueryClass(Chromosome.class);

        //        QueryField qfSequenceFeatureSecondaryIdentifier = new QueryField(
        //            qcSequenceFeature, "secondaryIdentifier");
        QueryField qfSequenceFeatureId = new QueryField(qcSequenceFeature,
                "id");
        // QueryField qfGenePId = new QueryField(qcGene, "primaryIdentifier");
        QueryField qfGeneId = new QueryField(qcGene, "id");
        // QueryField qfSequenceFeatureClass = new QueryField(qcSequenceFeature,
        // "class");
        // QueryField qfSubmissionId = new QueryField(qcSubmission, "id");
        // QueryField qfChrPId = new QueryField(qcChr, "primaryIdentifier");

        // query.addToSelect(qfGenePId);
        query.addToSelect(qfGeneId);
        //        query.addToSelect(qfSequenceFeatureSecondaryIdentifier);
        query.addToSelect(qfSequenceFeatureId);

        query.addFrom(qcSequenceFeature);
        query.addFrom(qcGene);
        query.addFrom(qcSequenceFeatureLoc);
        query.addFrom(qcGeneLoc);

        // query.addToOrderBy(qfGenePId, "ascending");

        ConstraintSet constraints = new ConstraintSet(ConstraintOp.AND);

        query.setConstraint(constraints);

        QueryObjectReference locGeneSubject = new QueryObjectReference(
                qcGeneLoc, "feature");
        ContainsConstraint ccLocGeneSubject = new ContainsConstraint(
                locGeneSubject, ConstraintOp.CONTAINS, qcGene);
        constraints.addConstraint(ccLocGeneSubject);

        QueryObjectReference locSequenceFeatureSubject = new QueryObjectReference(
                qcSequenceFeatureLoc, "feature");
        ContainsConstraint ccLocSequenceFeatureSubject = new ContainsConstraint(
                locSequenceFeatureSubject, ConstraintOp.CONTAINS,
                qcSequenceFeature);
        constraints.addConstraint(ccLocSequenceFeatureSubject);

        // SequenceFeaure.chromosome = Gene.chromosome
        constraints.addConstraint(new SimpleConstraint(new QueryForeignKey(
                qcGene, "chromosome"), ConstraintOp.EQUALS,
                new QueryForeignKey(qcSequenceFeature, "chromosome")));

        // SequenceFeaure.organism = Gene.organism
        constraints.addConstraint(new SimpleConstraint(new QueryForeignKey(
                qcGene, "organism"), ConstraintOp.EQUALS, new QueryForeignKey(
                        qcSequenceFeature, "organism")));

        SimpleConstraint scOrg = new SimpleConstraint(new QueryForeignKey(
                qcGene, "organism"), ConstraintOp.EQUALS, new QueryValue(
                        organismId));
        constraints.addConstraint(scOrg);

        // SequenceFeature.submissions = Submission
        // QueryCollectionReference submission = new
        // QueryCollectionReference(qcSequenceFeature,
        // "submissions");
        // ContainsConstraint ccSubmission = new ContainsConstraint(submission,
        // ConstraintOp.CONTAINS, qcSubmission);
        // constraints.addConstraint(ccSubmission);

        QueryCollectionReference qcrSubmission = new QueryCollectionReference(
                submission, "features");
        constraints.addConstraint(new ContainsConstraint(qcrSubmission,
                ConstraintOp.CONTAINS, qcSequenceFeature));

        // Submission.id = submissionId
        // constraints.addConstraint(new SimpleConstraint(qfSubmissionId,
        // ConstraintOp.EQUALS, new QueryValue(Integer
        // .valueOf(submissionId))));

        // Sequence feature location chromosome reference
        QueryObjectReference sequenceFeatureLocatedOnRef = new QueryObjectReference(
                qcSequenceFeatureLoc, "locatedOn");

        // Gene location chromosome reference
        QueryObjectReference geneLocatedOnRef = new QueryObjectReference(
                qcGeneLoc, "locatedOn");

        OverlapRange overlapInput = new OverlapRange(new QueryField(
                qcSequenceFeatureLoc, "start"), new QueryField(
                        qcSequenceFeatureLoc, "end"), sequenceFeatureLocatedOnRef);

        OverlapRange overlapFeature = new OverlapRange(new QueryExpression(
                new QueryField(qcGeneLoc, "start"), QueryExpression.SUBTRACT,
                new QueryValue(beforeStartOfGene)), new QueryExpression(
                        new QueryField(qcGeneLoc, "end"), QueryExpression.ADD,
                        new QueryValue(afterEndOfGene)), geneLocatedOnRef);

        OverlapConstraint oc = new OverlapConstraint(overlapInput,
                ConstraintOp.OVERLAPS, overlapFeature);

        constraints.addConstraint(oc);

        ObjectStoreInterMineImpl ob = (ObjectStoreInterMineImpl) im
                .getObjectStore();

        LOG.info("OVERLAPGENE FULL " + ob.generateSql(query));

        Results results = im.getObjectStore().execute(query);

        Map<String, Set<Integer>> queryResultsMap = new HashMap<String, Set<Integer>>();
        if (results == null || results.isEmpty()) {
            queryResultsMap = null;
        } else {
            Set<Integer> geneIdSet = new HashSet<Integer>();
            Set<Integer> sequenceFeatureIdSet = new HashSet<Integer>();
            for (Iterator<?> iter = results.iterator(); iter.hasNext();) {
                ResultsRow<?> row = (ResultsRow<?>) iter.next();
                geneIdSet.add((Integer) row.get(0));
                sequenceFeatureIdSet.add((Integer) row.get(1));
            }

            queryResultsMap.put("geneIdSet", geneIdSet);
            queryResultsMap.put("sequenceFeatureIdSet", sequenceFeatureIdSet);
        }
        LOG.info("OVERLAPGENE FULL TIME: " + (System.currentTimeMillis() - bT) + " ms");
        return queryResultsMap;
    }

    /**
     * @param im
     * @param submissionOverlapsForm
     * @param submissionId
     * @param q
     * @throws ObjectStoreException
     * @throws ClassNotFoundException
     */
    private void getFlankingGenesWithFeatures(final InterMineAPI im,
            SubmissionOverlapsForm submissionOverlapsForm, String submissionId,
            PathQuery q) throws ObjectStoreException, ClassNotFoundException {
        Map<String, Set<Integer>> queryResultsMap = getOverlappingGenes(
                submissionOverlapsForm, im);

        Submission submission = (Submission) im.getObjectStore().getObjectById(
                Integer.valueOf(submissionId));
        String organismShortName = submission.getOrganism().getShortName();

        // PathQuery
        q.addViews("Gene.overlappingFeatures.primaryIdentifier", "Gene.primaryIdentifier");

        if (queryResultsMap == null) {
            q.addConstraint(Constraints.lookup("Gene", "", ""));
            q.addConstraint(Constraints.lookup("Gene.overlappingFeatures", "", ""));
        } else {
            q.addConstraint(Constraints.inIds("Gene", queryResultsMap.get("geneIdSet")));
            q.addConstraint(Constraints.inIds("Gene.overlappingFeatures",
                    queryResultsMap.get("sequenceFeatureIdSet")));
        }

        q.addConstraint(Constraints.eq("Gene.organism.shortName", organismShortName));
        q.addConstraint(Constraints.eq("Gene.overlappingFeatures.organism.shortName",
                organismShortName));
        q.addConstraint(Constraints.eq("Gene.overlappingFeatures.submissions.id", submissionId));
    }

    /* TO RM ===============================================================*/



    /**
     * @param im
     * @param submissionOverlapsForm
     * @param submissionId
     * @param q
     * @throws ObjectStoreException
     * @throws ClassNotFoundException
     */
    private void getFlankingGenes(final InterMineAPI im,
            SubmissionOverlapsForm submissionOverlapsForm, PathQuery q)
                    throws ObjectStoreException, ClassNotFoundException {

        // PathQuery
        q.addViews("Gene.primaryIdentifier", "Gene.symbol");
        q.addConstraint(Constraints.inIds("Gene",
                getOverlappingGenesId(submissionOverlapsForm, im)));
    }



    private Set<Integer> getOverlappingGenesId(
            SubmissionOverlapsForm submissionOverlapsForm, InterMineAPI im)
                    throws ObjectStoreException, ClassNotFoundException {

        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        String direction = submissionOverlapsForm.getDirection();
        String distance = submissionOverlapsForm.getDistance();
        String featureType = submissionOverlapsForm.getFlankingFeatureType();
        String submissionId = submissionOverlapsForm.getSubmissionId();
        Submission submission = (Submission) im.getObjectStore().getObjectById(
                Integer.valueOf(submissionId));
        int organismId = submission.getOrganism().getId();

        int beforeStartOfGene = getLeftMargin(direction, distance);
        int afterEndOfGene = getRightMargin(direction, distance);

        String modelPackName = im.getModel().getPackageName();
        Class<?> featureCls = Class.forName(modelPackName + "." + featureType);

        Query query = new Query();
        query.setDistinct(false);

        QueryClass qcSequenceFeature = new QueryClass(featureCls);
        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcSequenceFeatureLoc = new QueryClass(Location.class);
        QueryClass qcGeneLoc = new QueryClass(Location.class);
        //        QueryField qfSequenceFeatureId = new QueryField(qcSequenceFeature, "id");
        QueryField qfGeneId = new QueryField(qcGene, "id");
        query.addToSelect(qfGeneId);
        //        query.addToSelect(qfSequenceFeatureId);

        query.addFrom(qcSequenceFeature);
        query.addFrom(qcGene);
        query.addFrom(qcSequenceFeatureLoc);
        query.addFrom(qcGeneLoc);

        ConstraintSet constraints = new ConstraintSet(ConstraintOp.AND);
        query.setConstraint(constraints);

        QueryObjectReference locGeneSubject = new QueryObjectReference(
                qcGeneLoc, "feature");
        ContainsConstraint ccLocGeneSubject = new ContainsConstraint(
                locGeneSubject, ConstraintOp.CONTAINS, qcGene);
        constraints.addConstraint(ccLocGeneSubject);

        QueryObjectReference locSequenceFeatureSubject = new QueryObjectReference(
                qcSequenceFeatureLoc, "feature");
        ContainsConstraint ccLocSequenceFeatureSubject = new ContainsConstraint(
                locSequenceFeatureSubject, ConstraintOp.CONTAINS,
                qcSequenceFeature);
        constraints.addConstraint(ccLocSequenceFeatureSubject);

        // SequenceFeaure.chromosome = Gene.chromosome
        constraints.addConstraint(new SimpleConstraint(new QueryForeignKey(
                qcGene, "chromosome"), ConstraintOp.EQUALS,
                new QueryForeignKey(qcSequenceFeature, "chromosome")));

        // SequenceFeaure.organism = Gene.organism
        constraints.addConstraint(new SimpleConstraint(new QueryForeignKey(
                qcGene, "organism"), ConstraintOp.EQUALS, new QueryForeignKey(
                        qcSequenceFeature, "organism")));

        SimpleConstraint scOrg = new SimpleConstraint(new QueryForeignKey(
                qcGene, "organism"), ConstraintOp.EQUALS, new QueryValue(
                        organismId));
        constraints.addConstraint(scOrg);

        QueryCollectionReference qcrSubmission = new QueryCollectionReference(
                submission, "features");
        constraints.addConstraint(new ContainsConstraint(qcrSubmission,
                ConstraintOp.CONTAINS, qcSequenceFeature));

        // Sequence feature location chromosome reference
        QueryObjectReference sequenceFeatureLocatedOnRef = new QueryObjectReference(
                qcSequenceFeatureLoc, "locatedOn");

        // Gene location chromosome reference
        QueryObjectReference geneLocatedOnRef = new QueryObjectReference(
                qcGeneLoc, "locatedOn");

        OverlapRange overlapInput = new OverlapRange(new QueryField(
                qcSequenceFeatureLoc, "start"), new QueryField(
                        qcSequenceFeatureLoc, "end"), sequenceFeatureLocatedOnRef);

        OverlapRange overlapFeature = new OverlapRange(new QueryExpression(
                new QueryField(qcGeneLoc, "start"), QueryExpression.SUBTRACT,
                new QueryValue(beforeStartOfGene)), new QueryExpression(
                        new QueryField(qcGeneLoc, "end"), QueryExpression.ADD,
                        new QueryValue(afterEndOfGene)), geneLocatedOnRef);

        OverlapConstraint oc = new OverlapConstraint(overlapInput,
                ConstraintOp.OVERLAPS, overlapFeature);

        constraints.addConstraint(oc);

        ObjectStoreInterMineImpl ob = (ObjectStoreInterMineImpl) im.getObjectStore();

        LOG.info("OVERLAP GENE: " + ob.generateSql(query));

        Results results = im.getObjectStore().execute(query, 100000,true, false, true);
//        SingletonResults results = im.getObjectStore()
//                .executeSingleton(query, 100000,true, false, true);
        if (results == null || results.isEmpty()) {
            return null;
        }
        Set<Integer> geneIdSet = new HashSet<Integer>();
        for (Iterator<?> iter = results.iterator(); iter.hasNext();) {
//            geneIdSet.add((Integer)iter.next());
            ResultsRow<?> row = (ResultsRow<?>) iter.next();
            geneIdSet.add((Integer) row.get(0));
        }

        LOG.info("OVERLAP GENE TIME: " + (System.currentTimeMillis() - bT) + " ms");
        return geneIdSet;
    }

    private Set<Integer> getOverlappingFeaturesIdOld(
            SubmissionOverlapsForm submissionOverlapsForm, InterMineAPI im)
                    throws ObjectStoreException, ClassNotFoundException {

        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        String featureType = submissionOverlapsForm.getOverlapFeatureType();  //GF
        String findFeatureType = submissionOverlapsForm.getOverlapFindType(); //OF

        String submissionId = submissionOverlapsForm.getSubmissionId();

        Submission submission = (Submission) im.getObjectStore().getObjectById(
                Integer.valueOf(submissionId));
        int organismId = submission.getOrganism().getId();

        String modelPackName = im.getModel().getPackageName();
        Class<?> featureCls = Class.forName(modelPackName + "." + featureType);
        Class<?> overlapFeatureCls = Class.forName(modelPackName + "." + findFeatureType);

        Query query = new Query();
        query.setDistinct(false);

        QueryClass qcGivenFeature = new QueryClass(featureCls);
        QueryClass qcOverlapFeature = new QueryClass(overlapFeatureCls);
        QueryClass qcGivenFeatureLoc = new QueryClass(Location.class);
        QueryClass qcOverlapFeatureLoc = new QueryClass(Location.class);
        //        QueryField qfSequenceFeatureId = new QueryField(qcSequenceFeature, "id");
        QueryField qfOFId = new QueryField(qcOverlapFeature, "id");
        query.addToSelect(qfOFId);
        //        query.addToSelect(qfSequenceFeatureId);

        query.addFrom(qcGivenFeature);
        query.addFrom(qcOverlapFeature);
        query.addFrom(qcGivenFeatureLoc);
        query.addFrom(qcOverlapFeatureLoc);

        ConstraintSet constraints = new ConstraintSet(ConstraintOp.AND);
        query.setConstraint(constraints);

        QueryObjectReference locOFSubject = new QueryObjectReference(
                qcOverlapFeatureLoc, "feature");
        ContainsConstraint ccLocOFSubject = new ContainsConstraint(
                locOFSubject, ConstraintOp.CONTAINS, qcOverlapFeature);
        constraints.addConstraint(ccLocOFSubject);

        QueryObjectReference locGFSubject = new QueryObjectReference(
                qcGivenFeatureLoc, "feature");
        ContainsConstraint ccLocGFSubject = new ContainsConstraint(
                locGFSubject, ConstraintOp.CONTAINS,
                qcGivenFeature);
        constraints.addConstraint(ccLocGFSubject);

        // SequenceFeaure.chromosome = Gene.chromosome
        constraints.addConstraint(new SimpleConstraint(new QueryForeignKey(
                qcOverlapFeature, "chromosome"), ConstraintOp.EQUALS,
                new QueryForeignKey(qcGivenFeature, "chromosome")));

        // SequenceFeaure.organism = Gene.organism
        constraints.addConstraint(new SimpleConstraint(new QueryForeignKey(
                qcOverlapFeature, "organism"), ConstraintOp.EQUALS, new QueryForeignKey(
                        qcGivenFeature, "organism")));

        SimpleConstraint scOrg = new SimpleConstraint(new QueryForeignKey(
                qcOverlapFeature, "organism"), ConstraintOp.EQUALS, new QueryValue(
                        organismId));
        constraints.addConstraint(scOrg);

        QueryCollectionReference qcrSubmission = new QueryCollectionReference(
                submission, "features");
        constraints.addConstraint(new ContainsConstraint(qcrSubmission,
                ConstraintOp.CONTAINS, qcGivenFeature));

        // Sequence feature location chromosome reference
        QueryObjectReference givenFeatureLocatedOnRef = new QueryObjectReference(
                qcGivenFeatureLoc, "locatedOn");

        // Gene location chromosome reference
        QueryObjectReference overlapFeatureLocatedOnRef = new QueryObjectReference(
                qcOverlapFeatureLoc, "locatedOn");

        OverlapRange givenFeatureRange = new OverlapRange(new QueryField(
                qcGivenFeatureLoc, "start"), new QueryField(
                        qcGivenFeatureLoc, "end"), givenFeatureLocatedOnRef);

        OverlapRange overlapFeatureRange = new OverlapRange(new QueryField(
                qcOverlapFeatureLoc, "start"), new QueryField(
                        qcOverlapFeatureLoc, "end"), overlapFeatureLocatedOnRef);

        OverlapConstraint oc = new OverlapConstraint(givenFeatureRange,
                ConstraintOp.OVERLAPS, overlapFeatureRange);

        constraints.addConstraint(oc);

        ObjectStoreInterMineImpl ob = (ObjectStoreInterMineImpl) im.getObjectStore();

        LOG.info("OVERLAP " + ob.generateSql(query));

//        Results results = im.getObjectStore().execute(query, 100000,true, false, false);
        SingletonResults results = im.getObjectStore()
                .executeSingleton(query, 100000,true, false, true);
        if (results == null || results.isEmpty()) {
            return null;
        }
        Set<Integer> overlapFeatureIdSet = new HashSet<Integer>();
        for (Iterator<?> iter = results.iterator(); iter.hasNext();) {
            overlapFeatureIdSet.add((Integer)iter.next());
//            ResultsRow<?> row = (ResultsRow<?>) iter.next();
//            geneIdSet.add((Integer) row.get(0));
        }

        LOG.info("OVERLAP TIME: " + (System.currentTimeMillis() - bT) + " ms");
        return overlapFeatureIdSet;
    }


}
