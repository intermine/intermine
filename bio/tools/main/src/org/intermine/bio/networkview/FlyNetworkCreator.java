package org.intermine.bio.networkview;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Set;

import org.apache.log4j.Logger;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.ProteinInteraction;
import org.intermine.bio.networkview.network.FlyNetwork;
import org.intermine.bio.networkview.network.FlyNode;

/**
 * creates a FlyNetwork from a given Collection of ProteinInteractionS
 * @author Florian Reisinger
 * @author Richard Smith
 *
 */
public class FlyNetworkCreator
{
    private static final Logger LOG = Logger.getLogger(FlyNetworkCreator.class);
    
    /**
     * Creates a flymine network from a given Collection of ProteinInteractions
     * @param proteinInteractions Collection of ProteinInteractionS
     * @return the network representing the protein interactions
     */
    public static FlyNetwork createFlyNetwork(Collection<ProteinInteraction> proteinInteractions) {
        FlyNetwork fn = new FlyNetwork();

        for (ProteinInteraction pIon : proteinInteractions) {
            
            // TODO: do we want to use the short name? is it unique? -> label or attribute
            // !?there are more than one ProteinInteraction objects for one real interaction!?
            //String name = pIon.getShortName();

            if (pIon != null) {
                
                Set<Protein> interacting = (Set<Protein>) pIon.getInteractingProteins();
                if (interacting != null && !interacting.isEmpty()) {
                    // set whether this is a binary interaction or a complex
                    String type = (interacting.size() > 1) ? FlyNetwork.COMPLEX_INTERACTION_TYPE
                                                           : FlyNetwork.DEFAULT_INTERACTION_TYPE;
                    
                    FlyNode fnProtein = new FlyNode(pIon.getProtein().getPrimaryAccession());
                    fn.addNode(fnProtein);
                
                    // TODO if we want to add multiple edges if an interaction is observed
                    // in more than one experiment then we should add the experiment name
                    // or pubmed id as a label.  Otherwise only one edge will be created
                    // between each pair of proteins.
                    // TODO this may create two edges as we create interactions in both
                    // directions.  Should check for source and target instead of label.
                    for (Protein interact : interacting) {
                        FlyNode fnInteract = new FlyNode(interact.getPrimaryAccession());
                        fn.addNode(fnInteract);
                        
                        fn.addEdge(fnProtein, fnInteract, type);
                    }
                }
            }
        }
        LOG.debug("FlyNetwork created by FlyNetworkCreator: \n" + fn.toString(true));
        return fn;
    }

}
