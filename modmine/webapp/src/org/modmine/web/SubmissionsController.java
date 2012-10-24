package org.modmine.web;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.model.bio.Submission;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for submissions.jsp
 * @author sc
 */
public class SubmissionsController extends TilesAction
{

    // this is defined here, so we can query only the first time.
    private Map<Submission, Map<String, Long>> submissions =
        new LinkedHashMap<Submission, Map<String, Long>>();

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {


        if (submissions.size() > 0) {
            request.setAttribute("subs", submissions);
            return null;
        }

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        ObjectStore os = im.getObjectStore();

        // a check has been added in case a submission is without features.
        // apparently an outer join was not possible since there would be the need
        // for 2 outer joins.
        // at the moment it is useful only when there are only submissions without features:
        //
        // code marked with CHECK

        // CHECK get all the submissions, to check if any without features
        Query q = new Query();
        q.setDistinct(false);
        QueryClass sub = new QueryClass(Submission.class);
        q.addFrom(sub);
        q.addToSelect(sub);
        Results results = os.execute(q);

        List<Submission> all = new ArrayList<Submission>();

        @SuppressWarnings("unchecked") Iterator<ResultsRow> resIter = (Iterator) results.iterator();
        while (resIter.hasNext()) {
            ResultsRow<?> row = resIter.next();
            Submission s = (Submission) row.get(0);
            all.add(s);
        }

        // get the classes and the counts
        q = new Query();
        q.setDistinct(false);

        QueryClass lsf = new QueryClass(SequenceFeature.class);

        QueryField qfClass = new QueryField(lsf, "class");

        q.addFrom(sub);
        q.addFrom(lsf);

        q.addToSelect(sub);
        q.addToSelect(qfClass);
        q.addToSelect(new QueryFunction());

        q.addToGroupBy(sub);
        q.addToGroupBy(qfClass);

        q.addToOrderBy(new QueryField(sub, "publicReleaseDate"), "desc");
        q.addToOrderBy(qfClass);

        QueryCollectionReference datasets = new QueryCollectionReference(lsf, "dataSets");
        ContainsConstraint cc = new ContainsConstraint(datasets, ConstraintOp.CONTAINS, sub);
        q.setConstraint(cc);

        results = os.execute(q);

        // for each class set the values for jsp
        resIter = (Iterator) results.iterator();
        while (resIter.hasNext()) {
            ResultsRow<?> row = resIter.next();
            Submission submission = (Submission) row.get(0);
            Class<?> feat = (Class<?>) row.get(1);
            Long count = (Long) row.get(2);

            Map<String, Long> featureCountMap = submissions.get(submission);
            if (featureCountMap == null) {
                featureCountMap = new LinkedHashMap<String, Long>();
                submissions.put(submission, featureCountMap);
            }
            featureCountMap.put(TypeUtil.unqualifiedName(feat.getName()), count);

            all.remove(submission); // CHECK
        }

        // CHECK
        Map<String, Long> noFeat = new LinkedHashMap<String, Long>();
        noFeat.put("-", null);

        if (!all.isEmpty()) {
            // add to the map all the subs without features
            Iterator<Submission> it = all.iterator();
            while (it.hasNext()) {
                submissions.put(it.next(), noFeat);
            }
        }
        // CHECK END

        request.setAttribute("subs", submissions);
        return null;
    }
}
