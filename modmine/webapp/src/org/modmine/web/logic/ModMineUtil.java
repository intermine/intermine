package org.modmine.web.logic;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Submission;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Utility methods for the modMine package.
 * Refer to BioUtil.
 *
 * @author Fengyuan Hu
 */
public final class ModMineUtil
{
    protected static final Logger LOG = Logger.getLogger(ModMineUtil.class);

    private static List<Double> geneExpressionScoreList = null;
    private static List<Double> exonExpressionScoreList = null;


    private ModMineUtil() {
        super();
    }

    /**
    * Query a list of gene expression scores in ascending order.
    *
    * @param session the HttpSession
    * @param im the InterMineAPI
    * @return list of gene expression scores in ascending order
    */
    public static synchronized List<Double> getGeneExpressionScores(
            HttpSession session, InterMineAPI im) {
        if (geneExpressionScoreList == null || exonExpressionScoreList == null) {
            queryExpressionScore(session, im);
        }

        return geneExpressionScoreList;
    }

    /**
    * Query a list of exon expression scores in ascending order.
    *
    * @param session the HttpSession
    * @param im the InterMineAPI
    * @return list of exon expression scores in ascending order
    */
    public static synchronized List<Double> getExonExpressionScores(
            HttpSession session, InterMineAPI im) {
        if (geneExpressionScoreList == null || exonExpressionScoreList == null) {
            queryExpressionScore(session, im);
        }

        return exonExpressionScoreList;
    }

    private static void queryExpressionScore(HttpSession session, InterMineAPI im) {

        Model model = im.getModel();
        Profile profile = SessionMethods.getProfile(session);
        PathQueryExecutor executor = im.getPathQueryExecutor(profile);

        geneExpressionScoreList = new ArrayList<Double>();

        PathQuery query = new PathQuery(model);
        query.addView("GeneExpressionScore.score");
        query.addOrderBy("GeneExpressionScore.score", OrderDirection.ASC);

        ExportResultsIterator geneResult = executor.execute(query);

        while (geneResult.hasNext()) {
            List<ResultElement> row = geneResult.next();
            Double score = (Double) row.get(0).getField();
            geneExpressionScoreList.add(score);
        }

        exonExpressionScoreList = new ArrayList<Double>();

        query = new PathQuery(model);
        query.addView("ExonExpressionScore.score");
        query.addOrderBy("ExonExpressionScore.score", OrderDirection.ASC);

        ExportResultsIterator exonResult = executor.execute(query);

        while (exonResult.hasNext()) {
            List<ResultElement> row = exonResult.next();
            Double score = (Double) row.get(0).getField();
            exonExpressionScoreList.add(score);
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
