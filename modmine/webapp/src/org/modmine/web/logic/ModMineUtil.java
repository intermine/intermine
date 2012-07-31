package org.modmine.web.logic;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.profile.InterMineBag;
import org.intermine.model.bio.ExonExpressionScore;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.GeneExpressionScore;
import org.intermine.model.bio.Submission;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

/**
 * Utility methods for the modMine package.
 * Refer to BioUtil.
 *
 * @author Fengyuan Hu
 */
public final class ModMineUtil
{
    protected static final Logger LOG = Logger.getLogger(ModMineUtil.class);

    private static Double geneExpressionScoreMax = null; // 53808
    private static Double geneExpressionScoreMin = null; // 0
    private static Double exonExpressionScoreMax = null; // 53808
    private static Double exonExpressionScoreMin = null; // 0

    private ModMineUtil() {
        super();
    }

    /**
     * Get the max value of gene expression scores.
     *
     * @param os the production objectStore
     * @return geneExpressionScoreMax
     */
    public static synchronized Double getMaxGeneExpressionScore(ObjectStore os) {
        if (geneExpressionScoreMax == null || geneExpressionScoreMin == null) {
            queryGeneExpressionScores(os);
        }

        return geneExpressionScoreMax;
    }

    /**
     * Get the min value of gene expression scores.
     *
     * @param os the production objectStore
     * @return geneExpressionScoreMin
     */
    public static synchronized Double getMinGeneExpressionScore(ObjectStore os) {
        if (geneExpressionScoreMax == null || geneExpressionScoreMin == null) {
            queryGeneExpressionScores(os);
        }

        return geneExpressionScoreMin;
    }

    /**
    * Get the max value of exon expression scores.
    *
    * @param os the production objectStore
    * @return exonExpressionScoreMax
    */
    public static synchronized Double getMaxExonExpressionScore(ObjectStore os) {
        if (exonExpressionScoreMax == null || exonExpressionScoreMin == null) {
            queryExonExpressionScores(os);
        }

        return exonExpressionScoreMax;
    }

    /**
    * Get the min value of exon expression scores.
    *
    * @param os the production objectStore
    * @return exonExpressionScoreMin
    */
    public static synchronized Double getMinExonExpressionScore(ObjectStore os) {
        if (exonExpressionScoreMax == null || exonExpressionScoreMin == null) {
            queryExonExpressionScores(os);
        }

        return exonExpressionScoreMin;
    }

    /**
    * Query the max and min values of gene expression scores.
    *
    * @param os the production objectStore
    */
    private static void queryGeneExpressionScores(ObjectStore os) {
        Query q = new Query();

        QueryClass qcObject = new QueryClass(GeneExpressionScore.class);
        QueryField qfObjectScore = new QueryField(qcObject, "score");
        QueryNode max = new QueryFunction(qfObjectScore, QueryFunction.MAX);
        QueryNode min = new QueryFunction(qfObjectScore, QueryFunction.MIN);

        q.addFrom(qcObject);
        q.addToSelect(min);
        q.addToSelect(max);

        Results r = os.execute(q);

        @SuppressWarnings({ "rawtypes", "unchecked" })
        Iterator<ResultsRow> it = (Iterator) r.iterator();
        while (it.hasNext()) {
            @SuppressWarnings("rawtypes")
            ResultsRow rr = it.next();
            geneExpressionScoreMin =  (Double) rr.get(0);
            geneExpressionScoreMax =  (Double) rr.get(1);
        }
    }

    /**
    * Query the max and min values of exon expression scores.
    *
    * @param os the production objectStore
    */
    private static void queryExonExpressionScores(ObjectStore os) {
        Query q = new Query();

        QueryClass qcObject = new QueryClass(ExonExpressionScore.class);
        QueryField qfObjectScore = new QueryField(qcObject, "score");
        QueryNode max = new QueryFunction(qfObjectScore, QueryFunction.MAX);
        QueryNode min = new QueryFunction(qfObjectScore, QueryFunction.MIN);

        q.addFrom(qcObject);
        q.addToSelect(min);
        q.addToSelect(max);

        Results r = os.execute(q);

        @SuppressWarnings({ "rawtypes", "unchecked" })
        Iterator<ResultsRow> it = (Iterator) r.iterator();
        while (it.hasNext()) {
            @SuppressWarnings("rawtypes")
            ResultsRow rr = it.next();
            exonExpressionScoreMin =  (Double) rr.get(0);
            exonExpressionScoreMax =  (Double) rr.get(1);
        }
    }

    /**
     * For a bag of Submission objects, returns a set of Submission objects.
     * @param os ObjectStore
     * @param bag InterMineBag
     * @return Set of Submissions
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Set<Submission> getSubmissions(ObjectStore os, InterMineBag bag) {

        Query q = new Query();

        QueryClass qcObject = new QueryClass(Submission.class);

        // InterMine id for any object
        QueryField qfObjectId = new QueryField(qcObject, "id");

        q.addFrom(qcObject);
        q.addToSelect(qcObject);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        BagConstraint bc = new BagConstraint(qfObjectId, ConstraintOp.IN, bag.getOsb());
        cs.addConstraint(bc);

        q.setConstraint(cs);

        Results r = os.execute(q);
        Iterator<ResultsRow> it = (Iterator) r.iterator();

        Set<Submission> subs = new LinkedHashSet<Submission>();

        while (it.hasNext()) {
            ResultsRow rr = it.next();
            Submission sub =  (Submission) rr.get(0);
            subs.add(sub);
        }
        return subs;
    }

    /**
     * Get a set of Gene primaryId from within a bag
     *
     * @param os the ObjectStore
     * @param bag a bag of Genes
     * @return a set of string as primaryId of the genes
     */
    public static Set<String> getGenes(ObjectStore os, InterMineBag bag) {

        Query q = new Query();

        QueryClass qcObject = new QueryClass(Gene.class);

        // InterMine id for any object
        QueryField qfObjectId = new QueryField(qcObject, "id");
        QueryField qfGenePID = new QueryField(qcObject, "primaryIdentifier");

        q.addFrom(qcObject);
        q.addToSelect(qfGenePID);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        BagConstraint bc = new BagConstraint(qfObjectId, ConstraintOp.IN, bag.getOsb());
        cs.addConstraint(bc);

        q.setConstraint(cs);

        Results r = os.execute(q);
        @SuppressWarnings({ "rawtypes", "unchecked" })
        Iterator<ResultsRow> it = (Iterator) r.iterator();

        Set<String> genePIds = new LinkedHashSet<String>();

        while (it.hasNext()) {
            @SuppressWarnings("rawtypes")
            ResultsRow rr = it.next();
            String genePId =  (String) rr.get(0);
            genePIds.add(genePId);
        }

        return genePIds;
    }
}
