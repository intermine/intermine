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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.intermine.objectstore.ObjectStore;


/**
 * just another user interface
 * this one uses a NetworkHandler to get information about 
 * the current network in cytoscape
 * @author Florian Reisinger
 */
public class QueryBuilderPlugin3 implements QueryBuilderPlugin
{
    private ObjectStore os;
    private QueryBuilderListener qbl;
    private NetworkHandlerIface handler;
    protected Collection accs;

    /**
     * set a ObjectStore that can be used to run queries
     * @param os ObjectStore to run queries on 
     */
    public void setObjectStore(ObjectStore os) {
        this.os = os;
    }

    /**
     * set a NetworkHandler that runs on the client an has access to 
     * current changes on the network
     * @param handler the NetworkHandler to use in the user interface
     */
    public void setNetworkHandler(NetworkHandlerIface handler) {
        this.handler = handler;
    }

    /**
     * retrieve this objects NetworkHandler
     * @return a NetworkHandler
     */
    public NetworkHandlerIface getNetworkHandler() {
        return this.handler;
    }

    /**
     * builds the actual user interface that will be integrated in 
     * the Cytoscape plugin to provide functionality
     * @return a JComponent that represents the user interface 
     */
    public JComponent buildUserInterface() {
        JScrollPane sp = new JScrollPane();
        sp.setName("expand from nodes");
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        
        JLabel text1 = new JLabel("<html><b>Expanding the network</b></html>");
        c.insets = new Insets(10, 0, 30, 0);
        c.gridy = 0;
        panel.add(text1, c);
        
        JLabel text2 = new JLabel("Expand network from this proteins:");
        c.insets = new Insets(10, 0, 0, 0);
        c.gridy = 1;
        panel.add(text2, c);
        
        JLabel proteins = new JLabel(handler.getSelectedNodesAsHTMLString());
        accs = handler.getSelectedNodes();
        //JLabel proteins = new JLabel("test1");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        c.gridy = 2;
        panel.add(proteins, c);
        
        JButton updateProteinsButton = new JButton(
                new UI31Action(this, "update list", proteins, accs));
        c.fill = 0;
        c.insets = new Insets(0, 50, 20, 0);
        c.gridy = 3;
        panel.add(updateProteinsButton, c);
        
//        JLabel text3 = new JLabel("in following organisms");
//        c.fill = 0;
//        c.insets = new Insets(20, 0, 0, 0);
//        c.gridy = 4;
//        panel.add(text3, c);
//        
//        JList list = new JList(getOrganisms());
//        list.setVisibleRowCount(3);
//        JScrollPane listScroller = new JScrollPane(list);
//        c.fill = GridBagConstraints.HORIZONTAL;
//        c.insets = new Insets(0, 0, 0, 0);
//        c.gridy = 5;
//        panel.add(listScroller, c);
//        
        JLabel fill = new JLabel();
        c.weighty = 0.5;
        c.fill = GridBagConstraints.REMAINDER;
        c.gridy = 6;
        panel.add(fill, c);
        
        JButton button1 = new JButton(new UI3Action(this, "expand", accs, os));
        c.weighty = 0;
        c.gridy = GridBagConstraints.PAGE_END;
        panel.add(button1, c);
        
        sp.getViewport().setView(panel);
        return sp;
    }

    /**
     * the QueryBuilderListener is used to integrate the inforation retrieved
     * by the user interface into the actual cytoscape network
     * @param qbl the listener that will take the results of the user interface
     */
    public void setQbl(QueryBuilderListener qbl) {
        this.qbl = qbl;
    }

    /**
     * get the QueryBuilderListener
     * @return this objects QueryBuilderListener
     * @see org.flymine.networkview.querybuilder.QueryBuilderPlugin#getQbl()
     */
    public QueryBuilderListener getQbl() {
        return qbl;
    }

    /**
     * this method has no funktionality yet, since there is only one QueryBuilderListener
     * and that should not be removed
     * @param listener the listener to remove
     */
    public void removeQueryBuilderListener(QueryBuilderListener listener) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @return true if this user interface needs an ObjectStore
     */
    public boolean needsObjectStore() {
        return true;
    }


    
}
