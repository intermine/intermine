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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JList;

import org.apache.log4j.Logger;
import org.flymine.networkview.network.FlyNetwork;

/**
 * @author Florian Reisinger
 */
public class UI4Action extends AbstractAction
{
    private static final Logger LOG = Logger.getLogger(UI4Action.class);
    // not very pretty not to use the interface but QueryBuilderPlugin4
    private QueryBuilderPlugin4 plugin;
    private static final long serialVersionUID = 9999903904902L;
    private JList publications;
    private boolean create;
    
    /**
     * Constructor
     * @param plugin the plugin this Action is used in
     * @param name the name of this Action
     * @param pubList reference to the list of publications
     * @param create will create a new network if set to true, appends current network otherwise
     */
    public UI4Action(QueryBuilderPlugin4 plugin, String name, JList pubList, boolean create) {
        super(name);
        this.plugin = plugin;
        this.publications = pubList;
        this.create = create;
    }

    /**
     * get the currently selected pubMedId and run a query to get all interactions 
     * for that publication, then create a new network
     * @param e the ActionEvent receiced
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        String pub = (String) publications.getSelectedValue();
        FlyNetwork net = plugin.getInteractionsForPub(pub);
        if (create) {   // do NOT append to current network
            LOG.debug("appending current network...");
            plugin.getQbl().integrateResults(net, false);
        } else {    // do append to current network
            LOG.debug("creating new network...");
            plugin.getQbl().integrateResults(net, true);
        }
        net = null; // do not keep a reference
    }
}
