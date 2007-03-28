package org.flymine.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Iterator;

import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.Constants;

import org.flymine.model.genomic.Gene;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * Controller for geneMicroArrayDisplayer.jsp
 * @author Tom Riley
 */
public class GeneMicroArrayDisplayerController extends TilesAction
{
    /**
     * @see TilesAction#execute(ComponentContext, ActionMapping, ActionForm, HttpServletRequest,
     *                          HttpServletResponse)
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        try {
            HttpSession session = request.getSession();
            ObjectStore os =
                (ObjectStore) session.getServletContext().getAttribute(Constants.OBJECTSTORE);
            Gene gene = (Gene) request.getAttribute("object");
            Results results =
                MicroArrayHelper.queryExperimentsInvolvingGene(gene.getIdentifier(), os);
            ArrayList experiments = new ArrayList();
            for (Iterator iter = results.iterator(); iter.hasNext(); ) {
                ResultsRow row = (ResultsRow) iter.next();
                experiments.add(row.get(0));
            }
            request.setAttribute("experiments", experiments);
        } catch (Exception err) {
            err.printStackTrace();
        }

        return null;
    }
}
