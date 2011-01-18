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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.model.bio.Submission;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for submissionDisplayer.jsp
 * @author julie sullivan
 */
public class SubmissionDisplayerController extends TilesAction
{
    private static final Logger LOG = Logger.getLogger(MetadataCache.class);

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(@SuppressWarnings("unused")  ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        final ServletContext servletContext = servlet.getServletContext();

        ObjectStore os = im.getObjectStore();

        // submission object
        InterMineObject o = (InterMineObject) request.getAttribute("object");

        Query q = new Query();
        q.setDistinct(false);

        QueryClass lsf = new QueryClass(SequenceFeature.class);
        QueryClass sub = new QueryClass(Submission.class);

        QueryField qfClass = new QueryField(lsf, "class");

        q.addFrom(sub);
        q.addFrom(lsf);

        q.addToSelect(qfClass);
        q.addToSelect(new QueryFunction());

        q.addToGroupBy(sub);
        q.addToGroupBy(qfClass);

//        q.addToOrderBy(new QueryField(sub, "publicReleaseDate"), "desc");
        q.addToOrderBy(qfClass);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference submissions = new QueryCollectionReference(lsf, "submissions");
        ContainsConstraint cc = new ContainsConstraint(submissions, ConstraintOp.CONTAINS, sub);
        cs.addConstraint(cc);

        QueryField qfId = new QueryField(sub, "id");
        SimpleConstraint sc = new SimpleConstraint(qfId, ConstraintOp.EQUALS,
                                                   new QueryValue(o.getId()));
        cs.addConstraint(sc);

        q.setConstraint(cs);

        Results results = os.execute(q);


        Map<String, Long> featureCounts = new LinkedHashMap<String, Long>();

        // for each classes set the values for jsp
        @SuppressWarnings("unchecked") Iterator<ResultsRow> iter =
            (Iterator) results.iterator();
        while (iter.hasNext()) {
            ResultsRow<?> row = iter.next();
            Class<?> feat = (Class<?>) row.get(0);
            Long count = (Long) row.get(1);

            featureCounts.put(TypeUtil.unqualifiedName(feat.getName()), count);
        }

        LOG.info("FC: " + featureCounts);


        request.setAttribute("featureCounts", featureCounts);

        Map<String, String> expFeatureDescription =
            MetadataCache.getFeatTypeDescription(servletContext);
        request.setAttribute("expFeatDescription", expFeatureDescription);


        return null;
    }
}
