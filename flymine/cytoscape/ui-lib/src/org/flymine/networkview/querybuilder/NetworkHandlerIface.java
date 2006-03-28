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

import java.util.Collection;

/**
 * @author Florian Reisinger
 */
public interface NetworkHandlerIface
{

    /**
     * gets all currently selected nodes of the current network view
     * @return a Collection of identifiers (StringS) of the nodes
     */
    public Collection getSelectedNodes();
    
    /**
     * creates a String of html that can be used to have mulitple lines an a JLabel
     * e.g. '<html>acc1<br>acc2</html>'
     * @return a String containing html
     */
    public String getSelectedNodesAsHTMLString();
    
    /**
     * gets all currently selected edges of the current network view
     * @return a Collection of identifiers (StringS) of the edges
     */
    public Collection getSelectedEdges();
    
    /**
     * sets all Nodes of the current network "selected"
     */
    public void flaggAllNodes();
    
    /**
     * sets all Nodes of the current network "unselected"
     */
    public void unflaggAllNodes();
    
    /**
     * sets all Edges of the current network "selected"
     */
    public void flaggAllEdges();
    
    /**
     * sets all Edges of the current network "unselected"
     */
    public void unflaggAllEdges();
}
