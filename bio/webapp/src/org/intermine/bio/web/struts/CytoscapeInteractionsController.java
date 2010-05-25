package org.intermine.bio.web.struts;

import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.bio.web.AttributeLinkDisplayerController;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Interaction;
import org.intermine.model.bio.Protein;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Set up interaction network data for the cytoscapeInteractionsDisplayer.jsp
 *
 * @author Julie Sullivan
 * @author Fengyuan Hu
 *
 */
public class CytoscapeInteractionsController extends TilesAction
{
    protected static final Logger LOG = Logger.getLogger(AttributeLinkDisplayerController.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        // Get object from request
        InterMineObject object = (InterMineObject) request.getAttribute("object");

        // network data in flat format
        StringBuffer theNetwork = new StringBuffer();

        // Whether the object is a Gene or Protein
        if (object instanceof Protein) {
           Protein protein = (Protein) object;
           // In most cases, there is only 1 gene in the collect, but rare cases with more than 1
           Set<Gene> genes = protein.getGenes();

           for (Iterator<Gene> it = genes.iterator(); it.hasNext(); ) {
               Gene gene = (Gene) it.next();
               StringBuffer aNetwork = new StringBuffer();
               // Add the interaction network for this gene to the whole network (with duplication)
               theNetwork.append(createNetwork(gene, aNetwork)).append("\\n");
           }
        }
        else if (object instanceof Gene) {
            Gene gene = (Gene) object;

            StringBuffer aNetwork = new StringBuffer();
            theNetwork.append(createNetwork(gene, aNetwork));
        }

        request.setAttribute("networkdata", theNetwork.toString());

        return null;

    }

    private String createNetwork(Gene gene, StringBuffer aNetwork) {

        String genePID = gene.getPrimaryIdentifier(); // Name of the hub gene
        Set<Interaction> interactions = gene.getInteractions();

        Iterator<Interaction> it = interactions.iterator();
        while (it.hasNext()) {

            Interaction aInteraction = it.next();
            String interactionType = aInteraction.getInteractionType();
            Set<Gene> interactingGenes = aInteraction.getInteractingGenes();

            Iterator<Gene> itr = interactingGenes.iterator();
            while (itr.hasNext()) {
                Gene aInteractingGene = (Gene) itr.next();
                String interactingGenePID = aInteractingGene.getPrimaryIdentifier();

                // Build a line of network data, the data will be used in javascript in jsp,
                // js can only take "\n" in a string instead of real new line, so use "\\n" here
                aNetwork.append(genePID);
                aNetwork.append("\\t");
                aNetwork.append(interactionType);
                aNetwork.append("\\t");
                aNetwork.append(interactingGenePID);
                aNetwork.append("\\n");
        }
    }

        return aNetwork.toString();

    }

}
