package org.modmine.web;

/*
 * Copyright (C) 2002-2013 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.Model;
import org.intermine.model.bio.Location;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.OverlapConstraint;
import org.intermine.objectstore.query.OverlapRange;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
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
 * Generate queries for overlaps of list of features and overlaps with gene
 * flanking regions.
 *
 * @author
 *
 */
public class FeaturesOverlapsAction extends InterMineAction
{
    private static final Logger LOG = Logger.getLogger(FeaturesOverlapsAction.class);

    // TODO: find another way of accessing the bag
    // this one uses the bag sent to init by the controller (..)
    static InterMineBag imBag = null;

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

        public ActionForward execute(
                ActionMapping mapping,
                ActionForm form,
                HttpServletRequest request,
                HttpServletResponse response)
        throws Exception {
            HttpSession session = request.getSession();
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);

            ObjectStoreBag osBag = imBag.getOsb();

            // TODO: add a check: are all the element in the bag of the same organism?
            // if yes, get it and constraint
            // if no return error message?
            // at the moment not constraining globally, but
            // overlappingFeature.organism = overlappedFeature.organism
            //
            // note: i think this is valid only for lists of genes
            // String OrganismName = dabag.getContents().get(0).getExtra();
            LOG.debug("OVERLAP gfBag: " + imBag);

            Model model = im.getModel();

        long bT = System.currentTimeMillis();     // to monitor time spent in the process
        FeaturesOverlapsForm featuresOverlapsForm = (FeaturesOverlapsForm) form;

        PathQuery q = new PathQuery(model);

        buildOverlapPathQuery(request, featuresOverlapsForm, q, im, osBag);

        String qid = SessionMethods.startQueryWithTimeout(request, false, q);
        Thread.sleep(200);

        LOG.info("OVERLAP:TOTALTIME: execute: " + (System.currentTimeMillis() - bT) + " ms");

        return new ForwardParameters(mapping.findForward("waiting")).addParameter("qid", qid)
                .forward();
//                .addParameter("trail", trail).forward();
    }

    /**
     * @param featuresOverlapsForm
     * @param q
     * @param im
     * @param bag
     */
    private void buildOverlapPathQuery(HttpServletRequest request,
            FeaturesOverlapsForm featuresOverlapsForm, PathQuery q,
            InterMineAPI im, ObjectStoreBag osBag)
        throws ObjectStoreException, ClassNotFoundException {

        String givenFeatureType = featuresOverlapsForm.getOverlapFeatureType();
        String overlapFeatureType = featuresOverlapsForm.getOverlapFindType();

        String description = "Results of searching for " + overlapFeatureType
                + "s that overlap the given " + givenFeatureType + ".";

        q.setDescription(description);

        q.addView(overlapFeatureType + ".primaryIdentifier");

        if ("Gene".equals(overlapFeatureType)) {
            q.addView(overlapFeatureType + ".symbol");
        }

        q.addView(overlapFeatureType + ".chromosomeLocation.start");
        q.addView(overlapFeatureType + ".chromosomeLocation.end");
        q.addView(overlapFeatureType + ".chromosomeLocation.strand");

        if ("Exon".equals(overlapFeatureType)) {
            q.addView(overlapFeatureType + ".gene.primaryIdentifier");
        }

        q.addConstraint(Constraints.inIds(overlapFeatureType,
                getOverlappingFeaturesId(featuresOverlapsForm, im, osBag)));

    }

