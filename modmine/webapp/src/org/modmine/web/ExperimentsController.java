package org.modmine.web;

/*
 * Copyright (C) 2002-2009 FlyMine
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
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.model.bio.Experiment;
import org.intermine.model.bio.Lab;
import org.intermine.model.bio.Project;
import org.intermine.model.bio.Submission;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.web.logic.Constants;

/**
 * 
 * @author contrino
 *
 */

public class ExperimentsController extends TilesAction 
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
/*
        try {
            HttpSession session = request.getSession();
            ObjectStore os =
                (ObjectStore) session.getServletContext().getAttribute(Constants.OBJECTSTORE);
            
            //get the list of projects 
            Query q = new Query();  
            QueryClass qc = new QueryClass(Project.class);
            QueryField qcName = new QueryField(qc, "name");

            q.addFrom(qc);
            q.addToSelect(qc);
            q.addToOrderBy(qcName);
            
            Results results = os.executeSingleton(q);

            Map<Project, Set<Lab>> pp =
                new LinkedHashMap<Project, Set<Lab>>();
            Map<Project, Integer> nr =
                new LinkedHashMap<Project, Integer>();
            
            // for each project, get its labs
            Iterator i = results.iterator();
            while (i.hasNext()) {
                Project project = (Project) i.next();
                Set<Lab> labs = project.getLabs();
                pp.put(project, labs);
                Integer subNr = 0;
                // for each lab, get its experiments
                Iterator p = labs.iterator();
                while (p.hasNext()) {
                    Lab lab = (Lab) p.next();
                    Set<Submission> subs = lab.getSubmissions();
                    subNr = subNr + subs.size();
                }
                nr.put(project, subNr);
            }
            request.setAttribute("labs", pp);
            request.setAttribute("counts", nr);
        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }
}
        */
        try {
            HttpSession session = request.getSession();
            ObjectStore os =
                (ObjectStore) session.getServletContext().getAttribute(Constants.OBJECTSTORE);
            
            //get the list of projects 
            Query q = new Query();  
            QueryClass qc = new QueryClass(Experiment.class);
            QueryField qcName = new QueryField(qc, "name");

            q.addFrom(qc);
            q.addToSelect(qc);
            q.addToOrderBy(qcName);
            
            Results results = os.executeSingleton(q);

            Map<Experiment, Set<Submission>> es =
                new LinkedHashMap<Experiment, Set<Submission>>();

            Map<Project, Set<Lab>> pp =
                new LinkedHashMap<Project, Set<Lab>>();
            Map<Project, Set<Experiment>> pe =
                new LinkedHashMap<Project, Set<Experiment>>();

            Map<Experiment, Integer> nr =
                new LinkedHashMap<Experiment, Integer>();
            
            // for each experiment, get its submissions
            Integer subNr = 0;
            Iterator i = results.iterator();
            while (i.hasNext()) {
                Experiment experiment = (Experiment) i.next();
                Set<Submission> subs = experiment.getSubmissions();
                es.put(experiment, subs);

                subNr = subs.size();
                nr.put(experiment, subNr);

//                Project project = (Project) i.next();
//                Set<Lab> labs = project.getLabs();
//                pp.put(project, labs);
//                
//                Set<Experiment> exps = project.getExperiments();
//                pe.put(project, exps);
//                
//                Integer subNr = 0;
//                // for each experiment, get its submissions
//                Iterator p = exps.iterator();
//                while (p.hasNext()) {
//                    Experiment exp = (Experiment) p.next();
//                    Set<Submission> subs = exp.getSubmissions();
//                    subNr = subNr + subs.size();
//                    nr.put(exp, subNr);
//                }
            }
            request.setAttribute("subs", es);
            request.setAttribute("counts", nr);
        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }

}
    

