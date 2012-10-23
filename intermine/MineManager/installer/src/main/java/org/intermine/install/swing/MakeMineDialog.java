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

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.intermine.common.swing.ButtonPanel;
import org.intermine.common.swing.Messages;
import org.intermine.common.swing.SystemProcessProgressDialog;
import org.intermine.common.swing.SystemProcessSwingWorker;
import org.intermine.install.project.event.ProjectListener;
import org.intermine.install.project.event.ProjectListenerSupport;
import org.intermine.install.properties.InterminePropertyKeys;
import org.intermine.modelviewer.ProjectLoader;
import org.intermine.modelviewer.project.Project;


/**
 * Dialog for running the <code>make_mine</code> script to create the mine.
 */
public class MakeMineDialog extends SystemProcessProgressDialog
{
    private static final long serialVersionUID = -4590502617295996140L;

    /**
     * Action to run the <code>make_mine</code> script.
     * @serial
     */
    private Action createAction;
    
    /**
     * Action to move on without running mine creation.
     * @serial
     */
    private Action skipAction;
    
    /**
     * Support for firing <code>ProjectEvent</code>s.
     * @serial
     */
    private ProjectListenerSupport projectListenerSupport = new ProjectListenerSupport(this);
    
    /**
     * The mine creation properties so far.
     * @serial
     */
    private Properties previousProperties;
    
    
    /**
     * The Intermine home directory.
     */
    private transient File intermineHome;
    
    /**
     * The mine name.
     * @serial
     */
    private String mineName;
    
    /**
     * The mine's home directory.
     */
    private transient File mineHomeDir;
    
    /**
     * The mine's base <code>project.xml</code> file.
     */
    private transient File projectFile;

    
    /**
     * Initialise with a parent Dialog.
     * @param owner The parent Dialog.
     */
    public MakeMineDialog(Dialog owner) {
        super(owner);
        init();
    }

    /**
     * Initialise with a parent Frame.
     * @param owner The parent Frame.
     */
    public MakeMineDialog(Frame owner) {
        super(owner);
        init();
    }

    /**
     * Initialise with a parent Window.
     * @param owner The parent Window.
     */
    public MakeMineDialog(Window owner) {
        super(owner);
        init();
    }

    /**
     * Common initialisation: lays out the child components and wires up the necessary
     * event listeners. 
     */
    private void init() {
        setName("Make Mine Dialog");
        setTitle(Messages.getMessage("makemine.title"));
        
        informationLabel.setFont(informationLabel.getFont().deriveFont(Font.PLAIN));
        informationLabel.setText(Messages.getMessage("makemine.message", "An Intermine Mine"));
        
        errorAreaScrollPane.setVisible(false);
        
        setPreferredSize(new Dimension(600, 240));
        pack();
    }
    
    /**
     * Overrides <code>SystemProcessProgressDialog</code>'s implementation to provide
     * a button panel with the create, skip and cancel buttons.
     * 
     * @return The ButtonPanel.
     */
    @Override
    protected JComponent initButtons() {
        createAction = new CreateAction();
        skipAction = new SkipAction();
        return new ButtonPanel(getRootPane(), 1, createAction, skipAction, new CancelAction());
    }

    /**
     * Prepare this dialog for running the <code>make_mine</code> script.
     * <p>This method does not make the dialog visible.</p>
     *  
     * @param props The properties so far for the mine.
     * @param location The position on the screen to position this dialog.
     */
    public void open(Properties props, Point location) {
        
        previousProperties = props;

        mineName = previousProperties.getProperty(InterminePropertyKeys.MINE_NAME);
        assert mineName != null;
        
        mineHomeDir = (File) previousProperties.get(InterminePropertyKeys.MINE_HOME);
        assert mineHomeDir != null;
        
        intermineHome = (File) previousProperties.get(InterminePropertyKeys.INTERMINE_HOME);
        assert intermineHome != null;
        
        projectFile = new File(mineHomeDir, "project.xml");

        informationLabel.setText(Messages.getMessage("makemine.message", mineName));
        resetOutput();

        skipAction.setEnabled(projectFile.exists());
        
        setLocation(location);
    }
    
    /**
     * Load the project file into its in-memory object form.
     * 
     * @param close Whether to close this dialog after a successful load.
     * 
     * @return <code>true</code> if the project loaded ok, <code>false</code>
     * if not.
     * 
     * @see ProjectLoader#loadProject
     */
    protected boolean loadProject(boolean close) {
        assert projectFile != null : "projectFile is unset";
        
        boolean success = false;
        try {
            Project project = new ProjectLoader().loadProject(projectFile);
            
            success = true;
            if (close) {
                setVisible(false);
            }
            
            projectListenerSupport.fireProjectLoaded(project, projectFile);
        } catch (Exception e) {
            logger.error(Messages.getMessage("project.load.failed.message"), e);
            
            StringWriter swriter = new StringWriter();
            PrintWriter writer = new PrintWriter(swriter);
            writer.println(Messages.getMessage("project.load.failed.message"));
            e.printStackTrace(writer);
            writer.close();
            
            JOptionPane.showMessageDialog(this, swriter.toString(),
                                          Messages.getMessage("project.load.failed.title"),
                                          JOptionPane.ERROR_MESSAGE);
        }
        return success;
    }
    
