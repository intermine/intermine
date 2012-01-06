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
    private static final Logger LOG = Logger
            .getLogger(SubmissionOverlapsAction.class);

    /**
     * Action for creating a bag of InterMineObjects or Strings from identifiers
     * in text field.
     *
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws an exception
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        final InterMineAPI im = SessionMethods.getInterMineAPI(request
                .getSession());
        ObjectStore os = im.getObjectStore();

        SubmissionOverlapsForm submissionOverlapsForm = (SubmissionOverlapsForm) form;

        String submissionTitle = submissionOverlapsForm.getSubmissionTitle();
        String submissionId = submissionOverlapsForm.getSubmissionId();

        PathQuery q = new PathQuery(os.getModel());

        if (request.getParameter("overlaps") != null) {
            String featureType = submissionOverlapsForm.getOverlapFeatureType();
            String findFeatureType = submissionOverlapsForm
                    .getOverlapFindType();
            String description = "Results of searching for " + featureType
                    + "s generated from DCC" + " submission " + submissionTitle
                    + " that overlap " + findFeatureType + "s.";
            q.setDescription(description);

            q.addView(findFeatureType + ".primaryIdentifier");
            q.addView(findFeatureType
                    + ".overlappingFeatures.secondaryIdentifier");
            q.addView(findFeatureType + ".chromosomeLocation.start");
            q.addView(findFeatureType + ".chromosomeLocation.end");
            q.addView(findFeatureType + ".chromosomeLocation.strand");

            if ("Exon".equals(findFeatureType)) {
                q.addView(findFeatureType + ".gene.primaryIdentifier");
            }

            q.addConstraint(Constraints.type(findFeatureType
                    + ".overlappingFeatures", featureType));
            q.addConstraint(Constraints
                    .eq(findFeatureType
                            + ".overlappingFeatures.submissions.title",
                            submissionTitle));

        } else if (request.getParameter("flanking") != null) {
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
                q.addConstraint(Constraints.inIds("Gene",
                        queryResultsMap.get("geneIdSet")));
                q.addConstraint(Constraints.inIds("Gene.overlappingFeatures",
                        queryResultsMap.get("sequenceFeatureIdSet")));
            }

            q.addConstraint(Constraints.eq("Gene.organism.shortName", organismShortName));
            q.addConstraint(Constraints.eq(
                    "Gene.overlappingFeatures.organism.shortName",
                    organismShortName));
            q.addConstraint(Constraints.eq(
                    "Gene.overlappingFeatures.submissions.id", submissionId));
        }

        String qid = SessionMethods.startQueryWithTimeout(request, false, q);
        Thread.sleep(200);

        String trail = "|" + submissionId;

        return new ForwardParameters(mapping.findForward("waiting"))
                .addParameter("qid", qid).addParameter("trail", trail)
                .forward();
    }

    private Map<String, Set<Integer>> getOverlappingGenes(
            SubmissionOverlapsForm submissionOverlapsForm, InterMineAPI im)
        throws ObjectStoreException, ClassNotFoundException {

        String direction = submissionOverlapsForm.getDirection();
        String distance = submissionOverlapsForm.getDistance();
        String featureType = submissionOverlapsForm.getFlankingFeatureType();
        String submissionId = submissionOverlapsForm.getSubmissionId();

        Submission submission = (Submission) im.getObjectStore().getObjectById(
                Integer.valueOf(submissionId));
        int organismId = submission.getOrganism().getId();

        int flankingSize = 0;
        int beforeStartOfGene = 0;
        int afterEndOfGene = 0;

        if ("0.5kb".equals(distance)) {
            flankingSize = 500;
        }
        if ("1.0kb".equals(distance)) {
            flankingSize = 1000;
        }
        if ("2.0kb".equals(distance)) {
            flankingSize = 2000;
        }
        if ("5.0kb".equals(distance)) {
            flankingSize = 5000;
        }
        if ("10.0kb".equals(distance)) {
            flankingSize = 10000;
        }

        if ("bothways".equalsIgnoreCase(direction)) {
            beforeStartOfGene = flankingSize;
            afterEndOfGene = flankingSize;
        }
        if ("upstream".equalsIgnoreCase(direction)) {
            beforeStartOfGene = flankingSize;
        }
        if ("downstream".equalsIgnoreCase(direction)) {
            afterEndOfGene = flankingSize;
        }

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

        LOG.info("OVERLAP " + ob.generateSql(query));
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
        return queryResultsMap;
    }
}
