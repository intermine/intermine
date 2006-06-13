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

import javax.swing.AbstractAction;
import javax.swing.JLabel;

/**
 * @author Florian Reisinger
 */
public final class UI31Action extends AbstractAction 
{
    private final QueryBuilderPlugin plugin;
    private static final long serialVersionUID = 9999903903902L;
    private JLabel proteins;
    protected Collection accs;
    
    /**
     * Constructor
     * @param plugin the plugin this Action is used in
     * @param name the name of this Action
     * @param label the JLabel to to show the info on
     * @param accs Collection of protein accessions
     */
    public UI31Action(QueryBuilderPlugin plugin, String name, JLabel label, Collection accs) {
        super(name);
        this.plugin = plugin;
        this.proteins = label;
        this.accs = accs;
    }

    /**
     * @param e the ActionEvent receiced
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        proteins.setText(this.plugin.getNetworkHandler().getSelectedNodesAsHTMLString());
        accs.addAll(this.plugin.getNetworkHandler().getSelectedNodes());
    }
}