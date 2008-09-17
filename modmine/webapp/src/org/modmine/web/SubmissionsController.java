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
import org.intermine.web.logic.Constants;

/**
 * Controller for submissions.jsp
 * @author sc
 */
public class SubmissionsController extends TilesAction
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
        try {
            HttpSession session = request.getSession();
            ObjectStore os =
                (ObjectStore) session.getServletContext().getAttribute(Constants.OBJECTSTORE);

            //get the classes and the counts                         
            Query q = new Query();

            QueryClass sub = new QueryClass(Submission.class);
            q.addFrom(sub);
//            QueryField qfTitle = new QueryField(sub, "title");
            q.addToSelect(sub);

            QueryClass lsf = new QueryClass(LocatedSequenceFeature.class);
            q.addFrom(lsf);
            QueryField qfClass = new QueryField(lsf, "class");
            q.addToSelect(qfClass);
            q.addToGroupBy(qfClass);
            
//            q.addToGroupBy(qfTitle);
            q.addToGroupBy(sub);
            
            q.addToOrderBy(sub);
            q.addToOrderBy(qfClass);
            
            q.setDistinct(false);                        
            q.addToSelect(new QueryFunction());
            
            QueryCollectionReference datasets = new QueryCollectionReference(lsf, "dataSets");
            ContainsConstraint cc = new ContainsConstraint(datasets, ConstraintOp.CONTAINS, sub);
            q.setConstraint(cc);

            Results results = os.execute(q);

//            Map<String, Map<String, Long>> subs =
//                new LinkedHashMap<String, Map<String, Long>>();
            Map<Submission, Map<String, Long>> subs =
                new LinkedHashMap<Submission, Map<String, Long>>();

            // for each classes set the values for jsp
            for (Iterator<ResultsRow> iter = results.iterator(); iter.hasNext(); ) {
                ResultsRow row = iter.next();
                Submission s = (Submission) row.get(0);
                Class feat = (Class) row.get(1);
                Long count = (Long) row.get(2);

                // don't record chromosome feature
                //if (TypeUtil.unqualifiedName(feat.getName()) == "Chromosome") { continue; }
                                
                Map<String, Long> fc =
                    new LinkedHashMap<String, Long>();

                fc.put(TypeUtil.unqualifiedName(feat.getName()), count);
                
                // check if there is the need to add to fc before putting it in subs
                // Note: the db query get the feature class in descending alphabetical order,
                // so this will bring back the ascending order
//                if (subs.containsKey(s)) {
//                    for (Map<String, Long> map: subs.values()) {
//                        fc.putAll(map);
//                    }
//                } 
                if (subs.containsKey(s)) {
                    Map<String, Long> ft =
                        new LinkedHashMap<String, Long>();
                    ft = subs.get(s);
                    ft.put(TypeUtil.unqualifiedName(feat.getName()), count);
                    subs.put(s, ft);
                } else {
                    subs.put(s, fc);
                }
            }            

           request.setAttribute("subs", subs);

        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }
}
