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
import java.util.List;
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
 * Controller for providerSubmissions.jsp
 * @author Tom Riley
 */
public class ProviderSubmissionsController extends TilesAction
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

            //get the list of providers 
            Query q = new Query();  
            QueryClass qc = new QueryClass(ModEncodeProvider.class);
            QueryField qcSurname = new QueryField(qc, "surname");

            q.addFrom(qc);
            q.addToSelect(qc);
            q.addToOrderBy(qcSurname);


            Results results = os.executeSingleton(q);

            Map<ModEncodeProvider, Set<ExperimentSubmission>> ps =
                new HashMap<ModEncodeProvider, Set<ExperimentSubmission>>();

            Map<ModEncodeProvider, ModEncodeProject> pp =
                new HashMap<ModEncodeProvider, ModEncodeProject>();

            Map<String, List<String>> providerSubs = 
                new HashMap<String, List<String>>();

            // for each provider, get its attributes and set the values for jsp
            Iterator i = results.iterator();
            while (i.hasNext()) {
                ModEncodeProvider provider = (ModEncodeProvider) i.next();
                Set<ExperimentSubmission> subs = provider.getExperimentSubmissions();
                ModEncodeProject project = provider.getProject();
                
                ps.put(provider, subs);
                pp.put(provider, project);
                
                //List<String> thisProviderSubs = providerSubs.get(provider);
//              for (ExperimentSubmission experiment: subs) {
//              ps.put(provider, experiment);                    
//              }
            }
            request.setAttribute("experiments", ps);
            request.setAttribute("project", pp);

        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }
}



//Map<String, String> ugo = 
//new HashMap<String, String>();

////for each provider, get its attributes and set the values for jsp
//Iterator i = results.iterator();
//while (i.hasNext()) {
//ModEncodeProvider provider = (ModEncodeProvider) i.next();
//Set<ExperimentSubmission> subs = provider.getExperimentSubmissions();

////List<String> thisProviderSubs = providerSubs.get(provider);
//for (ExperimentSubmission experiment: subs) {
//ugo.put(provider.getName(), experiment.getTitle());                    

////    thisProviderSubs.add(experiment.getTitle());
//}
////providerSubs.put(provider.getName(), thisProviderSubs);                    
//}
//String mostra = "qq77 sono qui";
//request.setAttribute("esempio", mostra);
////request.setAttribute("experiments", providerSubs);
//request.setAttribute("experiments", ugo);








//List<String> currentSubs = providerSubs.get(provider);
//
//for (ExperimentSubmission experiment: subs) {
//if (providerSubs.containsKey(provider)) {
//currentSubs.add(experiment.getTitle());
////providerSubs.remove(provider);
//} 
//providerSubs.put(provider.getName(), currentSubs);                    


//Iterator<ExperimentSubmission> e = subs.iterator();
//while (e.hasNext()) {
//if (providerSubs.containsKey(provider)) {
//List<String> currentSubs = providerSubs.get(provider);
//currentSubs.add(e.next().);
////get all the vlaues, put in the list, check , add, rm, put
//String daSub = providerSubs.get(provider);
//currentSubs.addAll(providerSubs.values());
//currentSubs..addAll(providerSubs.values());
//(providerSubs.values().toArray());
//providerSubs.values()..add(e.next().toString());
//.put(provider.getName(), (ExperimentSubmission) e.next().toString());                      
//}
//providerSubs.put(provider.getName(), (ExperimentSubmission) e.next().toString());
////providerSubs.addToMap(provider.getName(), e.next());
//}

//for (ModEncodeProvider modEncodeProvider: results) {
//modEncodeProvider


//while (i.hasNext()) {
//ModEncodeProvider provider = (ModEncodeProvider) i.next();
//Set<ExperimentSubmission> subs = provider.getExperimentSubmissions();
//
//Iterator e = subs.iterator();
//while (e.hasNext()) {
//providerSubs.put(provider.getName(), (ExperimentSubmission) e.next());
//}
//}



//Model model = os.getModel();
//PathQuery q = new PathQuery(model);
//
//q.setView("ModEncodeProvider.name, ModEncodeProvider.affiliation, " 
//+ "ModEncode.experimentSubmissions.title");

//ModEncodeProvider provider = (ModEncodeProvider) request.getAttribute("object");
//Set<ExperimentSubmission> subs = provider.getExperimentSubmissions();




