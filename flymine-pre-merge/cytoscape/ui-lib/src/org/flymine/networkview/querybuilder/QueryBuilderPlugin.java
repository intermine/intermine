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

import javax.swing.JComponent;

import org.intermine.objectstore.ObjectStore;

/**
 * interface for the user interfaces that provide funktionality for the Cytoscape plugin
 * @author Florian Reisinger
 */
public interface QueryBuilderPlugin
{

    /**
     * set a ObjectStore that can be used to run queries
     * @param os ObjectStore to run queries on 
     */
    public void setObjectStore(ObjectStore os);

    /**
     * set a NetworkHandler that runs on the client an has access to 
     * current changes on the network
     * @param handler the NetworkHandler to use in the user interface
     */
    public void setNetworkHandler(NetworkHandlerIface handler);
    
    /**
     * retrieve this objects NetworkHandler
     * @return a NetworkHandler
     */
    public NetworkHandlerIface getNetworkHandler();
    
    /**
     * builds the actual user interface that will be integrated in 
     * the Cytoscape plugin to provide functionality
     * @return a JComponent that represents the user interface 
     */
    public JComponent buildUserInterface();

    /**
     * the QueryBuilderListener is used to integrate the inforation retrieved
     * by the user interface into the actual cytoscape network
     * @param qbl the listener that will take the results of the user interface
     */
    public void setQbl(QueryBuilderListener qbl);
    
    /**
     * retrieves this objects QueryBuilderListener
     * @return the QueryBuilderListener
     */
    public QueryBuilderListener getQbl();

    /**
     * this method has no funktionality yet, since there is only one QueryBuilderListener
     * and that should not be removed
     * @param listener the listener to remove
     */
    public void removeQueryBuilderListener(QueryBuilderListener listener);

    /**
     * @return true if this user interface needs an ObjectStore
     */
    public boolean needsObjectStore();
    
}
