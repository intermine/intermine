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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.model.bio.Experiment;
import org.intermine.model.bio.Project;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.web.logic.Constants;

/**
 * Read experiment and submission details into DisplayExperiments, cache results.
 * @author Richard Smith
 *
 */

public class ExperimentController extends TilesAction 
{
    
    private static List<DisplayExperiment> experimentCache = null;
    
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(@SuppressWarnings("unused")  ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        
        if (experimentCache == null) {
            readExperiments(request);
        }
        request.setAttribute("experiments", experimentCache);
        return null;
    }
    
    private void readExperiments(HttpServletRequest request) {
        
        try {
            HttpSession session = request.getSession();
            ObjectStore os =
                (ObjectStore) session.getServletContext().getAttribute(Constants.OBJECTSTORE);
            
            Query q = new Query();  
            QueryClass qcProject = new QueryClass(Project.class);
            QueryField qcName = new QueryField(qcProject, "name");
            q.addFrom(qcProject);
            q.addToSelect(qcProject);
            
            QueryClass qcExperiment = new QueryClass(Experiment.class);
            q.addFrom(qcExperiment);
            q.addToSelect(qcExperiment);

            QueryCollectionReference projExperiments = new QueryCollectionReference(qcProject, "experiments");
            ContainsConstraint cc = new ContainsConstraint(projExperiments, ConstraintOp.CONTAINS, qcExperiment);
            
            q.setConstraint(cc);
            q.addToOrderBy(qcName);
            
            
            Results results = os.execute(q);
            
            experimentCache = new ArrayList<DisplayExperiment>();
            
            Iterator i = results.iterator();
            while (i.hasNext()) {
                ResultsRow row = (ResultsRow) i.next();

                Project project = (Project) row.get(0);
                Experiment experiment = (Experiment) row.get(1);

                DisplayExperiment displayExp = new DisplayExperiment(experiment, project);
                experimentCache.add(displayExp);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }
}
