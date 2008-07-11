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

public class ModEncodeProjectsController extends TilesAction 
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
             
            // for each project, get its providers
            Iterator i = results.iterator();
            while (i.hasNext()) {
                ModEncodeProject project = (ModEncodeProject) i.next();
                Set<ModEncodeProvider> providers = project.getProviders();
                pp.put(project, providers);
                
                // for each provider, get its experiments TODO
                Iterator p = providers.iterator();
                while (p.hasNext()) {
                    ModEncodeProvider provider = (ModEncodeProvider) p.next();
                    Set<ExperimentSubmission> subs = provider.getExperimentSubmissions();
                    ps.put(provider, subs);
                }                
            }
            request.setAttribute("experiments", ps);
            request.setAttribute("providers", pp);
        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }
}


