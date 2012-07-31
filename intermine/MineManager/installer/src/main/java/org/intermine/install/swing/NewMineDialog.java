package org.intermine.install.swing;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.Serializable;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import org.intermine.common.swing.ButtonPanel;
import org.intermine.common.swing.GridBagHelper;
import org.intermine.common.swing.Messages;
import org.intermine.common.swing.StandardJDialog;
import org.intermine.common.swing.text.RestrictedInputDocument;
import org.intermine.install.properties.InterminePropertyKeys;
import org.intermine.modelviewer.store.MineManagerBackingStore;


/**
 * Dialog for starting the mine creation process.
 */
public class NewMineDialog extends StandardJDialog
{
    private static final long serialVersionUID = 2042525611348637283L;

    /**
     * File chooser for selecting the Intermine home directory.
     * @serial
     */
    private JFileChooser directoryChooser;
    
    /**
     * Text field for the Intermine home directory.
     * @serial
     */
    private JTextField homeField = new JTextField(40);
    
    /**
     * Text field for the mine name.
     * @serial
     */
    private JTextField mineNameField =
        new JTextField(new RestrictedInputDocument(
                RestrictedInputDocument.WORD_CHARACTERS), "", 40);
    
    /**
     * Action to move on through the creation chain.
     * @serial
     */
    private Action okAction = new NextAction();
    
    /**
     * The database settings dialog that follows this dialog in the mine set up chain.
     * @serial
     */
    private CreateDatabaseDialog createDatabaseDialog;
    
    
    /**
     * The Intermine home directory.
     */
    private transient File intermineDir;
    
    /**
     * Flag indicating the Intermine home directory set actually is an
     * Intermine installation.
     */
    private transient boolean intermineDirCorrect;

    
    /**
     * Initialise with a parent Frame.
     * @param owner The parent Frame.
     */
    public NewMineDialog(Frame owner) {
        super(owner);
        init();
    }

    /**
     * Initialise with a parent Dialog.
     * @param owner The parent Dialog.
     */
    public NewMineDialog(Dialog owner) {
        super(owner);
        init();
    }

    /**
     * Initialise with a parent Window.
     * @param owner The parent Window.
     */
    public NewMineDialog(Window owner) {
        super(owner);
        init();
    }

