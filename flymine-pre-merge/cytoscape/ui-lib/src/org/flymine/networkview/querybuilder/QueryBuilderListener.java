package org.flymine.networkview.querybuilder;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.flymine.networkview.network.FlyNetwork;

/**
 * @author Florian Reisinger
 */
public interface QueryBuilderListener
{
    
    /**
     * send some results to the Cytoscape plugin to handle it
     * @param s String to send to the Cytoscape plugin
     */
    public void showResults(String s);
    
    /**
     * this methode is responsible for integrating the information 
     * contained in the FlyNetwork net into cytoscape
     * @param net the network containing the information to interate
     * @param append flag indicating whether to create a new network or to 
     * append to the currrent one
     */
    public void integrateResults(FlyNetwork net, boolean append);
    
    //TODO: add method to create new network
}
