package org.modmine.web;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for labs.jsp
 * @author Tom Riley
 */
public class LabsController extends TilesAction
{
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response)
        throws Exception {
        try {
            final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
            ObjectStore os = im.getObjectStore();

            //get the list of labs
            Query q = new Query();
            QueryClass qc = new QueryClass(Lab.class);
            QueryField qfSurname = new QueryField(qc, "surname");

            q.addFrom(qc);
            q.addToSelect(qc);
            q.addToOrderBy(qfSurname);

            //            Results results = os.executeSingleton(q);
            Results results = os.execute(q);

            Map<Lab, Set<Submission>> ps =
                    new LinkedHashMap<Lab, Set<Submission>>();

            Map<Lab, Project> pp =
                    new LinkedHashMap<Lab, Project>();

            // for each lab, get its attributes and set the values for jsp

            for (Iterator iter = results.iterator(); iter.hasNext(); ) {
                ResultsRow row = (ResultsRow) iter.next();

                Lab lab = (Lab) row.get(0);
                Set<Submission> subs = lab.getSubmissions();
                Project project = lab.getProject();

                ps.put(lab, subs);
                pp.put(lab, project);

            }

            request.setAttribute("experiments", ps);
            request.setAttribute("project", pp);

        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }
}
