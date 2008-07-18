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
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.web.logic.Constants;

/**
 * 
 * @author contrino
 *
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
            
            //get the list of projects 
            Query q = new Query();  
            QueryClass qc = new QueryClass(ExperimentSubmission.class);
            QueryField qfDate = new QueryField(qc, "publicReleaseDate");

            q.addFrom(qc);
            q.addToSelect(qc);
            q.addToOrderBy(qfDate, "desc");
            
            Results results = os.executeSingleton(q);

            Map< Integer, ExperimentSubmission> subs =
                new LinkedHashMap<Integer, ExperimentSubmission>();
//            Map<ModEncodeProject, Integer> nr =
//                new LinkedHashMap<ModEncodeProject, Integer>();
            
            // get all submission by date desc
            Integer order = 0;
            Iterator i = results.iterator();
            while (i.hasNext()) {
                order++;
                ExperimentSubmission sub = (ExperimentSubmission) i.next();
//                Set<ModEncodeProvider> providers = project.getProviders();
                subs.put(order, sub);
//                Integer subNr = 0;
//                // for each provider, get its experiments
//                Iterator p = providers.iterator();
//                while (p.hasNext()) {
//                    ModEncodeProvider provider = (ModEncodeProvider) p.next();
//                    Set<ExperimentSubmission> subs = provider.getExperimentSubmissions();
//                    subNr = subNr + subs.size();
//                }
//                nr.put(project, subNr);
            }
            request.setAttribute("subs", subs);
//            request.setAttribute("counts", nr);
        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }
}


/*
Results results = os.execute(q);

Map<ModEncodeProvider, Set<ExperimentSubmission>> ps =
    new LinkedHashMap<ModEncodeProvider, Set<ExperimentSubmission>>();

Map<ModEncodeProvider, ModEncodeProject> pp =
    new LinkedHashMap<ModEncodeProvider, ModEncodeProject>();

// for each provider, get its attributes and set the values for jsp

for (Iterator iter = results.iterator(); iter.hasNext(); ) {
    ResultsRow row = (ResultsRow) iter.next();

    ModEncodeProvider provider = (ModEncodeProvider) row.get(0);
    Set<ExperimentSubmission> subs = provider.getExperimentSubmissions();
    ModEncodeProject project = provider.getProject();
    
    ps.put(provider, subs);
    pp.put(provider, project);

}            
*/