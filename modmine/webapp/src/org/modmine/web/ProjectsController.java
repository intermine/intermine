package org.modmine.web;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.model.bio.Lab;
import org.intermine.model.bio.Project;
import org.intermine.model.bio.Submission;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.web.logic.session.SessionMethods;
import org.modmine.web.MetadataCache.GBrowseTrack;

/**
 * 
 * @author contrino
 *
 */

public class ProjectsController extends TilesAction 
{
    private static final Logger LOG = Logger.getLogger(MetadataCache.class);
    
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
            final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
            ObjectStore os = im.getObjectStore();            
            final ServletContext servletContext = servlet.getServletContext();

            
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
            
            
            
            
            
            
            
            List<DisplayExperiment> experiments;
            
            String experimentName = request.getParameter("experiment");
            if (experimentName != null) {
                experiments = new ArrayList<DisplayExperiment>();
                experiments.add(MetadataCache.getExperimentByName(os, experimentName));
            } else {
                experiments = MetadataCache.getExperiments(os);
            }
            request.setAttribute("experiments", experiments);
            
            Map<String, List<GBrowseTrack>> tracks = MetadataCache.getExperimentGBrowseTracks(os);
            request.setAttribute("tracks", tracks);
            
            Map<Integer, List<GBrowseTrack>> subTracks = MetadataCache.getGBrowseTracks();
            request.setAttribute("subTracks", subTracks);
            
//            Map<Integer, List<String>> files = MetadataCache.getSubmissionFiles(os);
//            request.setAttribute("files", files);
//
//            Map<Integer, Integer> filesPerSub = MetadataCache.getFilesPerSubmission(os);
//            request.setAttribute("filesPerSub", filesPerSub);

            Map<Integer, List<String[]>> submissionRepositoryEntries = MetadataCache.getRepositoryEntries(os);
            request.setAttribute("subRep", submissionRepositoryEntries);

            Map<Integer, List<String>> unlocatedFeatureTypes = MetadataCache.getUnlocatedFeatureTypes(os);
            request.setAttribute("unlocatedFeat", unlocatedFeatureTypes);
            
            Map<String, String> expFeatureDescription = MetadataCache.getFeatTypeDescription(servletContext);
            request.setAttribute("expFeatDescription", expFeatureDescription);
            

            
            
            Properties props = new Properties(); 
            
            InputStream is = servletContext.getResourceAsStream("/WEB-INF/experimentCategory.properties");
            if (is == null) {
                LOG.info("Unable to find /WEB-INF/experimentCategory.properties!");
            } else {
                try {
                    props.load(is);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            Map <String, List<String>> expCat = new HashMap<String, List<String>>();

            Set ugo = props.keySet();
            
            for (Object exp : ugo){
                String cats = props.getProperty(exp.toString());
                // an experiment can be associated to more than 1 category
                String[] cat = cats.split("#");
                List<String> catList = new ArrayList<String>();
                for (String c : cat){
                    catList.add(c);
                }
                expCat.put(exp.toString(), catList);
            }
            
            request.setAttribute("expCats", expCat);

            
            
            
            
            
        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }
}
