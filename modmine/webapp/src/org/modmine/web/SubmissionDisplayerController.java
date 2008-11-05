package org.modmine.web;

/*
 * Copyright (C) 2002-2008 FlyMine
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Submission;
import org.intermine.model.InterMineObject;
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
import org.intermine.web.logic.Constants;

/**
 * Controller for submissionDisplayer.jsp
 * @author julie sullivan
 */
public class SubmissionDisplayerController extends TilesAction
{
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(@SuppressWarnings("unused")  ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
    throws Exception {

        HttpSession session = request.getSession();
        ObjectStore os =
            (ObjectStore) session.getServletContext().getAttribute(Constants.OBJECTSTORE);

        // submission object
        InterMineObject o = (InterMineObject) request.getAttribute("object");

        Query q = new Query();
        q.setDistinct(false);

        QueryClass lsf = new QueryClass(LocatedSequenceFeature.class);
        QueryClass sub = new QueryClass(Submission.class);

        QueryField qfClass = new QueryField(lsf, "class");

        q.addFrom(sub);
        q.addFrom(lsf);

        q.addToSelect(qfClass);
        q.addToSelect(new QueryFunction());

        q.addToGroupBy(sub);
        q.addToGroupBy(qfClass);

        q.addToOrderBy(new QueryField(sub, "publicReleaseDate"), "desc");
        q.addToOrderBy(qfClass);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference datasets = new QueryCollectionReference(lsf, "dataSets");
        ContainsConstraint cc = new ContainsConstraint(datasets, ConstraintOp.CONTAINS, sub);
        cs.addConstraint(cc);

        QueryField qfId = new QueryField(sub, "id");
        SimpleConstraint sc = new SimpleConstraint(qfId, ConstraintOp.EQUALS,
                                                   new QueryValue(o.getId()));
        cs.addConstraint(sc);

        q.setConstraint(cs);

        Results results = os.execute(q);


        Map<String, Long> featureCounts = new LinkedHashMap<String, Long>();

        // for each classes set the values for jsp
        for (Iterator<ResultsRow> iter = results.iterator(); iter.hasNext(); ) {
            ResultsRow row = iter.next();
            Class feat = (Class) row.get(0);
            Long count = (Long) row.get(1);

            Map<String, Long> fc = new LinkedHashMap<String, Long>();
            fc.put(TypeUtil.unqualifiedName(feat.getName()), count);
            featureCounts.put(TypeUtil.unqualifiedName(feat.getName()), count);
        }

        request.setAttribute("featureCounts", featureCounts);

        return null;
    }
}