    /**
     * Add a ProjectListener to this dialog.
     * @param l The ProjectListener.
     */
    public void addProjectListener(ProjectListener l) {
        projectListenerSupport.addProjectListener(l);
    }
    
    /**
     * Remove a ProjectListener from this dialog.
     * @param l The ProjectListener.
     */
    public void removeProjectListener(ProjectListener l) {
        projectListenerSupport.removeProjectListener(l);
    }
    
    
    /**
     * Action to move on from the make mine dialog without running the
     * <code>make_mine</code> script.
     */
    private class SkipAction extends AbstractAction
    {
        private static final long serialVersionUID = -1904143654574056901L;

        /**
         * Constructor.
         */
        public SkipAction() {
            super(Messages.getMessage("skip"));
        }

        /**
         * Called to load the project file and close the dialog on success.
         * 
         * @param event The action event.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            loadProject(true);
        }
    }
    
    /**
     * Action to execute the <code>make_mine</code> script.
     */
    private class CreateAction extends AbstractAction
    {
        private static final long serialVersionUID = -2665957342688998300L;

        /**
         * Constructor.
         */
        public CreateAction() {
            super(Messages.getMessage("createmine"));
        }

        /**
         * Called to start the <code>make_mine</code> script. If the mine's directory already
         * exists, the user is prompted for confirmation to recreate the mine.
         * 
         * @param event The action event.
         * 
         * @see CreateMineWorker
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            
            boolean proceed = true;
            if (mineHomeDir.exists()) {
                
                proceed = false;
                int choice =
                    JOptionPane.showConfirmDialog(
                            MakeMineDialog.this,
                            Messages.getMessage("makemine.exists.message", mineName),
                            Messages.getMessage("makemine.exists.title"),
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                
                switch (choice) {
                    case JOptionPane.NO_OPTION:
                        loadProject(true);
                        break;
                        
                    case JOptionPane.YES_OPTION:
                        proceed = true;
                        break;
                }
            }
            
            if (proceed) {
                CreateMineWorker worker = new CreateMineWorker(previousProperties);
                setWorker(worker);
                worker.addPropertyChangeListener(new CreateMineListener());
                setEnabled(false);
                worker.execute();
            }
        }
    }
    
    /**
     * Property change listener for the <code>CreateMineWorker</code> executing
     * <code>make_mine</code>.
     */
    private class CreateMineListener implements PropertyChangeListener
    {
        /**
         * Property change method. Listens for the completed change event, then
         * proceeds or displays errors according to the end state of the process.
         * 
         * @param event The property change event.
         */
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            
            if (SystemProcessSwingWorker.COMPLETE.equals(event.getPropertyName())) {
                
                boolean failed = true;
                
                CreateMineWorker worker = (CreateMineWorker) event.getSource();
                Throwable failure = worker.getHaltingException();
                if (failure == null) {
                    String innerMessage = null;
                    Integer exitCode = worker.getExitCode();
                    if (exitCode == null || exitCode.intValue() != 0) {
                        innerMessage = Messages.getMessage("makemine.failure.exitcode", exitCode);
                    } else {
                        // All should be ok, but check the directory & project.xml is there.
                        if (mineHomeDir.exists() && mineHomeDir.isDirectory()) {
                            if (projectFile.exists()) {
                                // Finally!
                                failed = false;
                                projectListenerSupport.fireProjectCreated(projectFile);
                            } else {
                                innerMessage =
                                    Messages.getMessage("makemine.failure.noprojectfile");
                            }
                        } else {
                            innerMessage = Messages.getMessage("makemine.failure.nodirectory");
                        }
                    }
                    
                    if (failed) {
                        String errorMessage =
                            Messages.getMessage("makemine.failure.message", innerMessage);
                        JOptionPane.showMessageDialog(MakeMineDialog.this,
                                                      errorMessage,
                                                      Messages.getMessage("makemine.failure.title"),
                                                      JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    if (worker.getFailureCode() == CreateMineWorker.FailureCode.CLEAN_FAILURE) {
                        logger.error("Mine clean failed:", failure);
                        
                        String errorMessage =
                            Messages.getMessage("makemine.failure.message", mineName,
                                                mineHomeDir.getAbsolutePath());
                        
                        JOptionPane.showMessageDialog(MakeMineDialog.this,
                                                      errorMessage,
                                                      Messages.getMessage("makemine.clean.title"),
                                                      JOptionPane.ERROR_MESSAGE);
                    } else {
                        showExceptionDialog(failure, "makemine.failure.title",
                                            "makemine.failure.message");
                    }
                }
                
                if (!failed) {
                    assert projectFile.exists() : "Project file does not exist";
                    
                    loadProject(true);
                }
                
                createAction.setEnabled(true);
            }
        }
    }
}
