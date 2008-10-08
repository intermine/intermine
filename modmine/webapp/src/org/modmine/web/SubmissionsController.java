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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
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

            // a check has been added in case a submission is without features.
            // apparently an outer join was not possible since there would be the need
            // for 2 outer joins.
            // at the moment it is useful only when there are only submissions without features:
            // in case some is present, all the submissions will get the chromosome
            // feature, and this is dealt anyway. This will change.
            // 
            // code marked with CHECK
            
            // get the classes and the counts                         
            Query q = new Query();

            QueryClass sub = new QueryClass(Submission.class);
            q.addFrom(sub);
            q.addToSelect(sub);

            QueryClass lsf = new QueryClass(LocatedSequenceFeature.class);
            q.addFrom(lsf);
            QueryField qfClass = new QueryField(lsf, "class");
            q.addToSelect(qfClass);
            q.addToGroupBy(qfClass);
            
            q.addToGroupBy(sub);
            
            q.addToOrderBy(new QueryField(sub, "publicReleaseDate"), "desc");
            q.addToOrderBy(qfClass);
            
            q.setDistinct(false);                        
            q.addToSelect(new QueryFunction());
            
            QueryCollectionReference datasets = new QueryCollectionReference(lsf, "dataSets");
            ContainsConstraint cc = new ContainsConstraint(datasets, ConstraintOp.CONTAINS, sub);
            q.setConstraint(cc);

            Results results = os.execute(q);

          // CHECK get all the submissions, to check if any without features
          Query qA = new Query();
          QueryClass subA = new QueryClass(Submission.class);
          qA.addFrom(subA);
          qA.addToSelect(subA);
          qA.setDistinct(false);                        
          Results resA = os.execute(qA);

          List<Submission> all = new ArrayList<Submission>();

          for (Iterator<ResultsRow> row = resA.iterator(); row.hasNext(); ) {
              Submission s = (Submission) row.next().get(0);
              all.add(s);
          }

          // CHECK END
          
          // normal case 
          Map<Submission, Map<String, Long>> subs =
                new LinkedHashMap<Submission, Map<String, Long>>();

            // for each classes set the values for jsp
            for (Iterator<ResultsRow> iter = results.iterator(); iter.hasNext(); ) {
                ResultsRow row = iter.next();
                Submission s = (Submission) row.get(0);
                Class feat = (Class) row.get(1);
                Long count = (Long) row.get(2);

                // don't record chromosome feature
                // now done in the jsp
                // if (TypeUtil.unqualifiedName(feat.getName()) == "Chromosome") { continue; }
                                
                Map<String, Long> fc =
                    new LinkedHashMap<String, Long>();

                fc.put(TypeUtil.unqualifiedName(feat.getName()), count);
                
                // check if there is the need to add to fc before putting it in subs
                // Note: the db query get the feature class in descending alphabetical order,
                // so this will bring back the ascending order
                if (subs.containsKey(s)) {
                    Map<String, Long> ft =
                        new LinkedHashMap<String, Long>();
                    ft = subs.get(s);
                    ft.put(TypeUtil.unqualifiedName(feat.getName()), count);
                    subs.put(s, ft);
                } else {
                    subs.put(s, fc);
                }
              all.remove(s); // CHECK
            }            

            // CHECK
            Map<String, Long> noFeat =
                new LinkedHashMap<String, Long>();
            noFeat.put("-", null);

            if (!all.isEmpty()) {
                // add to the map all the subs without features
                Iterator <Submission> it  = all.iterator();
                while (it.hasNext()) {
                    subs.put(it.next(), noFeat);
                }
            }
            // CHECK END
            
            request.setAttribute("subs", subs);

        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }
}
