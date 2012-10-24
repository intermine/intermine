package org.flymine.web;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.model.bio.Gene;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for geneMicroArrayDisplayer.jsp
 * @author Tom Riley
 */
public class GeneMicroArrayDisplayerController extends TilesAction
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
            final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
            ObjectStore os = im.getObjectStore();
            Gene gene = (Gene) request.getAttribute("object");
            Results results =
                MicroArrayHelper.queryExperimentsInvolvingGene(gene.getPrimaryIdentifier(), os);
            if (results != null) {
                ArrayList<Object> experiments = new ArrayList<Object>();
                for (Iterator iter = results.iterator(); iter.hasNext(); ) {
                    ResultsRow row = (ResultsRow) iter.next();
                    experiments.add(row.get(0));
                }
                request.setAttribute("experiments", experiments);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }

        return null;
    }
}
