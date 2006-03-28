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
import java.util.Collection;
import java.util.Iterator;

import javax.swing.AbstractAction;

import org.apache.log4j.Logger;
import org.flymine.networkview.ProteinInteractionRetriever;
import org.flymine.networkview.network.FlyNetwork;
import org.flymine.networkview.network.FlyNode;
import org.intermine.objectstore.ObjectStore;

/**
 * Action to retrieve data for QueryBuilderPlugin3
 * @author Florian Reisinger
 */
public final class UI3Action extends AbstractAction 
{
    private static final Logger LOG = Logger.getLogger(UI3Action.class);
    private final QueryBuilderPlugin plugin;
    private static final long serialVersionUID = 9999903903901L;
    private Collection accs;
    private ObjectStore os;
    
    /**
     * @param plugin  the plugin this Action is used in
     * @param name the name of this Action
     * @param accs Collection of protein accessions
     * @param os the ObjectStore to run the Queries on
     */
    public UI3Action(QueryBuilderPlugin plugin, String name, Collection accs, ObjectStore os) {
        super(name);
        this.plugin = plugin;
        this.accs = accs;
        this.os = os;
    }

    /**
     * @param e the ActionEvent receiced
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        LOG.debug("======================= executing UI3Action");
        if (os == null) {
            LOG.warn("reference to ObjectStore is null.");
            // do nothing without ObjectStore
        } else { // ObjectStore is available
            if (accs == null) {
                LOG.warn("Collection of accessions is null.");
                // do nothing if there are no protein accessions to expand from
            } else { 
                if (accs.size() > 0) { // if protein accessions are specified -> expand
                    LOG.info(accs.size() + " protein accessions found.");
                    for (Iterator iter = accs.iterator(); iter.hasNext();) {
                        String acc = (String) iter.next();
                        LOG.debug("primary accession: " + acc);
                    }
                    LOG.info("fetching data to expand network...");
                    ProteinInteractionRetriever retriever = new ProteinInteractionRetriever(os);
                    LOG.info("building FlyNetwork...");
                    FlyNetwork net = retriever.expandNetworkFromProteins(accs);
                    //debugging
                    for (Iterator iter = net.getNodes().iterator(); iter.hasNext();) {
                        FlyNode element = (FlyNode) iter.next();
                        LOG.debug("retrieved Node (protein accession): " + element.getLabel());
                    } //debugging end
                    LOG.info("integrating FlyNetwork...");
                    this.plugin.getQbl().integrateResults(net, true);
                    LOG.info("FlyNetwork integrated.");
                } else {
                    LOG.warn("no accessions specified to expand from!");
                }
            }
        }
    }

}