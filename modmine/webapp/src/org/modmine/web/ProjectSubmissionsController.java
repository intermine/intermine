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

import java.util.HashMap;
import java.util.Iterator;
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
import org.flymine.model.genomic.ExperimentSubmission;
import org.flymine.model.genomic.ModEncodeProject;
import org.flymine.model.genomic.ModEncodeProvider;
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

public class ProjectSubmissionsController extends TilesAction 
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
            Query q = new Query();  
            QueryClass qc = new QueryClass(ModEncodeProject.class);
            QueryField qcName = new QueryField(qc, "name");

            q.addFrom(qc);
            q.addToSelect(qc);
            q.addToOrderBy(qcName);
            
            Results results = os.executeSingleton(q);

            Map<ModEncodeProject, Set<ModEncodeProvider>> pp =
                new HashMap<ModEncodeProject, Set<ModEncodeProvider>>();
            Map<ModEncodeProvider, Set<ExperimentSubmission>> ps =
                new HashMap<ModEncodeProvider, Set<ExperimentSubmission>>();
            Map<ModEncodeProject, Integer> nr =
                new HashMap<ModEncodeProject, Integer>();
            
            // for each project, get its providers
            Iterator i = results.iterator();
            while (i.hasNext()) {
                ModEncodeProject project = (ModEncodeProject) i.next();
                Set<ModEncodeProvider> providers = project.getProviders();
                pp.put(project, providers);
                Integer subNr = 0;
                // for each provider, get its experiments
                Iterator p = providers.iterator();
                while (p.hasNext()) {
                    ModEncodeProvider provider = (ModEncodeProvider) p.next();
                    Set<ExperimentSubmission> subs = provider.getExperimentSubmissions();
                    ps.put(provider, subs);
                    subNr = subNr + subs.size();
                }
                nr.put(project, subNr);
            }
//            request.setAttribute("experiments", ps);
            request.setAttribute("providers", pp);
            request.setAttribute("counts", nr);
        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }
}


//// to count submissions
//Query c = new Query();
//QueryClass sub = new QueryClass(ExperimentSubmission.class);
//QueryClass pj = new QueryClass(ModEncodeProject.class);
//QueryClass pv = new QueryClass(ModEncodeProvider.class);
//
//QueryField qfTitle = new QueryField(sub, "title");
//QueryField qfProj = new QueryField(pj, "title");
//QueryFunction qfCount = new QueryFunction();
//
//ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
//
//QueryCollectionReference r = new QueryCollectionReference(pj, "providers");
//cs.addConstraint(new ContainsConstraint(r, ConstraintOp.CONTAINS, pv));
//
//  c.addToSelect(qfTitle);
//  c.addToSelect(qfProj);
//  c.addToSelect(qfCount);
//
//  c.addFrom(sub);
//  c.addFrom(pj);
//  c.addFrom(pv);
//
//  c.addToGroupBy(qfTitle);
//  c.addToGroupBy(qfProj);
//
//  c.addToOrderBy(qfProj);
//  c.setConstraint(cs);
//
//  Results subsCount = os.execute(c);