/**
 *
 * @param featuresOverlapsForm
 * @param im
 * @param bag
 * @return the set of ids of the features (of type findFeatureType) overlapping the bag features
 * (of type featureType)
 * @throws ObjectStoreException
 * @throws ClassNotFoundException
 */
    private Set<Integer> getOverlappingFeaturesId(
            FeaturesOverlapsForm featuresOverlapsForm, InterMineAPI im, ObjectStoreBag osBag)
        throws ObjectStoreException, ClassNotFoundException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        String direction = featuresOverlapsForm.getDirection();
        int distance = Integer.valueOf(featuresOverlapsForm.getDistance());
        String featureType = featuresOverlapsForm.getOverlapFeatureType();  //GF
        String findFeatureType = featuresOverlapsForm.getOverlapFindType(); //OF--gene

        LOG.info("OL: finding " + findFeatureType + "s with a flanking region of "
                + distance + " bases " + direction + " overlapping the list of "
                + featureType + "s.");


        // TODO was int organismId = submission.getOrganism().getId();
        // int organismId = 1000001;

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


        QueryField qfOFId = new QueryField(qcOverlapFeature, "id");

        query.addToSelect(qfOFId);

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

        BagConstraint bcBag = new BagConstraint(
                qcGivenFeature, ConstraintOp.IN, osBag);
        constraints.addConstraint(bcBag);

        // SequenceFeaure.chromosome = Gene.chromosome
        constraints.addConstraint(new SimpleConstraint(new QueryForeignKey(
                qcOverlapFeature, "chromosome"), ConstraintOp.EQUALS,
                new QueryForeignKey(qcGivenFeature, "chromosome")));

        // SequenceFeaure.organism = Gene.organism
        constraints.addConstraint(new SimpleConstraint(new QueryForeignKey(
                qcOverlapFeature, "organism"), ConstraintOp.EQUALS, new QueryForeignKey(
                        qcGivenFeature, "organism")));

//        SimpleConstraint scOrg = new SimpleConstraint(new QueryForeignKey(
//                qcOverlapFeature, "organism"), ConstraintOp.EQUALS, new QueryValue(
//                        organismId));
//        constraints.addConstraint(scOrg);

        // Sequence feature location chromosome reference
        QueryObjectReference givenFeatureLocatedOnRef = new QueryObjectReference(
                qcGivenFeatureLoc, "locatedOn");

        // Gene location chromosome reference
        QueryObjectReference overlapFeatureLocatedOnRef = new QueryObjectReference(
                qcOverlapFeatureLoc, "locatedOn");

        OverlapRange givenFeatureRange = new OverlapRange(new QueryField(
                qcGivenFeatureLoc, "start"), new QueryField(
                        qcGivenFeatureLoc, "end"), givenFeatureLocatedOnRef);

        OverlapRange overlapFeatureRange = new OverlapRange(new QueryExpression(new QueryValue(1),
                QueryExpression.GREATEST,
                new QueryExpression(new QueryField(qcOverlapFeatureLoc, "start"),
                        QueryExpression.SUBTRACT, new QueryValue(beforeStartOfOF))),
                        new QueryExpression(
                        new QueryField(qcOverlapFeatureLoc, "end"), QueryExpression.ADD,
                        new QueryValue(afterEndOfOF)), overlapFeatureLocatedOnRef);


        OverlapConstraint oc = new OverlapConstraint(givenFeatureRange,
                ConstraintOp.OVERLAPS, overlapFeatureRange);

        constraints.addConstraint(oc);

        ObjectStoreInterMineImpl ob = (ObjectStoreInterMineImpl) im.getObjectStore();

        LOG.debug("OVERLAP --" + ob.generateSql(query));

        Results results = im.getObjectStore().execute(query, 100000, true, false, true);

        Set<Integer> overlapFeatureIdSet = new HashSet<Integer>();

        if (results == null || results.isEmpty()) {
            LOG.warn("OVERLAP no ovelappingIdSet!! " + overlapFeatureIdSet);
            return overlapFeatureIdSet;
        }

        for (Iterator<?> iter = results.iterator(); iter.hasNext();) {
            ResultsRow<?> row = (ResultsRow<?>) iter.next();
            overlapFeatureIdSet.add((Integer) row.get(0));
        }

        LOG.info("OVERLAP TIME: " + (System.currentTimeMillis() - bT) + " ms");
        return overlapFeatureIdSet;
    }

    /**
     * @param distance
     * @param direction
     * @return
     */
    private int getLeftMargin(String direction, int distance) {
        if ("downstream".equalsIgnoreCase(direction)) {
            return 0;
        } else {
            return distance;
        }
    }

    /**
     * @param distance
     * @param direction
     * @return
     */
    private int getRightMargin(String direction, int distance) {
        if ("upstream".equalsIgnoreCase(direction)) {
            return 0;
        } else {
            return distance;
        }
    }

    public static void initFeaturesOverlaps(InterMineAPI im, InterMineBag bag)
            throws ObjectStoreException {
        // set the bag
        imBag = bag;

    }

}
