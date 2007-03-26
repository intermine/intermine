package org.intermine.metadata.viewer;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.Set;


import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JFrame;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.intermine.util.TypeUtil;
import org.intermine.modelproduction.xml.InterMineModelParser;
import org.intermine.metadata.*;


/**
 * Simple Swing application for displaying an InterMine model XML file
 * with class browser and class detail panes.
 *
 * @author Richard Smith
 */
public class ModelViewer extends JPanel implements TreeSelectionListener
{
    private Model model;
    private JLabel cldLabel;
    private JTree tree;


    /**
     * Start up ModelViewer with an InterMine model
     * @param model the model to view
     */
    public ModelViewer(Model model) {
        super(new BorderLayout());
        
        this.model = model;
        DefaultMutableTreeNode top =
            new DefaultMutableTreeNode(model.getName());
        createNodes(top);
        
        //Create a tree that allows one selection at a time.
        tree = new JTree(top);
        tree.getSelectionModel().setSelectionMode
                (TreeSelectionModel.SINGLE_TREE_SELECTION);

        //Listen for when the selection changes.
        tree.addTreeSelectionListener(this);

        //Create the scroll pane and add the tree to it.
        JScrollPane treeView = new JScrollPane(tree);

        // Create ClassDescriptor display
        cldLabel = new JLabel();
        JScrollPane cldPane = new JScrollPane(cldLabel);

        //Add the scroll panes to a split pane.
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(treeView);
        splitPane.setRightComponent(cldPane);

        // dimensions
        Dimension minimumSize = new Dimension(400, 50);
        cldPane.setMinimumSize(minimumSize);
        treeView.setMinimumSize(minimumSize);
        splitPane.setDividerLocation(400);

        splitPane.setPreferredSize(new Dimension(800, 500));

        add(splitPane, BorderLayout.CENTER);
    }

    /**
     * @see TreeSelectionListener#valueChanged(TreeSelectionEvent)
     */
    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                           tree.getLastSelectedPathComponent();

        if (node == null) {
            return;
        }

        ClassDescriptor cld = model.
                        getClassDescriptorByName(model.getPackageName()
                             + "." + node.getUserObject().toString());
        if (cld != null) {
            renderClassDescriptor(cld);
        }
    }

    private void createNodes(DefaultMutableTreeNode top) {
        Iterator iter = model.getClassDescriptors().iterator();
        while (iter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) iter.next();
            Set supers = cld.getSuperDescriptors();
            // add any top level ClassDescriptors
            if (supers.size() == 0) {
                top.add(createNode(cld));
            }
        }
    }

    private DefaultMutableTreeNode createNode(ClassDescriptor cld) {
        DefaultMutableTreeNode node =
            new DefaultMutableTreeNode(TypeUtil.unqualifiedName(cld.getName()));
        Iterator subs = cld.getSubDescriptors().iterator();
        while (subs.hasNext()) {
            node.add(createNode((ClassDescriptor) subs.next()));
        }
        return node;
    }

    private void renderClassDescriptor(ClassDescriptor cld) {
        String text;
        text = "<html><b>" + TypeUtil.unqualifiedName(cld.getName()) + "</b><br>";
        text += "<table>";
        Iterator iter = cld.getAllAttributeDescriptors().iterator();
        while (iter.hasNext()) {
            AttributeDescriptor atd = (AttributeDescriptor) iter.next();
            text += "<tr><td>" + atd.getName() + "</td><td>" + atd.getType()
                + "</td></tr>";
        }
        iter = cld.getAllReferenceDescriptors().iterator();
        while (iter.hasNext()) {
            ReferenceDescriptor rfd = (ReferenceDescriptor) iter.next();
            text += "<tr><td>" + rfd.getName() + "</td><td>"
                + TypeUtil.unqualifiedName(rfd.getReferencedClassDescriptor().getName())
                + "</td></tr>";
        }
        iter = cld.getAllCollectionDescriptors().iterator();
        while (iter.hasNext()) {
            CollectionDescriptor cod = (CollectionDescriptor) iter.next();
            text += "<tr><td>" + cod.getName() + "</td><td>"
                + TypeUtil.unqualifiedName(cod.getReferencedClassDescriptor().getName())
                + "</td></tr>";
        }
        text += "</table></html>";
        cldLabel.setText(text);
    }

    private static void createAndShowGUI(Model model) {
        //Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        //Create and set up the window.
        JFrame frame = new JFrame("ModelViewer: " + model.getName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        ModelViewer viewer = new ModelViewer(model);
        viewer.setOpaque(true); //content panes must be opaque
        frame.getContentPane().add(viewer);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Start ModelViewer with given InterMine model XML file
     * @param args filename to view
     * @throws Exception if problem accessing file
     */
    public static void main(String[] args) throws Exception {
        String fileName = args[0];
        if (fileName == null || fileName.equals("")) {
            throw new IllegalArgumentException("Must supply a filename");
        }


        // how does this work with command line parameters?
       //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        //        javax.swing.SwingUtilities.invokeLater(new Runnable() {
        //    public void run() {
        InterMineModelParser parser = new InterMineModelParser();
        Model dataModel = parser.process(new FileReader(new File(fileName)));
        createAndShowGUI(dataModel);
                //    }
                //});
    }
}