    /**
     * Common initialisation: lays out the child components and wires up the necessary
     * event listeners. 
     */
    private void init() {
        setName("New Mine Dialog");
        setTitle(Messages.getMessage("newmine.title"));
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        
        Container cp = getContentPane();
        GridBagConstraints cons = GridBagHelper.setup(cp);
        
        cp.add(new JLabel(Messages.getMessage("intermine.home")), cons);
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("mine.name")), cons);
        
        cons.gridx++;
        cons.gridy = 0;
        cons.weightx = 1.0;
        cp.add(homeField, cons);
        
        cons.gridy++;
        cp.add(mineNameField, cons);
        
        cons.gridx++;
        cons.gridy = 0;
        cons.weightx = 0.0;
        cp.add(new JButton(new BrowseAction()), cons);
        
        cons.gridx = 0;
        cons.gridy = 2;
        cons.weightx = 1;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cp.add(new ButtonPanel(getRootPane(), 0, okAction, new CancelAction()), cons);
        
        directoryChooser = new JFileChooser();
        directoryChooser.setMultiSelectionEnabled(false);
        FileFilter filter = new DirectoryFilter();
        directoryChooser.addChoosableFileFilter(filter);
        directoryChooser.setAcceptAllFileFilterUsed(false);
        directoryChooser.setFileFilter(filter);
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        File lastHome = MineManagerBackingStore.getInstance().getIntermineHome();
        if (lastHome != null) {
            intermineDir = lastHome;
            intermineDirCorrect = isDirectoryIntermine(lastHome);
            directoryChooser.setSelectedFile(lastHome);
            homeField.setText(lastHome.getAbsolutePath());
        }
        okAction.setEnabled(false);
        
        mineNameField.getDocument().addDocumentListener(new MineNameDocumentListener());
        addComponentListener(new MyComponentListener());
        
        pack();
    }
    
    /**
     * Set the database settings dialog that will follow this dialog in
     * the mine creation chain.
     * 
     * @param createDatabaseDialog The CreateDatabaseDialog.
     */
    public void setCreateDatabaseDialog(CreateDatabaseDialog createDatabaseDialog) {
        this.createDatabaseDialog = createDatabaseDialog;
    }
    
    /**
     * Check whether the given directory is an intermine home directory by testing
     * whether it contains a directory called "bio".
     * 
     * @param dir The directory to check.
     * 
     * @return <code>true</code> if <code>dir</code> is an Intermine top level directory,
     * <code>false</code> if not.
     */
    public boolean isDirectoryIntermine(File dir) {
        boolean isIntermine = false;
        if (dir != null) {
            File bioDir = new File(dir, "bio");
            isIntermine = bioDir.exists() && bioDir.isDirectory();
            // Could put more checks in here.
        }
        return isIntermine;
    }
    
    /**
     * Set the Intermine home directory. Checks are made to see if the directory
     * actually is an Intermine top level directory.
     * 
     * @param dir The directory to check.
     * 
     * @see #isDirectoryIntermine
     */
    public void setIntermineHome(File dir) {
        intermineDirCorrect = isDirectoryIntermine(dir);
        homeField.setText(dir.getAbsolutePath());
        intermineDir = dir;
        
        if (intermineDirCorrect) {
            MineManagerBackingStore.getInstance().setIntermineHome(dir);
        }
        
        updateState();
    }
    
    /**
     * Update the state of the "ok" action according to whether the fields in this
     * dialog are valid.
     */
    protected void updateState() {
        boolean ok = intermineDirCorrect && mineNameField.getText().length() >= 4;
        okAction.setEnabled(ok);
    }
    
    /**
     * JFileChooser filter to select only directories.
     */
    private static class DirectoryFilter extends FileFilter
    {
        /**
         * Check whether the given file is acceptable, ie. is a directory.
         * 
         * @param f The file to check.
         * 
         * @return <code>true</code> if <code>f</code> is a directory,
         * <code>false</code> if not.
         */
        @Override
        public boolean accept(File f) {
            return f.isDirectory();
        }

        /**
         * Get the description string for this filter.
         * @return The display text.
         */
        @Override
        public String getDescription() {
            return Messages.getMessage("filefilter.directory");
        }
    }
    
    /**
     * Action to open the file browser to select the Intermine home directory.
     */
    private class BrowseAction extends AbstractAction
    {
        private static final long serialVersionUID = -5577949107483722371L;

        /**
         * Constructor.
         */
        public BrowseAction() {
            super(Messages.getMessage("browse"));
        }

        /**
         * Called to open the directory chooser and set the Intermine home
         * directory if a file is selected.
         * 
         * @param event The action event.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            int choice = directoryChooser.showOpenDialog(NewMineDialog.this);
            if (choice == JFileChooser.APPROVE_OPTION) {
                setIntermineHome(directoryChooser.getSelectedFile());
            }
        }
    }
    
    /**
     * Action to move on to the next dialog in the creation process.
     */
    private class NextAction extends AbstractAction
    {
        private static final long serialVersionUID = -1147824114894879125L;

        /**
         * Constructor.
         */
        public NextAction() {
            super(Messages.getMessage("next"));
            setEnabled(false);
        }
        
        /**
         * Called to set the properties for creation from the fields in this dialog
         * and move on to the database set up dialog.
         * 
         * @param event The action event.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            String mineName = mineNameField.getText();
            File mineHome = new File(intermineDir, mineName.toLowerCase());
            
            Properties props = new Properties();
            props.put(InterminePropertyKeys.INTERMINE_HOME, intermineDir);
            props.put(InterminePropertyKeys.MINE_NAME, mineName);
            props.put(InterminePropertyKeys.MINE_HOME, mineHome);
            
            createDatabaseDialog.open(props, getLocation());
            setVisible(false);
            createDatabaseDialog.setVisible(true);
        }
    }
    
    /**
     * Document listener for the mine name field. Updates the state of the "next"
     * action if a suitable name is supplied.
     * 
     * @see NewMineDialog#updateState()
     */
    private class MineNameDocumentListener implements DocumentListener, Serializable
    {
        private static final long serialVersionUID = 4421300997132550195L;

        @Override
        public void changedUpdate(DocumentEvent e) {
            updateState();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            updateState();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            updateState();
        }
    }
    
    /**
     * Component listener to put the keyboard focus in the mine name field when
     * this dialog is shown.
     */
    private class MyComponentListener extends ComponentAdapter
    {
        /**
         * Request keyboard focus goes into the name field.
         * 
         * @param event The component event.
         */
        @Override
        public void componentShown(ComponentEvent event) {
            mineNameField.requestFocusInWindow();
        }
    }
}
