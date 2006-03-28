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
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JList;
import javax.swing.JOptionPane;

/**
 * @author Florian Reisinger
 */
public final class UI41Action extends AbstractAction 
{
    // not very pretty not to use the interface but QueryBuilderPlugin4
    private final QueryBuilderPlugin4 plugin;
    private static final long serialVersionUID = 9999903904902L;
    private JList organisms;
    private JList publications;
    
    /**
     * Constructor
     * @param plugin the plugin this Action is used in
     * @param name the name of this Action
     * @param orgList reference to the list of organisms
     * @param pubList reference to the list of publications
     */
    public UI41Action(QueryBuilderPlugin4 plugin, String name, JList orgList, JList pubList) {
        super(name);
        this.plugin = plugin;
        this.organisms = orgList;
        this.publications = pubList;
    }

    /**
     * get the currently selected organism and run a query to get all publications on 
     * protein interactions for that organism, then update the publication list
     * @param e the ActionEvent receiced
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        String org = (String) organisms.getSelectedValue();
        if (org != null) {
            Vector pubs = plugin.getPubForOrg(org);
            publications.setListData(pubs);
        } else {
            JOptionPane.showMessageDialog(null, "choose an organism first!");
        }
    }
}
