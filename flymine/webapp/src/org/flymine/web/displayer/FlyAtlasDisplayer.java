package org.flymine.web.displayer;

/*
 * Copyright (C) 2002-2017 FlyMine
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

import org.intermine.api.InterMineAPI;
import org.intermine.model.bio.FlyAtlasResult;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.MicroArrayResult;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.json.JSONArray;

/**
 * Displayer for flyatlas expression data.
 * @author Alex
 */
public class FlyAtlasDisplayer extends ReportDisplayer
{

    /**
     * @param config configuration object
     * @param im intermine API
     */
    public FlyAtlasDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {

        Gene gene = (Gene) reportObject.getObject();

        List<Double> signals = new ArrayList<Double>();
        List<String> names = new ArrayList<String>();
        List<String> affyCalls = new ArrayList<String>();
        List<Double> enrichments = new ArrayList<Double>();
        List<Integer> presentCalls = new ArrayList<Integer>();
        List<String> objectIds = new ArrayList<String>();

        for (MicroArrayResult mar: gene.getMicroArrayResults()) {
            if (mar instanceof FlyAtlasResult) {
                FlyAtlasResult far = (FlyAtlasResult) mar;
                objectIds.add(String.valueOf(far.getId()));
                signals.add(far.getMRNASignal());
                names.add(far.getTissue().getName());
                affyCalls.add(far.getAffyCall());
                enrichments.add(far.getEnrichment());
                presentCalls.add(far.getPresentCall());
            }
        }

        request.setAttribute("signals", signals.toString());
        request.setAttribute("names", new JSONArray(names));
        request.setAttribute("affyCalls", new JSONArray(affyCalls));
        request.setAttribute("enrichments", enrichments.toString());
        request.setAttribute("presentCalls", presentCalls.toString());
        request.setAttribute("objectIds", new JSONArray(objectIds));
    }

}
