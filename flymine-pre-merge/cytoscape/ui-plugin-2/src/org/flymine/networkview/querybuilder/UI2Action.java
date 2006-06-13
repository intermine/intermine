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
import javax.swing.JLabel;
import javax.swing.JList;

/**
 * @author Florian Reisinger
 */
public final class UI2Action extends AbstractAction 
{
    private final QueryBuilderPlugin plugin;
    private static final long serialVersionUID = 9999903902901L;
    private JLabel proteinLabel;
    private JList orgs;
    
    /**
     * @param plugin  the plugin this Action is used in
     * @param name the name of this Action
     * @param label JLabel to get the info from
     * @param list JList to get the organisms from
     */
    public UI2Action(QueryBuilderPlugin plugin, String name, JLabel label, JList list) {
        super(name);
        this.plugin = plugin;
        proteinLabel = label;
        orgs = list;
    }

    /**
     * @param e the ActionEvent receiced
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        StringBuffer sb = new StringBuffer();
        sb.append("==Fetch ortologs for these proteins:==\n");
        sb.append(proteinLabel.getText());
        sb.append("\n==From following organisms:==\n");
        Object[] selOrgs = orgs.getSelectedValues();
        for (int i = 0; i < selOrgs.length; i++) {
            sb.append(selOrgs[i] + "\n");
        }
        this.plugin.getQbl().showResults(sb.toString());
    }
}