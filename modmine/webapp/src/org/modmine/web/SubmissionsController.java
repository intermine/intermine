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
import org.flymine.model.genomic.Lab;
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
 * Controller for results.jsp
 * @author Tom Riley
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
                        
//            Query q = new Query();
//
//            QueryClass sub = new QueryClass(Submission.class);
//            q.addFrom(sub);
//            QueryField qfTitle = new QueryField(sub, "title");
//            q.addToSelect(qfTitle);
//
//            QueryClass lsf = new QueryClass(LocatedSequenceFeature.class);
//            q.addFrom(lsf);
//            QueryField qfClass = new QueryField(lsf, "class");
//            q.addToSelect(qfClass);
//            q.addToGroupBy(qfClass);
//            
//            q.addToGroupBy(qfTitle);
//
//            q.setDistinct(false);                        
//            q.addToSelect(new QueryFunction());
//            
//            QueryCollectionReference datasets = new QueryCollectionReference(lsf, "dataSets");
//            ContainsConstraint cc = new ContainsConstraint(datasets, ConstraintOp.CONTAINS, sub);
//            q.setConstraint(cc);
//
//            Results results = os.execute(q);


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

            q.setDistinct(false);                        
            q.addToSelect(new QueryFunction());
            
            QueryCollectionReference datasets = new QueryCollectionReference(lsf, "dataSets");
            ContainsConstraint cc = new ContainsConstraint(datasets, ConstraintOp.CONTAINS, sub);
            q.setConstraint(cc);

            Results results = os.execute(q);

            
            //            Map<Class, Long> fc =
//                new LinkedHashMap<Class, Long>();
//
//            Map<String, Map<Class, Long>> subs =
//                new LinkedHashMap<String, Map<Class, Long>>();

            Map<String, Long> fc =
                new LinkedHashMap<String, Long>();

//            Map<String, Map<String, Long>> subs =
//                new LinkedHashMap<String, Map<String, Long>>();
            Map<Submission, Map<String, Long>> subs =
                new LinkedHashMap<Submission, Map<String, Long>>();

            
            StringBuffer lastSub = new StringBuffer("-");
            Integer iteration = 0;
            
            // for each classes set the values for jsp
            for (Iterator iter = results.iterator(); iter.hasNext(); ) {
                iteration++;
                ResultsRow row = (ResultsRow) iter.next();
                Submission s = (Submission) row.get(0);
                Class feat = (Class) row.get(1);
                Long count = (Long) row.get(2);
                
                String thisSub = s.getTitle();

                fc.put(TypeUtil.unqualifiedName(feat.getName()), count);
                
                if (!thisSub.equals(lastSub.toString())) { 
                    lastSub.delete(0, lastSub.length());
                    lastSub.append(thisSub);

                    // if not the first one
                    if (iteration > 1 && iteration < results.size()) {
                        subs.put(s, fc);
                        fc.clear();
                    }
                }

                if (iteration == results.size()) {
                    subs.put(s, fc);
                }
            }            

           request.setAttribute("features", fc);
           request.setAttribute("subs", subs);

        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }
}


//StringBuffer lastSub = new StringBuffer("-");
//Integer iteration = 0;
//
//
//// for each classes set the values for jsp
//for (Iterator iter = results.iterator(); iter.hasNext(); ) {
////    ResultsRow row = (ResultsRow) iter.next();
////    Class feat = (Class) row.get(0);
////    Long count = (Long) row.get(1);
////    fc.put(TypeUtil.unqualifiedName(feat.getName()), count);
//
//    iteration++;
//    
//    ResultsRow row = (ResultsRow) iter.next();
//    Submission s = (Submission) row.get(0);
//    Class feat = (Class) row.get(1);
//    Long count = (Long) row.get(2);
//    
//    String thisSub = s.getTitle();
//
//    fc.put(TypeUtil.unqualifiedName(feat.getName()), count);
//    
//    if (!thisSub.equals(lastSub.toString())) { 
//        lastSub.delete(0, lastSub.length());
//        lastSub.append(thisSub);
//
//        // if not the first one
//        if (iteration > 1 && iteration < results.size()) {
//            subs.put(lastSub.toString(), fc);
//            fc.clear();
//        }
//    }
//}            
//
//subs.put(lastSub.toString(), fc);
//
//request.setAttribute("features", fc);
////request.setAttribute("sub", lastSub.toString());
//request.setAttribute("subs", subs);
