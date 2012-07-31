package org.metabolicmine.web;

/*
 * Copyright (C) 2002-2012 metabolicMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.util.NameUtil;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.bag.BagHelper;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.ForwardParameters;
import org.intermine.web.struts.InterMineAction;

/**
 * metabolicMine SNP list to nearby Genes results/list
 * @author radek
 *
 */
public class SnpToGeneAction extends InterMineAction {

    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        final InterMineAPI im = SessionMethods.getInterMineAPI(request
                .getSession());
        ObjectStore os = im.getObjectStore();

        // get the associated form
        SnpToGeneForm snpToGeneForm = (SnpToGeneForm) form;

        // fetch the form input
        String bagName = snpToGeneForm.getBagName();
        String distance = snpToGeneForm.getDistance();
        String direction = snpToGeneForm.getDirection();

        // create PathQuery
        PathQuery q = new PathQuery(os.getModel());

        String action = (String) request.getParameter("action");
        // based on <submit> button pressed...
        if (action.equals("Result")) {
            // a results table
            q = snpToGene(q, bagName, direction, distance);

            String qid = SessionMethods.startQueryWithTimeout(request, false, q);
            Thread.sleep(200);
            String trail = "|" + bagName;
            // do not forget to setup in "struts-config-model.xml"
            return new ForwardParameters(mapping.findForward("waiting"))
                    .addParameter("qid", qid).addParameter("trail", trail)
                    .forward();
        } else {
            // a list
            q = snpToGeneList(q, bagName, direction, distance);

            HttpSession session = request.getSession();
            Profile profile = SessionMethods.getProfile(session);

            String newBagName = NameUtil.generateNewName(profile.getSavedBags().keySet(), bagName);
            InterMineBag imBag = BagHelper.createBagFromPathQuery(q, newBagName, q.getDescription(), "Gene", profile, im);

            // on empty new bag, return the old bag
            if (imBag.getSize() > 0) {
                // do not forget to setup in "struts-config-model.xml"
                ForwardParameters forwardParameters = new ForwardParameters(mapping.findForward("bagDetails"));
                return forwardParameters.addParameter("bagName", newBagName).forward();
            } else {
                // message
                SessionMethods.recordError("No results found, reverting to the original SNP list.", session);
                // show the old stuff
                ForwardParameters forwardParameters = new ForwardParameters(mapping.findForward("bagDetails"));
                return forwardParameters.addParameter("bagName", bagName).forward();
            }
        }
    }

    /**
     * Construct PathQuery for result page
     * @param query
     * @param bagName
     * @param direction
     * @param distance
     * @return
     */
    private PathQuery snpToGene(PathQuery query, String bagName,
            String direction, String distance) {
        query
                .addViews(
                        "SNP.overlappingFeatures.gene.primaryIdentifier",
                        "SNP.overlappingFeatures.gene.name",
                        "SNP.overlappingFeatures.gene.symbol",
                        "SNP.overlappingFeatures.direction",
                        "SNP.overlappingFeatures.distance",
                        "SNP.overlappingFeatures.gene.chromosomeLocation.start",
                        "SNP.overlappingFeatures.gene.chromosomeLocation.end",
                        "SNP.overlappingFeatures.gene.chromosomeLocation.locatedOn.primaryIdentifier");

        query.addConstraint(Constraints.in("SNP", bagName));
        query.addConstraint(Constraints.type("SNP.overlappingFeatures",
                "GeneFlankingRegion"));

        if (! direction.equals("both")) {
            query.addConstraint(Constraints.eq("SNP.overlappingFeatures.direction", direction));
        }

        query.addConstraint(Constraints.eq("SNP.overlappingFeatures.distance",
                distance));
        query.addConstraint(Constraints.eq(
                "SNP.overlappingFeatures.includeGene", "true"));

        return query;
    }

    /**
     * Construct PathQuery for list page (only add main *.id view)
     * @param query
     * @param bagName
     * @param direction
     * @param distance
     * @return
     */
    private PathQuery snpToGeneList(PathQuery query, String bagName,
            String direction, String distance) {
        query.addView("SNP.overlappingFeatures.gene.id");

        query.addConstraint(Constraints.in("SNP", bagName));
        query.addConstraint(Constraints.type("SNP.overlappingFeatures",
                "GeneFlankingRegion"));

        if (! direction.equals("both")) {
            query.addConstraint(Constraints.eq("SNP.overlappingFeatures.direction", direction));
        }

        query.addConstraint(Constraints.eq("SNP.overlappingFeatures.distance",
                distance));
        query.addConstraint(Constraints.eq(
                "SNP.overlappingFeatures.includeGene", "true"));

        return query;
    }

}
