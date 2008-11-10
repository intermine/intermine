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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.flymine.model.genomic.Submission;
import org.flymine.model.genomic.Lab;
import org.flymine.model.genomic.Project;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.web.logic.Constants;

/**
 * 
 * @author contrino
 *
 */

public class ProjectsSummaryController extends TilesAction 
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
            
            //get the list of projects 
            Query qPro = new Query();  
            QueryClass qcPro = new QueryClass(Project.class);
            QueryField qfName = new QueryField(qcPro, "name");

            qPro.addFrom(qcPro);
            qPro.addToSelect(qcPro);
            qPro.addToOrderBy(qfName);
            
            Results results = os.executeSingleton(qPro);
            
            
//            //get the list of submissions for a lab, order by publicReleaseDate 
//            Query qSub = new Query();  
//            QueryClass qcSub = new QueryClass(Submission.class);
//            QueryField qfDate = new QueryField(qcSub, "publicReleaseDate");
//            QueryField qfLab = new QueryField(qcSub, "lab");
//
//            qSub.addFrom(qcSub);
//            qSub.addToSelect(qcSub);
//
//            SimpleConstraint sc = new SimpleConstraint(qfLab, ConstraintOp.EQUALS,
//                    new QueryValue(new Integer(7227)));
//            qSub.setConstraint(sc);
//
//            qSub.addToOrderBy(qfDate, "desc");
//            
//            Results resultSub = os.executeSingleton(qSub);

            
//            Map<Project, Set<Lab>> pl =
//                new LinkedHashMap<Project, Set<Lab>>();
            Map<Project, Set<Submission>> ps =
                new LinkedHashMap<Project, Set<Submission>>();
            Map<Project, Integer> nr =
                new LinkedHashMap<Project, Integer>();
            Map<String, Set<Submission>> pp =
                new LinkedHashMap<String, Set<Submission>>();

            // for each project, get its labs and fill the subs
            // will be direct when submission has a collection of subs
            Iterator i = results.iterator();
            while (i.hasNext()) {
                Project project = (Project) i.next();

/*                //BEGIN
                //get the list of submissions for a lab, order by publicReleaseDate 
                Query qSub = new Query();  
                QueryClass qcSub = new QueryClass(Submission.class);
                QueryField qfDate = new QueryField(qcSub, "publicReleaseDate");
                QueryField qfLab = new QueryField(qcSub, "lab");

                QueryClass qcP = new QueryClass(Project.class);
                QueryField qfP = new QueryField(qcP, "name");

                QueryClass qcL = new QueryClass(Lab.class);
                QueryField qfL = new QueryField(qcL, "name");

                qSub.addFrom(qcSub);
                qSub.addFrom(qcP);
                qSub.addFrom(qcL);
                
                qSub.addToSelect(qcSub);

                SimpleConstraint sc = new SimpleConstraint(qfP, ConstraintOp.EQUALS,
                        new QueryValue(project.getName()));
                SimpleConstraint sc1 = new SimpleConstraint(qfLab, ConstraintOp.EQUALS,
                        qfL);

                // Create a set to hold constraints that will be ANDed together
                ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
                cs.addConstraint(sc);
                cs.addConstraint(sc1);
//                qSub.setConstraint(sc);
//                qSub.setConstraint(sc1);

                qSub.setConstraint(cs);
                
                qSub.addToOrderBy(qfDate, "desc");

                Results resultSub = os.executeSingleton(qSub);
*/
                //BEGIN
                //get the list of submissions for a lab, order by publicReleaseDate 
                Query qSub = new Query();  
                QueryClass qcP = new QueryClass(Project.class);
                QueryField qfP = new QueryField(qcP, "name");

                QueryClass qcSub = new QueryClass(Submission.class);
                QueryField qfDate = new QueryField(qcSub, "publicReleaseDate");

                QueryClass qcL = new QueryClass(Lab.class);

                qSub.addFrom(qcSub);
                qSub.addFrom(qcP);
                qSub.addFrom(qcL);
                
                qSub.addToSelect(qcSub);

                SimpleConstraint sc = new SimpleConstraint(qfP, ConstraintOp.EQUALS,
                        new QueryValue(project.getName()));

                // Create a set to hold constraints that will be ANDed together
                ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
                cs.addConstraint(sc);
                // reference.
                QueryObjectReference ref1 = new QueryObjectReference(qcL, "project");
                ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcP);
                cs.addConstraint(cc1);

                QueryObjectReference ref2 = new QueryObjectReference(qcSub, "lab");
                ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcL);
                cs.addConstraint(cc2);
                
                // Set the constraint of the query
                qSub.setConstraint(cs);

                qSub.addToOrderBy(qfDate, "desc");

                Results resultSub = os.executeSingleton(qSub);
                
              Set<Submission> subs = new LinkedHashSet<Submission>();
//              Set subs = new LinkedHashSet();
              Iterator<Submission> s = resultSub.iterator();
              while (s.hasNext()) {
                  subs.add(s.next());                  
              }
                ps.put(project, subs);
                nr.put(project, subs.size());
                pp.put(project.getName(), subs);

//                Set<Lab> labs = project.getLabs();
//                Set<Submission> subs = new HashSet<Submission>();
//                pl.put(project, labs);
//                Integer subNr = 0;
//                // for each lab, get its submissions
//                Iterator p = labs.iterator();
//                while (p.hasNext()) {
//                    Lab lab = (Lab) p.next();
//                    
//                    Set<Submission> labSubs = lab.getSubmissions();
//                    subNr = subNr + labSubs.size();
//                    subs.addAll(labSubs);                
//                }
//                nr.put(project, subNr);
//                ps.put(project, subs);
            }
//            request.setAttribute("labs", pl);
            request.setAttribute("test", pp);            
            request.setAttribute("subs", ps);            
            request.setAttribute("counts", nr);
        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }

}







//END
//------------
/*Map<Project, Set<Lab>> pl =
    new LinkedHashMap<Project, Set<Lab>>();
Map<Project, Set<Submission>> ps =
    new LinkedHashMap<Project, Set<Submission>>();
Map<Project, Integer> nr =
    new LinkedHashMap<Project, Integer>();

// for each project, get its labs and fill the subs
// will be direct when submission has a collection of subs
Iterator i = results.iterator();
while (i.hasNext()) {
    Project project = (Project) i.next();
    Set<Lab> labs = project.getLabs();
    Set<Submission> subs = new HashSet<Submission>();
    pl.put(project, labs);
    Integer subNr = 0;
    // for each lab, get its submissions
    Iterator p = labs.iterator();
    while (p.hasNext()) {
        Lab lab = (Lab) p.next();
        
        Set<Submission> labSubs = lab.getSubmissions();
        subNr = subNr + labSubs.size();
        subs.addAll(labSubs);                
    }
    nr.put(project, subNr);
    ps.put(project, subs);
}
request.setAttribute("labs", pl);
request.setAttribute("subs", ps);            
request.setAttribute("counts", nr);
*/