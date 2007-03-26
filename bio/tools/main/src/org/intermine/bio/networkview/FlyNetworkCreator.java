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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.intermine.bio.networkview.network.FlyEdge;
import org.intermine.bio.networkview.network.FlyNetwork;
import org.intermine.bio.networkview.network.FlyNode;

import org.apache.log4j.Logger;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.ProteinInteraction;
import org.flymine.model.genomic.ProteinInteractor;

/**
 * creates a FlyNetwork from a given Collection of ProteinInteractionS
 * @author Florian Reisinger
 *
 */
public class FlyNetworkCreator
{
    private static final Logger LOG = Logger.getLogger(FlyNetworkCreator.class);
    
    /**
     * Creates a flymine network from a given Collection of ProteinInteractionS
     * @param proteinInteractions Collection of ProteinInteractionS
     * @return the network representing the protein interactions
     */
    public static FlyNetwork createFlyNetwork(Collection proteinInteractions) {
        FlyNetwork fn = new FlyNetwork();

        // TODO: check that Collection really contains ProteinInteractions
        for (Iterator iter = proteinInteractions.iterator(); iter.hasNext();) {
            ProteinInteraction pIon = (ProteinInteraction) iter.next();
            // TODO: do we want to use the short name? is it unique? -> label or attribute
            // !?there are more than one ProteinInteraction objects for one real interaction!?
            //String name = pIon.getShortName();

            if (pIon != null) {
                ArrayList preys = new ArrayList();
                ArrayList baits = new ArrayList();
                ArrayList others = new ArrayList();

                Collection pIors = pIon.getInteractors();
                for (Iterator iterator = pIors.iterator(); iterator.hasNext();) {
                    ProteinInteractor pIor = (ProteinInteractor) iterator.next();
                    if (pIor.getRole().equalsIgnoreCase("prey")) {
                        preys.add(pIor.getProtein());
                    } else if (pIor.getRole().equalsIgnoreCase("bait")) {
                        baits.add(pIor.getProtein());
                    } else {
                        others.add(pIor.getProtein());
                    }
                }
                // one prey one bait -> standard case
                if (baits.size() == 1 && preys.size() == 1 && others.size() == 0) {
                    // TODO: check that all proteins have primaryAccession
                    FlyNode prey = new FlyNode(((Protein) preys.get(0)).getPrimaryAccession());
                    FlyNode bait = new FlyNode(((Protein) baits.get(0)).getPrimaryAccession());
                    FlyEdge fe = new FlyEdge(prey, bait);

                    fn.addNode(prey);
                    fn.addNode(bait);
                    fn.addEdge(fe);
                } else { // not prey-bait standard case
                    // link all proteins to each other
                    // TODO: test this
                    others.addAll(preys);
                    others.addAll(baits);
                    Protein[] p = new Protein[1];
                    p = (Protein[]) others.toArray(p);
                    FlyNode[] nx = new FlyNode[p.length];
                    for (int i = 0; i < p.length; i++) {
                        String s = p[i].getPrimaryAccession();
                        nx[i] = new FlyNode(s);
                        fn.addNode(nx[i]);
                    }
                    for (int i = 0; i < others.size(); i++) {
                        for (int j = i + 1; j < others.size(); j++) {
                            FlyEdge ex = new FlyEdge(nx[i], nx[j],
                                    FlyNetwork.COMPLEX_INTERACTION_TYPE);
                            fn.addEdge(ex);
                        }
                    }
                }
                
            }
        }
        LOG.debug("FlyNetwork created by FlyNetworkCreator: \n" + fn.toString(true));
        return fn;
    }

}
