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

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Protein;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;

/**
 * Displayer for gene/protein interactions using cytoscape plugin
 * @author Yo
 */
public class CytoscapeNetworkDisplayer extends ReportDisplayer
{


    /**
     * Construct with config and the InterMineAPI.
     * @param config to describe the report displayer
     * @param im the InterMine API
     */
    public CytoscapeNetworkDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
         InterMineObject object = reportObject.getObject();
         request.setAttribute("cytoscapeInteractionObjectId", object.getId());
         // chenyian: determine the the interaction size here instead of jsp 
         int size = 0;
         if (object instanceof Gene) {
        	 size = ((Gene) object).getInteractions().size();
         } else if (object instanceof Protein) {
        	 if (((Protein) object).getGenes() != null) {
        		 // if there are multiple genes, just arbitrary take the first one 
        		 size = ((Protein) object).getGenes().iterator().next().getInteractions().size();
        	 }
         } else {
         	throw new RuntimeException("Unexpected type: " + object.getClass().getName());
         }
         request.setAttribute("interactionSize", Integer.valueOf(size));
    }
}
