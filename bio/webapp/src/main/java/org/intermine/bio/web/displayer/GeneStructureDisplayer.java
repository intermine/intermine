package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.bio.web.model.GeneModel;
import org.intermine.bio.web.model.GeneModelCache;
import org.intermine.bio.web.model.GeneModelSettings;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;

/**
 * Custom displayer for gene structure.  A list of GeneModels will be added to the request, with
 * each GeneModel representing one transcript of the gene.  Each gene model includes a transcript,
 * exons, introns, UTRs and CDSs where available.  This displayer can be used for any component of a
 * gene model and the parent gene will be retrieved first.
 * @author rns
 *
 */
public class GeneStructureDisplayer extends ReportDisplayer
{
    /**
     * Construct with config and the InterMineAPI.
     * @param config to describe the report displayer
     * @param im the InterMine API
     */
    public GeneStructureDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        List<GeneModel> geneModels =
            GeneModelCache.getGeneModels(reportObject.getObject(), im.getModel());

        if (geneModels != null && !geneModels.isEmpty()) {
            Gene gene = geneModels.get(0).getGene();
            request.setAttribute("gene", gene);
        }
        String organismName = ((SequenceFeature) reportObject.getObject()).getOrganism().getName();
        GeneModelSettings settings =
            GeneModelCache.getGeneModelOrganismSettings(organismName, im.getObjectStore());
        request.setAttribute("settings", settings);
        request.setAttribute("geneModels", geneModels);
        request.setAttribute("actualId", reportObject.getId());
    }
}
