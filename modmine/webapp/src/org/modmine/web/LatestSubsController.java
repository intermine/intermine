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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.model.bio.Submission;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.session.SessionMethods;

/**
 *
 * @author contrino
 *
 */

public class LatestSubsController extends TilesAction
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

            //get the list of projects
            Query q = new Query();
            QueryClass qc = new QueryClass(Submission.class);
            QueryField qfDate = new QueryField(qc, "publicReleaseDate");

            q.addFrom(qc);
            q.addToSelect(qc);
            q.addToOrderBy(qfDate, "desc");

            Results results = os.executeSingleton(q);

            Map<Integer, Submission> subs =
                    new LinkedHashMap<Integer, Submission>();
            // get all submission by date desc
            Integer order = 0;
            Iterator<Object> i = results.iterator();
            while (i.hasNext()) {
                order++;
                Submission sub = (Submission) i.next();
                subs.put(order, sub);
            }
            request.setAttribute("subs", subs);
        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }
}
