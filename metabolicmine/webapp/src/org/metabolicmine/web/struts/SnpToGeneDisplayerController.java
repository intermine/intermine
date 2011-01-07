package org.metabolicmine.web.struts;

/*
 * Copyright (C) 2002-2011 metabolicMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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

        try {
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

                ArrayList<ArrayList<String>> list = new ArrayList<ArrayList<String>>();
                int size = 0; String lastID = "";

                listing:
                    while (result.hasNext()) {
                        // bags
                        List<ResultElement> row = result.next();
                        ArrayList<String> columns = new ArrayList<String>();

                        // locations
                        int snpStart = 0, geneStart = 0, geneEnd = 0; String direction = null, currentID = null;

                        // traverse columns returned (query.addViews)
                        if (size == 0) size = row.size();
                        for (int i=1; i<size; i++) { // skip SNP.primaryIdentifier
                            Object e = row.get(i).getField();

                            // parse result
                            switch (i) {
                                case 2: // Gene primaryIdentifier
                                    currentID = e.toString();
                                    if (lastID.equals(currentID)) {
                                        // do not repeat ourselves in saving the same Gene 2x
                                        continue listing;
                                    } else {
                                        columns.add(currentID);
                                        lastID = currentID;
                                        break;
                                    }
                                case 5: snpStart = Integer.parseInt(e.toString()); break; // SNP start
                                case 6: geneStart = Integer.parseInt(e.toString()); break; // Gene start
                                case 7: geneEnd = Integer.parseInt(e.toString()); break; // Gene end
                                case 8: direction = e.toString(); break; // direction
                                default: //everything else
                                    if (e != null) {
                                        columns.add(e.toString());
                                    } else {
                                        columns.add("[no value]");
                                    }
                            }
                        }

                        // calculate distance
                        if (snpStart <= geneEnd) {
                            if (snpStart >= geneStart) {
                                //columns.add("genic");
                                columns.add("0"); // distance for the comparator, comes last!
                                columns.add("");
                            } else {
                                //columns.add(geneStart - snpStart + "b " + direction);
                                columns.add(Integer.toString(geneStart - snpStart)); // distance for the comparator, comes last!
                                columns.add(direction);
                            }
                        } else {
                            //columns.add(snpStart - geneEnd + "b " + direction);
                            columns.add(Integer.toString(snpStart - geneEnd)); // distance for the comparator, comes last!
                            columns.add(direction);
                        }

                        // add row
                        list.add(columns);
                    }

                // sort list by distance
                Collections.sort(list, new Comparator(){
                    // comparator
                    public int compare(Object first, Object second) {
                        // convert from generic Object
                        ArrayList<String> firstGene = (ArrayList<String>)first; ArrayList<String> secondGene = (ArrayList<String>)second;
                        // get the distance as an int
                        int firstGeneDistance = Integer.parseInt(firstGene.get(firstGene.size() -2));
                        int secondGeneDistance = Integer.parseInt(secondGene.get(secondGene.size() -2));

                        // "comparator"
                        return firstGeneDistance - secondGeneDistance;
                    }

                });

                request.setAttribute("list", list);
            //}
        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }

    private PathQuery snpToGene(String snpPrimaryIdentifier, PathQuery query) {
        query.addViews(
                "SNP.primaryIdentifier", // will be skipped, required for query to run

                "SNP.overlappingFeatures.gene.id",
                "SNP.overlappingFeatures.gene.primaryIdentifier",

                "SNP.overlappingFeatures.gene.name",
                "SNP.overlappingFeatures.gene.symbol",
                "SNP.locations.start",
                "SNP.overlappingFeatures.gene.locations.start",
                "SNP.overlappingFeatures.gene.locations.end",
                "SNP.overlappingFeatures.direction"
                );

        query.addOrderBy("SNP.primaryIdentifier", OrderDirection.ASC);

        query.addConstraint(Constraints.eq("SNP.primaryIdentifier", snpPrimaryIdentifier));
        query.addConstraint(Constraints.type("SNP.overlappingFeatures", "GeneFlankingRegion"));
        query.addConstraint(Constraints.eq("SNP.overlappingFeatures.distance", "10.0kb"));

        return query;
    }

}
