package org.metabolicmine.web.struts;

/*
 * Copyright (C) 2002-2010 metabolicMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.SNP;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.pathquery.OrderDirection;

public class SnpToGeneDisplayerController extends TilesAction {

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "unchecked", "null" })
    public ActionForward execute(@SuppressWarnings("unused")  ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {

        //try {
            HttpSession session = request.getSession();
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            Model model = im.getModel();
            PathQuery query = new PathQuery(model);

            InterMineObject object = (InterMineObject) request.getAttribute("object");

            //if (object instanceof SNP) {
                SNP snp = (SNP)object;
                query = snpToGene(snp.getPrimaryIdentifier(), query);

                Profile profile = SessionMethods.getProfile(session);
                PathQueryExecutor executor = im.getPathQueryExecutor(profile);
                ExportResultsIterator result = executor.execute(query);

                List<List<ResultElement>> stuff = new ArrayList<List<ResultElement>>();
                while (result.hasNext()) {
                    List<ResultElement> row = result.next();
                    stuff.add(row);
                }

                request.setAttribute("response", stuff);
            //}
        //} catch (Exception err) {
        //    err.printStackTrace();
        //}
        return null;
    }

    private PathQuery snpToGene(String snpPrimaryIdentifier, PathQuery query) {
        // Add views
        query.addViews("SNP.primaryIdentifier", "SNP.overlappingFeatures.gene.primaryIdentifier", "SNP.overlappingFeatures.gene.symbol");

        // Add orderby
        query.addOrderBy("SNP.primaryIdentifier", OrderDirection.ASC);

        // Add constraints and you can edit the constraint values below
        query.addConstraint(Constraints.eq("SNP.primaryIdentifier", snpPrimaryIdentifier));
        query.addConstraint(Constraints.type("SNP.overlappingFeatures", "GeneFlankingRegion"));
        query.addConstraint(Constraints.eq("SNP.overlappingFeatures.distance", "10.0kb"));

        return query;
    }

}
